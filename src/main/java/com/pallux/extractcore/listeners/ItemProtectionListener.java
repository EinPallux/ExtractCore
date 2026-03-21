package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class ItemProtectionListener implements Listener {

    private final ExtractCore plugin;

    public ItemProtectionListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // Prevent dropping extract items
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getArmoryManager().isExtractItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // Prevent moving armor out of armor slots, and protect tools
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // Ignore our plugin GUIs (handled by GUIListener)
        if (event.getInventory().getType() == InventoryType.CHEST
                || event.getInventory().getType() == InventoryType.HOPPER) return;

        ItemStack cursor  = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Disallow moving extract items out of armor slots
        if (isArmorSlot(event.getRawSlot()) && isExtract(current)) {
            event.setCancelled(true);
            return;
        }

        // Disallow placing non-extract items into armor slots if armor is extract item
        if (isArmorSlot(event.getRawSlot()) && isExtract(cursor)) {
            // Allow only if upgrading (handled by armory)
            event.setCancelled(true);
            return;
        }

        // Prevent shift-click moving armor out
        if (event.isShiftClick() && isExtract(current) && isArmorSlot(event.getRawSlot())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (isExtract(event.getMainHandItem()) || isExtract(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isExtract(ItemStack item) {
        return plugin.getArmoryManager().isExtractItem(item);
    }

    private boolean isArmorSlot(int rawSlot) {
        // Player inventory armor slots: 5 (helmet), 6 (chest), 7 (legs), 8 (boots)
        return rawSlot >= 5 && rawSlot <= 8;
    }
}
