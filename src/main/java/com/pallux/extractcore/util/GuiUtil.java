package com.pallux.extractcore.util;

import com.pallux.extractcore.ExtractCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central helper for resolving GUI text from guis.yml.
 * All strings pass through ColorUtil.color() automatically.
 *
 * Usage:
 *   GuiUtil gui = new GuiUtil(plugin, "armory");
 *   String title = gui.title();
 *   String name  = gui.str("info-item.name", placeholders);
 *   List<String> lore = gui.lore("info-item.lore", placeholders);
 */
public class GuiUtil {

    private final FileConfiguration cfg;
    private final String section;

    public GuiUtil(ExtractCore plugin, String section) {
        this.cfg     = plugin.getConfigManager().getGuiConfig();
        this.section = section;
    }

    // ── Convenience accessors ─────────────────────────────────────

    /** Returns colored title for this GUI section. */
    public String title(Map<String, String> ph) {
        return apply(cfg.getString(section + ".title", "&cMissing title"), ph);
    }
    public String title() { return title(Map.of()); }

    /** Returns rows count for this GUI section. */
    public int rows() { return cfg.getInt(section + ".rows", 6); }

    /** Returns a colored string at section.path */
    public String str(String path, Map<String, String> ph) {
        return apply(cfg.getString(section + "." + path, "&cMissing: " + path), ph);
    }
    public String str(String path) { return str(path, Map.of()); }

    /** Returns a colored lore list at section.path */
    public List<String> lore(String path, Map<String, String> ph) {
        List<String> raw = cfg.getStringList(section + "." + path);
        return raw.stream().map(l -> apply(l, ph)).collect(Collectors.toList());
    }
    public List<String> lore(String path) { return lore(path, Map.of()); }

    /** Returns a slot integer at section.path */
    public int slot(String path) { return cfg.getInt(section + "." + path + ".slot", 0); }

    /** Returns a material string at section.path */
    public String material(String path) {
        return cfg.getString(section + "." + path + ".material", "BARRIER");
    }

    /** Returns integer value */
    public int getInt(String path, int def) { return cfg.getInt(section + "." + path, def); }

    /** Returns boolean value */
    public boolean getBool(String path, boolean def) { return cfg.getBoolean(section + "." + path, def); }

    // ── Placeholder engine ────────────────────────────────────────

    private String apply(String text, Map<String, String> ph) {
        if (text == null) return "";
        for (Map.Entry<String, String> e : ph.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return ColorUtil.color(text);
    }

    // ── Static builder for placeholders ──────────────────────────

    public static Map<String, String> ph(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
