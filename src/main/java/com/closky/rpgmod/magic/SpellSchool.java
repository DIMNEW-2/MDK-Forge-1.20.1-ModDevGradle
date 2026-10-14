package com.closky.rpgmod.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** The four magic schools (section 6.1). */
public enum SpellSchool {
    FIRE(ChatFormatting.RED, 0xFF6A2A),
    FROST(ChatFormatting.AQUA, 0x7FD7FF),
    LIGHTNING(ChatFormatting.YELLOW, 0xFFF26B),
    NATURE(ChatFormatting.GREEN, 0x62C85A);

    private final ChatFormatting color;
    private final int rgb;
    private final String key = name().toLowerCase(Locale.ROOT);

    SpellSchool(ChatFormatting color, int rgb) {
        this.color = color;
        this.rgb = rgb;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public int getRgb() {
        return rgb;
    }

    public String getKey() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("spell_school.rpgmod." + key).withStyle(color);
    }

    public static SpellSchool byKey(String key) {
        for (SpellSchool s : values()) {
            if (s.key.equalsIgnoreCase(key)) {
                return s;
            }
        }
        return FIRE;
    }

    public static SpellSchool byOrdinal(int ordinal) {
        SpellSchool[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
