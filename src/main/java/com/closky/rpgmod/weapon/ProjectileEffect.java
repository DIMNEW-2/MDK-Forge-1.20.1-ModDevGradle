package com.closky.rpgmod.weapon;

import java.util.Locale;

/**
 * Special effects of custom arrows and bolts (section 2B.4).
 */
public enum ProjectileEffect {
    BASIC(0.0F, true, true),
    FIRE(0.5F, true, true),
    FROST(0.0F, true, true),
    EXPLOSIVE(1.0F, true, false),
    POISON(0.0F, true, true),
    PIERCING(0.5F, true, true),
    GLOWING(0.0F, true, false),
    LIGHTNING(1.0F, false, true);

    private final float bonusDamage;
    private final boolean arrow;
    private final boolean bolt;
    private final String key = name().toLowerCase(Locale.ROOT);

    ProjectileEffect(float bonusDamage, boolean arrow, boolean bolt) {
        this.bonusDamage = bonusDamage;
        this.arrow = arrow;
        this.bolt = bolt;
    }

    public float getBonusDamage() {
        return bonusDamage;
    }

    /** Whether an arrow item exists for this effect. */
    public boolean hasArrow() {
        return arrow;
    }

    /** Whether a bolt item exists for this effect. */
    public boolean hasBolt() {
        return bolt;
    }

    public String getKey() {
        return key;
    }

    public static ProjectileEffect byOrdinal(int ordinal) {
        ProjectileEffect[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
