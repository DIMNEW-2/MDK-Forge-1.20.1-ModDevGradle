package com.closky.rpgmod.weapon;

import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModSounds;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;

import java.util.List;
import java.util.UUID;

/**
 * RPG spear (section 2A.4): long reach in melee, can be charged and thrown; the thrown spear sticks into blocks
 * and can be picked up again.
 */
public class RpgSpearItem extends RpgMeleeWeaponItem {
    public static final UUID REACH_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0010");
    public static final double EXTRA_REACH = 1.5D;
    public static final int MIN_THROW_TICKS = 10;
    public static final int CHARGE_TICKS = 40;
    public static final float BASE_THROW_SPEED = 2.5F;

    public RpgSpearItem(WeaponTier tier, Properties properties) {
        super(tier, tier.getSpearMeleeDamage(), 1.4F, properties);
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.SPEAR;
    }

    @Override
    protected void addExtraDefaultModifiers(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder) {
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_UUID, "Spear reach", EXTRA_REACH, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack) - timeLeft;
        if (used < MIN_THROW_TICKS) {
            return;
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(entity.getUsedItemHand()));

            boolean charged = RpgConfig.COMMON.spearChargeEnabled.get() && used >= CHARGE_TICKS;
            float damage = tier.getSpearThrowDamage() * (charged ? 1.5F : 1.0F);
            float speed = BASE_THROW_SPEED;
            WeaponMaterial material = RpgItemData.getMaterial(stack);
            if (material != null) {
                speed *= material.getProjectileSpeedMultiplier();
                speed *= material.getRangeMultiplier();
            }

            ThrownSpearEntity spear = new ThrownSpearEntity(level, player, stack.copy());
            boolean spearman = PlayerClass.of(player) == PlayerClass.SPEARMAN;
            if (spearman) {
                damage *= 1.2F;
                spear.setReturnToOwner(RpgConfig.COMMON.spearReturnEnabled.get());
            }
            spear.setSpearDamage(damage);
            spear.setArmorPenetration(material == null ? 0.0F : material.getArmorPenetration());
            spear.setCharged(charged);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 1.0F);
            if (player.getAbilities().instabuild) {
                spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(spear);
            level.playSound(null, spear, ModSounds.SPEAR_THROW.get(), SoundSource.PLAYERS, 1.0F, charged ? 0.8F : 1.0F);
            if (!player.getAbilities().instabuild) {
                player.getInventory().removeItem(stack);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    protected void appendSpecialTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.rpgmod.spear.throw_damage", Math.round(tier.getSpearThrowDamage())).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.rpgmod.spear.special").withStyle(ChatFormatting.DARK_GREEN));
    }

    /** Allows hurting while the spear is being charged, like the trident. */
    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
}
