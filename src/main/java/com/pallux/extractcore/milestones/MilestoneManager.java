package com.pallux.extractcore.milestones;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Milestone flow:
 *   1. check() — called after any stat change.
 *      Moves eligible, un-seen milestones into claimableMilestones.
 *      Does NOT award anything automatically.
 *   2. claim() — called when the player clicks a claimable slot in MilestonesGUI.
 *      Moves the id from claimable → completed, distributes rewards.
 */
public class MilestoneManager {

    private final ExtractCore plugin;

    // Canonical category order for the GUI slots
    public static final List<String> CATEGORIES = List.of(
            "EXTRACTIONS", "KILLS", "PLAYTIME_MINUTES", "CORES_DESTROYED", "LEVEL"
    );

    public MilestoneManager(ExtractCore plugin) {
        this.plugin = plugin;
    }

    // ── Check & mark claimable ────────────────────────────────────────────────

    public void check(Player player, PlayerData data) {
        FileConfiguration cfg = plugin.getConfigManager().getMilestonesConfig();
        ConfigurationSection ms = cfg.getConfigurationSection("milestones");
        if (ms == null) return;

        boolean anyNewlyClaimable = false;
        for (String id : ms.getKeys(false)) {
            if (data.getCompletedMilestones().contains(id)) continue;
            if (data.getClaimableMilestones().contains(id)) continue;

            String type   = cfg.getString("milestones." + id + ".type", "");
            int    target = cfg.getInt("milestones." + id + ".target", 0);

            boolean eligible = switch (type) {
                case "EXTRACTIONS"      -> data.getExtractions()     >= target;
                case "KILLS"            -> data.getKills()           >= target;
                case "PLAYTIME_MINUTES" -> data.getPlaytimeMinutes() >= target;
                case "CORES_DESTROYED"  -> data.getCoresDestroyed()  >= target;
                case "LEVEL"            -> data.getLevel()           >= target;
                default                 -> false;
            };

            if (eligible) {
                data.getClaimableMilestones().add(id);
                anyNewlyClaimable = true;
            }
        }

        if (anyNewlyClaimable) {
            plugin.getPlayerDataManager().saveAsync(data);
            // Notify the player a milestone is ready to collect
            String msg = plugin.getConfigManager().getMessages()
                    .getString("milestones.claimable-notify",
                        "&#FFD700★ &eA Milestone is ready to claim! &7Use &e/milestones &7to collect.");
            player.sendMessage(ColorUtil.color(msg));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        }
    }

    // ── Claim ─────────────────────────────────────────────────────────────────

    /**
     * Claims a specific milestone id for the player.
     * Returns true if successful, false if not claimable.
     */
    public boolean claim(Player player, PlayerData data, String id) {
        if (!data.getClaimableMilestones().contains(id)) return false;

        FileConfiguration cfg = plugin.getConfigManager().getMilestonesConfig();
        String path = "milestones." + id;

        data.getClaimableMilestones().remove(id);
        data.getCompletedMilestones().add(id);

        long scrap  = cfg.getLong(path + ".rewards.scrap", 0);
        long screws = cfg.getLong(path + ".rewards.screws", 0);
        long energy = cfg.getLong(path + ".rewards.energy-cells", 0);
        long bio    = cfg.getLong(path + ".rewards.bio-samples", 0);
        long tech   = cfg.getLong(path + ".rewards.tech-shards", 0);
        int  points = cfg.getInt(path + ".rewards.points", 0);

        data.addMaterials(scrap, screws, energy, bio, tech);

        if (points > 0 && plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
            try {
                org.black_ixx.playerpoints.PlayerPoints.getInstance().getAPI()
                        .give(player.getUniqueId(), points);
            } catch (Exception ignored) {}
        }

        plugin.getPlayerDataManager().saveAsync(data);

        // Build reward summary
        StringBuilder rewards = new StringBuilder();
        if (scrap  > 0) rewards.append("&e").append(ColorUtil.formatNumber(scrap)).append(" Scrap ");
        if (screws > 0) rewards.append("&b").append(ColorUtil.formatNumber(screws)).append(" Screws ");
        if (energy > 0) rewards.append("&a").append(ColorUtil.formatNumber(energy)).append(" Energy ");
        if (bio    > 0) rewards.append("&d").append(ColorUtil.formatNumber(bio)).append(" Bio ");
        if (tech   > 0) rewards.append("&6").append(ColorUtil.formatNumber(tech)).append(" Tech ");
        if (points > 0) rewards.append("&f").append(points).append(" Points");

        String displayName = cfg.getString(path + ".display-name", id);
        String msg = plugin.getConfigManager().getMessages()
                .getString("milestones.completed", "")
                .replace("{milestone}", displayName)
                .replace("{rewards}", rewards.toString().trim());
        player.sendMessage(ColorUtil.color(msg));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        return true;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns the next milestone id in this category for the player:
     *   - First checks claimable (ready to claim)
     *   - Then falls back to the next uncompleted one
     * Returns null if all milestones in this category are done.
     */
    public String getNextForCategory(PlayerData data, String category) {
        FileConfiguration cfg = plugin.getConfigManager().getMilestonesConfig();
        ConfigurationSection ms = cfg.getConfigurationSection("milestones");
        if (ms == null) return null;

        // Build sorted list of milestones in this category by target
        List<Map.Entry<String, Integer>> inCategory = new ArrayList<>();
        for (String id : ms.getKeys(false)) {
            if (!cfg.getString("milestones." + id + ".type", "").equals(category)) continue;
            inCategory.add(Map.entry(id, cfg.getInt("milestones." + id + ".target", 0)));
        }
        inCategory.sort(Comparator.comparingInt(Map.Entry::getValue));

        // 1. First claimable in this category
        for (var e : inCategory) {
            if (data.getClaimableMilestones().contains(e.getKey())) return e.getKey();
        }
        // 2. First not yet completed
        for (var e : inCategory) {
            if (!data.getCompletedMilestones().contains(e.getKey())) return e.getKey();
        }
        return null; // all done
    }

    public boolean isCategoryComplete(PlayerData data, String category) {
        return getNextForCategory(data, category) == null;
    }

    public int countCompleted(PlayerData data, String category) {
        FileConfiguration cfg = plugin.getConfigManager().getMilestonesConfig();
        ConfigurationSection ms = cfg.getConfigurationSection("milestones");
        if (ms == null) return 0;
        int count = 0;
        for (String id : ms.getKeys(false)) {
            if (!cfg.getString("milestones." + id + ".type", "").equals(category)) continue;
            if (data.getCompletedMilestones().contains(id)) count++;
        }
        return count;
    }

    public int countTotal(String category) {
        FileConfiguration cfg = plugin.getConfigManager().getMilestonesConfig();
        ConfigurationSection ms = cfg.getConfigurationSection("milestones");
        if (ms == null) return 0;
        int count = 0;
        for (String id : ms.getKeys(false)) {
            if (cfg.getString("milestones." + id + ".type", "").equals(category)) count++;
        }
        return count;
    }

    public Map<String, MilestoneEntry> getAllMilestones() {
        FileConfiguration cfg = plugin.getConfigManager().getMilestonesConfig();
        ConfigurationSection ms = cfg.getConfigurationSection("milestones");
        Map<String, MilestoneEntry> result = new LinkedHashMap<>();
        if (ms == null) return result;
        for (String id : ms.getKeys(false)) {
            result.put(id, new MilestoneEntry(
                    id,
                    cfg.getString("milestones." + id + ".display-name", id),
                    cfg.getString("milestones." + id + ".type", ""),
                    cfg.getInt("milestones." + id + ".target", 0),
                    cfg.getString("milestones." + id + ".icon", "PAPER"),
                    cfg.getStringList("milestones." + id + ".description"),
                    cfg.getLong("milestones." + id + ".rewards.scrap", 0),
                    cfg.getLong("milestones." + id + ".rewards.screws", 0),
                    cfg.getLong("milestones." + id + ".rewards.energy-cells", 0),
                    cfg.getLong("milestones." + id + ".rewards.bio-samples", 0),
                    cfg.getLong("milestones." + id + ".rewards.tech-shards", 0),
                    cfg.getInt("milestones." + id + ".rewards.points", 0)
            ));
        }
        return result;
    }

    public MilestoneEntry getEntry(String id) {
        return getAllMilestones().get(id);
    }

    public record MilestoneEntry(
            String id, String displayName, String type,
            int target, String icon,
            java.util.List<String> description,
            long rewardScrap, long rewardScrews, long rewardEnergy,
            long rewardBio, long rewardTech, int rewardPoints
    ) {}
}
