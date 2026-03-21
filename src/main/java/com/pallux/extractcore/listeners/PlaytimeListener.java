package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class PlaytimeListener implements Listener {

    private final ExtractCore plugin;

    public PlaytimeListener(ExtractCore plugin) {
        this.plugin = plugin;

        // Every 60 seconds: increment playtime, award passive XP
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long xpPerMin = plugin.getConfigManager().getLevelsConfig()
                    .getLong("xp-sources.playtime-per-minute", 2);

            for (PlayerData data : plugin.getPlayerDataManager().getAll()) {
                data.addPlaytimeMinutes(1);
                data.addXp(xpPerMin);

                // Level-up check must happen on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var player = Bukkit.getPlayer(data.getUuid());
                    if (player == null) return;

                    int maxLevel = plugin.getConfigManager().getLevelsConfig().getInt("max-level", 5000);
                    while (data.getLevel() < maxLevel
                            && data.getXp() >= plugin.getLevelManager().xpRequired(data.getLevel())) {
                        data.setXp(data.getXp() - plugin.getLevelManager().xpRequired(data.getLevel()));
                        data.setLevel(data.getLevel() + 1);
                        // Level-up notification handled inside LevelManager for explicit sources;
                        // silent increment here to avoid spam
                    }
                    plugin.getMilestoneManager().check(player, data);
                });
            }
        }, 1200L, 1200L); // 60 seconds
    }
}
