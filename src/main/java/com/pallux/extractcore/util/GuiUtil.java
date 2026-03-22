package com.pallux.extractcore.util;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central helper for resolving GUI config values from guis.yml.
 * All strings are automatically passed through ColorUtil.color().
 */
public class GuiUtil {

    private final FileConfiguration cfg;
    private final String section;

    public GuiUtil(ExtractCore plugin, String section) {
        this.cfg     = plugin.getConfigManager().getGuiConfig();
        this.section = section;
    }

    /** Colored title for this GUI section, with optional placeholders. */
    public String title(Map<String, String> ph) {
        return apply(cfg.getString(section + ".title", "&cMissing title"), ph);
    }
    public String title() { return title(Map.of()); }

    /** Row count for this GUI section. */
    public int rows() { return cfg.getInt(section + ".rows", 6); }

    /** Colored string at {@code section.path}. */
    public String str(String path, Map<String, String> ph) {
        return apply(cfg.getString(section + "." + path, "&cMissing: " + path), ph);
    }
    public String str(String path) { return str(path, Map.of()); }

    /** Colored lore list at {@code section.path}. */
    public List<String> lore(String path, Map<String, String> ph) {
        return cfg.getStringList(section + "." + path)
                .stream().map(l -> apply(l, ph)).collect(Collectors.toList());
    }
    public List<String> lore(String path) { return lore(path, Map.of()); }

    /** Slot integer at {@code section.path.slot}. */
    public int slot(String path) { return cfg.getInt(section + "." + path + ".slot", 0); }

    /** Material string at {@code section.path.material}. */
    public String material(String path) {
        return cfg.getString(section + "." + path + ".material", "BARRIER");
    }

    /** Integer value at {@code section.path}. */
    public int getInt(String path, int def) { return cfg.getInt(section + "." + path, def); }

    /** Boolean value at {@code section.path}. */
    public boolean getBool(String path, boolean def) { return cfg.getBoolean(section + "." + path, def); }

    /** Integer list at {@code section.path} — used for slot lists (border, placeholder, etc.). */
    public List<Integer> intList(String path) {
        return cfg.getIntegerList(section + "." + path);
    }

    // ── Internal ──────────────────────────────────────────────────

    private String apply(String text, Map<String, String> ph) {
        if (text == null) return "";
        for (Map.Entry<String, String> e : ph.entrySet())
            text = text.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        return ColorUtil.color(text);
    }

    /** Builds a placeholder map from alternating key-value pairs. */
    public static Map<String, String> ph(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2)
            map.put(pairs[i], pairs[i + 1]);
        return map;
    }
}