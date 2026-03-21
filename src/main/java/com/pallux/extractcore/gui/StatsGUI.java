package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class StatsGUI extends BaseGUI {

    private final GuiUtil g;

    public StatsGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
            new GuiUtil(plugin, "stats").title(GuiUtil.ph("player", player.getName())),
            new GuiUtil(plugin, "stats").rows());
        this.g = new GuiUtil(plugin, "stats");
    }

    @Override
    protected void build() {
        fillBorder();
        fill(ItemBuilder.filler());

        PlayerData data = plugin.getPlayerDataManager().get(player);
        var lm = plugin.getLevelManager();

        Map<String, String> ph = GuiUtil.ph(
            "player",         player.getName(),
            "level",          lm.getLevelColored(data.getLevel()),
            "xp_raw",         ColorUtil.formatNumber(data.getXp()),
            "xp_needed",      ColorUtil.formatNumber(lm.xpRequired(data.getLevel())),
            "progress",       String.format("%.1f", lm.getProgressPercent(data)),
            "scrap",          ColorUtil.formatNumber(data.getScrap()),
            "screws",         ColorUtil.formatNumber(data.getScrews()),
            "energy",         ColorUtil.formatNumber(data.getEnergyCells()),
            "bio",            ColorUtil.formatNumber(data.getBioSamples()),
            "tech",           ColorUtil.formatNumber(data.getTechShards()),
            "kills",          String.valueOf(data.getKills()),
            "deaths",         String.valueOf(data.getDeaths()),
            "kd",             String.format("%.2f", data.getKD()),
            "cores_destroyed",String.valueOf(data.getCoresDestroyed()),
            "extractions",    String.valueOf(data.getExtractions()),
            "playtime_h",     String.valueOf(data.getPlaytimeMinutes() / 60),
            "playtime_m",     String.valueOf(data.getPlaytimeMinutes() % 60),
            "milestones_done",String.valueOf(data.getCompletedMilestones().size()),
            "sword_t",        String.valueOf(data.getSwordTier()),
            "pickaxe_t",      String.valueOf(data.getPickaxeTier()),
            "axe_t",          String.valueOf(data.getAxeTier()),
            "helmet_t",       String.valueOf(data.getHelmetTier()),
            "chest_t",        String.valueOf(data.getChestplateTier()),
            "legs_t",         String.valueOf(data.getLeggingsTier()),
            "boots_t",        String.valueOf(data.getBootsTier())
        );

        set(g.slot("level-item"),      build(g, "level-item",      ph));
        set(g.slot("materials-item"),  build(g, "materials-item",  ph));
        set(g.slot("combat-item"),     build(g, "combat-item",     ph));
        set(g.slot("extraction-item"), build(g, "extraction-item", ph));
        set(g.slot("playtime-item"),   build(g, "playtime-item",   ph));
        set(g.slot("gear-item"),       build(g, "gear-item",       ph));

        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
            .name(g.str("close-button.name"))
            .lore(g.lore("close-button.lore"))
            .hideAll().build());
    }

    private org.bukkit.inventory.ItemStack build(GuiUtil g, String key, Map<String, String> ph) {
        return new ItemBuilder(mat(g.material(key)))
            .name(g.str(key + ".name", ph))
            .lore(g.lore(key + ".lore", ph))
            .hideAll().build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == g.slot("close-button")) player.closeInventory();
    }

    private Material mat(String n) { try { return Material.valueOf(n); } catch (Exception e) { return Material.PAPER; } }
}
