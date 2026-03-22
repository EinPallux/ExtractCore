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

/**
 * Main shop menu — shows the two category buttons.
 * Styled to match the screenshot: grey filler, black border, items centred.
 */
public class ShopGUI extends BaseGUI {

    private final GuiUtil g;

    public ShopGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
                new GuiUtil(plugin, "shop").title(),
                new GuiUtil(plugin, "shop").rows());
        this.g = new GuiUtil(plugin, "shop");
    }

    @Override
    protected void build() {
        // Filler
        if (g.getBool("filler.enabled", true))
            fill(new ItemBuilder(mat(g.material("filler"))).name(" ").hideAll().build());

        // Border
        if (g.getBool("border.enabled", true)) {
            Material borderMat = mat(g.material("border"));
            for (int slot : g.intList("border.slots"))
                set(slot, new ItemBuilder(borderMat).name(" ").hideAll().build());
            if (g.intList("border.slots").isEmpty()) fillBorderWith(borderMat);
        }

        // Player scrap display
        PlayerData data = plugin.getPlayerDataManager().get(player);
        Map<String, String> ph = GuiUtil.ph("scrap", ColorUtil.formatNumber(data.getScrap()));

        set(g.slot("scrap-display"), new ItemBuilder(mat(g.material("scrap-display")))
                .name(g.str("scrap-display.name", ph))
                .lore(g.lore("scrap-display.lore", ph))
                .hideAll().build());

        // Normal blocks category button
        set(g.slot("normal-blocks-button"), new ItemBuilder(mat(g.material("normal-blocks-button")))
                .name(g.str("normal-blocks-button.name"))
                .lore(g.lore("normal-blocks-button.lore"))
                .hideAll().build());

        // Defense blocks category button
        set(g.slot("defense-blocks-button"), new ItemBuilder(mat(g.material("defense-blocks-button")))
                .name(g.str("defense-blocks-button.name"))
                .lore(g.lore("defense-blocks-button.lore"))
                .hideAll().build());

        // Close button
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

        if (slot == g.slot("close-button")) {
            player.closeInventory();
        } else if (slot == g.slot("normal-blocks-button")) {
            player.closeInventory();
            new ShopCategoryGUI(plugin, player, ShopCategoryGUI.Category.NORMAL).open();
        } else if (slot == g.slot("defense-blocks-button")) {
            player.closeInventory();
            new ShopCategoryGUI(plugin, player, ShopCategoryGUI.Category.DEFENSE).open();
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

    private Material mat(String n) {
        try { return Material.valueOf(n); } catch (Exception e) { return Material.BARRIER; }
    }
}