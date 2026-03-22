package com.pallux.extractcore.blocks;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * Tracks normal shop blocks placed in the world and removes them after a
 * configured number of hours.
 *
 * Persistence: plugins/ExtractCore/data/normal-blocks.yml
 *   Maps locKey → placedAtMillis
 *
 * A recurring task (every 60 s) scans the map and removes expired blocks.
 */
public class NormalBlockManager {

    private final ExtractCore plugin;
    private final File dataFile;

    /** locKey → System.currentTimeMillis() when placed */
    private final Map<String, Long> placedAt = new HashMap<>();

    private BukkitTask sweepTask;

    public NormalBlockManager(ExtractCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/normal-blocks.yml");
        dataFile.getParentFile().mkdirs();
        load();
        startSweep();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    public void registerBlock(Location loc) {
        placedAt.put(locKey(loc), System.currentTimeMillis());
        saveAsync();
    }

    public void unregisterBlock(Location loc) {
        placedAt.remove(locKey(loc));
        saveAsync();
    }

    public boolean isNormalShopBlock(Location loc) {
        return placedAt.containsKey(locKey(loc));
    }

    // ── Sweep ─────────────────────────────────────────────────────────────────

    private void startSweep() {
        if (sweepTask != null) sweepTask.cancel();
        long intervalTicks = 20L * 60; // every 60 seconds
        sweepTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::sweep, intervalTicks, intervalTicks);
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        long lifetimeMs = getDespawnMs();

        Iterator<Map.Entry<String, Long>> it = placedAt.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() >= lifetimeMs) {
                Location loc = parseLocKey(entry.getKey());
                if (loc != null && isShopBlockMaterial(loc.getBlock().getType())) {
                    loc.getBlock().setType(Material.AIR);
                }
                it.remove();
                changed = true;
            }
        }
        if (changed) saveAsync();
    }

    private long getDespawnMs() {
        double hours = plugin.getConfigManager().getShopConfig()
                .getDouble("settings.normal-block-despawn-hours", 2.0);
        return (long)(hours * 3600 * 1000);
    }

    /**
     * Checks whether a block material is one sold in the normal shop,
     * so we don't accidentally remove naturally-placed vanilla blocks.
     */
    private boolean isShopBlockMaterial(Material mat) {
        return plugin.getShopManager().getNormalBlocks().stream()
                .anyMatch(e -> {
                    try { return Material.valueOf(e.material()) == mat; }
                    catch (Exception ex) { return false; }
                });
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yaml.getKeys(false)) {
            placedAt.put(key, yaml.getLong(key, 0));
        }
        plugin.getLogger().info("[ExtractCore] Loaded " + placedAt.size() + " normal shop block(s) from disk.");
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Long> e : placedAt.entrySet()) {
            yaml.set(e.getKey(), e.getValue());
        }
        try { yaml.save(dataFile); }
        catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save normal block data", e);
        }
    }

    public void shutdown() {
        if (sweepTask != null) sweepTask.cancel();
        save();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String locKey(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    private Location parseLocKey(String key) {
        try {
            String[] parts = key.split(":");
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }
}