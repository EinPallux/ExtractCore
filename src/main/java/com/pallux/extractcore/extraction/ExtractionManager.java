package com.pallux.extractcore.extraction;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Extraction Zones, the extraction cycle timer, BossBar, and reward distribution.
 */
public class ExtractionManager {

    private final ExtractCore plugin;

    // zone name -> ExtractionZone
    private final Map<String, ExtractionZone> zones = new LinkedHashMap<>();

    private BossBar bossBar;
    private BukkitTask cycleTask;

    // State
    private boolean extractionActive = false;
    private ExtractionZone activeZone = null;
    private long countdownSeconds = 0;
    private long activeSecondsLeft = 0;

    public ExtractionManager(ExtractCore plugin) {
        this.plugin = plugin;
        loadZones();
        initBossBar();
        startCycle();
    }

    // ── Zone Management ───────────────────────────────────────────────────────

    private void loadZones() {
        zones.clear();
        FileConfiguration cfg = plugin.getConfigManager().getExtractionsConfig();
        ConfigurationSection zonesSection = cfg.getConfigurationSection("zones");
        if (zonesSection == null) return;

        for (String name : zonesSection.getKeys(false)) {
            try {
                String path = "zones." + name;
                String world = cfg.getString(path + ".world");
                int minX = cfg.getInt(path + ".min-x"), minY = cfg.getInt(path + ".min-y"),
                    minZ = cfg.getInt(path + ".min-z"), maxX = cfg.getInt(path + ".max-x"),
                    maxY = cfg.getInt(path + ".max-y"), maxZ = cfg.getInt(path + ".max-z");

                long scrap  = cfg.getLong(path + ".rewards.scrap",  getDefaultReward("scrap"));
                long screws = cfg.getLong(path + ".rewards.screws", getDefaultReward("screws"));
                long energy = cfg.getLong(path + ".rewards.energy-cells", getDefaultReward("energy-cells"));
                long bio    = cfg.getLong(path + ".rewards.bio-samples",  getDefaultReward("bio-samples"));
                long tech   = cfg.getLong(path + ".rewards.tech-shards",  getDefaultReward("tech-shards"));

                zones.put(name, new ExtractionZone(name, world, minX, minY, minZ, maxX, maxY, maxZ,
                        scrap, screws, energy, bio, tech));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load extraction zone: " + name + " - " + e.getMessage());
            }
        }
        plugin.getLogger().info("[ExtractCore] Loaded " + zones.size() + " extraction zone(s).");
    }

    public void defineZone(String name, String world,
                           int minX, int minY, int minZ,
                           int maxX, int maxY, int maxZ) {
        FileConfiguration cfg = plugin.getConfigManager().getExtractionsConfig();
        String path = "zones." + name;
        cfg.set(path + ".world", world);
        cfg.set(path + ".min-x", minX); cfg.set(path + ".min-y", minY); cfg.set(path + ".min-z", minZ);
        cfg.set(path + ".max-x", maxX); cfg.set(path + ".max-y", maxY); cfg.set(path + ".max-z", maxZ);
        cfg.set(path + ".rewards.scrap",        getDefaultReward("scrap"));
        cfg.set(path + ".rewards.screws",       getDefaultReward("screws"));
        cfg.set(path + ".rewards.energy-cells", getDefaultReward("energy-cells"));
        cfg.set(path + ".rewards.bio-samples",  getDefaultReward("bio-samples"));
        cfg.set(path + ".rewards.tech-shards",  getDefaultReward("tech-shards"));
        plugin.getConfigManager().saveExtractionsConfig();

        ExtractionZone zone = new ExtractionZone(name, world, minX, minY, minZ, maxX, maxY, maxZ,
                getDefaultReward("scrap"), getDefaultReward("screws"), getDefaultReward("energy-cells"),
                getDefaultReward("bio-samples"), getDefaultReward("tech-shards"));
        zones.put(name, zone);
    }

    private long getDefaultReward(String type) {
        return plugin.getConfigManager().getExtractionsConfig()
                .getLong("default-rewards." + type, 100);
    }

    // ── Boss Bar ──────────────────────────────────────────────────────────────

    private void initBossBar() {
        String loadingMsg = plugin.getConfigManager().getMessages()
                .getString("extraction.bossbar-loading", "&#5B8DD9⚡ Loading extraction info...");
        bossBar = Bukkit.createBossBar(
                ColorUtil.color(loadingMsg),
                BarColor.BLUE, BarStyle.SOLID);
        bossBar.setVisible(true);
        for (Player p : Bukkit.getOnlinePlayers()) bossBar.addPlayer(p);
    }

    public void addPlayerToBossBar(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public void removePlayerFromBossBar(Player player) {
        if (bossBar != null) bossBar.removePlayer(player);
    }

    // ── Cycle ─────────────────────────────────────────────────────────────────

    private void startCycle() {
        FileConfiguration cfg = plugin.getConfigManager().getExtractionsConfig();
        long cycleMinutes  = cfg.getLong("cycle-interval-minutes", 15);
        long activeMins    = cfg.getLong("active-duration-minutes", 5);

        countdownSeconds  = cycleMinutes * 60;
        activeSecondsLeft = activeMins * 60;

        cycleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (zones.isEmpty()) {
                    updateBossBar("&#5B8DD9⚡ &7No extraction zones configured.", 1.0, false);
                    return;
                }

                if (!extractionActive) {
                    countdownSeconds--;
                    double progress = (double) countdownSeconds / (cycleMinutes * 60);
                    String msg = plugin.getConfigManager().getMessages()
                            .getString("extraction.bossbar-next", "&#5B8DD9⚡ Next Extraction in: &e{time}");
                    msg = msg.replace("{time}", ColorUtil.formatDuration(countdownSeconds));
                    updateBossBar(msg, Math.max(0, progress), false);

                    if (countdownSeconds <= 0) {
                        startExtraction();
                        activeSecondsLeft = activeMins * 60;
                    }
                } else {
                    activeSecondsLeft--;
                    double progress = (double) activeSecondsLeft / (activeMins * 60);
                    String msg = plugin.getConfigManager().getMessages()
                            .getString("extraction.bossbar-active",
                                    "&#FF6B35⚡ Extraction Active: &e{name} &8— &7Ends in: &e{time}");
                    msg = msg.replace("{name}", activeZone != null ? activeZone.getName() : "?")
                             .replace("{time}", ColorUtil.formatDuration(activeSecondsLeft));
                    updateBossBar(msg, Math.max(0, progress), true);

                    if (activeSecondsLeft <= 0) {
                        endExtraction();
                        countdownSeconds = cycleMinutes * 60;
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void updateBossBar(String message, double progress, boolean active) {
        if (bossBar == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            bossBar.setTitle(ColorUtil.color(message));
            bossBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
            bossBar.setColor(active ? BarColor.RED : BarColor.BLUE);
        });
    }

    private void startExtraction() {
        if (zones.isEmpty()) return;
        List<ExtractionZone> zoneList = new ArrayList<>(zones.values());
        activeZone = zoneList.get(new Random().nextInt(zoneList.size()));
        extractionActive = true;

        // Center coords
        int cx = (activeZone.getMinX() + activeZone.getMaxX()) / 2;
        int cz = (activeZone.getMinZ() + activeZone.getMaxZ()) / 2;
        long duration = plugin.getConfigManager().getExtractionsConfig()
                .getLong("active-duration-minutes", 5);

        // Broadcast start
        String startMsg = plugin.getConfigManager().getMessages()
                .getString("extraction.started", "");
        startMsg = startMsg.replace("{name}", activeZone.getName())
                .replace("{x}", String.valueOf(cx))
                .replace("{z}", String.valueOf(cz))
                .replace("{duration}", String.valueOf(duration));
        MessageUtil.broadcast(startMsg);

        // Play sound for all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.2f);
        }
    }

    private void endExtraction() {
        if (activeZone == null) { extractionActive = false; return; }

        // Find players inside zone
        List<Player> participants = getPlayersInZone(activeZone);

        // Announce end
        if (participants.isEmpty()) {
            MessageUtil.broadcast(
                plugin.getConfigManager().getMessages()
                    .getString("extraction.ended-no-participants",
                        "&#8888AANo players participated in this extraction."));
        } else {
            // Share rewards equally
            int count = participants.size();
            long shareScrap  = activeZone.getRewardScrap()  / count;
            long shareScrews = activeZone.getRewardScrews() / count;
            long shareEnergy = activeZone.getRewardEnergy() / count;
            long shareBio    = activeZone.getRewardBio()    / count;
            long shareTech   = activeZone.getRewardTech()   / count;

            List<String> names = new ArrayList<>();
            for (Player p : participants) {
                PlayerData data = plugin.getPlayerDataManager().get(p);
                data.addScrap(shareScrap);
                data.addScrews(shareScrews);
                data.addEnergyCells(shareEnergy);
                data.addBioSamples(shareBio);
                data.addTechShards(shareTech);
                data.incrementExtractions();
                plugin.getPlayerDataManager().saveAsync(data);

                // XP + milestones
                plugin.getLevelManager().addXp(p, "extraction-participate");
                plugin.getMilestoneManager().check(p, data);

                // Notify player
                String reward = plugin.getConfigManager().getMessages()
                        .getString("extraction.reward-received", "");
                reward = reward.replace("{scrap}",  ColorUtil.formatNumber(shareScrap))
                        .replace("{screws}", ColorUtil.formatNumber(shareScrews))
                        .replace("{energy}", ColorUtil.formatNumber(shareEnergy))
                        .replace("{bio}",    ColorUtil.formatNumber(shareBio))
                        .replace("{tech}",   ColorUtil.formatNumber(shareTech));
                p.sendMessage(ColorUtil.color(reward));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                names.add(p.getName());
            }

            // Broadcast end summary
            String header = plugin.getConfigManager().getMessages()
                    .getString("extraction.ended-header", "")
                    .replace("{name}", activeZone.getName());
            String partLine = plugin.getConfigManager().getMessages()
                    .getString("extraction.ended-participants", "")
                    .replace("{players}", String.join("&7, &e", names));
            MessageUtil.broadcast(header);
            MessageUtil.broadcast(partLine);
        }

        extractionActive = false;
        activeZone = null;
    }

    private List<Player> getPlayersInZone(ExtractionZone zone) {
        List<Player> result = new ArrayList<>();
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return result;

        for (Player p : world.getPlayers()) {
            Location loc = p.getLocation();
            if (loc.getBlockX() >= zone.getMinX() && loc.getBlockX() <= zone.getMaxX()
             && loc.getBlockY() >= zone.getMinY() && loc.getBlockY() <= zone.getMaxY()
             && loc.getBlockZ() >= zone.getMinZ() && loc.getBlockZ() <= zone.getMaxZ()) {
                result.add(p);
            }
        }
        return result;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isExtractionActive() { return extractionActive; }
    public ExtractionZone getActiveZone() { return activeZone; }
    public long getCountdownSeconds() { return countdownSeconds; }
    public long getActiveSecondsLeft() { return activeSecondsLeft; }
    public Map<String, ExtractionZone> getZones() { return zones; }

    public void reload() {
        if (cycleTask != null) cycleTask.cancel();
        if (bossBar != null) bossBar.setVisible(false);
        loadZones();
        initBossBar();
        for (Player p : Bukkit.getOnlinePlayers()) addPlayerToBossBar(p);
        startCycle();
    }

    public void shutdown() {
        if (cycleTask != null) cycleTask.cancel();
        if (bossBar != null) { bossBar.setVisible(false); bossBar.removeAll(); }
    }
}
