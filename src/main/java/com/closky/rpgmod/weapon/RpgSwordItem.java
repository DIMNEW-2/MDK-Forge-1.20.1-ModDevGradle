package com.closky.rpgmod.weapon;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.registry.ModSounds;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * RPG sword (section 2A.2). Right click performs a dash that empowers the next hit (5 s cooldown, costs mana).
 */
public class RpgSwordItem extends SwordItem implements IRpgWeapon {
    public static final int DASH_COOLDOWN_TICKS = 100;
    public static final float DASH_MANA_COST = 8.0F;
    public static final int EMPOWER_WINDOW_TICKS = 40;
    public static final float EMPOWERED_MULTIPLIER = 1.5F;

    private final WeaponTier tier;

    public RpgSwordItem(WeaponTier tier, Properties properties) {
        super(tier, Math.round(tier.getSwordDamage() - 1.0F), tier.getSwordSpeed() - 4.0F, properties);
        this.tier = tier;
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.SWORD;
    }

    @Override
    public WeaponTier getTier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!RpgConfig.COMMON.swordDashEnabled.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null || data.getMana() < DASH_MANA_COST) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.rpgmod.not_enough_mana"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, Math.max(0.1D, look.y * 0.5D), look.z).normalize().scale(1.1D);
        player.setDeltaMovement(player.getDeltaMovement().add(dash));
        player.hurtMarked = true;
        player.getCooldowns().addCooldown(this, DASH_COOLDOWN_TICKS);
        player.swing(hand, true);

        if (!level.isClientSide) {
            data.consumeMana(DASH_MANA_COST);
            data.setEmpoweredUntil(level.getGameTime() + EMPOWER_WINDOW_TICKS);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SWORD_DASH.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2D, player.getZ(), 12, 0.3D, 0.1D, 0.3D, 0.02D);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return WeaponAttributes.withRpgBonuses(stack, slot, super.getAttributeModifiers(slot, stack), true);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return WeaponAttributes.scaledMaxDamage(stack, super.getMaxDamage(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.rpgmod.sword.special").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
