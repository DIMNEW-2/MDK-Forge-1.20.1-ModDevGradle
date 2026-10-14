package com.closky.rpgmod.weapon;

import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModItems;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Thrown bomb projectile (section 2A.5). Explodes on impact or when the fuse runs out; damage is dealt in an
 * area with per-type status effects. Blocks are only destroyed when {@code bombsDestroyBlocks} is enabled.
 */
public class ThrownBombEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> ID_TYPE = SynchedEntityData.defineId(ThrownBombEntity.class, EntityDataSerializers.INT);
    public static final int FUSE_TICKS = 60;

    private float damage = 6.0F;
    private float radius = 3.0F;
    private int fuse = FUSE_TICKS;

    public ThrownBombEntity(EntityType<? extends ThrownBombEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBombEntity(Level level, LivingEntity shooter, BombType type) {
        super(ModEntities.THROWN_BOMB.get(), shooter, level);
        this.entityData.set(ID_TYPE, type.ordinal());
        this.damage = type.getDamage();
        this.radius = type.getRadius();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_TYPE, 0);
    }

    public BombType getBombType() {
        return BombType.byOrdinal(this.entityData.get(ID_TYPE));
    }

    public void setExplosionStats(float damage, float radius) {
        this.damage = damage;
        this.radius = radius;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOMB_BASIC.get();
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 pos = this.position();
            this.level().addParticle(ParticleTypes.SMOKE, pos.x, pos.y + 0.2D, pos.z, 0.0D, 0.02D, 0.0D);
            if (this.tickCount % 4 == 0) {
                this.level().addParticle(ParticleTypes.FLAME, pos.x, pos.y + 0.25D, pos.z, 0.0D, 0.01D, 0.0D);
            }
        } else if (--fuse <= 0) {
            explode();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        BombType type = getBombType();
        Vec3 center = this.position();
        level.sendParticles(radius >= 4.0F ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        switch (type) {
            case FIRE -> level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5D, center.z, 60, radius * 0.5D, 0.5D, radius * 0.5D, 0.05D);
            case FROST -> level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.5D, center.z, 80, radius * 0.5D, 0.5D, radius * 0.5D, 0.05D);
            case LIGHTNING -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.5D, center.z, 80, radius * 0.5D, 0.5D, radius * 0.5D, 0.2D);
            case POISON -> level.sendParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.5D, center.z, 80, radius * 0.5D, 0.5D, radius * 0.5D, 0.05D);
            case SMOKE -> level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.5D, center.z, 120, radius * 0.5D, 0.8D, radius * 0.5D, 0.02D);
            default -> level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.5D, center.z, 40, radius * 0.4D, 0.4D, radius * 0.4D, 0.05D);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.BOMB_EXPLODE.get(), SoundSource.PLAYERS, 2.0F, type == BombType.SMOKE ? 1.4F : 0.9F + level.random.nextFloat() * 0.2F);

        Entity owner = this.getOwner();
        DamageSource source = level.damageSources().explosion(this, owner);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), LivingEntity::isAlive);
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity target : targets) {
            double dist = Math.sqrt(target.distanceToSqr(center));
            if (dist > radius) {
                continue;
            }
            float falloff = (float) (1.0D - (dist / radius) * 0.5D);
            float dealt = damage * falloff;
            if (target == owner) {
                dealt *= 0.5F;
            }
            if (dealt > 0.0F) {
                target.hurt(source, dealt);
                Vec3 push = target.position().subtract(center).normalize().scale(0.6D * falloff);
                target.push(push.x, 0.3D * falloff, push.z);
            }
            applyEffect(type, target, owner);
            hit.add(target);
        }
        if (type == BombType.LIGHTNING) {
            chainLightning(level, center, hit, owner, source);
        }
        if (RpgConfig.COMMON.bombsDestroyBlocks.get() && type != BombType.SMOKE) {
            destroyBlocks(level, center);
        }
        this.discard();
    }

    private void applyEffect(BombType type, LivingEntity target, Entity owner) {
        switch (type) {
            case FIRE -> target.setSecondsOnFire(4);
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), owner);
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 140));
            }
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
            case SMOKE -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0), owner);
            default -> {
            }
        }
    }

    private void chainLightning(ServerLevel level, Vec3 center, List<LivingEntity> alreadyHit, Entity owner, DamageSource source) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius + 5.0D),
                e -> e.isAlive() && e != owner && !alreadyHit.contains(e));
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        Vec3 from = center;
        int chained = 0;
        for (LivingEntity target : candidates) {
            if (chained >= 3) {
                break;
            }
            target.hurt(source, damage * 0.6F);
            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            int steps = 8;
            for (int i = 0; i <= steps; i++) {
                Vec3 p = from.lerp(to, i / (double) steps);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
            }
            from = to;
            chained++;
        }
    }

    private void destroyBlocks(ServerLevel level, Vec3 center) {
        int r = Math.max(1, Math.round(radius * 0.6F));
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            if (pos.distSqr(origin) > r * r) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < 10.0F && state.getFluidState().isEmpty()) {
                level.destroyBlock(pos, level.random.nextFloat() < 0.3F);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BombType", this.entityData.get(ID_TYPE));
        tag.putFloat("Damage", damage);
        tag.putFloat("Radius", radius);
        tag.putInt("Fuse", fuse);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ID_TYPE, tag.getInt("BombType"));
        this.damage = tag.getFloat("Damage");
        this.radius = tag.getFloat("Radius");
        this.fuse = tag.contains("Fuse") ? tag.getInt("Fuse") : FUSE_TICKS;
    }
}
