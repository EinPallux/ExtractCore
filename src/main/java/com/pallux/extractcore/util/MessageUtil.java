package com.pallux.extractcore.util;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {}

    /**
     * Sends a message looked up by key from messages.yml.
     * {prefix} in the message value is resolved automatically by ColorUtil.color().
     * Do NOT prepend the prefix here — it would double it for messages that
     * already contain {prefix} in their yml value.
     */
    public static void send(CommandSender sender, String key, String... replacements) {
        var msgs = ExtractCore.getInstance().getConfigManager().getMessages();
        String raw = msgs.getString(key, "&cMissing message: " + key);
        if (replacements.length > 0) raw = ColorUtil.replace(raw, replacements);
        sender.sendMessage(ColorUtil.color(raw));
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
        String colored = ColorUtil.color(message);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(colored);
        }
    }

    public static void broadcastCentered(String message) {
        String colored = ColorUtil.color(message);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(centered(colored));
        }
    }

    public static String centered(String message) {
        String stripped = ColorUtil.strip(message);
        int spaces = Math.max(0, (80 - stripped.length()) / 2);
        return " ".repeat(spaces) + message;
    }
}