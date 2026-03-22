package com.pallux.extractcore;

import com.pallux.extractcore.armory.ArmoryManager;
import com.pallux.extractcore.commands.admin.AdminCommand;
import com.pallux.extractcore.commands.player.*;
import com.pallux.extractcore.core.CoreManager;
import com.pallux.extractcore.core.HologramManager;
import com.pallux.extractcore.data.ConfigManager;
import com.pallux.extractcore.data.PlayerDataManager;
import com.pallux.extractcore.exchange.ExchangeManager;
import com.pallux.extractcore.extraction.ExtractionManager;
import com.pallux.extractcore.leveling.LevelManager;
import com.pallux.extractcore.listeners.*;
import com.pallux.extractcore.milestones.MilestoneManager;
import com.pallux.extractcore.placeholders.ExtractPlaceholders;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.worldguard.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExtractCore extends JavaPlugin {

    private static ExtractCore instance;

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private CoreManager coreManager;
    private ArmoryManager armoryManager;
    private ExtractionManager extractionManager;
    private LevelManager levelManager;
    private MilestoneManager milestoneManager;
    private ExchangeManager exchangeManager;
    private HologramManager hologramManager;

    /**
     * onLoad() runs before any plugin is enabled, which is the required moment
     * to register custom WorldGuard flags.
     */
    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardHook.registerFlag();
            getLogger().info("[ExtractCore] Registered custom WorldGuard flag: extract-core-place");
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        // ── Config ─────────────────────────────
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // ── Data ───────────────────────────────
        playerDataManager = new PlayerDataManager(this);

        // ── Managers ───────────────────────────
        coreManager       = new CoreManager(this);
        hologramManager   = new HologramManager(this);
        armoryManager     = new ArmoryManager(this);
        extractionManager = new ExtractionManager(this);
        levelManager      = new LevelManager(this);
        milestoneManager  = new MilestoneManager(this);
        exchangeManager   = new ExchangeManager(this);

        // ── WorldGuard ─────────────────────────
        WorldGuardHook.init();
        if (WorldGuardHook.isHooked()) {
            getLogger().info("[ExtractCore] WorldGuard hooked successfully.");
            getLogger().info("[ExtractCore] Use flag 'extract-core-place deny' on spawn/safe regions to block Core placement.");
        } else {
            getLogger().info("[ExtractCore] WorldGuard not found — region protection disabled.");
        }

        // ── Listeners ──────────────────────────
        registerListeners();

        // ── Commands ───────────────────────────
        registerCommands();

        // ── PlaceholderAPI ─────────────────────
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ExtractPlaceholders(this).register();
            getLogger().info("PlaceholderAPI hooked successfully.");
        } else {
            getLogger().warning("PlaceholderAPI not found — placeholders will not work.");
        }

        // ── Auto-save ─────────────────────────
        int interval = getConfig().getInt("settings.autosave-interval", 300);
        if (interval <= 0) interval = 300;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> playerDataManager.saveAll(), interval * 20L, interval * 20L);

        getLogger().info(ColorUtil.strip("&a[ExtractCore] Plugin enabled successfully! Running on MC 1.21.4"));
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (extractionManager != null) extractionManager.shutdown();
        if (hologramManager != null) hologramManager.shutdown();
        if (coreManager != null) coreManager.shutdown();
        getLogger().info("[ExtractCore] Plugin disabled. All data saved.");
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new ItemProtectionListener(this), this);
        pm.registerEvents(new CoreInteractListener(this), this);
        pm.registerEvents(new GUIListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new PlaytimeListener(this), this);
    }

    private void registerCommands() {
        getCommand("armory").setExecutor(new ArmoryCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("extract").setExecutor(new ExtractCommand(this));
        getCommand("core").setExecutor(new CoreCommand(this));
        getCommand("exchange").setExecutor(new ExchangeCommand(this));
        getCommand("milestones").setExecutor(new MilestonesCommand(this));
        getCommand("ex").setExecutor(new AdminCommand(this));
        getCommand("ex").setTabCompleter(new AdminCommand(this));
    }

    public void reload() {
        configManager.loadAll();
        extractionManager.reload();
        hologramManager.reload();
        armoryManager.reload();
        getLogger().info("[ExtractCore] All configs reloaded.");
    }

    // ── Getters ────────────────────────────────
    public static ExtractCore getInstance() { return instance; }
    public ConfigManager getConfigManager()       { return configManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public CoreManager getCoreManager()           { return coreManager; }
    public ArmoryManager getArmoryManager()       { return armoryManager; }
    public ExtractionManager getExtractionManager() { return extractionManager; }
    public LevelManager getLevelManager()         { return levelManager; }
    public MilestoneManager getMilestoneManager() { return milestoneManager; }
    public ExchangeManager getExchangeManager()   { return exchangeManager; }
    public HologramManager getHologramManager()   { return hologramManager; }
}