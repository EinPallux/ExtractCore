package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.*;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {

    private final ExtractCore plugin;
    // Track which GUI a player currently has open
    private static final Map<UUID, BaseGUI> openGUIs = new ConcurrentHashMap<>();

    public GUIListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    public static void registerGUI(Player player, BaseGUI gui) {
        openGUIs.put(player.getUniqueId(), gui);
    }

    public static void unregisterGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        if (gui == null) return;

        // Only cancel if clicking inside our GUI
        if (!event.getInventory().equals(gui.getInventory())) return;
        gui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        if (gui == null) return;
        // Cancel all drags in plugin GUIs
        for (int slot : event.getRawSlots()) {
            if (slot < gui.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
