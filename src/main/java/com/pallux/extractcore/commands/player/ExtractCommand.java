package com.pallux.extractcore.commands.player;

import com.pallux.extractcore.ExtractCore;
import com.pallux.extractcore.gui.MainMenuGUI;
import com.pallux.extractcore.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ExtractCommand implements CommandExecutor {
    private final ExtractCore plugin;
    public ExtractCommand(ExtractCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessages().getString("general.player-only"))); return true;
        }
        new MainMenuGUI(plugin, player).open();
        return true;
    }
}
