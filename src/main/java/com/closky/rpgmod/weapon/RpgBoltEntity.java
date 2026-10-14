package com.closky.rpgmod.weapon;

import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Crossbow bolt: heavier arrow with more damage and innate piercing (section 2B.3/2B.4).
 */
public class RpgBoltEntity extends RpgArrowEntity {
    public RpgBoltEntity(EntityType<? extends RpgBoltEntity> type, Level level) {
        super(type, level);
    }

    public RpgBoltEntity(Level level, LivingEntity shooter, ProjectileEffect effect) {
        super(ModEntities.RPG_BOLT.get(), level, shooter, effect);
        this.setBaseDamage(this.getBaseDamage() + 1.0D);
        this.setPierceLevel((byte) Math.max(this.getPierceLevel(), 1));
    }

    @Override
    public boolean isBolt() {
        return true;
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.boltFor(getEffect()));
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.CROSSBOW_HIT;
    }
}
