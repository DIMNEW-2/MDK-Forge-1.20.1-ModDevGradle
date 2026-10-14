package com.closky.rpgmod.weapon;

import java.util.Locale;

/**
 * Bomb variants (section 2A.5).
 */
public enum BombType {
    BASIC(6.0F, 3.0F, 0x5A5A5A),
    FIRE(5.0F, 3.0F, 0xE25822),
    FROST(4.0F, 4.0F, 0x7FD7FF),
    LIGHTNING(5.0F, 3.0F, 0xF8F32B),
    POISON(3.0F, 4.0F, 0x4E9A2A),
    SMOKE(0.0F, 4.0F, 0xB0B0B0);

    private final float damage;
    private final float radius;
    private final int color;
    private final String key = name().toLowerCase(Locale.ROOT);

    BombType(float damage, float radius, int color) {
        this.damage = damage;
        this.radius = radius;
        this.color = color;
    }

    public float getDamage() {
        return damage;
    }

    public float getRadius() {
        return radius;
    }

    public int getColor() {
        return color;
    }

    public String getKey() {
        return key;
    }

    public static BombType byOrdinal(int ordinal) {
        BombType[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
