package com.closky.rpgmod.weapon;

import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.registry.ModEntities;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Thrown spear projectile (section 2A.4). Behaves like a trident: sticks into blocks, can be picked up by the
 * thrower, has a chance to root the target and optionally returns to a spearman.
 */
public class ThrownSpearEntity extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> ID_ITEM = SynchedEntityData.defineId(ThrownSpearEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> ID_RETURNING = SynchedEntityData.defineId(ThrownSpearEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ID_CHARGED = SynchedEntityData.defineId(ThrownSpearEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float ROOT_CHANCE = 0.3F;
    public static final int ROOT_TICKS = 30;
    public static final int RETURN_DELAY_TICKS = 60;

    private float spearDamage = 6.0F;
    private float armorPenetration;
    private boolean returnToOwner;
    private boolean dealtDamage;
    private int returnTicks;
    private int groundLife;

    public ThrownSpearEntity(EntityType<? extends ThrownSpearEntity> type, Level level) {
        super(type, level);
    }

    public ThrownSpearEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.THROWN_SPEAR.get(), shooter, level);
        this.entityData.set(ID_ITEM, stack.copy());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_ITEM, ItemStack.EMPTY);
        this.entityData.define(ID_RETURNING, false);
        this.entityData.define(ID_CHARGED, false);
    }

    public ItemStack getSpearItem() {
        return this.entityData.get(ID_ITEM);
    }

    public void setSpearDamage(float damage) {
        this.spearDamage = damage;
    }

    public void setArmorPenetration(float armorPenetration) {
        this.armorPenetration = armorPenetration;
    }

    public float getArmorPenetration() {
        return armorPenetration;
    }

    public void setReturnToOwner(boolean returnToOwner) {
        this.returnToOwner = returnToOwner;
    }

    public void setCharged(boolean charged) {
        this.entityData.set(ID_CHARGED, charged);
    }

    public boolean isCharged() {
        return this.entityData.get(ID_CHARGED);
    }

    public boolean isReturning() {
        return this.entityData.get(ID_RETURNING);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }
        Entity owner = this.getOwner();
        if (this.returnToOwner && (this.dealtDamage || this.isNoPhysics()) && owner != null) {
            if (++returnTicks >= RETURN_DELAY_TICKS) {
                if (!isAcceptableReturnOwner()) {
                    if (!this.level().isClientSide && this.pickup == AbstractArrow.Pickup.ALLOWED) {
                        this.spawnAtLocation(this.getPickupItem(), 0.1F);
                    }
                    this.discard();
                } else {
                    this.setNoPhysics(true);
                    this.entityData.set(ID_RETURNING, true);
                    Vec3 toOwner = owner.getEyePosition().subtract(this.position());
                    this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.045D, this.getZ());
                    if (this.level().isClientSide) {
                        this.yOld = this.getY();
                    }
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(toOwner.normalize().scale(0.15D)));
                    if (returnTicks == RETURN_DELAY_TICKS) {
                        this.playSound(SoundEvents.TRIDENT_RETURN, 2.0F, 1.0F);
                    }
                }
            }
        }
        super.tick();
    }

    private boolean isAcceptableReturnOwner() {
        Entity owner = this.getOwner();
        if (owner != null && owner.isAlive()) {
            return !(owner instanceof ServerPlayer) || !owner.isSpectator();
        }
        return false;
    }

    @Override
    protected ItemStack getPickupItem() {
        return getSpearItem().copy();
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return this.dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = this.spearDamage;
        if (target instanceof LivingEntity living) {
            damage += EnchantmentHelper.getDamageBonus(getSpearItem(), living.getMobType());
            if (armorPenetration > 0.0F) {
                // Armor penetration: add back a share of what the armor would absorb (approximation of the vanilla formula).
                damage += living.getArmorValue() * 0.04F * damage * armorPenetration;
            }
        }
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        this.dealtDamage = true;
        if (target.hurt(source, damage)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }
            if (target instanceof LivingEntity living) {
                if (owner instanceof LivingEntity livingOwner) {
                    EnchantmentHelper.doPostHurtEffects(living, livingOwner);
                    EnchantmentHelper.doPostDamageEffects(livingOwner, living);
                }
                this.doPostHurtEffects(living);
                if (this.random.nextFloat() < ROOT_CHANCE * (isCharged() ? 1.5F : 1.0F)) {
                    living.addEffect(new MobEffectInstance(ModEffects.ROOT.get(), ROOT_TICKS, 0, false, true), owner);
                }
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(ModSounds.SPEAR_HIT.get(), 1.0F, 1.0F);
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player) || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void playerTouch(Player player) {
        if (this.ownedBy(player) || this.getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spear", 10)) {
            this.entityData.set(ID_ITEM, ItemStack.of(tag.getCompound("Spear")));
        }
        this.dealtDamage = tag.getBoolean("DealtDamage");
        this.spearDamage = tag.getFloat("SpearDamage");
        this.armorPenetration = tag.getFloat("ArmorPen");
        this.returnToOwner = tag.getBoolean("Return");
        this.entityData.set(ID_CHARGED, tag.getBoolean("Charged"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Spear", getSpearItem().save(new CompoundTag()));
        tag.putBoolean("DealtDamage", this.dealtDamage);
        tag.putFloat("SpearDamage", this.spearDamage);
        tag.putFloat("ArmorPen", this.armorPenetration);
        tag.putBoolean("Return", this.returnToOwner);
        tag.putBoolean("Charged", isCharged());
    }

    /** Spears stay around for 5 minutes (vanilla arrows: 1 minute) so that they can be collected again. */
    @Override
    protected void tickDespawn() {
        if (this.pickup != AbstractArrow.Pickup.ALLOWED) {
            super.tickDespawn();
            return;
        }
        if (!this.returnToOwner && ++groundLife >= 6000) {
            this.discard();
        }
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }
}
