package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.armory.ArmoryManager;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArmoryGUI extends BaseGUI {

    private static final String[] GEAR_TYPES = {"sword","pickaxe","axe","helmet","chestplate","leggings","boots"};
    private final GuiUtil g;

    public ArmoryGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
                new GuiUtil(plugin, "armory").title(),
                new GuiUtil(plugin, "armory").rows());
        this.g = new GuiUtil(plugin, "armory");
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

        // Info item
        set(g.slot("info-item"), new ItemBuilder(mat(g.material("info-item")))
                .name(g.str("info-item.name"))
                .lore(g.lore("info-item.lore"))
                .hideAll().build());

        // Gear items
        PlayerData data = plugin.getPlayerDataManager().get(player);
        ArmoryManager am = plugin.getArmoryManager();
        List<Integer> slots = g.intList("gear-slots");
        for (int i = 0; i < GEAR_TYPES.length; i++) {
            int slot = i < slots.size() ? slots.get(i) : 10 + i * 2;
            set(slot, buildGearItem(am, data, GEAR_TYPES[i]));
        }

        // Close
        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
                .name(g.str("close-button.name"))
                .lore(g.lore("close-button.lore"))
                .hideAll().build());

        buildPlaceholders(g);
    }

    private ItemStack buildGearItem(ArmoryManager am, PlayerData data, String type) {
        int tier    = am.getCurrentTier(data, type);
        int maxTier = am.getMaxTier(type);
        ArmoryManager.UpgradeCost cost = tier < maxTier ? am.getUpgradeCost(type, tier) : null;

        String label = g.str("gear-labels." + type);
        Map<String, String> ph = GuiUtil.ph(
                "tier", String.valueOf(tier), "max_tier", String.valueOf(maxTier), "gear_label", label);

        String name = g.str("gear-name-format", ph);
        List<String> lore = new ArrayList<>(g.lore("gear-lore-header", ph));

        if (cost != null) {
            lore.add(g.str("gear-lore-cost-header", ph));
            if (cost.scrap()  > 0) lore.add(g.str("gear-lore-cost-scrap",
                    GuiUtil.ph("cost_scrap",  ColorUtil.formatNumber(cost.scrap()))));
            if (cost.screws() > 0) lore.add(g.str("gear-lore-cost-screws",
                    GuiUtil.ph("cost_screws", ColorUtil.formatNumber(cost.screws()))));
            if (cost.energy() > 0) lore.add(g.str("gear-lore-cost-energy",
                    GuiUtil.ph("cost_energy", ColorUtil.formatNumber(cost.energy()))));
            if (cost.bio()    > 0) lore.add(g.str("gear-lore-cost-bio",
                    GuiUtil.ph("cost_bio",    ColorUtil.formatNumber(cost.bio()))));
            if (cost.tech()   > 0) lore.add(g.str("gear-lore-cost-tech",
                    GuiUtil.ph("cost_tech",   ColorUtil.formatNumber(cost.tech()))));
            lore.add("");
            boolean canAfford = data.hasMaterials(cost.scrap(), cost.screws(), cost.energy(), cost.bio(), cost.tech());
            lore.add(canAfford ? g.str("gear-lore-can-afford") : g.str("gear-lore-cant-afford"));
        } else {
            lore.add(g.str("gear-lore-max-tier"));
        }
        lore.addAll(g.lore("gear-lore-footer"));

        ItemStack base = switch (type) {
            case "sword"      -> am.createSword(tier);
            case "pickaxe"    -> am.createPickaxe(tier);
            case "axe"        -> am.createAxe(tier);
            case "helmet"     -> am.createHelmet(tier);
            case "chestplate" -> am.createChestplate(tier);
            case "leggings"   -> am.createLeggings(tier);
            case "boots"      -> am.createBoots(tier);
            default           -> new ItemBuilder(Material.BARRIER).build();
        };
        return new ItemBuilder(base).name(name).lore(lore).hideAll().build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;
        if (slot == g.slot("close-button")) { player.closeInventory(); return; }

        List<Integer> gearSlots = g.intList("gear-slots");
        for (int i = 0; i < gearSlots.size(); i++) {
            if (slot == gearSlots.get(i) && i < GEAR_TYPES.length) {
                plugin.getArmoryManager().upgradeGear(player, GEAR_TYPES[i]);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
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