package com.pallux.extractcore.exchange;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ExchangeManager {

    private final ExtractCore plugin;

    public ExchangeManager(ExtractCore plugin) {
        this.plugin = plugin;
    }

    public long getCostInScrap(String material, long amount) {
        FileConfiguration cfg = plugin.getConfig();
        long unitCost = switch (material.toLowerCase()) {
            case "screws"       -> cfg.getLong("exchange.screws-cost",       500);
            case "energy-cells" -> cfg.getLong("exchange.energy-cells-cost", 1200);
            case "bio-samples"  -> cfg.getLong("exchange.bio-samples-cost",  800);
            case "tech-shards"  -> cfg.getLong("exchange.tech-shards-cost",  2000);
            default             -> Long.MAX_VALUE;
        };
        return unitCost * amount;
    }

    public boolean purchase(Player player, String material, long amount) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        long cost = getCostInScrap(material, amount);

        if (data.getScrap() < cost) {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("exchange.not-enough-scrap", "&cNot enough Scrap. You need &e{cost} Scrap&c.")
                    .replace("{cost}", ColorUtil.formatNumber(cost));
            player.sendMessage(ColorUtil.color(msg));
            return false;
        }

        data.setScrap(data.getScrap() - cost);
        switch (material.toLowerCase()) {
            case "screws"       -> data.addScrews(amount);
            case "energy-cells" -> data.addEnergyCells(amount);
            case "bio-samples"  -> data.addBioSamples(amount);
            case "tech-shards"  -> data.addTechShards(amount);
        }

        plugin.getPlayerDataManager().saveAsync(data);

        String matDisplay = switch (material.toLowerCase()) {
            case "screws"       -> "&bScrews";
            case "energy-cells" -> "&aEnergy Cells";
            case "bio-samples"  -> "&dBio Samples";
            case "tech-shards"  -> "&6Tech Shards";
            default             -> material;
        };
        String msg = plugin.getConfigManager().getMessages()
                .getString("exchange.purchased", "&#5B8DD9Purchased &e{amount}x {material} &7for &e{cost} Scrap&8.")
                .replace("{amount}", String.valueOf(amount))
                .replace("{material}", matDisplay)
                .replace("{cost}", ColorUtil.formatNumber(cost));
        player.sendMessage(ColorUtil.color(msg));
        return true;
    }
}
