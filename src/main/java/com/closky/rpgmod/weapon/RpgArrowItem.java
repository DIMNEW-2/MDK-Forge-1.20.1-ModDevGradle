package com.closky.rpgmod.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Custom arrow ammunition. Works with vanilla and RPG bows (tagged {@code minecraft:arrows}).
 */
public class RpgArrowItem extends ArrowItem {
    protected final ProjectileEffect effect;

    public RpgArrowItem(ProjectileEffect effect, Properties properties) {
        super(properties);
        this.effect = effect;
    }

    public ProjectileEffect getEffect() {
        return effect;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new RpgArrowEntity(level, shooter, effect);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rpgmod.projectile." + effect.getKey()).withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
