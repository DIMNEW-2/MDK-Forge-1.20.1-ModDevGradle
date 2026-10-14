package com.closky.rpgmod.weapon;

import com.closky.rpgmod.loot.WeaponClass;

/**
 * Implemented by every RPG weapon item. Provides the static (per item type) data; dynamic data lives in NBT,
 * see {@link com.closky.rpgmod.loot.RpgItemData}.
 */
public interface IRpgWeapon {
    WeaponClass getWeaponClass();

    WeaponTier getTier();

    /** Whether the weapon can be forged with a material in the smithing table. */
    default boolean isForgeable() {
        return true;
    }
}
