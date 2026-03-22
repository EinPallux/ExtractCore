package com.pallux.extractcore.shop;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads shop entries from shop.yml and processes purchases.
 *
 * Two categories:
 *   normal-blocks  – standard placeable blocks that auto-despawn
 *   defense-blocks – custom HP blocks loaded from defense-blocks.yml
 */
public class ShopManager {

    private final ExtractCore plugin;

    /** PDC key stamped on every shop-block item so we can identify them. */
    public final NamespacedKey SHOP_BLOCK_KEY;
    /** PDC key for defense blocks — value = block ID string from config. */
    public final NamespacedKey DEFENSE_BLOCK_KEY;

    // category id → list of entries
    private final Map<String, List<ShopEntry>> categories = new LinkedHashMap<>();

    public ShopManager(ExtractCore plugin) {
        this.plugin = plugin;
        this.SHOP_BLOCK_KEY    = new NamespacedKey(plugin, "extractcore_shop_block");
        this.DEFENSE_BLOCK_KEY = new NamespacedKey(plugin, "extractcore_defense_block");
        reload();
    }

    public void reload() {
        categories.clear();
        loadNormalBlocks();
        loadDefenseBlocks();
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private void loadNormalBlocks() {
        FileConfiguration cfg = plugin.getConfigManager().getShopConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("normal-blocks");
        if (sec == null) return;
        List<ShopEntry> entries = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            String path = "normal-blocks." + key;
            entries.add(new ShopEntry(
                    key,
                    ShopEntry.Category.NORMAL,
                    cfg.getString(path + ".material", "STONE"),
                    cfg.getString(path + ".display-name", key),
                    cfg.getStringList(path + ".lore"),
                    cfg.getLong(path + ".cost-scrap", 100),
                    cfg.getInt(path + ".amount", 1),
                    0, // no HP for normal blocks
                    cfg.getInt(path + ".gui-slot", entries.size())
            ));
        }
        categories.put("normal-blocks", entries);
    }

    private void loadDefenseBlocks() {
        FileConfiguration cfg = plugin.getConfigManager().getDefenseBlocksConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("defense-blocks");
        if (sec == null) return;
        List<ShopEntry> entries = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            String path = "defense-blocks." + key;
            entries.add(new ShopEntry(
                    key,
                    ShopEntry.Category.DEFENSE,
                    cfg.getString(path + ".material", "COBBLESTONE"),
                    cfg.getString(path + ".display-name", key),
                    cfg.getStringList(path + ".lore"),
                    cfg.getLong(path + ".cost-scrap", 500),
                    cfg.getInt(path + ".amount", 1),
                    cfg.getInt(path + ".hp", 25),
                    cfg.getInt(path + ".gui-slot", entries.size())
            ));
        }
        categories.put("defense-blocks", entries);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public List<ShopEntry> getNormalBlocks()  { return categories.getOrDefault("normal-blocks",  List.of()); }
    public List<ShopEntry> getDefenseBlocks() { return categories.getOrDefault("defense-blocks", List.of()); }

    public ShopEntry getDefenseEntry(String id) {
        return getDefenseBlocks().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }

    public ShopEntry getNormalEntry(String id) {
        return getNormalBlocks().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    /**
     * Attempts to purchase amount units of the entry for the player.
     * Deducts scrap and gives the item(s) on success.
     */
    public boolean purchase(Player player, ShopEntry entry, int amount) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        long totalCost = entry.costScrap() * amount;

        if (data.getScrap() < totalCost) {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("shop.not-enough-scrap",
                            "&cNot enough Scrap. You need &e{cost} Scrap&c.")
                    .replace("{cost}", ColorUtil.formatNumber(totalCost));
            player.sendMessage(ColorUtil.color(msg));
            return false;
        }

        // Check inventory space
        int stackSize = entry.amount() * amount;
        if (player.getInventory().firstEmpty() == -1) {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("shop.inventory-full", "&cYour inventory is full.");
            player.sendMessage(ColorUtil.color(msg));
            return false;
        }

        data.setScrap(data.getScrap() - totalCost);
        plugin.getPlayerDataManager().saveAsync(data);

        // Give item(s)
        ItemStack item = buildItem(entry);
        item.setAmount(Math.min(stackSize, item.getMaxStackSize()));
        player.getInventory().addItem(item);

        String msg = plugin.getConfigManager().getMessages()
                .getString("shop.purchased",
                        "&#5B8DD9Purchased &e{amount}x {item} &7for &e{cost} Scrap&8.")
                .replace("{amount}", String.valueOf(stackSize))
                .replace("{item}", ColorUtil.strip(ColorUtil.color(entry.displayName())))
                .replace("{cost}", ColorUtil.formatNumber(totalCost));
        player.sendMessage(ColorUtil.color(msg));
        return true;
    }

    // ── Item Builder ──────────────────────────────────────────────────────────

    /**
     * Builds a tagged ItemStack for the given shop entry.
     * Defense blocks carry their ID in PDC; normal blocks carry "normal".
     */
    public ItemStack buildItem(ShopEntry entry) {
        Material mat;
        try { mat = Material.valueOf(entry.material()); }
        catch (Exception e) { mat = Material.STONE; }

        ItemStack item = new ItemStack(mat, entry.amount());
        ItemMeta meta = item.getItemMeta();

        // Display name
        meta.displayName(ColorUtil.component(entry.displayName()));

        // Lore
        List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
        for (String line : entry.lore()) {
            loreComponents.add(ColorUtil.component(
                    line.replace("{hp}", String.valueOf(entry.hp()))
                            .replace("{cost}", ColorUtil.formatNumber(entry.costScrap()))
                            .replace("{amount}", String.valueOf(entry.amount()))
            ));
        }
        meta.lore(loreComponents);

        // PDC tagging
        if (entry.category() == ShopEntry.Category.DEFENSE) {
            meta.getPersistentDataContainer().set(DEFENSE_BLOCK_KEY, PersistentDataType.STRING, entry.id());
        } else {
            meta.getPersistentDataContainer().set(SHOP_BLOCK_KEY, PersistentDataType.STRING, entry.id());
        }

        // Hide attributes for clean look
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    // ── PDC Checks ────────────────────────────────────────────────────────────

    /** Returns true if the item is any shop block (normal or defense). */
    public boolean isShopBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(SHOP_BLOCK_KEY, PersistentDataType.STRING)
                || pdc.has(DEFENSE_BLOCK_KEY, PersistentDataType.STRING);
    }

    /** Returns true if the item is a defense block. */
    public boolean isDefenseBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(DEFENSE_BLOCK_KEY, PersistentDataType.STRING);
    }

    /** Returns true if the item is a normal shop block. */
    public boolean isNormalShopBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(SHOP_BLOCK_KEY, PersistentDataType.STRING);
    }

    /** Returns the defense block ID from the item, or null. */
    public String getDefenseBlockId(ItemStack item) {
        if (!isDefenseBlock(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(DEFENSE_BLOCK_KEY, PersistentDataType.STRING);
    }

    /** Returns the normal block entry ID from the item, or null. */
    public String getNormalBlockId(ItemStack item) {
        if (!isNormalShopBlock(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(SHOP_BLOCK_KEY, PersistentDataType.STRING);
    }
}