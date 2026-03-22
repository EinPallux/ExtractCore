package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.CoreGUI;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.worldguard.WorldGuardHook;
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

/**
 * Handles Core placement, right-click GUI opening, and raiding.
 *
 * WorldGuard integration:
 *   - Core PLACEMENT is blocked if the region has flag extract-core-place set to DENY.
 *   - Core PLACEMENT and core RAIDING always bypass WorldGuard's own block protection,
 *     because we cancel the vanilla event ourselves at HIGH priority (before WG's HIGHEST).
 */
public class CoreInteractListener implements Listener {

    private final ExtractCore plugin;

    public CoreInteractListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // ── Place Core ────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item.getType() != Material.BEACON) return;
        if (!plugin.getCoreManager().isCoreItem(item)) return;

        // ── WorldGuard: check custom extract-core-place flag ─────────────────
        // This lets admins deny Core placement in specific regions (e.g. spawn)
        // independently of the normal WG block-place protection.
        if (!WorldGuardHook.canPlaceCore(player, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getMessages()
                    .getString("core.placement-denied-region",
                            "&cYou cannot place your Core in this area.");
            player.sendMessage(ColorUtil.color(msg));
            return;
        }

        // We cancel the event so WorldGuard (HIGHEST) sees it as already handled
        // and skips its own block-place protection — the core is placed via CoreManager.
        event.setCancelled(true);

        // Manually place the beacon block (WG would have blocked it otherwise)
        event.getBlockPlaced().setType(Material.BEACON);

        plugin.getCoreManager().placeCore(player, event.getBlockPlaced().getLocation());

        // Consume the core item from hand
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (plugin.getCoreManager().isCoreItem(hand)) {
                if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
            }
        });
    }

    // ── Right-click placed Core → open GUI (owner) or inform (others) ────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        UUID owner = plugin.getCoreManager().getOwnerAtLocation(event.getClickedBlock().getLocation());
        if (owner == null) return; // not a plugin core — let vanilla handle it

        // Cancel the event so WG's HIGHEST handler skips its interact protection.
        event.setCancelled(true);

        if (owner.equals(player.getUniqueId())) {
            new CoreGUI(plugin, player).open();
        } else {
            player.sendMessage(new GuiUtil(plugin, "core-gui").str("not-your-core-right-click"));
        }
    }

    // ── Break Core (left-click / mining = raiding) ────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;

        Player breaker = event.getPlayer();
        UUID owner = plugin.getCoreManager().getOwnerAtLocation(event.getBlock().getLocation());
        if (owner == null) return; // not a plugin core

        // Cancel vanilla break (prevents drops, bypasses WG's HIGHEST handler).
        event.setCancelled(true);
        event.setDropItems(false);

        if (owner.equals(breaker.getUniqueId())) {
            // Owner tries to mine their own core → redirect to GUI
            breaker.sendMessage(new GuiUtil(plugin, "core-gui").str("redirect-to-gui"));
            return;
        }

        // ── Attacker raiding an enemy core ────────────────────────────────────
        Player ownerPlayer = plugin.getServer().getPlayer(owner);
        if (ownerPlayer != null) {
            plugin.getCoreManager().destroyCore(ownerPlayer, breaker);
        } else {
            // Owner offline — load data async then destroy on main thread
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