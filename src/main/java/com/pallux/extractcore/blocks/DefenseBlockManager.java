package com.pallux.extractcore.blocks;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.shop.ShopEntry;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages Defense Block HP.
 *
 * Each break event on a registered defense block counts as one "hit".
 * When HP reaches 0 the block is removed. HP is persisted to disk so it
 * survives server restarts.
 *
 * Data file: plugins/ExtractCore/data/defense-blocks.yml
 * Key format: "world:x:y:z"
 */
public class DefenseBlockManager {

    private final ExtractCore plugin;
    private final File dataFile;

    /** locKey → current HP */
    private final Map<String, int[]> hpMap = new HashMap<>();
    /** locKey → defense block entry ID */
    private final Map<String, String> idMap = new HashMap<>();

    public DefenseBlockManager(ExtractCore plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/defense-blocks.yml");
        dataFile.getParentFile().mkdirs();
        load();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    public void registerBlock(Location loc, String entryId, int maxHp) {
        String key = locKey(loc);
        hpMap.put(key, new int[]{ maxHp });
        idMap.put(key, entryId);
        saveAsync();
    }

    public void unregisterBlock(Location loc) {
        String key = locKey(loc);
        hpMap.remove(key);
        idMap.remove(key);
        saveAsync();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isDefenseBlock(Location loc) {
        return hpMap.containsKey(locKey(loc));
    }

    public int getHp(Location loc) {
        int[] hp = hpMap.get(locKey(loc));
        return hp == null ? -1 : hp[0];
    }

    public int getMaxHp(Location loc) {
        String id = idMap.get(locKey(loc));
        if (id == null) return 0;
        ShopEntry entry = plugin.getShopManager().getDefenseEntry(id);
        return entry == null ? 0 : entry.hp();
    }

    // ── Hit Logic ─────────────────────────────────────────────────────────────

    /**
     * Applies one hit to the defense block at loc.
     * Shows action-bar feedback and plays sounds/particles to the attacker.
     *
     * @return true if the block was destroyed, false if it survived.
     */
    public boolean hit(Player attacker, Location loc) {
        String key = locKey(loc);
        int[]  hp  = hpMap.get(key);
        if (hp == null) return false;

        hp[0] = Math.max(0, hp[0] - 1);
        int maxHp = getMaxHp(loc);

        FileConfiguration cfg = plugin.getConfigManager().getDefenseBlocksConfig();

        // Entry info for messages
        String    entryId   = idMap.get(key);
        ShopEntry entry     = entryId != null ? plugin.getShopManager().getDefenseEntry(entryId) : null;
        String    blockName = entry != null
                ? ColorUtil.strip(ColorUtil.color(entry.displayName()))
                : "Defense Block";

        // Action-bar HP display
        String barTemplate = cfg.getString("settings.hit-actionbar",
                "&c{name} &8| &eHP: &f{hp}&8/&f{max_hp}");
        attacker.sendActionBar(ColorUtil.component(
                barTemplate
                        .replace("{name}",   blockName)
                        .replace("{hp}",     String.valueOf(hp[0]))
                        .replace("{max_hp}", String.valueOf(maxHp))
        ));

        // Hit sound (configurable)
        String hitSoundName = cfg.getString("settings.hit-sound", "BLOCK_STONE_HIT");
        float  hitVolume    = (float) cfg.getDouble("settings.hit-sound-volume", 0.5);
        float  hitPitch     = (float) cfg.getDouble("settings.hit-sound-pitch-min", 0.8)
                + ((float) cfg.getDouble("settings.hit-sound-pitch-range", 0.4))
                * ((float) hp[0] / Math.max(1, maxHp));
        try {
            attacker.playSound(loc, Sound.valueOf(hitSoundName), hitVolume, hitPitch);
        } catch (IllegalArgumentException ignored) {
            attacker.playSound(loc, Sound.BLOCK_STONE_HIT, hitVolume, 1.0f);
        }

        // Hit particle
        loc.getWorld().spawnParticle(
                Particle.BLOCK,
                loc.clone().add(0.5, 0.5, 0.5),
                6, 0.3, 0.3, 0.3, 0,
                loc.getBlock().getBlockData());

        if (hp[0] <= 0) {
            // Block destroyed
            loc.getBlock().setType(Material.AIR);
            hpMap.remove(key);
            idMap.remove(key);
            saveAsync();

            // Destroy particle
            loc.getWorld().spawnParticle(
                    Particle.BLOCK,
                    loc.clone().add(0.5, 0.5, 0.5),
                    20, 0.3, 0.3, 0.3, 0,
                    Material.COBBLESTONE.createBlockData());

            // Destroy sound (configurable)
            String destroySoundName = cfg.getString("settings.destroy-sound", "BLOCK_STONE_BREAK");
            float  destroyVolume    = (float) cfg.getDouble("settings.destroy-sound-volume", 1.0);
            float  destroyPitch     = (float) cfg.getDouble("settings.destroy-sound-pitch", 1.0);
            try {
                attacker.playSound(loc, Sound.valueOf(destroySoundName), destroyVolume, destroyPitch);
            } catch (IllegalArgumentException ignored) {
                attacker.playSound(loc, Sound.BLOCK_STONE_BREAK, 1f, 1f);
            }

            String msg = plugin.getConfigManager().getMessages()
                    .getString("shop.defense-block-destroyed",
                            "&#5B8DD9You destroyed a &e{name}&8!")
                    .replace("{name}", blockName);
            attacker.sendMessage(ColorUtil.color(msg));
            return true;
        }

        saveAsync();
        return false;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yaml.getKeys(false)) {
            int    hp = yaml.getInt(key + ".hp", 0);
            String id = yaml.getString(key + ".id", "");
            hpMap.put(key, new int[]{ hp });
            idMap.put(key, id);
        }
        plugin.getLogger().info("[ExtractCore] Loaded " + hpMap.size() + " defense block(s) from disk.");
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, int[]> e : hpMap.entrySet()) {
            yaml.set(e.getKey() + ".hp", e.getValue()[0]);
            yaml.set(e.getKey() + ".id", idMap.getOrDefault(e.getKey(), ""));
        }
        try { yaml.save(dataFile); }
        catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save defense block data", e);
        }
    }

    public void shutdown() { save(); }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String locKey(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }
}