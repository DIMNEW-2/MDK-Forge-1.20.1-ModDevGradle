package com.closky.rpgmod.weapon;

import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Throwable bomb (section 2A.5). Hold right click to throw further; explodes on impact or after 3 seconds.
 */
public class RpgBombItem extends Item implements IRpgWeapon {
    public static final int MAX_CHARGE_TICKS = 20;
    public static final float MIN_SPEED = 0.7F;
    public static final float MAX_SPEED = 1.6F;

    private final BombType type;

    public RpgBombItem(BombType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public BombType getBombType() {
        return type;
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.BOMB;
    }

    @Override
    public WeaponTier getTier() {
        return WeaponTier.IRON;
    }

    @Override
    public boolean isForgeable() {
        return false;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int used = Math.min(MAX_CHARGE_TICKS, getUseDuration(stack) - timeLeft);
        float power = used / (float) MAX_CHARGE_TICKS;
        float speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * power;

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BOMB_FUSE.get(), SoundSource.PLAYERS, 0.6F, 0.9F + level.random.nextFloat() * 0.2F);
        if (!level.isClientSide) {
            ThrownBombEntity bomb = new ThrownBombEntity(level, player, type);
            bomb.setItem(stack.copyWithCount(1));
            float damage = type.getDamage() + RpgItemData.getItemLevel(stack) * 0.25F;
            float radius = type.getRadius();
            if (PlayerClass.of(player) == PlayerClass.BOMBER) {
                damage *= 1.25F;
                radius += 1.0F;
            }
            bomb.setExplosionStats(damage, radius);
            bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, speed, 1.0F);
            level.addFreshEntity(bomb);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 10);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.rpgmod.bomb.stats", Math.round(type.getDamage()), Math.round(type.getRadius())).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.rpgmod.bomb." + type.getKey()).withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
