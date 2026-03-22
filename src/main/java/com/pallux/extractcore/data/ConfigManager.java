package com.pallux.extractcore.data;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {

    private final ExtractCore plugin;

    private FileConfiguration messages;
    private FileConfiguration coreConfig;
    private FileConfiguration armoryConfig;
    private FileConfiguration extractionsConfig;
    private FileConfiguration levelsConfig;
    private FileConfiguration milestonesConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration shopConfig;
    private FileConfiguration defenseBlocksConfig;

    public ConfigManager(ExtractCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        messages          = loadConfig("config/messages.yml");
        coreConfig        = loadConfig("config/core.yml");
        armoryConfig      = loadConfig("config/armory.yml");
        extractionsConfig = loadConfig("config/extractions.yml");
        levelsConfig      = loadConfig("config/levels.yml");
        milestonesConfig  = loadConfig("config/milestones.yml");
        guiConfig         = loadConfig("config/guis.yml");
        shopConfig        = loadConfig("config/shop.yml");
        defenseBlocksConfig = loadConfig("config/defense-blocks.yml");

        plugin.getLogger().info("[ExtractCore] All configuration files loaded.");
    }

    private FileConfiguration loadConfig(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(path, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream is = plugin.getResource(path)) {
            if (is != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(is, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load defaults for " + path, e);
        }
        return config;
    }

    public void saveExtractionsConfig() {
        try {
            File file = new File(plugin.getDataFolder(), "config/extractions.yml");
            extractionsConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save extractions.yml", e);
        }
    }

    public FileConfiguration getMain()               { return plugin.getConfig(); }
    public FileConfiguration getMessages()           { return messages; }
    public FileConfiguration getCoreConfig()         { return coreConfig; }
    public FileConfiguration getArmoryConfig()       { return armoryConfig; }
    public FileConfiguration getExtractionsConfig()  { return extractionsConfig; }
    public FileConfiguration getLevelsConfig()       { return levelsConfig; }
    public FileConfiguration getMilestonesConfig()   { return milestonesConfig; }
    public FileConfiguration getGuiConfig()          { return guiConfig; }
    public FileConfiguration getShopConfig()         { return shopConfig; }
    public FileConfiguration getDefenseBlocksConfig(){ return defenseBlocksConfig; }
}