package com.closky.rpgmod.magic;

import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModParticles;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * Projectile fired by {@link ProjectileSpell}s: a glowing bolt of the school's colour. Flies straight (no gravity),
 * deals magic damage and applies the school's on-hit effect.
 */
public class SpellProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> SCHOOL = SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.INT);

    private float damage = 4.0F;
    private float splashRadius;
    private int life;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SpellProjectileEntity(Level level, LivingEntity owner, SpellSchool school, float damage, float splashRadius) {
        super(ModEntities.SPELL_PROJECTILE.get(), owner, level);
        this.entityData.set(SCHOOL, school.ordinal());
        this.damage = damage;
        this.splashRadius = splashRadius;
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCHOOL, 0);
    }

    public SpellSchool getSchool() {
        return SpellSchool.byOrdinal(this.entityData.get(SCHOOL));
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnTrail();
        } else if (++life > 100) {
            this.discard();
        }
    }

    private void spawnTrail() {
        ParticleOptions particle = switch (getSchool()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ModParticles.NATURE_LEAF.get();
        };
        Vec3 motion = this.getDeltaMovement();
        for (int i = 0; i < 2; i++) {
            double t = i / 2.0D;
            this.level().addParticle(particle, this.getX() - motion.x * t, this.getY() - motion.y * t, this.getZ() - motion.z * t,
                    (this.random.nextDouble() - 0.5D) * 0.05D, (this.random.nextDouble() - 0.5D) * 0.05D, (this.random.nextDouble() - 0.5D) * 0.05D);
        }
        this.level().addParticle(ModParticles.MANA_SPARK.get(), this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (target == owner) {
            return;
        }
        DamageSource source = this.damageSources().indirectMagic(this, owner);
        if (target.hurt(source, damage) && target instanceof LivingEntity living) {
            applySchoolEffect(living);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel server = (ServerLevel) this.level();
        if (splashRadius > 0.0F) {
            Entity owner = this.getOwner();
            AABB box = this.getBoundingBox().inflate(splashRadius);
            List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive());
            for (LivingEntity living : targets) {
                if (result instanceof EntityHitResult ehr && ehr.getEntity() == living) {
                    continue;
                }
                double dist = living.distanceTo(this);
                float falloff = (float) Math.max(0.25D, 1.0D - dist / (splashRadius + 1.0D));
                if (living.hurt(this.damageSources().indirectMagic(this, owner), damage * falloff * 0.6F)) {
                    applySchoolEffect(living);
                }
            }
        }
        spawnImpact(server);
        this.discard();
    }

    private void applySchoolEffect(LivingEntity living) {
        switch (getSchool()) {
            case FIRE -> living.setSecondsOnFire(4);
            case FROST -> {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                living.setTicksFrozen(Math.min(living.getTicksRequiredToFreeze() + 40, living.getTicksFrozen() + 80));
            }
            case LIGHTNING -> {
                living.addEffect(new MobEffectInstance(ModEffects.STUN.get(), 15, 0));
                living.invulnerableTime = 0;
            }
            case NATURE -> living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        }
    }

    private void spawnImpact(ServerLevel server) {
        ParticleOptions particle = switch (getSchool()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
        };
        server.sendParticles(particle, this.getX(), this.getY(), this.getZ(), 12, 0.2D, 0.2D, 0.2D, 0.08D);
        server.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SPELL_IMPACT.get(), SoundSource.PLAYERS, 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Splash", splashRadius);
        tag.putInt("School", this.entityData.get(SCHOOL));
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        splashRadius = tag.getFloat("Splash");
        this.entityData.set(SCHOOL, tag.getInt("School"));
        life = tag.getInt("Life");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
