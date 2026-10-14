package com.closky.rpgmod.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Crossbow bolt ammunition (tagged {@code rpgmod:bolts}; only RPG crossbows accept it).
 */
public class RpgBoltItem extends RpgArrowItem {
    public RpgBoltItem(ProjectileEffect effect, Properties properties) {
        super(effect, properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new RpgBoltEntity(level, shooter, effect);
    }
}
