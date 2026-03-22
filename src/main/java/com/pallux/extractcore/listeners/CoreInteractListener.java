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
 * WorldGuard bypass strategy:
 *   We run at HIGHEST priority (same as WG) with ignoreCancelled = false,
 *   so we see events WG already cancelled and can un-cancel them for plugin cores.
 *
 *   For placement: we un-cancel what WG cancelled, letting vanilla keep the
 *   block in the world. We never call setType() during the event — doing so
 *   on a cancelled BlockPlaceEvent causes Paper 1.21 to revert it next tick.
 *
 *   For breaking (raiding): same — un-cancel, remove block ourselves, handle
 *   raid logic, and suppress vanilla drops.
 */
public class CoreInteractListener implements Listener {

    private final ExtractCore plugin;

    public CoreInteractListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // ── Place Core ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item.getType() != Material.BEACON) return;
        if (!plugin.getCoreManager().isCoreItem(item)) return;

        // ── Custom flag: block placement in spawn/safe regions ───────────────
        if (!WorldGuardHook.canPlaceCore(player, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getMessages()
                    .getString("core.placement-denied-region",
                            "&cYou cannot place your Core in this area.");
            player.sendMessage(ColorUtil.color(msg));
            return;
        }

        // ── WG bypass: un-cancel whatever WG may have cancelled ──────────────
        // Vanilla already placed the block before this event fired.
        // Un-cancelling lets Paper keep the placed block in the world.
        event.setCancelled(false);

        // Register with CoreManager (hologram, generation task, etc.)
        plugin.getCoreManager().placeCore(player, event.getBlockPlaced().getLocation());

        // Remove the core item from the player's hand on the next tick
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        UUID owner = plugin.getCoreManager().getOwnerAtLocation(event.getClickedBlock().getLocation());
        if (owner == null) return; // not a plugin core — let vanilla handle it

        // Always cancel interact on plugin cores to prevent vanilla beacon UI
        event.setCancelled(true);

        if (owner.equals(player.getUniqueId())) {
            new CoreGUI(plugin, player).open();
        } else {
            player.sendMessage(new GuiUtil(plugin, "core-gui").str("not-your-core-right-click"));
        }
    }

    // ── Break Core (left-click / mining = raiding) ────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;

        Player breaker = event.getPlayer();
        UUID owner = plugin.getCoreManager().getOwnerAtLocation(event.getBlock().getLocation());
        if (owner == null) return; // not a plugin core — let vanilla handle it

        if (owner.equals(breaker.getUniqueId())) {
            // Owner tries to mine their own core → redirect to GUI pickup
            event.setCancelled(true);
            breaker.sendMessage(new GuiUtil(plugin, "core-gui").str("redirect-to-gui"));
            return;
        }

        // ── Attacker raiding an enemy core ────────────────────────────────────
        // Un-cancel WG's protection, suppress drops, remove block ourselves.
        event.setCancelled(false);
        event.setDropItems(false);
        event.getBlock().setType(Material.AIR);

        Player ownerPlayer = plugin.getServer().getPlayer(owner);
        if (ownerPlayer != null) {
            plugin.getCoreManager().destroyCore(ownerPlayer, breaker);
        } else {
            // Owner offline — load data async, destroy on main thread
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                PlayerData ownerData = plugin.getPlayerDataManager().get(owner);
                if (ownerData == null) return;
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getCoreManager().destroyCoreOfflineOwner(owner, breaker));
            });
        }
    }
}