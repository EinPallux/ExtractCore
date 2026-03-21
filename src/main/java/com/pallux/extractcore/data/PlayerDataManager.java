package com.pallux.extractcore.data;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages loading and saving of player data YAML files.
 * Files stored at: plugins/ExtractCore/playerdata/<UUID>.yml
 */
public class PlayerDataManager {

    private final ExtractCore plugin;
    private final File playerDataDir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(ExtractCore plugin) {
        this.plugin = plugin;
        this.playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) playerDataDir.mkdirs();
    }

    /**
     * Loads a player's data synchronously (called from async thread is fine for file I/O).
     */
    public PlayerData load(Player player) {
        return load(player.getUniqueId(), player.getName());
    }

    public PlayerData load(UUID uuid, String name) {
        if (cache.containsKey(uuid)) return cache.get(uuid);

        File file = getFile(uuid);
        PlayerData data;

        if (file.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                data = deserialize(uuid, name, yaml);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + uuid, e);
                data = new PlayerData(uuid, name);
            }
        } else {
            data = new PlayerData(uuid, name);
        }

        cache.put(uuid, data);
        return data;
    }

    public void save(PlayerData data) {
        File file = getFile(data.getUuid());
        try {
            YamlConfiguration yaml = serialize(data);
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + data.getUuid(), e);
        }
    }

    public void saveAsync(PlayerData data) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> save(data));
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            save(data);
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) save(data);
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData get(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) data = load(player);
        return data;
    }

    public Collection<PlayerData> getAll() {
        return cache.values();
    }

    private File getFile(UUID uuid) {
        return new File(playerDataDir, uuid + ".yml");
    }

    // ── Serialization ─────────────────────────
    private YamlConfiguration serialize(PlayerData d) {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", d.getName());
        yaml.set("currency.scrap",        d.getScrap());
        yaml.set("currency.screws",       d.getScrews());
        yaml.set("currency.energy-cells", d.getEnergyCells());
        yaml.set("currency.bio-samples",  d.getBioSamples());
        yaml.set("currency.tech-shards",  d.getTechShards());

        yaml.set("leveling.level", d.getLevel());
        yaml.set("leveling.xp",    d.getXp());

        yaml.set("stats.kills",          d.getKills());
        yaml.set("stats.deaths",         d.getDeaths());
        yaml.set("stats.extractions",    d.getExtractions());
        yaml.set("stats.cores-destroyed",d.getCoresDestroyed());
        yaml.set("stats.playtime-minutes",d.getPlaytimeMinutes());
        yaml.set("stats.first-join",     d.getFirstJoinTime());
        yaml.set("stats.last-seen",      d.getLastSeenTime());

        yaml.set("core.placed",    d.isCorePlaced());
        yaml.set("core.world",     d.getCoreWorld());
        yaml.set("core.x",         d.getCoreX());
        yaml.set("core.y",         d.getCoreY());
        yaml.set("core.z",         d.getCoreZ());
        yaml.set("core.speed-level",      d.getCoreSpeedLevel());
        yaml.set("core.production-level", d.getCoreProductionLevel());
        yaml.set("core.stored-scrap",  d.getCoreStoredScrap());

        yaml.set("armory.sword",       d.getSwordTier());
        yaml.set("armory.pickaxe",     d.getPickaxeTier());
        yaml.set("armory.axe",         d.getAxeTier());
        yaml.set("armory.helmet",      d.getHelmetTier());
        yaml.set("armory.chestplate",  d.getChestplateTier());
        yaml.set("armory.leggings",    d.getLeggingsTier());
        yaml.set("armory.boots",       d.getBootsTier());

        yaml.set("milestones", d.getCompletedMilestones().stream().toList());
        yaml.set("claimable-milestones", d.getClaimableMilestones().stream().toList());

        return yaml;
    }

    private PlayerData deserialize(UUID uuid, String name, YamlConfiguration yaml) {
        PlayerData d = new PlayerData(uuid, yaml.getString("name", name));

        d.setScrap(yaml.getLong("currency.scrap", 0));
        d.setScrews(yaml.getLong("currency.screws", 0));
        d.setEnergyCells(yaml.getLong("currency.energy-cells", 0));
        d.setBioSamples(yaml.getLong("currency.bio-samples", 0));
        d.setTechShards(yaml.getLong("currency.tech-shards", 0));

        d.setLevel(yaml.getInt("leveling.level", 1));
        d.setXp(yaml.getLong("leveling.xp", 0));

        d.setKills(yaml.getInt("stats.kills", 0));
        d.setDeaths(yaml.getInt("stats.deaths", 0));
        d.setExtractions(yaml.getInt("stats.extractions", 0));
        d.setCoresDestroyed(yaml.getInt("stats.cores-destroyed", 0));
        d.setPlaytimeMinutes(yaml.getLong("stats.playtime-minutes", 0));
        d.setFirstJoinTime(yaml.getLong("stats.first-join", System.currentTimeMillis()));
        d.setLastSeenTime(yaml.getLong("stats.last-seen", System.currentTimeMillis()));

        d.setCorePlaced(yaml.getBoolean("core.placed", false));
        d.setCoreWorld(yaml.getString("core.world", null));
        d.setCoreX(yaml.getInt("core.x", 0));
        d.setCoreY(yaml.getInt("core.y", 0));
        d.setCoreZ(yaml.getInt("core.z", 0));
        d.setCoreSpeedLevel(yaml.getInt("core.speed-level", 0));
        d.setCoreProductionLevel(yaml.getInt("core.production-level", 0));
        d.setCoreStoredScrap(yaml.getLong("core.stored-scrap", 0));

        d.setSwordTier(yaml.getInt("armory.sword", 1));
        d.setPickaxeTier(yaml.getInt("armory.pickaxe", 1));
        d.setAxeTier(yaml.getInt("armory.axe", 1));
        d.setHelmetTier(yaml.getInt("armory.helmet", 1));
        d.setChestplateTier(yaml.getInt("armory.chestplate", 1));
        d.setLeggingsTier(yaml.getInt("armory.leggings", 1));
        d.setBootsTier(yaml.getInt("armory.boots", 1));

        var milestones = yaml.getStringList("milestones");
        d.getCompletedMilestones().addAll(milestones);
        var claimable = yaml.getStringList("claimable-milestones");
        d.getClaimableMilestones().addAll(claimable);

        return d;
    }
}
