package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.CoreGUI;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.GuiUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CoreInteractListener implements Listener {

    private final ExtractCore plugin;

    public CoreInteractListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // ── Place Core ────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item.getType() != Material.BEACON) return;
        if (!plugin.getCoreManager().isCoreItem(item)) return;

        plugin.getCoreManager().placeCore(player, event.getBlockPlaced().getLocation());

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (plugin.getCoreManager().isCoreItem(hand)) {
                if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
            }
        });
    }

    // ── Right-click placed Core → open GUI (owner) or deny (others) ──
    // NOTE: we ONLY handle RIGHT_CLICK_BLOCK here.
    // Left-click (mining/raiding) must NOT be intercepted — it falls through
    // to onBlockBreak so the attacker can destroy enemy cores.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // Only RIGHT-click, only main hand
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        UUID owner = plugin.getCoreManager().getOwnerAtLocation(event.getClickedBlock().getLocation());
        if (owner == null) return;  // not a plugin core — let vanilla handle it

        event.setCancelled(true);   // prevent vanilla beacon GUI for both owner and non-owner

        if (owner.equals(player.getUniqueId())) {
            // Owner → open Core management GUI
            new CoreGUI(plugin, player).open();
        } else {
            // Non-owner right-click → inform them they need to mine it to raid
            player.sendMessage(new GuiUtil(plugin, "core-gui").str("not-your-core-right-click"));
        }
    }

    // ── Break Core (left-click mining) ────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;

        Player breaker = event.getPlayer();
        UUID owner = plugin.getCoreManager().getOwnerAtLocation(event.getBlock().getLocation());
        if (owner == null) return;  // not a plugin core — let vanilla handle it

        // Always cancel the vanilla break (no drops, we handle everything)
        event.setCancelled(true);
        event.setDropItems(false);

        if (owner.equals(breaker.getUniqueId())) {
            // Owner tries to mine their own core → redirect to GUI pickup button
            breaker.sendMessage(new GuiUtil(plugin, "core-gui").str("redirect-to-gui"));
            return;
        }

        // ── Attacker raiding an enemy core ────────────────────────
        Player ownerPlayer = plugin.getServer().getPlayer(owner);
        if (ownerPlayer != null) {
            // Owner is online
            plugin.getCoreManager().destroyCore(ownerPlayer, breaker);
        } else {
            // Owner is offline — load data async then destroy on main thread
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                PlayerData ownerData = plugin.getPlayerDataManager().get(owner);
                if (ownerData == null) return;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    event.getBlock().setType(Material.AIR);
                    plugin.getCoreManager().destroyCoreOfflineOwner(owner, breaker);
                });
            });
        }
    }
}
