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
        fillBorder();
        fill(ItemBuilder.filler());

        PlayerData data = plugin.getPlayerDataManager().get(player);
        var em = plugin.getExchangeManager();

        Map<String, String> infoPh = GuiUtil.ph("scrap", ColorUtil.formatNumber(data.getScrap()));
        set(g.slot("info-item"), new ItemBuilder(mat(g.material("info-item")))
            .name(g.str("info-item.name", infoPh))
            .lore(g.lore("info-item.lore", infoPh))
            .hideAll().build());

        // Each material entry from config
        ConfigurationSection mats = plugin.getConfigManager().getGuiConfig()
            .getConfigurationSection("exchange.materials");
        if (mats != null) {
            for (String key : mats.getKeys(false)) {
                String path = "materials." + key;
                int slot    = g.slot(path);
                long cost1  = em.getCostInScrap(key, 1);
                Map<String, String> ph = GuiUtil.ph(
                    "cost_1",  ColorUtil.formatNumber(cost1),
                    "cost_10", ColorUtil.formatNumber(cost1 * 10),
                    "cost_64", ColorUtil.formatNumber(cost1 * 64)
                );
                set(slot, new ItemBuilder(mat(g.material(path)))
                    .name(g.str(path + ".name", ph))
                    .lore(g.lore(path + ".lore", ph))
                    .hideAll().build());
            }
        }

        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
            .name(g.str("close-button.name"))
            .lore(g.lore("close-button.lore"))
            .hideAll().build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
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

    private Material mat(String n) { try { return Material.valueOf(n); } catch (Exception e) { return Material.BARRIER; } }
}
