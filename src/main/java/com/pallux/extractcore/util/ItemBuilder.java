package com.pallux.extractcore.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for ItemStacks with full color and PDC support.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack base) {
        this.item = base.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.displayName(ColorUtil.component(name));
        return this;
    }

    public ItemBuilder nameComponent(Component component) {
        meta.displayName(component);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            components.add(ColorUtil.component(line));
        }
        meta.lore(components);
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(List.of(lines));
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder enchants(Map<String, Integer> enchants) {
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            try {
                Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                if (enchant != null) meta.addEnchant(enchant, entry.getValue(), true);
            } catch (Exception ignored) {}
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder pdc(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    public ItemBuilder pdc(NamespacedKey key, int value) {
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        return this;
    }

    public ItemBuilder leatherColor(Color color) {
        if (meta instanceof LeatherArmorMeta lam) {
            lam.setColor(color);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    // ── GUI filler ────────────────────────
    public static ItemStack filler() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideAll()
                .build();
    }

    public static ItemStack filler(Material mat) {
        return new ItemBuilder(mat)
                .name(" ")
                .hideAll()
                .build();
    }

    public static ItemStack border() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name(" ")
                .hideAll()
                .build();
    }
}
