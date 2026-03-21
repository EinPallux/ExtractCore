package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {

    private final ExtractCore plugin;

    public PlayerJoinListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getPlayerDataManager().load(player);
            data.setName(player.getName());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.setGameMode(GameMode.SURVIVAL);
                plugin.getArmoryManager().integrityCheck(player);
                plugin.getExtractionManager().addPlayerToBossBar(player);

                if (!player.hasPlayedBefore()) {
                    // Pull first-join message lines from guis.yml
                    List<String> lines = plugin.getConfigManager().getGuiConfig()
                        .getStringList("join.first-join-message");
                    for (String line : lines) {
                        player.sendMessage(ColorUtil.color(line));
                    }
                }
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data != null) {
            data.setLastSeenTime(System.currentTimeMillis());
            plugin.getPlayerDataManager().saveAsync(data);
        }
        plugin.getCoreManager().cancelPickup(player);
        plugin.getExtractionManager().removePlayerFromBossBar(player);
        GUIListener.unregisterGUI(player);
        plugin.getPlayerDataManager().unload(player.getUniqueId());
    }
}
