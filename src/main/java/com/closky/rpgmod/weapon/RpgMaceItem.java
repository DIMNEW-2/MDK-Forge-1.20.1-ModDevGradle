package com.closky.rpgmod.weapon;

import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModEffects;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * RPG mace (section 2A.3): slow, heavy, chance to stun; Shift + right click performs an area smash.
 */
public class RpgMaceItem extends RpgMeleeWeaponItem {
    public static final int STUN_TICKS = 40;
    public static final int AOE_COOLDOWN_TICKS = 160;
    public static final double AOE_RADIUS = 2.0D;

    public RpgMaceItem(WeaponTier tier, Properties properties) {
        super(tier, tier.getMaceDamage(), tier.getMaceSpeed(), properties);
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.MACE;
    }

    public float getStunChance(LivingEntity attacker) {
        float chance = tier.getMaceStunChance();
        if (PlayerClass.of(attacker) == PlayerClass.WARRIOR) {
            chance += 0.05F;
        }
        return chance;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide) {
            if (RpgConfig.COMMON.maceStunEnabled.get() && attacker.getRandom().nextFloat() < getStunChance(attacker)) {
                target.addEffect(new MobEffectInstance(ModEffects.STUN.get(), STUN_TICKS, 0, false, true), attacker);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.1D);
                }
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.MACE_SMASH.get(), SoundSource.PLAYERS, 0.7F, 0.8F + attacker.getRandom().nextFloat() * 0.3F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.1D, target.getZ(), 6, 0.3D, 0.05D, 0.3D, 0.02D);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !RpgConfig.COMMON.maceAoeEnabled.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, AOE_COOLDOWN_TICKS);
        player.swing(hand, true);
        if (!level.isClientSide) {
            float damage = (getAttackDamage() + 1.0F) * (PlayerClass.of(player) == PlayerClass.WARRIOR ? 1.15F : 1.0F);
            AABB area = player.getBoundingBox().inflate(AOE_RADIUS, 1.0D, AOE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player) && player.distanceToSqr(e) <= AOE_RADIUS * AOE_RADIUS + 1.0D);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), damage);
                Vec3 push = target.position().subtract(player.position()).normalize().scale(0.8D);
                target.push(push.x, 0.4D, push.z);
                if (RpgConfig.COMMON.maceStunEnabled.get() && player.getRandom().nextFloat() < getStunChance(player)) {
                    target.addEffect(new MobEffectInstance(ModEffects.STUN.get(), STUN_TICKS, 0, false, true), player);
                }
            }
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MACE_SMASH.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2.0D) * i / 24.0D;
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            player.getX() + Math.cos(angle) * AOE_RADIUS, player.getY() + 0.1D, player.getZ() + Math.sin(angle) * AOE_RADIUS,
                            1, 0.0D, 0.02D, 0.0D, 0.0D);
                }
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    protected void appendSpecialTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.rpgmod.mace.stun", Math.round(tier.getMaceStunChance() * 100.0F)).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.rpgmod.mace.special").withStyle(ChatFormatting.DARK_GREEN));
    }
}
