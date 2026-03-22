package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class ResetConfirmGUI extends BaseGUI {

    private final OfflinePlayer target;
    private final String resetType;
    private final GuiUtil g;

    public ResetConfirmGUI(ExtractCore plugin, Player admin, OfflinePlayer target, String resetType) {
        super(plugin, admin,
                new GuiUtil(plugin, "reset-confirm").title(
                        GuiUtil.ph("player", target.getName() != null ? target.getName() : "Unknown")),
                new GuiUtil(plugin, "reset-confirm").rows());
        this.target    = target;
        this.resetType = resetType;
        this.g         = new GuiUtil(plugin, "reset-confirm");
    }

    @Override
    protected void build() {
        // Filler — material comes from config
        if (g.getBool("filler.enabled", true))
            fill(new ItemBuilder(mat(g.material("filler"))).name(" ").hideAll().build());

        Map<String, String> ph = GuiUtil.ph(
                "player",     target.getName() != null ? target.getName() : "Unknown",
                "reset_type", resetType.toUpperCase()
        );

        set(g.slot("confirm-button"), new ItemBuilder(mat(g.material("confirm-button")))
                .name(g.str("confirm-button.name", ph))
                .lore(g.lore("confirm-button.lore", ph))
                .hideAll().build());

        set(g.slot("cancel-button"), new ItemBuilder(mat(g.material("cancel-button")))
                .name(g.str("cancel-button.name", ph))
                .lore(g.lore("cancel-button.lore", ph))
                .hideAll().build());

        buildPlaceholders(g);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;

        if (slot == g.slot("confirm-button")) {
            PlayerData data = plugin.getPlayerDataManager().load(
                    target.getUniqueId(), target.getName() != null ? target.getName() : "Unknown");
            executeReset(data);
            plugin.getPlayerDataManager().save(data);
            String msg = plugin.getConfigManager().getMessages()
                    .getString("admin.reset-success", "&#5B8DD9Reset &e{type} &7for &e{player}&8.")
                    .replace("{type}", resetType)
                    .replace("{player}", target.getName() != null ? target.getName() : "?");
            player.sendMessage(ColorUtil.color(msg));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.closeInventory();

        } else if (slot == g.slot("cancel-button")) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages()
                    .getString("admin.reset-cancel", "&7Reset cancelled.")));
            player.closeInventory();
        }
    }

    private void executeReset(PlayerData d) {
        switch (resetType.toLowerCase()) {
            case "scrap"       -> d.setScrap(0);
            case "screws"      -> d.setScrews(0);
            case "energycells" -> d.setEnergyCells(0);
            case "biosamples"  -> d.setBioSamples(0);
            case "techshards"  -> d.setTechShards(0);
            case "level"       -> { d.setLevel(1); d.setXp(0); }
            case "core"        -> { d.setCorePlaced(false); d.setCoreSpeedLevel(0); d.setCoreProductionLevel(0); }
            case "all"         -> {
                d.setScrap(0); d.setScrews(0); d.setEnergyCells(0); d.setBioSamples(0); d.setTechShards(0);
                d.setLevel(1); d.setXp(0);
                d.setCorePlaced(false); d.setCoreSpeedLevel(0); d.setCoreProductionLevel(0);
                d.setSwordTier(1); d.setPickaxeTier(1); d.setAxeTier(1);
                d.setHelmetTier(1); d.setChestplateTier(1); d.setLeggingsTier(1); d.setBootsTier(1);
                d.getCompletedMilestones().clear();
            }
        }
    }

    private Material mat(String n) { try { return Material.valueOf(n); } catch (Exception e) { return Material.BARRIER; } }
}