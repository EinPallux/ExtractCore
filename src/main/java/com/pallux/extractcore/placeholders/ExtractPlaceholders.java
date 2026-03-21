package com.pallux.extractcore.placeholders;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for ExtractCore.
 *
 * Registered identifiers (all prefixed %ex_):
 *
 * MATERIALS:
 *   %ex_scrap%                  raw scrap amount
 *   %ex_scrap_formatted%        formatted (7.4K, 1.2M …)
 *   %ex_screws%
 *   %ex_screws_formatted%
 *   %ex_energycells%
 *   %ex_energycells_formatted%
 *   %ex_biosamples%
 *   %ex_biosamples_formatted%
 *   %ex_techshards%
 *   %ex_techshards_formatted%
 *
 * CORE:
 *   %ex_scrap_multi%            raw scrap produced per interval
 *   %ex_scrap_multi_formatted%  formatted
 *   %ex_core_speed%             speed upgrade level
 *   %ex_core_production%        production upgrade level
 *   %ex_core_placed%            true/false
 *   %ex_core_x%  %ex_core_y%  %ex_core_z%
 *
 * LEVELING:
 *   %ex_level%                  colored level string
 *   %ex_level_raw%              raw integer
 *   %ex_xp%                     % progress toward next level  (e.g. "80%")
 *   %ex_xp_raw%                 raw XP value
 *
 * STATS:
 *   %ex_kills%  %ex_deaths%  %ex_kd%
 *   %ex_extractions%
 *   %ex_cores_destroyed%
 *   %ex_playtime%               formatted "Xh Ym"
 *
 * EXTRACTION:
 *   %ex_next_extraction%        time until next extraction (formatted)
 *   %ex_current_extraction%     time left in active extraction (or "Inactive")
 */
public class ExtractPlaceholders extends PlaceholderExpansion {

    private final ExtractCore plugin;

    public ExtractPlaceholders(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "ex"; }
    @Override public @NotNull String getAuthor()     { return "Pallux"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }
    @Override public boolean canRegister()           { return true; }

    @Override
    public String onRequest(OfflinePlayer offline, @NotNull String params) {
        if (offline == null) return "";

        // ── Extraction timers (no player data needed) ──────────────────────
        var em = plugin.getExtractionManager();
        if (params.equals("next_extraction")) {
            return em.isExtractionActive() ? "Active" :
                    ColorUtil.formatDuration(em.getCountdownSeconds());
        }
        if (params.equals("current_extraction")) {
            return em.isExtractionActive() ?
                    ColorUtil.formatDuration(em.getActiveSecondsLeft()) : "Inactive";
        }

        // ── Player-specific data ───────────────────────────────────────────
        PlayerData data = plugin.getPlayerDataManager().get(offline.getUniqueId());
        if (data == null) return "";

        var lm = plugin.getLevelManager();
        var cm = plugin.getCoreManager();

        return switch (params) {
            // Materials - raw
            case "scrap"               -> String.valueOf(data.getScrap());
            case "screws"              -> String.valueOf(data.getScrews());
            case "energycells"         -> String.valueOf(data.getEnergyCells());
            case "biosamples"          -> String.valueOf(data.getBioSamples());
            case "techshards"          -> String.valueOf(data.getTechShards());

            // Materials - formatted
            case "scrap_formatted"       -> ColorUtil.formatNumber(data.getScrap());
            case "screws_formatted"      -> ColorUtil.formatNumber(data.getScrews());
            case "energycells_formatted" -> ColorUtil.formatNumber(data.getEnergyCells());
            case "biosamples_formatted"  -> ColorUtil.formatNumber(data.getBioSamples());
            case "techshards_formatted"  -> ColorUtil.formatNumber(data.getTechShards());

            // Core multiplier
            case "scrap_multi"           -> String.valueOf(cm.getScrapPerInterval(data));
            case "scrap_multi_formatted" -> ColorUtil.formatNumber(cm.getScrapPerInterval(data));

            // Core info
            case "core_speed"      -> String.valueOf(data.getCoreSpeedLevel());
            case "core_production" -> String.valueOf(data.getCoreProductionLevel());
            case "core_placed"     -> String.valueOf(data.isCorePlaced());
            case "core_x"          -> String.valueOf(data.getCoreX());
            case "core_y"          -> String.valueOf(data.getCoreY());
            case "core_z"          -> String.valueOf(data.getCoreZ());

            // Leveling
            case "level"     -> lm.getLevelColored(data.getLevel());
            case "level_raw" -> String.valueOf(data.getLevel());
            case "xp"        -> String.format("%.1f", lm.getProgressPercent(data)) + "%";
            case "xp_raw"    -> String.valueOf(data.getXp());

            // Stats
            case "kills"          -> String.valueOf(data.getKills());
            case "deaths"         -> String.valueOf(data.getDeaths());
            case "kd"             -> String.format("%.2f", data.getKD());
            case "extractions"    -> String.valueOf(data.getExtractions());
            case "cores_destroyed"-> String.valueOf(data.getCoresDestroyed());
            case "playtime"       -> {
                long h = data.getPlaytimeMinutes() / 60;
                long m = data.getPlaytimeMinutes() % 60;
                yield h + "h " + m + "m";
            }

            default -> null;
        };
    }
}
