package com.pallux.extractcore.gui;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import com.pallux.extractcore.util.GuiUtil;
import com.pallux.extractcore.util.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreGUI extends BaseGUI {

    private final GuiUtil g;

    public CoreGUI(ExtractCore plugin, Player player) {
        super(plugin, player,
                new GuiUtil(plugin, "core-gui").title(),
                new GuiUtil(plugin, "core-gui").rows());
        this.g = new GuiUtil(plugin, "core-gui");
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
        var cm  = plugin.getCoreManager();
        var cfg = plugin.getConfigManager().getCoreConfig();

        int speedMax    = cfg.getInt("core.upgrades.speed.max-level", 100);
        int prodMax     = cfg.getInt("core.upgrades.production.max-level", 100);
        int pickupSec   = cfg.getInt("core.pickup-duration-seconds", 10);
        int pickupRange = plugin.getConfigManager().getMain().getInt("core.pickup-range", 10);

        boolean inRange = isInPickupRange(data, pickupRange);

        Map<String, String> ph = GuiUtil.ph(
                "scrap_rate",     ColorUtil.formatNumber(cm.getScrapPerInterval(data)),
                "interval",       String.valueOf(cm.getIntervalTicks(data)),
                "core_x",         String.valueOf(data.getCoreX()),
                "core_y",         String.valueOf(data.getCoreY()),
                "core_z",         String.valueOf(data.getCoreZ()),
                "speed_lvl",      String.valueOf(data.getCoreSpeedLevel()),
                "prod_lvl",       String.valueOf(data.getCoreProductionLevel()),
                "speed_max",      String.valueOf(speedMax),
                "prod_max",       String.valueOf(prodMax),
                "pickup_seconds", String.valueOf(pickupSec),
                "stored_scrap",   ColorUtil.formatNumber(data.getCoreStoredScrap()),
                "pickup_range",   String.valueOf(pickupRange),
                "in_range",       inRange
                        ? g.str("pickup-in-range-label")
                        : g.str("pickup-out-of-range-label")
        );

        set(g.slot("info-item"), new ItemBuilder(mat(g.material("info-item")))
                .name(g.str("info-item.name", ph))
                .lore(g.lore("info-item.lore", ph))
                .hideAll().build());

        set(g.slot("collect-button"), new ItemBuilder(mat(g.material("collect-button")))
                .name(g.str("collect-button.name", ph))
                .lore(g.lore("collect-button.lore", ph))
                .hideAll().build());

        set(g.slot("speed-upgrade"),      buildUpgradeItem("speed-upgrade",      "speed",      data, cfg, ph, speedMax));
        set(g.slot("production-upgrade"), buildUpgradeItem("production-upgrade", "production", data, cfg, ph, prodMax));

        String pickupKey = inRange ? "pickup-button" : "pickup-button-out-of-range";
        set(g.slot("pickup-button"), new ItemBuilder(mat(g.material(pickupKey)))
                .name(g.str(pickupKey + ".name", ph))
                .lore(g.lore(pickupKey + ".lore", ph))
                .hideAll().build());

        set(g.slot("close-button"), new ItemBuilder(mat(g.material("close-button")))
                .name(g.str("close-button.name"))
                .lore(g.lore("close-button.lore"))
                .hideAll().build());

        buildPlaceholders(g);
    }

    // ── Range check ───────────────────────────────────────────────────────────

    private boolean isInPickupRange(PlayerData data, int range) {
        Location coreLoc = plugin.getCoreManager().getCoreLocation(data);
        if (coreLoc == null) return false;
        if (!player.getWorld().equals(coreLoc.getWorld())) return false;
        return player.getLocation().distanceSquared(coreLoc.clone().add(0.5, 0.5, 0.5))
                <= (double) range * range;
    }

    // ── Sound helper ──────────────────────────────────────────────────────────

    private void playSound(String configKey, String defaultSound, float defaultVolume, float defaultPitch) {
        String soundName = plugin.getConfigManager().getGuiConfig()
                .getString("core-gui.sounds." + configKey, defaultSound);
        float volume = (float) plugin.getConfigManager().getGuiConfig()
                .getDouble("core-gui.sounds." + configKey + "-volume", defaultVolume);
        float pitch  = (float) plugin.getConfigManager().getGuiConfig()
                .getDouble("core-gui.sounds." + configKey + "-pitch",  defaultPitch);
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException ignored) {
            // Invalid sound name in config — silent fail
        }
    }

    // ── Upgrade item builder ──────────────────────────────────────────────────

    private org.bukkit.inventory.ItemStack buildUpgradeItem(
            String guiKey, String upgradeType, PlayerData data,
            FileConfiguration cfg, Map<String, String> basePh, int maxLevel) {

        boolean isSpeed  = upgradeType.equals("speed");
        int currentLevel = isSpeed ? data.getCoreSpeedLevel() : data.getCoreProductionLevel();
        int nextLevel    = currentLevel + 1;
        Map<String, String> ph = new HashMap<>(basePh);
        List<String> lore;

        if (currentLevel < maxLevel) {
            String basePath = "core.upgrades." + upgradeType + ".levels." + nextLevel;
            long scrap  = cfg.getLong(basePath + ".cost.scrap",        0);
            long screws = cfg.getLong(basePath + ".cost.screws",       0);
            long energy = cfg.getLong(basePath + ".cost.energy-cells", 0);
            long bio    = cfg.getLong(basePath + ".cost.bio-samples",  0);
            long tech   = cfg.getLong(basePath + ".cost.tech-shards",  0);

            ph.put("next_display", ColorUtil.color(cfg.getString(basePath + ".display", "Level " + nextLevel)));
            ph.put("cost_scrap",   ColorUtil.formatNumber(scrap));
            ph.put("cost_screws",  ColorUtil.formatNumber(screws));
            ph.put("cost_energy",  ColorUtil.formatNumber(energy));
            ph.put("cost_bio",     ColorUtil.formatNumber(bio));
            ph.put("cost_tech",    ColorUtil.formatNumber(tech));

            lore = new ArrayList<>(g.lore(guiKey + ".lore-header", ph));
            if (scrap  > 0) lore.add(g.str(guiKey + ".lore-cost-scrap",  ph));
            if (screws > 0) lore.add(g.str(guiKey + ".lore-cost-screws", ph));
            if (energy > 0) lore.add(g.str(guiKey + ".lore-cost-energy", ph));
            if (bio    > 0) lore.add(g.str(guiKey + ".lore-cost-bio",    ph));
            if (tech   > 0) lore.add(g.str(guiKey + ".lore-cost-tech",   ph));
            lore.add("");
            boolean canAfford = data.hasMaterials(scrap, screws, energy, bio, tech);
            lore.add(canAfford ? g.str(guiKey + ".lore-can-afford") : g.str(guiKey + ".lore-cant-afford"));
        } else {
            lore = new ArrayList<>(g.lore(guiKey + ".lore-header", ph));
            lore.add(g.str(guiKey + ".lore-max-level"));
        }
        lore.addAll(g.lore(guiKey + ".lore-footer", ph));

        return new ItemBuilder(mat(g.material(guiKey)))
                .name(g.str(guiKey + ".name", ph))
                .lore(lore)
                .hideAll().build();
    }

    // ── Click handler ─────────────────────────────────────────────────────────

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= inventory.getSize()) return;

        if (slot == g.slot("close-button")) {
            player.closeInventory();

        } else if (slot == g.slot("collect-button")) {
            plugin.getCoreManager().collectStoredScrap(player);
            build();

        } else if (slot == g.slot("pickup-button")) {
            PlayerData data  = plugin.getPlayerDataManager().get(player);
            int range        = plugin.getConfigManager().getMain().getInt("core.pickup-range", 10);
            if (!isInPickupRange(data, range)) {
                String msg = plugin.getConfigManager().getMessages()
                        .getString("core.pickup-too-far",
                                "&cYou are too far from your Core. Get within &e{range} blocks &cto pick it up.")
                        .replace("{range}", String.valueOf(range));
                player.sendMessage(ColorUtil.color(msg));
                playSound("pickup-denied", "BLOCK_ANVIL_LAND", 0.5f, 0.8f);
                return;
            }
            player.closeInventory();
            plugin.getCoreManager().initiatePickup(player);

        } else if (slot == g.slot("speed-upgrade")) {
            upgradeCore("speed");

        } else if (slot == g.slot("production-upgrade")) {
            upgradeCore("production");
        }
    }

    private void upgradeCore(String type) {
        PlayerData data  = plugin.getPlayerDataManager().get(player);
        var cfg          = plugin.getConfigManager().getCoreConfig();
        boolean isSpeed  = type.equals("speed");
        int current      = isSpeed ? data.getCoreSpeedLevel() : data.getCoreProductionLevel();
        int max          = cfg.getInt("core.upgrades." + type + ".max-level", 100);

        if (current >= max) {
            player.sendMessage(g.str("upgrade-messages.max-level"));
            return;
        }

        int    next   = current + 1;
        String base   = "core.upgrades." + type + ".levels." + next + ".cost";
        long   scrap  = cfg.getLong(base + ".scrap",        0);
        long   screws = cfg.getLong(base + ".screws",       0);
        long   energy = cfg.getLong(base + ".energy-cells", 0);
        long   bio    = cfg.getLong(base + ".bio-samples",  0);
        long   tech   = cfg.getLong(base + ".tech-shards",  0);

        if (!data.hasMaterials(scrap, screws, energy, bio, tech)) {
            player.sendMessage(g.str("upgrade-messages.not-enough"));
            playSound("upgrade-fail", "BLOCK_ANVIL_LAND", 0.5f, 0.8f);
            return;
        }

        data.deductMaterials(scrap, screws, energy, bio, tech);
        if (isSpeed) data.setCoreSpeedLevel(next);
        else         data.setCoreProductionLevel(next);
        plugin.getPlayerDataManager().saveAsync(data);

        String msgKey = isSpeed ? "upgrade-messages.success-speed" : "upgrade-messages.success-production";
        player.sendMessage(g.str(msgKey, GuiUtil.ph("level", String.valueOf(next))));
        playSound("upgrade-success", "BLOCK_BEACON_POWER_SELECT", 1.0f, 1.2f);
        build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fillBorderWith(Material mat) {
        int size = inventory.getSize();
        int rows = size / 9;
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