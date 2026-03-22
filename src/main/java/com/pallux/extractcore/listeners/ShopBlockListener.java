package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.shop.ShopEntry;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.worldguard.WorldGuardHook;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles placement and breaking of shop blocks (normal + defense).
 *
 * WorldGuard bypass strategy — same pattern as CoreInteractListener:
 *   Run at HIGHEST with ignoreCancelled=false so we see events WG already
 *   cancelled. For our tracked blocks we un-cancel them, letting vanilla
 *   keep the placed/broken block. We never call setType() during a place
 *   event — Paper 1.21 reverts that on the next tick for cancelled events.
 *
 * Placement rules:
 *   - Blocked if the region has extract-core-place=DENY (spawn/safe zones).
 *   - Allowed everywhere else, including WG-protected arena regions.
 *   - Only blocks sold via /shop are affected — vanilla blocks are untouched,
 *     so players cannot grief non-shop blocks in the arena.
 *
 * Break rules:
 *   - Defense blocks: always intercepted (HP system), no drops, WG bypassed.
 *   - Normal shop blocks: WG bypassed so the player can remove their own
 *     placed blocks; tracked location is unregistered; vanilla drops proceed.
 *   - ALL other blocks: this listener does nothing — WG protection stays
 *     in place and players cannot break arena terrain.
 */
public class ShopBlockListener implements Listener {

    private final ExtractCore plugin;

    public ShopBlockListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // ── Place ─────────────────────────────────────────────────────────────────

    /**
     * HIGHEST + ignoreCancelled=false: we run after WorldGuard and can
     * un-cancel events WG blocked.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player    player = event.getPlayer();
        ItemStack item   = event.getItemInHand();

        boolean isNormal  = plugin.getShopManager().isNormalShopBlock(item);
        boolean isDefense = plugin.getShopManager().isDefenseBlock(item);

        // Not a shop block — leave WG's decision untouched
        if (!isNormal && !isDefense) return;

        // Custom flag: block placement in spawn/safe regions regardless of WG state
        if (!WorldGuardHook.canPlaceCore(player, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getMessages()
                    .getString("shop.placement-denied-region",
                            "&cYou cannot place blocks in this protected area.");
            player.sendMessage(ColorUtil.color(msg));
            return;
        }

        // WG bypass: un-cancel whatever WG set. Vanilla already placed the block
        // before the event fired — un-cancelling tells Paper to keep it.
        // We never call setType() here; that triggers Paper 1.21's block revert.
        event.setCancelled(false);

        // Register the placed block
        if (isNormal) {
            plugin.getNormalBlockManager().registerBlock(event.getBlockPlaced().getLocation());
        } else {
            String    entryId = plugin.getShopManager().getDefenseBlockId(item);
            ShopEntry entry   = plugin.getShopManager().getDefenseEntry(entryId);
            if (entry == null) return;
            plugin.getDefenseBlockManager().registerBlock(
                    event.getBlockPlaced().getLocation(), entryId, entry.hp());
        }
    }

    // ── Break ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // ── Defense block: intercept every hit, apply HP logic ────────────────
        if (plugin.getDefenseBlockManager().isDefenseBlock(event.getBlock().getLocation())) {
            // Un-cancel WG's protection, then take full control
            event.setCancelled(false);
            event.setDropItems(false);
            // Cancel the vanilla break so the block stays until HP=0
            event.setCancelled(true);
            plugin.getDefenseBlockManager().hit(player, event.getBlock().getLocation());
            return;
        }

        // ── Normal shop block: remove silently, no drops ─────────────────────
        if (plugin.getNormalBlockManager().isNormalShopBlock(event.getBlock().getLocation())) {
            event.setCancelled(false);
            event.setDropItems(false);
            plugin.getNormalBlockManager().unregisterBlock(event.getBlock().getLocation());
            return;
        }

        // ── All other blocks: do nothing ──────────────────────────────────────
        // WG's cancellation remains intact — players cannot break arena terrain.
    }
}