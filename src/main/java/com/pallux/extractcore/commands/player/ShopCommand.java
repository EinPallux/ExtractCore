package com.pallux.extractcore.commands.player;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.ShopGUI;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final ExtractCore plugin;

    public ShopCommand(ExtractCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(
                    plugin.getConfigManager().getMessages().getString("general.player-only")));
            return true;
        }
        new ShopGUI(plugin, player).open();
        return true;
    }
}