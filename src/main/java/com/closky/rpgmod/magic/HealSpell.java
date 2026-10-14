package com.closky.rpgmod.magic;

import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Nature healing: heals the caster (and, with a radius, nearby players / tamed animals). */
public class HealSpell extends Spell {
    private final float healAmount;
    private final int regenTicks;

    public HealSpell(SpellSchool school, Properties properties, float healAmount, int regenTicks) {
        super(school, properties);
        this.healAmount = healAmount;
        this.regenTicks = regenTicks;
    }

    @Override
    public boolean cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        AttributeInstance healing = caster.getAttribute(ModAttributes.HEALING_POWER.get());
        float multiplier = power * (healing == null ? 1.0F : (float) healing.getValue());
        float amount = healAmount * multiplier;
        if (getRadius() <= 0.0F) {
            if (caster.getHealth() >= caster.getMaxHealth() && regenTicks <= 0) {
                return false;
            }
            heal(level, caster, amount);
            return true;
        }
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(getRadius()),
                e -> e == caster || e instanceof Player || e.isAlliedTo(caster));
        for (LivingEntity ally : allies) {
            heal(level, ally, amount);
        }
        return true;
    }

    private void heal(ServerLevel level, LivingEntity target, float amount) {
        target.heal(amount);
        if (regenTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0));
        }
        level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.2D, target.getZ(), 6, 0.4D, 0.4D, 0.4D, 0.0D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.HEAL.get(), SoundSource.PLAYERS, 0.8F, 1.1F);
    }
}
