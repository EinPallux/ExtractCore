package com.pallux.extractcore.core;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TextDisplay holograms above placed cores.
 * Uses native Paper 1.19.4+ TextDisplay entities — no external plugin required.
 *
 * Each core hologram consists of multiple stacked TextDisplay entities,
 * one per line. Lines are configured in core.yml under core.hologram.lines.
 *
 * Supported placeholders in line strings:
 *   {owner}          player name
 *   {stored}         formatted stored scrap
 *   {stored_raw}     raw stored scrap number
 *   {rate}           scrap per interval (formatted)
 *   {interval}       generation interval in seconds
 *   {speed_lvl}      speed upgrade level
 *   {prod_lvl}       production upgrade level
 *   {speed_max}      max speed level
 *   {prod_max}       max production level
 */
public class HologramManager {

    private final ExtractCore plugin;

    // ownerUUID -> list of spawned TextDisplay entities (one per line)
    private final Map<UUID, List<TextDisplay>> holograms = new ConcurrentHashMap<>();

    // Refresh task
    private BukkitTask refreshTask;

    // Line vertical spacing in blocks
    private static final double LINE_SPACING = 0.28;

    public HologramManager(ExtractCore plugin) {
        this.plugin = plugin;
        startRefreshTask();
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    public void spawnHologram(PlayerData data) {
        removeHologram(data.getUuid());

        Location coreLoc = plugin.getCoreManager().getCoreLocation(data);
        if (coreLoc == null) return;

        FileConfiguration cfg = plugin.getConfigManager().getCoreConfig();
        List<String> lineTemplates = cfg.getStringList("core.hologram.lines");
        if (lineTemplates.isEmpty()) return;

        double yOffset = cfg.getDouble("core.hologram.y-offset", 2.5);
        World world = coreLoc.getWorld();
        if (world == null) return;

        List<TextDisplay> entities = new ArrayList<>();
        int lineCount = lineTemplates.size();

        // Spawn top-to-bottom: first line at highest Y
        for (int i = 0; i < lineCount; i++) {
            double y = coreLoc.getY() + yOffset + (lineCount - 1 - i) * LINE_SPACING;
            Location spawnLoc = new Location(world,
                    coreLoc.getX() + 0.5,
                    y,
                    coreLoc.getZ() + 0.5);

            TextDisplay td = (TextDisplay) world.spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
            configureDisplay(td, cfg);
            td.text(buildLine(lineTemplates.get(i), data));
            entities.add(td);
        }

        holograms.put(data.getUuid(), entities);
    }

    private void configureDisplay(TextDisplay td, FileConfiguration cfg) {
        td.setInvisible(false);
        td.setPersistent(false);                  // never survives restart — we re-spawn on load
        td.setGravity(false);
        td.setInvulnerable(true);
        td.setSilent(true);

        // Background: semi-transparent dark panel or none
        boolean showBackground = cfg.getBoolean("core.hologram.background", false);
        if (showBackground) {
            td.setBackgroundColor(org.bukkit.Color.fromARGB(160, 0, 0, 0));
        } else {
            td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        }

        td.setAlignment(TextDisplay.TextAlignment.CENTER);
        td.setBillboard(Display.Billboard.CENTER); // always faces player
        td.setSeeThrough(false);

        // Scale
        float scale = (float) cfg.getDouble("core.hologram.scale", 1.0);
        td.setTransformationMatrix(new Matrix4f().scale(scale));

        // Shadow
        td.setShadowed(cfg.getBoolean("core.hologram.shadow", true));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updateHologram(UUID ownerUuid) {
        List<TextDisplay> lines = holograms.get(ownerUuid);
        if (lines == null || lines.isEmpty()) return;

        PlayerData data = plugin.getPlayerDataManager().get(ownerUuid);
        if (data == null || !data.isCorePlaced()) { removeHologram(ownerUuid); return; }

        FileConfiguration cfg = plugin.getConfigManager().getCoreConfig();
        List<String> lineTemplates = cfg.getStringList("core.hologram.lines");

        for (int i = 0; i < lines.size() && i < lineTemplates.size(); i++) {
            TextDisplay td = lines.get(i);
            if (!td.isValid()) {
                // Entity was removed — respawn the whole hologram
                spawnHologram(data);
                return;
            }
            td.text(buildLine(lineTemplates.get(i), data));
        }
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    public void removeHologram(UUID ownerUuid) {
        List<TextDisplay> lines = holograms.remove(ownerUuid);
        if (lines == null) return;
        for (TextDisplay td : lines) {
            if (td.isValid()) td.remove();
        }
    }

    public void removeAll() {
        for (UUID uuid : new HashSet<>(holograms.keySet())) {
            removeHologram(uuid);
        }
    }

    // ── Refresh task ──────────────────────────────────────────────────────────

    private void startRefreshTask() {
        if (refreshTask != null) refreshTask.cancel();

        int intervalTicks = plugin.getConfigManager().getCoreConfig()
                .getInt("core.hologram.refresh-ticks", 40); // default 2s

        refreshTask = new BukkitRunnable() {
            @Override public void run() {
                for (UUID uuid : new HashSet<>(holograms.keySet())) {
                    updateHologram(uuid);
                }
            }
        }.runTaskTimer(plugin, 20L, intervalTicks);
    }

    public void reload() {
        removeAll();
        startRefreshTask();
        // Re-spawn for all currently placed cores
        for (PlayerData data : plugin.getPlayerDataManager().getAll()) {
            if (data.isCorePlaced()) spawnHologram(data);
        }
    }

    public void shutdown() {
        if (refreshTask != null) refreshTask.cancel();
        removeAll();
    }

    // ── Line builder ──────────────────────────────────────────────────────────

    private Component buildLine(String template, PlayerData data) {
        FileConfiguration cfg = plugin.getConfigManager().getCoreConfig();
        int speedMax = cfg.getInt("core.upgrades.speed.max-level", 100);
        int prodMax  = cfg.getInt("core.upgrades.production.max-level", 100);

        long scrapRate = plugin.getCoreManager().getScrapPerInterval(data);
        long interval  = plugin.getCoreManager().getIntervalTicks(data);

        String line = template
                .replace("{owner}",      data.getName())
                .replace("{stored}",     ColorUtil.formatNumber(data.getCoreStoredScrap()))
                .replace("{stored_raw}", String.valueOf(data.getCoreStoredScrap()))
                .replace("{rate}",       ColorUtil.formatNumber(scrapRate))
                .replace("{interval}",   String.valueOf(interval))
                .replace("{speed_lvl}",  String.valueOf(data.getCoreSpeedLevel()))
                .replace("{prod_lvl}",   String.valueOf(data.getCoreProductionLevel()))
                .replace("{speed_max}",  String.valueOf(speedMax))
                .replace("{prod_max}",   String.valueOf(prodMax));

        // Run through ColorUtil (supports &#RRGGBB and &c legacy)
        String colored = ColorUtil.color(line);
        // Convert to Adventure component via legacy serializer
        return LegacyComponentSerializer.legacySection().deserialize(colored);
    }
}
