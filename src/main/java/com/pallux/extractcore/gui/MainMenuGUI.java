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
import java.util.stream.Collectors;

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
        // Filler
        if (g.getBool("filler.enabled", true)) {
            fill(new ItemBuilder(mat(g.material("filler"))).name(" ").hideAll().build());
        }

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

        // Player data
        PlayerData data = plugin.getPlayerDataManager().get(player);
        var lm = plugin.getLevelManager();
        var em = plugin.getExtractionManager();

        Map<String, String> ph = GuiUtil.ph(
                "player",             player.getName(),
                "level",              lm.getLevelColored(data.getLevel()),
                "progress",           String.format("%.1f", lm.getProgressPercent(data)),
                "scrap",              ColorUtil.formatNumber(data.getScrap()),
                "screws",             ColorUtil.formatNumber(data.getScrews()),
                "energy",             ColorUtil.formatNumber(data.getEnergyCells()),
                "bio",                ColorUtil.formatNumber(data.getBioSamples()),
                "tech",               ColorUtil.formatNumber(data.getTechShards()),
                "sword_t",            String.valueOf(data.getSwordTier()),
                "pickaxe_t",          String.valueOf(data.getPickaxeTier()),
                "axe_t",              String.valueOf(data.getAxeTier()),
                "helmet_t",           String.valueOf(data.getHelmetTier()),
                "speed_lvl",          String.valueOf(data.getCoreSpeedLevel()),
                "prod_lvl",           String.valueOf(data.getCoreProductionLevel()),
                "core_x",             String.valueOf(data.getCoreX()),
                "core_y",             String.valueOf(data.getCoreY()),
                "core_z",             String.valueOf(data.getCoreZ()),
                "milestones_done",    String.valueOf(data.getCompletedMilestones().size()),
                "next_extraction",    ColorUtil.formatDuration(em.getCountdownSeconds()),
                "current_extraction", ColorUtil.formatDuration(em.getActiveSecondsLeft()),
                "zone_name",          em.getActiveZone() != null ? em.getActiveZone().getName() : "?"
        );

        // Player overview
        if (g.getBool("player-overview.enabled", true))
            set(g.slot("player-overview"), new ItemBuilder(Material.PLAYER_HEAD)
                    .name(g.str("player-overview.name", ph))
                    .lore(g.lore("player-overview.lore", ph))
                    .hideAll().build());

        // Armory
        if (g.getBool("armory-button.enabled", true))
            set(g.slot("armory-button"), new ItemBuilder(mat(g.material("armory-button")))
                    .name(g.str("armory-button.name", ph))
                    .lore(g.lore("armory-button.lore", ph))
                    .hideAll().build());

        // Core
        if (g.getBool("core-button.enabled", true)) {
            String statusLine = data.isCorePlaced()
                    ? g.str("core-button.core-placed-line", ph)
                    : g.str("core-button.core-not-placed-line", ph);
            final boolean placed = data.isCorePlaced();
            List<String> coreLore = g.lore("core-button.lore", ph).stream()
                    .map(l -> (l.contains("{core_x}") || ColorUtil.strip(l).contains("Core placed")
                            || ColorUtil.strip(l).contains("Core not placed")) ? statusLine : l)
                    .filter(l -> placed
                            ? !ColorUtil.strip(l).contains("Core not placed")
                            : !ColorUtil.strip(l).contains("Core placed at"))
                    .collect(Collectors.toList());
            set(g.slot("core-button"), new ItemBuilder(mat(g.material("core-button")))
                    .name(g.str("core-button.name", ph))
                    .lore(coreLore)
                    .hideAll().build());
        }

        // Exchange
        if (g.getBool("exchange-button.enabled", true))
            set(g.slot("exchange-button"), new ItemBuilder(mat(g.material("exchange-button")))
                    .name(g.str("exchange-button.name", ph))
                    .lore(g.lore("exchange-button.lore", ph))
                    .hideAll().build());

        // Shop
        if (g.getBool("shop-button.enabled", true))
            set(g.slot("shop-button"), new ItemBuilder(mat(g.material("shop-button")))
                    .name(g.str("shop-button.name", ph))
                    .lore(g.lore("shop-button.lore", ph))
                    .hideAll().build());

        // Extraction status
        if (g.getBool("extraction-status.enabled", true)) {
            Map<String, String> exPh = new java.util.HashMap<>(ph);
            exPh.put("extraction_status", em.isExtractionActive()
                    ? g.str("extraction-status.status-active", ph)
                    : g.str("extraction-status.status-waiting", ph));
            set(g.slot("extraction-status"), new ItemBuilder(mat(g.material("extraction-status")))
                    .name(g.str("extraction-status.name", ph))
                    .lore(g.lore("extraction-status.lore", exPh))
                    .hideAll().build());
        }

        // Milestones
        if (g.getBool("milestones-button.enabled", true))
            set(g.slot("milestones-button"), new ItemBuilder(mat(g.material("milestones-button")))
                    .name(g.str("milestones-button.name", ph))
                    .lore(g.lore("milestones-button.lore", ph))
                    .hideAll().build());

        // Stats
        if (g.getBool("stats-button.enabled", true))
            set(g.slot("stats-button"), new ItemBuilder(mat(g.material("stats-button")))
                    .name(g.str("stats-button.name", ph))
                    .lore(g.lore("stats-button.lore", ph))
                    .hideAll().build());

        // Close
        if (g.getBool("close-button.enabled", true))
            set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
                    .name(g.str("close-button.name"))
                    .lore(g.lore("close-button.lore"))
                    .hideAll().build());

        // Placeholders — rendered last
        buildPlaceholders(g);
    }

    private void fillBorderWith(Material mat) {
        int size = inventory.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++)
            set(i, new ItemBuilder(mat).name(" ").hideAll().build());
        for (int i = size - 9; i < size; i++)
            set(i, new ItemBuilder(mat).name(" ").hideAll().build());
        for (int r = 1; r < rows - 1; r++) {
            set(r * 9,     new ItemBuilder(mat).name(" ").hideAll().build());
            set(r * 9 + 8, new ItemBuilder(mat).name(" ").hideAll().build());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;

        if (g.getBool("close-button.enabled", true)      && slot == g.slot("close-button"))      { player.closeInventory(); return; }
        if (g.getBool("armory-button.enabled", true)     && slot == g.slot("armory-button"))     { player.closeInventory(); new ArmoryGUI(plugin, player).open(); return; }
        if (g.getBool("stats-button.enabled", true)      && slot == g.slot("stats-button"))      { player.closeInventory(); new StatsGUI(plugin, player).open(); return; }
        if (g.getBool("exchange-button.enabled", true)   && slot == g.slot("exchange-button"))   { player.closeInventory(); new ExchangeGUI(plugin, player).open(); return; }
        if (g.getBool("milestones-button.enabled", true) && slot == g.slot("milestones-button")) { player.closeInventory(); new MilestonesGUI(plugin, player).open(); return; }
        if (g.getBool("shop-button.enabled", true)       && slot == g.slot("shop-button"))       { player.closeInventory(); new ShopGUI(plugin, player).open(); return; }
        if (g.getBool("core-button.enabled", true)       && slot == g.slot("core-button")) {
            if (plugin.getPlayerDataManager().get(player).isCorePlaced()) {
                player.closeInventory(); new CoreGUI(plugin, player).open();
            } else {
                player.sendMessage(g.str("no-core-message"));
            }
        }
    }

    private Material mat(String name) {
        try { return Material.valueOf(name); } catch (Exception e) { return Material.BARRIER; }
    }
}