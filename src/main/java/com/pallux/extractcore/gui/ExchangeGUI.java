package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;

public class ExchangeGUI extends BaseGUI {

    private final GuiUtil g;

    public ExchangeGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
                new GuiUtil(plugin, "exchange").title(),
                new GuiUtil(plugin, "exchange").rows());
        this.g = new GuiUtil(plugin, "exchange");
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
        var em = plugin.getExchangeManager();

        Map<String, String> infoPh = GuiUtil.ph("scrap", ColorUtil.formatNumber(data.getScrap()));
        set(g.slot("info-item"), new ItemBuilder(mat(g.material("info-item")))
                .name(g.str("info-item.name", infoPh))
                .lore(g.lore("info-item.lore", infoPh))
                .hideAll().build());

        ConfigurationSection mats = plugin.getConfigManager().getGuiConfig()
                .getConfigurationSection("exchange.materials");
        if (mats != null) {
            for (String key : mats.getKeys(false)) {
                String path = "materials." + key;
                long cost1  = em.getCostInScrap(key, 1);
                Map<String, String> ph = GuiUtil.ph(
                        "cost_1",  ColorUtil.formatNumber(cost1),
                        "cost_10", ColorUtil.formatNumber(cost1 * 10),
                        "cost_64", ColorUtil.formatNumber(cost1 * 64)
                );
                set(g.slot(path), new ItemBuilder(mat(g.material(path)))
                        .name(g.str(path + ".name", ph))
                        .lore(g.lore(path + ".lore", ph))
                        .hideAll().build());
            }
        }

        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
                .name(g.str("close-button.name"))
                .lore(g.lore("close-button.lore"))
                .hideAll().build());

        buildPlaceholders(g);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;
        if (slot == g.slot("close-button")) { player.closeInventory(); return; }

        ConfigurationSection mats = plugin.getConfigManager().getGuiConfig()
                .getConfigurationSection("exchange.materials");
        if (mats == null) return;

        for (String key : mats.getKeys(false)) {
            if (slot == g.slot("materials." + key)) {
                long amount = 1;
                if (event.getClick() == ClickType.RIGHT) amount = 10;
                if (event.isShiftClick()) amount = 64;
                plugin.getExchangeManager().purchase(player, key, amount);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                build();
                return;
            }
        }
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

    private Material mat(String n) { try { return Material.valueOf(n); } catch (Exception e) { return Material.BARRIER; } }
}