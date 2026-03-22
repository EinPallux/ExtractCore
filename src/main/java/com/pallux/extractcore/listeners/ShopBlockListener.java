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
 * Handles placement and breaking of shop blocks.
 *
 * WorldGuard bypass:
 *   Both normal shop blocks and defense blocks bypass WG's block-place and
 *   block-break protection (so players can fortify their arena regions).
 *   However, placement is still blocked in regions with extract-core-place=DENY
 *   (the same spawn-protection flag used for cores).
 */
public class ShopBlockListener implements Listener {

    private final ExtractCore plugin;

    public ShopBlockListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // ── Place ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        boolean isNormal  = plugin.getShopManager().isNormalShopBlock(item);
        boolean isDefense = plugin.getShopManager().isDefenseBlock(item);

        if (!isNormal && !isDefense) return;

        // ── Spawn-protection check (extract-core-place flag) ─────────────────
        if (!WorldGuardHook.canPlaceCore(player, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getMessages()
                    .getString("shop.placement-denied-region",
                            "&cYou cannot place blocks in this protected area.");
            player.sendMessage(ColorUtil.color(msg));
            return;
        }

        // ── WG bypass: cancel the vanilla event, place block ourselves ────────
        // This prevents WG's HIGHEST handler from blocking placement in arena regions.
        event.setCancelled(true);
        Material mat;
        try { mat = Material.valueOf(event.getItemInHand().getType().name()); }
        catch (Exception e) { mat = event.getItemInHand().getType(); }
        event.getBlockPlaced().setType(mat);

        if (isNormal) {
            String entryId = plugin.getShopManager().getNormalBlockId(item);
            plugin.getNormalBlockManager().registerBlock(event.getBlockPlaced().getLocation());

        } else {
            // Defense block
            String entryId = plugin.getShopManager().getDefenseBlockId(item);
            ShopEntry entry = plugin.getShopManager().getDefenseEntry(entryId);
            if (entry == null) return;
            plugin.getDefenseBlockManager().registerBlock(
                    event.getBlockPlaced().getLocation(), entryId, entry.hp());
        }
    }

    // ── Break ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // ── Defense block hit ─────────────────────────────────────────────────
        if (plugin.getDefenseBlockManager().isDefenseBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.setDropItems(false);
            plugin.getDefenseBlockManager().hit(player, event.getBlock().getLocation());
            return;
        }

        // ── Normal shop block broken by a player ──────────────────────────────
        if (plugin.getNormalBlockManager().isNormalShopBlock(event.getBlock().getLocation())) {
            // Let vanilla break proceed but unregister from tracking
            // (WG bypass: cancel + re-place = not needed here since players breaking
            // their own blocks in protected regions is handled by WG's member check.
            // If needed, you can add the same cancel+setType(AIR) pattern below.)
            plugin.getNormalBlockManager().unregisterBlock(event.getBlock().getLocation());
            // Don't cancel — allow vanilla block break and drops to happen normally
        }
    }
}