package com.pallux.extractcore.commands.player;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.model.PlayerData;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class CoreCommand implements CommandExecutor {
    private final ExtractCore plugin;
    public CoreCommand(ExtractCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages().getString("general.player-only"))); return true;
        }
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (!data.isCorePlaced()) {
            player.sendMessage(ColorUtil.color(
                plugin.getConfigManager().getMessages().getString("core.no-core-placed",
                    "&7You haven't placed your Core yet.")));
            return true;
        }
        String msg = plugin.getConfigManager().getMessages()
                .getString("core.coordinates",
                    "&#5B8DD9Your Core is located at &eX: {x} &8| &eY: {y} &8| &eZ: {z} &8(&7{world}&8)")
                .replace("{x}", String.valueOf(data.getCoreX()))
                .replace("{y}", String.valueOf(data.getCoreY()))
                .replace("{z}", String.valueOf(data.getCoreZ()))
                .replace("{world}", data.getCoreWorld() != null ? data.getCoreWorld() : "?");
        player.sendMessage(ColorUtil.color(msg));
        return true;
    }
}
