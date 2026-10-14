package com.closky.rpgmod.magic;

import com.closky.rpgmod.registry.ModEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Damages (or affects) every hostile living entity around the caster, applying the school's crowd-control effect.
 */
public class AoeSpell extends Spell {
    private final boolean knockback;

    public AoeSpell(SpellSchool school, Properties properties, boolean knockback) {
        super(school, properties);
        this.knockback = knockback;
    }

    @Override
    public boolean cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        float radius = Math.max(1.0F, getRadius());
        AABB box = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && !e.isAlliedTo(caster) && caster.distanceTo(e) <= radius + 0.5F);
        float damage = getDamage(power);
        for (LivingEntity target : targets) {
            if (damage > 0 && target.hurt(level.damageSources().indirectMagic(caster, caster), damage)) {
                target.invulnerableTime = 0;
            }
            applyEffect(target, power);
            if (knockback) {
                Vec3 push = target.position().subtract(caster.position()).normalize().scale(0.8D);
                target.push(push.x, 0.3D, push.z);
                target.hurtMarked = true;
            }
        }
        spawnRing(level, caster, radius);
        return true;
    }

    protected void applyEffect(LivingEntity target, float power) {
        switch (getSchool()) {
            case FIRE -> target.setSecondsOnFire(5);
            case FROST -> {
                target.addEffect(new MobEffectInstance(ModEffects.ROOT.get(), 40, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            case LIGHTNING -> target.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 30, 0));
            case NATURE -> {
                target.addEffect(new MobEffectInstance(ModEffects.ROOT.get(), 60, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            }
        }
    }

    private void spawnRing(ServerLevel level, Player caster, float radius) {
        ParticleOptions particle = switch (getSchool()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.COMPOSTER;
        };
        int points = (int) (radius * 12);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 * i) / points;
            double x = caster.getX() + Math.cos(angle) * radius;
            double z = caster.getZ() + Math.sin(angle) * radius;
            level.sendParticles(particle, x, caster.getY() + 0.3D, z, 1, 0.0D, 0.15D, 0.0D, 0.02D);
        }
    }
}
