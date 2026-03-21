package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.milestones.MilestoneManager;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Compact 3-row Milestones GUI.
 *
 * Layout (27 slots):
 *
 *  [border][border][border][border][border][border][border][border][border]
 *  [border][ EXT ] [KILLS] [TIME ] [CORES] [LEVEL] [border][border][TOTAL]
 *  [border][border][border][border][border][border][border][border][CLOSE]
 *
 * Each category slot shows the NEXT unclaimed milestone:
 *   - If claimable (target met) → enchant glint + "CLAIM" prompt
 *   - If in progress            → normal icon + progress info
 *   - If all complete           → lime stained glass + "Completed!" message
 *
 * Clicking a claimable slot claims it immediately and refreshes.
 */
public class MilestonesGUI extends BaseGUI {

    // Category slot positions in the 3-row (27-slot) inventory
    private static final int[] CATEGORY_SLOTS = {10, 12, 14, 16, 13};
    // Matches MilestoneManager.CATEGORIES order: EXTRACTIONS, KILLS, PLAYTIME, CORES, LEVEL
    // We use 5 categories across the middle row

    private static final int SLOT_TOTAL = 17;
    private static final int SLOT_CLOSE = 26;

    private final GuiUtil g;

    public MilestonesGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
            new GuiUtil(plugin, "milestones").title(),
            3);   // 3 rows = 27 slots
        this.g = new GuiUtil(plugin, "milestones");
    }

    @Override
    protected void build() {
        fillBorder();
        fill(ItemBuilder.filler());

        PlayerData data = plugin.getPlayerDataManager().get(player);
        MilestoneManager mm = plugin.getMilestoneManager();

        // ── Category slots ────────────────────────────────────────
        List<String> categories = MilestoneManager.CATEGORIES;
        int[] slots = {10, 12, 14, 16, 13}; // EXT, KILLS, TIME, CORES, LEVEL

        for (int i = 0; i < categories.size(); i++) {
            String cat  = categories.get(i);
            int slot    = slots[i];
            set(slot, buildCategoryItem(mm, data, cat));
        }

        // ── Overall progress item ─────────────────────────────────
        int totalDone = data.getCompletedMilestones().size();
        int totalAll  = mm.getAllMilestones().size();
        int claimableCount = data.getClaimableMilestones().size();

        Map<String, String> ph = GuiUtil.ph(
            "milestones_done",     String.valueOf(totalDone),
            "milestones_total",    String.valueOf(totalAll),
            "claimable_count",     String.valueOf(claimableCount)
        );
        set(SLOT_TOTAL, new ItemBuilder(mat(g.material("header-item")))
            .name(g.str("header-item.name", ph))
            .lore(g.lore("header-item.lore", ph))
            .hideAll().build());

        // ── Close ─────────────────────────────────────────────────
        set(SLOT_CLOSE, new ItemBuilder(mat(g.material("close-button")))
            .name(g.str("close-button.name"))
            .lore(g.lore("close-button.lore"))
            .hideAll().build());
    }

    private ItemStack buildCategoryItem(MilestoneManager mm, PlayerData data, String category) {
        String nextId = mm.getNextForCategory(data, category);
        int done      = mm.countCompleted(data, category);
        int total     = mm.countTotal(category);

        // ── All milestones in this category done ──────────────────
        if (nextId == null) {
            return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name(g.str("category-complete.name",
                    GuiUtil.ph("category", categoryLabel(category))))
                .lore(buildCompleteLore(category, done, total))
                .hideAll().build();
        }

        MilestoneManager.MilestoneEntry entry = mm.getEntry(nextId);
        if (entry == null) return ItemBuilder.filler();

        boolean claimable = data.getClaimableMilestones().contains(nextId);

        Material icon;
        try { icon = Material.valueOf(entry.icon()); } catch (Exception e) { icon = Material.PAPER; }

        String namePrefix = claimable
            ? g.str("milestone-claimable-prefix")
            : g.str("milestone-incomplete-prefix");
        String name = namePrefix + ColorUtil.color(entry.displayName());

        List<String> lore = buildMilestoneLore(entry, data, done, total, claimable);

        ItemStack item = new ItemBuilder(icon)
            .name(name)
            .lore(lore)
            .hideAll()
            .build();

        // Enchantment glint when claimable
        if (claimable) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    private List<String> buildMilestoneLore(MilestoneManager.MilestoneEntry entry,
                                             PlayerData data,
                                             int done, int total, boolean claimable) {
        List<String> lore = new ArrayList<>();
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━");

        // Category + progress counter
        lore.add("&8◈ &7" + categoryLabel(entry.type()) + "  &8|  &7" + done + " &8/ &7" + total);
        lore.add("");

        // Description
        entry.description().stream().map(ColorUtil::color).forEach(lore::add);
        lore.add("");

        // Target progress
        long current = getStatValue(data, entry.type());
        lore.add("&7Progress: &e" + ColorUtil.formatNumber(current)
                + " &8/ &e" + ColorUtil.formatNumber(entry.target()));

        // Rewards
        lore.add("");
        lore.add("&8◈ &7Rewards:");
        if (entry.rewardScrap()  > 0) lore.add("  &8◆ &eScrap:        &f" + ColorUtil.formatNumber(entry.rewardScrap()));
        if (entry.rewardScrews() > 0) lore.add("  &8◆ &bScrews:       &f" + ColorUtil.formatNumber(entry.rewardScrews()));
        if (entry.rewardEnergy() > 0) lore.add("  &8◆ &aEnergy Cells: &f" + ColorUtil.formatNumber(entry.rewardEnergy()));
        if (entry.rewardBio()    > 0) lore.add("  &8◆ &dBio Samples:  &f" + ColorUtil.formatNumber(entry.rewardBio()));
        if (entry.rewardTech()   > 0) lore.add("  &8◆ &6Tech Shards:  &f" + ColorUtil.formatNumber(entry.rewardTech()));
        if (entry.rewardPoints() > 0) lore.add("  &8◆ &fPoints:       &f" + entry.rewardPoints());

        lore.add("");
        if (claimable) {
            lore.add("&#FFD700★ &aReady to claim! Click to collect.");
        } else {
            lore.add("&8▸ &7Keep going to unlock this reward.");
        }
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━");

        return lore.stream().map(ColorUtil::color).collect(java.util.stream.Collectors.toList());
    }

    private List<String> buildCompleteLore(String category, int done, int total) {
        return List.of(
            ColorUtil.color("&8━━━━━━━━━━━━━━━━━━━━━━━━"),
            ColorUtil.color("&8◈ &7" + categoryLabel(category)),
            ColorUtil.color(""),
            ColorUtil.color("&#5B8DD9✔ &aAll " + total + " milestones completed!"),
            ColorUtil.color("&7You've mastered this category."),
            ColorUtil.color("&8━━━━━━━━━━━━━━━━━━━━━━━━")
        );
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }

        // Check if a category slot was clicked
        List<String> categories = MilestoneManager.CATEGORIES;
        int[] slots = {10, 12, 14, 16, 13};

        for (int i = 0; i < slots.length; i++) {
            if (slot != slots[i]) continue;

            String category = categories.get(i);
            PlayerData data = plugin.getPlayerDataManager().get(player);
            MilestoneManager mm = plugin.getMilestoneManager();
            String nextId = mm.getNextForCategory(data, category);

            if (nextId == null) return; // all done, nothing to claim

            if (data.getClaimableMilestones().contains(nextId)) {
                mm.claim(player, data, nextId);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                build(); // refresh GUI
            } else {
                // Not claimable yet — play deny sound
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
            return;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long getStatValue(PlayerData data, String type) {
        return switch (type) {
            case "EXTRACTIONS"      -> data.getExtractions();
            case "KILLS"            -> data.getKills();
            case "PLAYTIME_MINUTES" -> data.getPlaytimeMinutes();
            case "CORES_DESTROYED"  -> data.getCoresDestroyed();
            case "LEVEL"            -> data.getLevel();
            default                 -> 0;
        };
    }

    private String categoryLabel(String type) {
        return switch (type) {
            case "EXTRACTIONS"      -> "Extractions";
            case "KILLS"            -> "Player Kills";
            case "PLAYTIME_MINUTES" -> "Playtime";
            case "CORES_DESTROYED"  -> "Cores Raided";
            case "LEVEL"            -> "Level";
            default                 -> type;
        };
    }

    private Material mat(String n) {
        try { return Material.valueOf(n); } catch (Exception e) { return Material.PAPER; }
    }
}
