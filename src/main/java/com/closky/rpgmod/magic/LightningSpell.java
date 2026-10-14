package com.closky.rpgmod.magic;

import com.closky.rpgmod.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calls a (visual-only) lightning bolt at the targeted point and deals magic damage, optionally chaining to
 * nearby targets.
 */
public class LightningSpell extends Spell {
    private final double range;
    private final int chainTargets;

    public LightningSpell(SpellSchool school, Properties properties, double range, int chainTargets) {
        super(school, properties);
        this.range = range;
        this.chainTargets = chainTargets;
    }

    @Override
    public boolean cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation();
        }
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, caster, eye, end,
                caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D), e -> e instanceof LivingEntity && e != caster && e.isAlive());
        Vec3 strikePos = entityHit != null ? entityHit.getEntity().position() : end;
        LivingEntity first = entityHit != null ? (LivingEntity) entityHit.getEntity() : null;

        strike(level, strikePos);
        float damage = getDamage(power);
        List<LivingEntity> hitList = new ArrayList<>();
        if (first != null) {
            damage(level, caster, first, damage);
            hitList.add(first);
        } else {
            AABB area = new AABB(strikePos, strikePos).inflate(1.5D);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive())) {
                damage(level, caster, living, damage);
                hitList.add(living);
            }
        }
        // chain
        LivingEntity last = hitList.isEmpty() ? null : hitList.get(hitList.size() - 1);
        for (int i = 0; i < chainTargets && last != null; i++) {
            LivingEntity current = last;
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, current.getBoundingBox().inflate(5.0D),
                    e -> e != caster && e.isAlive() && !hitList.contains(e));
            candidates.sort(Comparator.comparingDouble(current::distanceToSqr));
            if (candidates.isEmpty()) {
                break;
            }
            LivingEntity next = candidates.get(0);
            strike(level, next.position());
            damage(level, caster, next, damage * 0.6F);
            hitList.add(next);
            last = next;
        }
        return true;
    }

    private void strike(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private void damage(ServerLevel level, Player caster, Entity target, float damage) {
        target.hurt(level.damageSources().indirectMagic(caster, caster), damage);
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 20, 0));
        }
    }
}
