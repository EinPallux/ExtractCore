package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final ExtractCore plugin;

    public PlayerMoveListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only trigger on actual block movement (not just head rotation)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
         && event.getFrom().getBlockY() == event.getTo().getBlockY()
         && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        if (plugin.getCoreManager().isPickingUp(player.getUniqueId())) {
            plugin.getCoreManager().cancelPickup(player);
        }
    }
}
