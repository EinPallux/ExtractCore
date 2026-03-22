package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.shop.ShopEntry;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shop category screen — shows all items in a given category.
 *
 * Click behaviour:
 *   Left-click  → buy ×1
 *   Right-click → buy ×8
 *   Shift+click → buy ×64  (normal blocks only; defense blocks always ×1)
 */
public class ShopCategoryGUI extends BaseGUI {

    public enum Category { NORMAL, DEFENSE }

    private final Category category;
    private final GuiUtil g;
    private final String cfgKey;

    public ShopCategoryGUI(ExtractCore plugin, Player player, Category category) {
        super(plugin, player,
                resolveTitle(plugin, category),
                new GuiUtil(plugin, "shop-" + catKey(category)).rows());
        this.category = category;
        this.cfgKey   = "shop-" + catKey(category);
        this.g        = new GuiUtil(plugin, cfgKey);
    }

    private static String catKey(Category c) {
        return c == Category.NORMAL ? "normal" : "defense";
    }

    private static String resolveTitle(ExtractCore plugin, Category c) {
        return new GuiUtil(plugin, "shop-" + catKey(c)).title();
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
            if (borderSlots.isEmpty()) fillBorderWith(borderMat);
            else for (int s : borderSlots) set(s, new ItemBuilder(borderMat).name(" ").hideAll().build());
        }

        // Player scrap
        PlayerData data = plugin.getPlayerDataManager().get(player);
        Map<String, String> ph = GuiUtil.ph("scrap", ColorUtil.formatNumber(data.getScrap()));

        set(g.slot("scrap-display"), new ItemBuilder(mat(g.material("scrap-display")))
                .name(g.str("scrap-display.name", ph))
                .lore(g.lore("scrap-display.lore", ph))
                .hideAll().build());

        set(g.slot("back-button"), new ItemBuilder(mat(g.material("back-button")))
                .name(g.str("back-button.name"))
                .lore(g.lore("back-button.lore"))
                .hideAll().build());

        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
                .name(g.str("close-button.name"))
                .lore(g.lore("close-button.lore"))
                .hideAll().build());

        // Shop items
        List<ShopEntry> entries = category == Category.NORMAL
                ? plugin.getShopManager().getNormalBlocks()
                : plugin.getShopManager().getDefenseBlocks();

        List<Integer> itemSlots = g.intList("item-slots");
        for (int i = 0; i < entries.size(); i++) {
            int slot = (i < itemSlots.size()) ? itemSlots.get(i) : -1;
            if (slot < 0 || slot >= inventory.getSize()) continue;
            set(slot, buildShopItem(entries.get(i), data));
        }

        buildPlaceholders(g);
    }

    private org.bukkit.inventory.ItemStack buildShopItem(ShopEntry entry, PlayerData data) {
        Material mat;
        try { mat = Material.valueOf(entry.material()); } catch (Exception e) { mat = Material.STONE; }

        boolean canAfford = data.getScrap() >= entry.costScrap();

        List<String> processedLore = new ArrayList<>();
        for (String line : entry.lore()) {
            processedLore.add(line
                    .replace("{hp}",     String.valueOf(entry.hp()))
                    .replace("{cost}",   ColorUtil.formatNumber(entry.costScrap()))
                    .replace("{amount}", String.valueOf(entry.amount())));
        }

        processedLore.addAll(g.lore("buy-hint-lore", GuiUtil.ph(
                "cost_1",  ColorUtil.formatNumber(entry.costScrap()),
                "cost_8",  ColorUtil.formatNumber(entry.costScrap() * 8),
                "cost_64", ColorUtil.formatNumber(entry.costScrap() * 64)
        )));
        processedLore.add(canAfford ? g.str("can-afford") : g.str("cant-afford"));

        return new ItemBuilder(mat)
                .name(entry.displayName())
                .lore(processedLore)
                .hideAll()
                .build();
    }

    // ── Sound helper ──────────────────────────────────────────────────────────

    private void playSound(String configKey, String defaultSound, float defaultVolume, float defaultPitch) {
        String soundName = plugin.getConfigManager().getGuiConfig()
                .getString(cfgKey + ".sounds." + configKey, defaultSound);
        float volume = (float) plugin.getConfigManager().getGuiConfig()
                .getDouble(cfgKey + ".sounds." + configKey + "-volume", defaultVolume);
        float pitch  = (float) plugin.getConfigManager().getGuiConfig()
                .getDouble(cfgKey + ".sounds." + configKey + "-pitch",  defaultPitch);
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    // ── Click handler ─────────────────────────────────────────────────────────

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;

        if (slot == g.slot("close-button")) { player.closeInventory(); return; }
        if (slot == g.slot("back-button"))  {
            player.closeInventory();
            new ShopGUI(plugin, player).open();
            return;
        }

        List<ShopEntry> entries = category == Category.NORMAL
                ? plugin.getShopManager().getNormalBlocks()
                : plugin.getShopManager().getDefenseBlocks();

        List<Integer> itemSlots = g.intList("item-slots");
        for (int i = 0; i < itemSlots.size(); i++) {
            if (slot != itemSlots.get(i) || i >= entries.size()) continue;

            ShopEntry entry = entries.get(i);

            int amount = 1;
            if (event.getClick() == ClickType.RIGHT) amount = 8;
            if (event.isShiftClick())                amount = 64;
            if (category == Category.DEFENSE)        amount = 1;

            boolean success = plugin.getShopManager().purchase(player, entry, amount);
            if (success) {
                playSound("purchase-success", "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.2f);
                build();
            } else {
                playSound("purchase-fail", "BLOCK_ANVIL_LAND", 0.5f, 0.8f);
            }
            return;
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