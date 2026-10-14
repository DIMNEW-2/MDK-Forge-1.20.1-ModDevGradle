package com.closky.rpgmod.weapon;

import com.closky.rpgmod.loot.RpgItemData;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base class for custom melee weapons that are not swords (maces, spears). Mirrors the relevant parts of
 * {@link net.minecraft.world.item.SwordItem} and adds material / bonus modifiers.
 */
public abstract class RpgMeleeWeaponItem extends TieredItem implements Vanishable, IRpgWeapon {
    protected final WeaponTier tier;
    protected final float attackDamage;
    protected final float attackSpeed;
    /** Built lazily: extra modifiers may reference attributes that are registered after items (e.g. Forge reach). */
    private Multimap<Attribute, AttributeModifier> defaultModifiers;

    /**
     * @param displayedDamage total attack damage shown in the tooltip (includes the player's base 1.0)
     * @param attacksPerSecond attack speed shown in the tooltip
     */
    protected RpgMeleeWeaponItem(WeaponTier tier, float displayedDamage, float attacksPerSecond, Properties properties) {
        super(tier, properties);
        this.tier = tier;
        this.attackDamage = displayedDamage - 1.0F;
        this.attackSpeed = attacksPerSecond - 4.0F;
    }

    private Multimap<Attribute, AttributeModifier> defaultModifiers() {
        if (defaultModifiers == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", this.attackDamage, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", this.attackSpeed, AttributeModifier.Operation.ADDITION));
            addExtraDefaultModifiers(builder);
            defaultModifiers = builder.build();
        }
        return defaultModifiers;
    }

    /** Hook for subclasses to add further main-hand modifiers (e.g. reach). */
    protected void addExtraDefaultModifiers(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder) {
    }

    @Override
    public WeaponTier getTier() {
        return tier;
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(2, entity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? defaultModifiers() : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return WeaponAttributes.withRpgBonuses(stack, slot, getDefaultAttributeModifiers(slot), true);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return WeaponAttributes.scaledMaxDamage(stack, super.getMaxDamage(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        appendSpecialTooltip(stack, tooltip);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    protected void appendSpecialTooltip(ItemStack stack, List<Component> tooltip) {
    }
}
