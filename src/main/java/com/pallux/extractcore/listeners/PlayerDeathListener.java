package com.pallux.extractcore.listeners;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerDeathListener implements Listener {

    private final ExtractCore plugin;
    private final Random random = new Random();
    // Store pending teleport-to-core on respawn
    private final Map<UUID, Location> pendingTeleport = new ConcurrentHashMap<>();

    public PlayerDeathListener(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Always keep inventory for plugin items — clear item drops of protected items
        event.getDrops().removeIf(item -> plugin.getArmoryManager().isExtractItem(item));
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        PlayerData victimData = plugin.getPlayerDataManager().get(victim);
        victimData.incrementDeaths();

        // Random loss percent
        int minLoss = plugin.getConfig().getInt("death.material-loss-min", 5);
        int maxLoss = plugin.getConfig().getInt("death.material-loss-max", 30);
        int lossPercent = minLoss + random.nextInt(maxLoss - minLoss + 1);

        // Calculate losses
        long lostScrap  = applyLoss(victimData.getScrap(),       lossPercent, "scrap");
        long lostScrews = applyLoss(victimData.getScrews(),      lossPercent, "screws");
        long lostEnergy = applyLoss(victimData.getEnergyCells(), lossPercent, "energy-cells");
        long lostBio    = applyLoss(victimData.getBioSamples(),  lossPercent, "bio-samples");
        long lostTech   = applyLoss(victimData.getTechShards(),  lossPercent, "tech-shards");

        victimData.setScrap(victimData.getScrap()           - lostScrap);
        victimData.setScrews(victimData.getScrews()         - lostScrews);
        victimData.setEnergyCells(victimData.getEnergyCells() - lostEnergy);
        victimData.setBioSamples(victimData.getBioSamples() - lostBio);
        victimData.setTechShards(victimData.getTechShards() - lostTech);

        // Victim message
        String victimMsg = plugin.getConfigManager().getMessages()
                .getString("kill.victim-loss", "")
                .replace("{scrap}",  ColorUtil.formatNumber(lostScrap))
                .replace("{screws}", ColorUtil.formatNumber(lostScrews))
                .replace("{energy}", ColorUtil.formatNumber(lostEnergy))
                .replace("{bio}",    ColorUtil.formatNumber(lostBio))
                .replace("{tech}",   ColorUtil.formatNumber(lostTech));
        victim.sendMessage(ColorUtil.color(victimMsg));

        // Killer rewards
        Player killer = event.getEntity().getKiller();
        if (killer != null && !killer.equals(victim)) {
            PlayerData killerData = plugin.getPlayerDataManager().get(killer);
            int killerPct = plugin.getConfig().getInt("death.killer-reward-percent", 10);

            long gainScrap  = lostScrap  * killerPct / 100;
            long gainScrews = lostScrews * killerPct / 100;
            long gainEnergy = lostEnergy * killerPct / 100;
            long gainBio    = lostBio    * killerPct / 100;
            long gainTech   = lostTech   * killerPct / 100;

            killerData.addMaterials(gainScrap, gainScrews, gainEnergy, gainBio, gainTech);
            killerData.incrementKills();
            plugin.getLevelManager().addXp(killer, "player-kill");
            plugin.getMilestoneManager().check(killer, killerData);
            plugin.getPlayerDataManager().saveAsync(killerData);

            String killerMsg = plugin.getConfigManager().getMessages()
                    .getString("kill.killer-reward", "")
                    .replace("{victim}", victim.getName())
                    .replace("{scrap}",  ColorUtil.formatNumber(gainScrap))
                    .replace("{screws}", ColorUtil.formatNumber(gainScrews))
                    .replace("{energy}", ColorUtil.formatNumber(gainEnergy))
                    .replace("{bio}",    ColorUtil.formatNumber(gainBio))
                    .replace("{tech}",   ColorUtil.formatNumber(gainTech));
            killer.sendMessage(ColorUtil.color(killerMsg));
        }

        plugin.getPlayerDataManager().saveAsync(victimData);

        // Queue teleport to core on respawn
        if (plugin.getConfig().getBoolean("death.teleport-to-core", true) && victimData.isCorePlaced()) {
            Location coreLoc = plugin.getCoreManager().getCoreLocation(victimData);
            if (coreLoc != null) pendingTeleport.put(victim.getUniqueId(), coreLoc);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location loc = pendingTeleport.remove(player.getUniqueId());
        if (loc != null) {
            event.setRespawnLocation(loc);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ColorUtil.color(
                        plugin.getConfigManager().getMessages()
                            .getString("core.teleported-to-core", "&#5B8DD9Teleported to your Core.")));
                }
            }, 5L);
        }
    }

    private long applyLoss(long amount, int percent, String material) {
        if (!plugin.getConfig().getBoolean("kill-loss." + material.replace("-", ""), true)) return 0;
        return Math.max(0, amount * percent / 100);
    }
}
