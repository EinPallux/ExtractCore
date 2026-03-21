package com.pallux.extractcore.leveling;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Handles XP gain, level calculation, level-up rewards and the level color system.
 */
public class LevelManager {

    private final ExtractCore plugin;
    private PlayerPointsAPI pointsAPI;

    public LevelManager(ExtractCore plugin) {
        this.plugin = plugin;

        // Hook PlayerPoints
        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            pointsAPI = PlayerPoints.getInstance().getAPI();
            plugin.getLogger().info("[ExtractCore] PlayerPoints hooked.");
        }
    }

    // ── XP & Leveling ─────────────────────────────────────────────────────────

    public void addXp(Player player, String source) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        FileConfiguration cfg = plugin.getConfigManager().getLevelsConfig();
        long xpGain = cfg.getLong("xp-sources." + source, 0);
        if (xpGain <= 0) return;

        data.addXp(xpGain);

        // Check for level ups
        int maxLevel = cfg.getInt("max-level", 5000);
        while (data.getLevel() < maxLevel && data.getXp() >= xpRequired(data.getLevel())) {
            data.setXp(data.getXp() - xpRequired(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
            onLevelUp(player, data);
        }

        // Cap at max
        if (data.getLevel() >= maxLevel) data.setXp(0);

        plugin.getPlayerDataManager().saveAsync(data);

        // Notify XP gain
        String msg = plugin.getConfigManager().getMessages()
                .getString("level.xp-gained", "&#5B8DD9+{xp} XP &8({source})")
                .replace("{xp}", String.valueOf(xpGain))
                .replace("{source}", formatSource(source));
        player.sendMessage(ColorUtil.color(msg));
    }

    private void onLevelUp(Player player, PlayerData data) {
        FileConfiguration cfg = plugin.getConfigManager().getLevelsConfig();
        int level = data.getLevel();

        // Material rewards
        long scrap = cfg.getLong("level-up-materials.scrap", 100);
        data.addScrap(scrap);

        StringBuilder matMsg = new StringBuilder();
        matMsg.append("&e").append(ColorUtil.formatNumber(scrap)).append(" Scrap");

        if (level >= cfg.getInt("level-up-materials.screws-threshold", 10)) {
            long s = cfg.getLong("level-up-materials.screws-amount", 2);
            data.addScrews(s); matMsg.append(" &8| &b+").append(s).append(" Screws");
        }
        if (level >= cfg.getInt("level-up-materials.energy-threshold", 50)) {
            long e = cfg.getLong("level-up-materials.energy-amount", 1);
            data.addEnergyCells(e); matMsg.append(" &8| &a+").append(e).append(" Energy");
        }
        if (level >= cfg.getInt("level-up-materials.bio-threshold", 100)) {
            long b = cfg.getLong("level-up-materials.bio-amount", 1);
            data.addBioSamples(b); matMsg.append(" &8| &d+").append(b).append(" Bio");
        }
        if (level >= cfg.getInt("level-up-materials.tech-threshold", 200)) {
            long t = cfg.getLong("level-up-materials.tech-amount", 1);
            data.addTechShards(t); matMsg.append(" &8| &6+").append(t).append(" Tech");
        }

        // PlayerPoints
        if (pointsAPI != null && plugin.getConfig().getBoolean("playerpoints.enabled", true)) {
            int points = plugin.getConfig().getInt("playerpoints.points-per-level", 5);
            if (level % plugin.getConfig().getInt("playerpoints.milestone-bonus-every", 100) == 0) {
                points += plugin.getConfig().getInt("playerpoints.milestone-bonus-points", 50);
            }
            int finalPoints = points;
            Bukkit.getScheduler().runTask(plugin, () ->
                pointsAPI.give(player.getUniqueId(), finalPoints));

            // Level-up message
            String msg = plugin.getConfigManager().getMessages()
                    .getString("level.level-up", "&#FFD700&l✦ LEVEL UP! Level {level}")
                    .replace("{level}", getLevelColored(level))
                    .replace("{points}", String.valueOf(points))
                    .replace("{materials}", matMsg.toString());
            player.sendMessage(ColorUtil.color(msg));
        } else {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("level.level-up", "")
                    .replace("{level}", getLevelColored(level))
                    .replace("{points}", "0")
                    .replace("{materials}", matMsg.toString());
            player.sendMessage(ColorUtil.color(msg));
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);

        // Check milestones
        plugin.getMilestoneManager().check(player, data);
    }

    // ── Level Color ───────────────────────────────────────────────────────────

    /**
     * Returns the level number with its color stage applied.
     */
    public String getLevelColored(int level) {
        FileConfiguration cfg = plugin.getConfigManager().getLevelsConfig();
        ConfigurationSection stages = cfg.getConfigurationSection("color-stages");

        String color = "&7";
        if (stages != null) {
            int lastMatch = 0;
            for (String key : stages.getKeys(false)) {
                try {
                    int threshold = Integer.parseInt(key);
                    if (level >= threshold && threshold >= lastMatch) {
                        color = stages.getString(key, "&7");
                        lastMatch = threshold;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        // Special level 5000
        if (level >= 5000) return "&#FFD700&l" + level;
        return color + level;
    }

    // ── XP Calculation ────────────────────────────────────────────────────────

    /**
     * XP required to level up from the given level.
     * Formula: base * growth^(level-1)
     */
    public long xpRequired(int level) {
        FileConfiguration cfg = plugin.getConfigManager().getLevelsConfig();
        double base   = cfg.getDouble("base-xp", 1000);
        double growth = cfg.getDouble("growth-factor", 1.085);
        return (long) (base * Math.pow(growth, level - 1));
    }

    /**
     * Returns the percentage progress to next level (0-100).
     */
    public double getProgressPercent(PlayerData data) {
        long needed = xpRequired(data.getLevel());
        if (needed == 0) return 100.0;
        return (data.getXp() * 100.0) / needed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatSource(String src) {
        return switch (src) {
            case "player-kill"            -> "Player Kill";
            case "extraction-participate" -> "Extraction";
            case "core-destroy"           -> "Core Destroyed";
            default                       -> src;
        };
    }

    private org.bukkit.configuration.ConfigurationSection cfg() {
        return plugin.getConfigManager().getLevelsConfig()
                .getConfigurationSection("color-stages");
    }
}
