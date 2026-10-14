package com.closky.rpgmod.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Casino rarity tiers: grey / green / blue / purple / gold (section 9.2).
 */
public enum ItemRarity {
    COMMON(ChatFormatting.GRAY, 0xFFAAAAAA),
    UNCOMMON(ChatFormatting.GREEN, 0xFF55FF55),
    RARE(ChatFormatting.BLUE, 0xFF5555FF),
    EPIC(ChatFormatting.LIGHT_PURPLE, 0xFFAA00AA),
    LEGENDARY(ChatFormatting.GOLD, 0xFFFFAA00);

    private final ChatFormatting color;
    private final int argb;
    private final String key = name().toLowerCase(Locale.ROOT);

    ItemRarity(ChatFormatting color, int argb) {
        this.color = color;
        this.argb = argb;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public int getArgb() {
        return argb;
    }

    public String getSerializedName() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("rarity.rpgmod." + key).withStyle(color);
    }

    public static ItemRarity byOrdinal(int ordinal) {
        ItemRarity[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }

    /** Rarity derived from an item level (used for non-tiered loot such as scrolls or armor). */
    public static ItemRarity byItemLevel(int level) {
        if (level >= 20) return LEGENDARY;
        if (level >= 14) return EPIC;
        if (level >= 9) return RARE;
        if (level >= 4) return UNCOMMON;
        return COMMON;
    }
}
