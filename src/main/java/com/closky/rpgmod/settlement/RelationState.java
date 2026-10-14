package com.closky.rpgmod.settlement;

import java.util.Locale;

/** Relation states between two settlements (section 17.3). Pure Java so it can be unit tested. */
public enum RelationState {
    WAR(-100, -71, 0xE03030),
    HOSTILE(-70, -41, 0xE08A2A),
    TENSE(-40, -11, 0xE0D030),
    NEUTRAL(-10, 39, 0x9A9A9A),
    FRIENDLY(40, 79, 0x4A8AE0),
    ALLIED(80, 100, 0x40C860);

    private final int min;
    private final int max;
    private final int color;
    private final String key = name().toLowerCase(Locale.ROOT);

    RelationState(int min, int max, int color) {
        this.min = min;
        this.max = max;
        this.color = color;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getColor() {
        return color;
    }

    public String getKey() {
        return key;
    }

    public String getTranslationKey() {
        return "relation.rpgmod." + key;
    }

    public static RelationState of(int value) {
        int v = RelationMath.clamp(value);
        for (RelationState state : values()) {
            if (v >= state.min && v <= state.max) {
                return state;
            }
        }
        return NEUTRAL;
    }
}
