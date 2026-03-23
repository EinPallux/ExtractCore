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
 *
 * Runs at HIGHEST with ignoreCancelled=false so it fires even if
 * ItemProtectionListener or CoreInteractListener already cancelled the event.
 */
public class ArmoryShortcutListener implements Listener {

    private final ExtractCore plugin;

    public ArmoryShortcutListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        // Only main hand
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Only right-click actions
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Only while sneaking
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        // Must have an item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        ArmoryManager am = plugin.getArmoryManager();

        // Check if the held item is one of the three tool types
        boolean isTool =
                hasKey(item, am.getKey("sword"))   ||
                        hasKey(item, am.getKey("pickaxe")) ||
                        hasKey(item, am.getKey("axe"));

        if (!isTool) return;

        // Consume the event fully and open the GUI
        event.setCancelled(true);

        // Open on next tick to avoid inventory glitches from the interact event
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                new ArmoryGUI(plugin, player).open();
            }
        });
    }

    private boolean hasKey(ItemStack item, org.bukkit.NamespacedKey key) {
        if (key == null) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.INTEGER);
    }
}