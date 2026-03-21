package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;

public class MainMenuGUI extends BaseGUI {

    private final GuiUtil g;

    public MainMenuGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
            new GuiUtil(plugin, "main-menu").title(),
            new GuiUtil(plugin, "main-menu").rows());
        this.g = new GuiUtil(plugin, "main-menu");
    }

    @Override
    protected void build() {
        fillBorder();
        fill(ItemBuilder.filler());

        PlayerData data = plugin.getPlayerDataManager().get(player);
        var lm = plugin.getLevelManager();
        var em = plugin.getExtractionManager();

        Map<String, String> ph = GuiUtil.ph(
            "player",           player.getName(),
            "level",            lm.getLevelColored(data.getLevel()),
            "progress",         String.format("%.1f", lm.getProgressPercent(data)),
            "scrap",            ColorUtil.formatNumber(data.getScrap()),
            "screws",           ColorUtil.formatNumber(data.getScrews()),
            "energy",           ColorUtil.formatNumber(data.getEnergyCells()),
            "bio",              ColorUtil.formatNumber(data.getBioSamples()),
            "tech",             ColorUtil.formatNumber(data.getTechShards()),
            "sword_t",          String.valueOf(data.getSwordTier()),
            "pickaxe_t",        String.valueOf(data.getPickaxeTier()),
            "axe_t",            String.valueOf(data.getAxeTier()),
            "helmet_t",         String.valueOf(data.getHelmetTier()),
            "speed_lvl",        String.valueOf(data.getCoreSpeedLevel()),
            "prod_lvl",         String.valueOf(data.getCoreProductionLevel()),
            "core_x",           String.valueOf(data.getCoreX()),
            "core_y",           String.valueOf(data.getCoreY()),
            "core_z",           String.valueOf(data.getCoreZ()),
            "milestones_done",  String.valueOf(data.getCompletedMilestones().size()),
            "next_extraction",  ColorUtil.formatDuration(em.getCountdownSeconds()),
            "current_extraction", ColorUtil.formatDuration(em.getActiveSecondsLeft()),
            "zone_name",        em.getActiveZone() != null ? em.getActiveZone().getName() : "?"
        );

        // Player overview
        set(g.slot("player-overview"), new ItemBuilder(Material.PLAYER_HEAD)
            .name(g.str("player-overview.name", ph))
            .lore(g.lore("player-overview.lore", ph))
            .hideAll().build());

        // Armory button
        set(g.slot("armory-button"), new ItemBuilder(Material.DIAMOND_SWORD)
            .name(g.str("armory-button.name", ph))
            .lore(g.lore("armory-button.lore", ph))
            .hideAll().build());

        // Core button — pick right status line
        String coreStatusLine = data.isCorePlaced()
            ? g.str("core-button.core-placed-line", ph)
            : g.str("core-button.core-not-placed-line", ph);
        List<String> coreLore = g.lore("core-button.lore", ph);
        // Replace the placeholder line index that contains {core_x} or "not placed"
        coreLore = coreLore.stream()
            .map(l -> (l.contains("{core_x}") || l.contains("Core placed") || l.contains("Core not placed"))
                ? coreStatusLine : l)
            .collect(java.util.stream.Collectors.toList());
        // Remove duplicate (the lore has both lines; filter out the wrong one)
        final boolean placed = data.isCorePlaced();
        coreLore.removeIf(l -> placed
            ? ColorUtil.strip(l).contains("Core not placed")
            : ColorUtil.strip(l).contains("Core placed at"));
        set(g.slot("core-button"), new ItemBuilder(Material.BEACON)
            .name(g.str("core-button.name", ph))
            .lore(coreLore)
            .hideAll().build());

        // Stats button
        set(g.slot("stats-button"), new ItemBuilder(Material.PAPER)
            .name(g.str("stats-button.name", ph))
            .lore(g.lore("stats-button.lore", ph))
            .hideAll().build());

        // Exchange button
        set(g.slot("exchange-button"), new ItemBuilder(Material.EMERALD)
            .name(g.str("exchange-button.name", ph))
            .lore(g.lore("exchange-button.lore", ph))
            .hideAll().build());

        // Milestones button
        set(g.slot("milestones-button"), new ItemBuilder(Material.NETHER_STAR)
            .name(g.str("milestones-button.name", ph))
            .lore(g.lore("milestones-button.lore", ph))
            .hideAll().build());

        // Extraction status
        String exStatus = em.isExtractionActive()
            ? g.str("extraction-status.status-active", ph)
            : g.str("extraction-status.status-waiting", ph);
        Map<String, String> exPh = new java.util.HashMap<>(ph);
        exPh.put("extraction_status", exStatus);
        set(g.slot("extraction-status"), new ItemBuilder(Material.ENDER_CHEST)
            .name(g.str("extraction-status.name", ph))
            .lore(g.lore("extraction-status.lore", exPh))
            .hideAll().build());

        // Close
        set(g.slot("close-button"), new ItemBuilder(
            mat(g.material("close-button")))
            .name(g.str("close-button.name"))
            .lore(g.lore("close-button.lore"))
            .hideAll().build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int armorySlot     = g.slot("armory-button");
        int coreSlot       = g.slot("core-button");
        int statsSlot      = g.slot("stats-button");
        int exchangeSlot   = g.slot("exchange-button");
        int milestonesSlot = g.slot("milestones-button");
        int closeSlot      = g.slot("close-button");

        if (slot == closeSlot)      { player.closeInventory(); }
        else if (slot == armorySlot)     { player.closeInventory(); new ArmoryGUI(plugin, player).open(); }
        else if (slot == statsSlot)      { player.closeInventory(); new StatsGUI(plugin, player).open(); }
        else if (slot == exchangeSlot)   { player.closeInventory(); new ExchangeGUI(plugin, player).open(); }
        else if (slot == milestonesSlot) { player.closeInventory(); new MilestonesGUI(plugin, player).open(); }
        else if (slot == coreSlot) {
            if (plugin.getPlayerDataManager().get(player).isCorePlaced()) {
                player.closeInventory();
                new CoreGUI(plugin, player).open();
            } else {
                player.sendMessage(g.str("no-core-message"));
            }
        }
    }

    private Material mat(String name) {
        try { return Material.valueOf(name); } catch (Exception e) { return Material.BARRIER; }
    }
}
