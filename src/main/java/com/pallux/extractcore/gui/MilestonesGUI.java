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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MilestonesGUI extends BaseGUI {

    private final GuiUtil g;

    public MilestonesGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
                new GuiUtil(plugin, "milestones").title(),
                new GuiUtil(plugin, "milestones").rows());
        this.g = new GuiUtil(plugin, "milestones");
    }

    @Override
    protected void build() {
        // Filler
        if (g.getBool("filler.enabled", true))
            fill(new ItemBuilder(mat(g.material("filler"))).name(" ").hideAll().build());

        // Border
        if (g.getBool("border.enabled", true)) {
            Material borderMat = mat(g.material("border"));
            List<Integer> borderSlots = g.intList("border.slots");
            if (borderSlots.isEmpty()) {
                fillBorderWith(borderMat);
            } else {
                for (int slot : borderSlots)
                    set(slot, new ItemBuilder(borderMat).name(" ").hideAll().build());
            }
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        MilestoneManager mm = plugin.getMilestoneManager();

        // Category items — slots and category order both come from config
        List<String> categories = MilestoneManager.CATEGORIES;
        List<Integer> catSlots  = g.intList("category-slots");
        for (int i = 0; i < categories.size(); i++) {
            int slot = i < catSlots.size() ? catSlots.get(i) : i + 10;
            set(slot, buildCategoryItem(mm, data, categories.get(i)));
        }

        // Header / progress item
        int totalDone      = data.getCompletedMilestones().size();
        int totalAll       = mm.getAllMilestones().size();
        int claimableCount = data.getClaimableMilestones().size();
        Map<String, String> ph = GuiUtil.ph(
                "milestones_done",  String.valueOf(totalDone),
                "milestones_total", String.valueOf(totalAll),
                "claimable_count",  String.valueOf(claimableCount)
        );
        set(g.slot("header-item"), new ItemBuilder(mat(g.material("header-item")))
                .name(g.str("header-item.name", ph))
                .lore(g.lore("header-item.lore", ph))
                .hideAll().build());

        // Close
        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
                .name(g.str("close-button.name"))
                .lore(g.lore("close-button.lore"))
                .hideAll().build());

        buildPlaceholders(g);
    }

    private ItemStack buildCategoryItem(MilestoneManager mm, PlayerData data, String category) {
        String nextId = mm.getNextForCategory(data, category);
        int done  = mm.countCompleted(data, category);
        int total = mm.countTotal(category);

        // All done in this category
        if (nextId == null) {
            Map<String, String> ph = GuiUtil.ph(
                    "category", g.str("category-labels." + category.toLowerCase()),
                    "done", String.valueOf(done), "total", String.valueOf(total)
            );
            return new ItemBuilder(mat(g.material("category-complete")))
                    .name(g.str("category-complete.name", ph))
                    .lore(g.lore("category-complete.lore", ph))
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

        ItemStack item = new ItemBuilder(icon).name(name).lore(lore).hideAll().build();
        if (claimable) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> buildMilestoneLore(MilestoneManager.MilestoneEntry entry,
                                            PlayerData data, int done, int total, boolean claimable) {
        String categoryLabel = g.str("category-labels." + entry.type().toLowerCase());
        long current = getStatValue(data, entry.type());

        Map<String, String> ph = GuiUtil.ph(
                "category",    categoryLabel,
                "done",        String.valueOf(done),
                "total",       String.valueOf(total),
                "current",     ColorUtil.formatNumber(current),
                "target",      ColorUtil.formatNumber(entry.target()),
                "reward_scrap",   entry.rewardScrap()  > 0 ? ColorUtil.formatNumber(entry.rewardScrap())  : "",
                "reward_screws",  entry.rewardScrews() > 0 ? ColorUtil.formatNumber(entry.rewardScrews()) : "",
                "reward_energy",  entry.rewardEnergy() > 0 ? ColorUtil.formatNumber(entry.rewardEnergy()) : "",
                "reward_bio",     entry.rewardBio()    > 0 ? ColorUtil.formatNumber(entry.rewardBio())    : "",
                "reward_tech",    entry.rewardTech()   > 0 ? ColorUtil.formatNumber(entry.rewardTech())   : "",
                "reward_points",  String.valueOf(entry.rewardPoints())
        );

        List<String> lore = new ArrayList<>(g.lore("milestone-lore-header", ph));

        // Description lines come from milestones.yml, passed through colour
        entry.description().stream().map(ColorUtil::color).forEach(lore::add);
        lore.addAll(g.lore("milestone-lore-progress", ph));

        // Rewards — only add lines where the reward is > 0
        lore.addAll(g.lore("milestone-lore-rewards-header", ph));
        if (entry.rewardScrap()  > 0) lore.add(g.str("milestone-lore-reward-scrap",  ph));
        if (entry.rewardScrews() > 0) lore.add(g.str("milestone-lore-reward-screws", ph));
        if (entry.rewardEnergy() > 0) lore.add(g.str("milestone-lore-reward-energy", ph));
        if (entry.rewardBio()    > 0) lore.add(g.str("milestone-lore-reward-bio",    ph));
        if (entry.rewardTech()   > 0) lore.add(g.str("milestone-lore-reward-tech",   ph));
        if (entry.rewardPoints() > 0) lore.add(g.str("milestone-lore-reward-points", ph));

        lore.addAll(claimable
                ? g.lore("milestone-lore-footer-claimable", ph)
                : g.lore("milestone-lore-footer-locked",    ph));

        return lore.stream().map(ColorUtil::color).collect(Collectors.toList());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;

        if (slot == g.slot("close-button")) { player.closeInventory(); return; }

        List<String> categories = MilestoneManager.CATEGORIES;
        List<Integer> catSlots  = g.intList("category-slots");

        for (int i = 0; i < catSlots.size(); i++) {
            if (slot != catSlots.get(i) || i >= categories.size()) continue;
            PlayerData data = plugin.getPlayerDataManager().get(player);
            MilestoneManager mm = plugin.getMilestoneManager();
            String nextId = mm.getNextForCategory(data, categories.get(i));
            if (nextId == null) return;
            if (data.getClaimableMilestones().contains(nextId)) {
                mm.claim(player, data, nextId);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                build();
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
            return;
        }
    }

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

    private void fillBorderWith(Material mat) {
        int size = inventory.getSize(); int rows = size / 9;
        for (int i = 0; i < 9; i++) set(i, new ItemBuilder(mat).name(" ").hideAll().build());
        for (int i = size - 9; i < size; i++) set(i, new ItemBuilder(mat).name(" ").hideAll().build());
        for (int r = 1; r < rows - 1; r++) {
            set(r * 9,     new ItemBuilder(mat).name(" ").hideAll().build());
            set(r * 9 + 8, new ItemBuilder(mat).name(" ").hideAll().build());
        }
    }

    private Material mat(String n) { try { return Material.valueOf(n); } catch (Exception e) { return Material.PAPER; } }
}