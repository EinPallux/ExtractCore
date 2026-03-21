package com.pallux.extractcore.util;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {}

    public static void send(CommandSender sender, String key, String... replacements) {
        var msgs = ExtractCore.getInstance().getConfigManager().getMessages();
        String prefix = ColorUtil.color(msgs.getString("prefix", "&#5B8DD9◈ &7ExtractCore &8| "));
        String raw = msgs.getString(key, "&cMissing message: " + key);
        if (replacements.length > 0) raw = ColorUtil.replace(raw, replacements);
        String colored = ColorUtil.color(raw);
        sender.sendMessage(prefix + colored);
    }

    public static void sendRaw(CommandSender sender, String key, String... replacements) {
        var msgs = ExtractCore.getInstance().getConfigManager().getMessages();
        String raw = msgs.getString(key, "&cMissing message: " + key);
        if (replacements.length > 0) raw = ColorUtil.replace(raw, replacements);
        sender.sendMessage(ColorUtil.color(raw));
    }

    public static void sendDirect(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.color(message));
    }

    public static void broadcast(String message) {
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(ColorUtil.color(message));
        }
    }

    public static void broadcastCentered(String message) {
        String colored = ColorUtil.color(message);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(centered(colored));
        }
    }

    public static String centered(String message) {
        // Simple center approximation for chat (80 char width)
        String stripped = ColorUtil.strip(message);
        int spaces = Math.max(0, (80 - stripped.length()) / 2);
        return " ".repeat(spaces) + message;
    }
}
