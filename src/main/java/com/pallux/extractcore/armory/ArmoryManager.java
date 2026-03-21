package com.pallux.extractcore.armory;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/**
 * Manages Armory gear creation, integrity checks and upgrade logic.
 */
public class ArmoryManager {

    private final ExtractCore plugin;

    // PDC Keys for gear identification
    private final NamespacedKey swordKey;
    private final NamespacedKey pickaxeKey;
    private final NamespacedKey axeKey;
    private final NamespacedKey helmetKey;
    private final NamespacedKey chestplateKey;
    private final NamespacedKey leggingsKey;
    private final NamespacedKey bootsKey;

    public ArmoryManager(ExtractCore plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfigManager().getArmoryConfig();
        swordKey      = new NamespacedKey(plugin, cfg.getString("nbt-keys.sword",      "extractcore_sword"));
        pickaxeKey    = new NamespacedKey(plugin, cfg.getString("nbt-keys.pickaxe",    "extractcore_pickaxe"));
        axeKey        = new NamespacedKey(plugin, cfg.getString("nbt-keys.axe",        "extractcore_axe"));
        helmetKey     = new NamespacedKey(plugin, cfg.getString("nbt-keys.helmet",     "extractcore_helmet"));
        chestplateKey = new NamespacedKey(plugin, cfg.getString("nbt-keys.chestplate", "extractcore_chestplate"));
        leggingsKey   = new NamespacedKey(plugin, cfg.getString("nbt-keys.leggings",   "extractcore_leggings"));
        bootsKey      = new NamespacedKey(plugin, cfg.getString("nbt-keys.boots",      "extractcore_boots"));
    }

    public void reload() {
        // Keys are final; nothing to reload beyond what the config provides
    }

    // ── Item Creation ─────────────────────────────────────────────────────────

    public ItemStack createSword(int tier)      { return buildGear("sword",      tier, swordKey); }
    public ItemStack createPickaxe(int tier)    { return buildGear("pickaxe",    tier, pickaxeKey); }
    public ItemStack createAxe(int tier)        { return buildGear("axe",        tier, axeKey); }
    public ItemStack createHelmet(int tier)     { return buildGear("helmet",     tier, helmetKey); }
    public ItemStack createChestplate(int tier) { return buildGear("chestplate", tier, chestplateKey); }
    public ItemStack createLeggings(int tier)   { return buildGear("leggings",   tier, leggingsKey); }
    public ItemStack createBoots(int tier)      { return buildGear("boots",      tier, bootsKey); }

    private ItemStack buildGear(String type, int tier, NamespacedKey key) {
        FileConfiguration cfg = plugin.getConfigManager().getArmoryConfig();
        String path = type + ".tiers." + tier;

        String matStr = cfg.getString(path + ".material", "WOODEN_SWORD");
        Material mat;
        try { mat = Material.valueOf(matStr); }
        catch (Exception e) { mat = Material.WOODEN_SWORD; }

        String displayName = cfg.getString(path + ".display-name", "&7Unknown Item");
        List<String> lore  = cfg.getStringList(path + ".lore");
        boolean unbreakable = cfg.getBoolean(path + ".unbreakable", true);

        ItemBuilder builder = new ItemBuilder(mat)
                .name(displayName)
                .lore(lore)
                .unbreakable(unbreakable)
                .hideAll()
                .pdc(key, tier);

        // Apply enchants
        ConfigurationSection enchSection = cfg.getConfigurationSection(path + ".enchants");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                int level = enchSection.getInt(enchName);
                Enchantment ench = Enchantment.getByKey(
                        org.bukkit.NamespacedKey.minecraft(enchName.toLowerCase()));
                if (ench != null) builder.enchant(ench, level);
            }
        }

        return builder.build();
    }

    // ── Upgrade Logic ─────────────────────────────────────────────────────────

    public UpgradeCost getUpgradeCost(String type, int currentTier) {
        int nextTier = currentTier + 1;
        FileConfiguration cfg = plugin.getConfigManager().getArmoryConfig();
        String path = type + ".tiers." + nextTier + ".cost";
        ConfigurationSection section = cfg.getConfigurationSection(path);
        if (section == null) return null;

        return new UpgradeCost(
            section.getLong("scrap", 0),
            section.getLong("screws", 0),
            section.getLong("energy-cells", 0),
            section.getLong("bio-samples", 0),
            section.getLong("tech-shards", 0)
        );
    }

    public int getMaxTier(String type) {
        return plugin.getConfigManager().getArmoryConfig().getInt(type + ".max-tier", 10);
    }

    // ── PDC Identification ────────────────────────────────────────────────────

    public boolean isExtractItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(swordKey, PersistentDataType.INTEGER)
            || pdc.has(pickaxeKey, PersistentDataType.INTEGER)
            || pdc.has(axeKey, PersistentDataType.INTEGER)
            || pdc.has(helmetKey, PersistentDataType.INTEGER)
            || pdc.has(chestplateKey, PersistentDataType.INTEGER)
            || pdc.has(leggingsKey, PersistentDataType.INTEGER)
            || pdc.has(bootsKey, PersistentDataType.INTEGER)
            || plugin.getCoreManager().isCoreItem(item);
    }

    public int getItemTier(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return 0;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return pdc.get(key, PersistentDataType.INTEGER);
        }
        return 0;
    }

    // ── Gear Integrity Check ──────────────────────────────────────────────────

    /**
     * Verifies a player has the correct gear matching their PlayerData tiers.
     * Replaces any missing or incorrect items. Called on login.
     */
    public void integrityCheck(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        var inv = player.getInventory();

        // Tools in hotbar / inventory
        ensureItemInInventory(player, createSword(data.getSwordTier()), swordKey);
        ensureItemInInventory(player, createPickaxe(data.getPickaxeTier()), pickaxeKey);
        ensureItemInInventory(player, createAxe(data.getAxeTier()), axeKey);

        // Armor slots
        ensureArmorSlot(player, createHelmet(data.getHelmetTier()),     helmetKey,     0);
        ensureArmorSlot(player, createChestplate(data.getChestplateTier()), chestplateKey, 1);
        ensureArmorSlot(player, createLeggings(data.getLeggingsTier()),  leggingsKey,   2);
        ensureArmorSlot(player, createBoots(data.getBootsTier()),        bootsKey,      3);

        // Core
        if (!data.isCorePlaced()) ensureCoreInInventory(player);
    }

    private void ensureItemInInventory(Player player, ItemStack correct, NamespacedKey key) {
        var inv = player.getInventory();
        int correctTier = getItemTier(correct, key);

        // Check if player already has correct item
        for (ItemStack item : inv.getContents()) {
            if (item != null && getItemTier(item, key) == correctTier && correctTier > 0) return;
        }

        // Remove any old version and add correct
        for (int i = 0; i < inv.getContents().length; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && getItemTier(item, key) > 0) {
                inv.setItem(i, null);
            }
        }
        inv.addItem(correct);
    }

    private void ensureArmorSlot(Player player, ItemStack correct, NamespacedKey key, int slot) {
        var armor = player.getInventory().getArmorContents();
        int armorIndex = 3 - slot; // Bukkit order: boots=0, leggings=1, chest=2, helmet=3
        ItemStack current = armor[armorIndex];
        int correctTier = getItemTier(correct, key);

        if (current != null && getItemTier(current, key) == correctTier && correctTier > 0) return;
        armor[armorIndex] = correct;
        player.getInventory().setArmorContents(armor);
    }

    private void ensureCoreInInventory(Player player) {
        var inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (plugin.getCoreManager().isCoreItem(item)) return;
        }
        inv.addItem(plugin.getCoreManager().createCoreItem());
    }

    // ── Upgrade Tier ─────────────────────────────────────────────────────────

    public void upgradeGear(Player player, String type) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        int currentTier = getCurrentTier(data, type);
        int maxTier = getMaxTier(type);

        if (currentTier >= maxTier) {
            plugin.getPlayerDataManager().get(player); // refresh
            player.sendMessage(ColorUtil.color(
                plugin.getConfigManager().getMessages().getString("armory.max-tier")));
            return;
        }

        UpgradeCost cost = getUpgradeCost(type, currentTier);
        if (cost == null) return;

        if (!data.hasMaterials(cost.scrap, cost.screws, cost.energy, cost.bio, cost.tech)) {
            player.sendMessage(ColorUtil.color(
                plugin.getConfigManager().getMessages().getString("armory.not-enough-resources")));
            return;
        }

        data.deductMaterials(cost.scrap, cost.screws, cost.energy, cost.bio, cost.tech);
        int newTier = currentTier + 1;
        setTier(data, type, newTier);

        // Replace gear
        ItemStack newItem = buildGear(type, newTier,  getKey(type));
        replaceGearItem(player, type, newItem);

        plugin.getPlayerDataManager().saveAsync(data);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1.2f);

        String msg = plugin.getConfigManager().getMessages().getString("armory.upgraded",
            "&#5B8DD9Upgraded &e{item} &7to &e{tier}&8.");
        msg = msg.replace("{item}", type).replace("{tier}", "T" + newTier);
        player.sendMessage(ColorUtil.color(msg));
    }

    private void replaceGearItem(Player player, String type, ItemStack newItem) {
        var inv = player.getInventory();
        NamespacedKey key = getKey(type);

        if (isArmorType(type)) {
            var armor = inv.getArmorContents();
            int slot = getArmorIndex(type);
            armor[slot] = newItem;
            inv.setArmorContents(armor);
        } else {
            // Find and replace in inventory
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && getItemTier(item, key) > 0) {
                    inv.setItem(i, newItem);
                    return;
                }
            }
            inv.addItem(newItem);
        }
    }

    private boolean isArmorType(String type) {
        return type.equals("helmet") || type.equals("chestplate")
            || type.equals("leggings") || type.equals("boots");
    }

    private int getArmorIndex(String type) {
        return switch (type) {
            case "boots"      -> 0;
            case "leggings"   -> 1;
            case "chestplate" -> 2;
            case "helmet"     -> 3;
            default           -> 0;
        };
    }

    public int getCurrentTier(PlayerData data, String type) {
        return switch (type) {
            case "sword"       -> data.getSwordTier();
            case "pickaxe"     -> data.getPickaxeTier();
            case "axe"         -> data.getAxeTier();
            case "helmet"      -> data.getHelmetTier();
            case "chestplate"  -> data.getChestplateTier();
            case "leggings"    -> data.getLeggingsTier();
            case "boots"       -> data.getBootsTier();
            default            -> 1;
        };
    }

    private void setTier(PlayerData data, String type, int tier) {
        switch (type) {
            case "sword"      -> data.setSwordTier(tier);
            case "pickaxe"    -> data.setPickaxeTier(tier);
            case "axe"        -> data.setAxeTier(tier);
            case "helmet"     -> data.setHelmetTier(tier);
            case "chestplate" -> data.setChestplateTier(tier);
            case "leggings"   -> data.setLeggingsTier(tier);
            case "boots"      -> data.setBootsTier(tier);
        }
    }

    public NamespacedKey getKey(String type) {
        return switch (type) {
            case "sword"      -> swordKey;
            case "pickaxe"    -> pickaxeKey;
            case "axe"        -> axeKey;
            case "helmet"     -> helmetKey;
            case "chestplate" -> chestplateKey;
            case "leggings"   -> leggingsKey;
            case "boots"      -> bootsKey;
            default           -> swordKey;
        };
    }

    // ── Upgrade Cost Model ────────────────────────────────────────────────────

    public record UpgradeCost(long scrap, long screws, long energy, long bio, long tech) {}
}
