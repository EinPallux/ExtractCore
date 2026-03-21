package com.pallux.extractcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color code parsing.
 * Supports &#RRGGBB hex format, &x legacy hex, and standard &c legacy codes.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {}

    /**
     * Translates &#RRGGBB and legacy &c color codes into a colored string.
     */
    public static String color(String text) {
        if (text == null) return "";
        // Replace &#RRGGBB with §x§R§R§G§G§B§B
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        // Now translate legacy &c codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Converts a color-coded string to an Adventure Component.
     */
    public static Component component(String text) {
        return LEGACY.deserialize(color(text).replace("§", "&"));
    }

    /**
     * Strips all color codes from a string.
     */
    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }

    /**
     * Formats a number with K, M, B, T suffixes.
     * e.g. 7462 -> "7.4K", 1500000 -> "1.5M"
     */
    public static String formatNumber(long value) {
        if (value < 0) return "-" + formatNumber(-value);
        if (value >= 1_000_000_000_000L) return String.format("%.1fT", value / 1_000_000_000_000.0);
        if (value >= 1_000_000_000L)     return String.format("%.1fB", value / 1_000_000_000.0);
        if (value >= 1_000_000L)         return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000L)             return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }

    /**
     * Formats seconds into a human-readable duration: "5m 30s" or "1h 2m 3s"
     */
    public static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    /**
     * Replaces placeholders in a message string.
     * Usage: replace(msg, "player", "Steve", "amount", "100")
     */
    public static String replace(String text, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return text;
    }
}
