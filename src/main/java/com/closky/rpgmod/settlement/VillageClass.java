package com.closky.rpgmod.settlement;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Specialisation of a settlement (section 17.1). Influences growth, military strength and raid composition. */
public enum VillageClass {
    FARMING(0x7CBF4F, 1.2F, 0.6F),
    MINING(0x9A8F7A, 1.0F, 0.9F),
    MILITARY(0xC94A3C, 0.7F, 1.6F),
    MAGIC(0x9B6BD6, 0.9F, 1.1F),
    TRADING(0xE0B340, 1.4F, 0.7F);

    private final int color;
    private final float prosperityRate;
    private final float militaryRate;
    private final String key = name().toLowerCase(Locale.ROOT);

    VillageClass(int color, float prosperityRate, float militaryRate) {
        this.color = color;
        this.prosperityRate = prosperityRate;
        this.militaryRate = militaryRate;
    }

    public int getColor() {
        return color;
    }

    public float getProsperityRate() {
        return prosperityRate;
    }

    public float getMilitaryRate() {
        return militaryRate;
    }

    public String getKey() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("village_class.rpgmod." + key);
    }

    public static VillageClass byKey(String key) {
        for (VillageClass c : values()) {
            if (c.key.equalsIgnoreCase(key)) {
                return c;
            }
        }
        return FARMING;
    }

    public static VillageClass byOrdinal(int ordinal) {
        VillageClass[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
