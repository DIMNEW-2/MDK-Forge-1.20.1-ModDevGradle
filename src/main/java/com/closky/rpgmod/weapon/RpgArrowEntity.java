package com.closky.rpgmod.weapon;

import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModItems;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Custom arrow with elemental effects (section 2B.4).
 */
public class RpgArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> ID_EFFECT = SynchedEntityData.defineId(RpgArrowEntity.class, EntityDataSerializers.INT);
    public static final float EXPLOSION_RADIUS = 2.0F;

    private float armorPenetration;

    public RpgArrowEntity(EntityType<? extends RpgArrowEntity> type, Level level) {
        super(type, level);
    }

    public RpgArrowEntity(Level level, LivingEntity shooter, ProjectileEffect effect) {
        this(ModEntities.RPG_ARROW.get(), level, shooter, effect);
    }

    protected RpgArrowEntity(EntityType<? extends RpgArrowEntity> type, Level level, LivingEntity shooter, ProjectileEffect effect) {
        super(type, shooter, level);
        setEffect(effect);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_EFFECT, 0);
    }

    public ProjectileEffect getEffect() {
        return ProjectileEffect.byOrdinal(this.entityData.get(ID_EFFECT));
    }

    public void setEffect(ProjectileEffect effect) {
        this.entityData.set(ID_EFFECT, effect.ordinal());
        this.setBaseDamage(this.getBaseDamage() + effect.getBonusDamage());
        if (effect == ProjectileEffect.PIERCING) {
            this.setPierceLevel((byte) Math.max(this.getPierceLevel(), 2));
        }
        if (effect == ProjectileEffect.FIRE) {
            this.setSecondsOnFire(100);
        }
    }

    public void setArmorPenetration(float armorPenetration) {
        this.armorPenetration = armorPenetration;
    }

    public float getArmorPenetration() {
        return armorPenetration;
    }

    /** Whether this projectile is a crossbow bolt (affects renderer texture and pickup item). */
    public boolean isBolt() {
        return false;
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.arrowFor(getEffect()));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            ParticleOptions particle = switch (getEffect()) {
                case FIRE -> ParticleTypes.FLAME;
                case FROST -> ParticleTypes.SNOWFLAKE;
                case POISON -> ParticleTypes.SNEEZE;
                case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
                case GLOWING -> ParticleTypes.END_ROD;
                case EXPLOSIVE -> ParticleTypes.SMOKE;
                default -> null;
            };
            if (particle != null) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        Entity owner = this.getOwner();
        switch (getEffect()) {
            case FIRE -> target.setSecondsOnFire(5);
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), owner);
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 100));
            }
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
            case GLOWING -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), owner);
            case LIGHTNING -> chain(target, owner);
            default -> {
            }
        }
    }

    private void chain(LivingEntity first, Entity owner) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(5.0D),
                e -> e.isAlive() && e != first && e != owner);
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(first)));
        Vec3 from = first.position().add(0.0D, first.getBbHeight() * 0.5D, 0.0D);
        int chained = 0;
        for (LivingEntity target : candidates) {
            if (chained >= 2) {
                break;
            }
            target.hurt(level.damageSources().arrow(this, owner), (float) this.getBaseDamage() * 1.5F);
            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            for (int i = 0; i <= 6; i++) {
                Vec3 p = from.lerp(to, i / 6.0D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
            }
            from = to;
            chained++;
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (armorPenetration > 0.0F && result.getEntity() instanceof LivingEntity living) {
            // Compensate for the target's armor by scaling the base damage.
            double bonus = living.getArmorValue() * 0.04D * armorPenetration;
            this.setBaseDamage(this.getBaseDamage() * (1.0D + bonus));
        }
        super.onHitEntity(result);
        if (getEffect() == ProjectileEffect.EXPLOSIVE) {
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (getEffect() == ProjectileEffect.EXPLOSIVE) {
            explode();
        }
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        Vec3 center = this.position();
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, ModSounds.BOMB_EXPLODE.get(), SoundSource.NEUTRAL, 1.0F, 1.3F);
        Entity owner = this.getOwner();
        float damage = (float) Math.max(2.0D, this.getBaseDamage() * 1.5D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(EXPLOSION_RADIUS), LivingEntity::isAlive)) {
            double dist = Math.sqrt(target.distanceToSqr(center));
            if (dist <= EXPLOSION_RADIUS) {
                target.hurt(level.damageSources().explosion(this, owner), damage * (float) (1.0D - dist / EXPLOSION_RADIUS * 0.5D));
            }
        }
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Effect", this.entityData.get(ID_EFFECT));
        tag.putFloat("ArmorPen", armorPenetration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ID_EFFECT, tag.getInt("Effect"));
        this.armorPenetration = tag.getFloat("ArmorPen");
    }
}
