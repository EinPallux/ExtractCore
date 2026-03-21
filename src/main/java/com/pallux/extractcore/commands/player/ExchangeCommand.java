package com.pallux.extractcore.commands.player;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.ExchangeGUI;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ExchangeCommand implements CommandExecutor {
    private final ExtractCore plugin;
    public ExchangeCommand(ExtractCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages().getString("general.player-only"))); return true;
        }
        new ExchangeGUI(plugin, player).open();
        return true;
    }
}
