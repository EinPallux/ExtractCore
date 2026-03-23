package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.armory.ArmoryManager;
import com.pallux.extractcore.gui.ArmoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Opens the Armory GUI when a player shift-right-clicks while holding
 * one of their extract sword, pickaxe or axe items.
 */
public class ArmoryShortcutListener implements Listener {

    private final ExtractCore plugin;

    public ArmoryShortcutListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // Only main hand, only right-click (air or block), only while sneaking
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ArmoryManager am = plugin.getArmoryManager();

        // Check if it's one of the three tool types
        boolean isTool =
                hasKey(item, am.getKey("sword"))   ||
                        hasKey(item, am.getKey("pickaxe")) ||
                        hasKey(item, am.getKey("axe"));

        if (!isTool) return;

        event.setCancelled(true);
        new ArmoryGUI(plugin, player).open();
    }

    private boolean hasKey(ItemStack item, org.bukkit.NamespacedKey key) {
        return item.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.INTEGER);
    }
}