package com.closky.rpgmod.loot;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Gameplay class of a weapon (section 2.2).
 */
public enum WeaponClass {
    SWORD,
    MACE,
    SPEAR,
    BOMB,
    BOW,
    CROSSBOW,
    AXE,
    DAGGER,
    STAFF,
    TOOL;

    private final String key = name().toLowerCase(Locale.ROOT);

    public String getSerializedName() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("weapon_class.rpgmod." + key);
    }

    public static WeaponClass byName(String name) {
        for (WeaponClass c : values()) {
            if (c.key.equals(name)) {
                return c;
            }
        }
        return SWORD;
    }
}
