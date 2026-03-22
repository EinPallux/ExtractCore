package com.pallux.extractcore.shop;

import java.util.List;

/**
 * Immutable data record for a single shop item entry.
 */
public record ShopEntry(
        String id,
        Category category,
        String material,
        String displayName,
        List<String> lore,
        long costScrap,
        int amount,
        int hp,           // 0 for normal blocks
        int guiSlot
) {
    public enum Category { NORMAL, DEFENSE }
}