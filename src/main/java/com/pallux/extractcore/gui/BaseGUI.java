package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.listeners.GUIListener;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Abstract base for all ExtractCore GUIs.
 */
public abstract class BaseGUI {

    protected final ExtractCore plugin;
    protected Inventory inventory;
    protected final Player player;

    protected BaseGUI(ExtractCore plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, rows * 9, ColorUtil.component(title));
    }

    public void open() {
        build();
        GUIListener.registerGUI(player, this);
        player.openInventory(inventory);
    }

    protected abstract void build();
    public abstract void handleClick(InventoryClickEvent event);

    // ── Layout Helpers ────────────────────────────────────────────────────────

    protected void fill(ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    protected void fillBorder() {
        int size = inventory.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) inventory.setItem(i, ItemBuilder.border());
        for (int i = size - 9; i < size; i++) inventory.setItem(i, ItemBuilder.border());
        for (int r = 1; r < rows - 1; r++) {
            inventory.setItem(r * 9, ItemBuilder.border());
            inventory.setItem(r * 9 + 8, ItemBuilder.border());
        }
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected boolean isBorderSlot(int slot) {
        int size = inventory.getSize();
        int rows = size / 9;
        int row  = slot / 9;
        int col  = slot % 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    public Inventory getInventory() { return inventory; }
}
