package com.closky.rpgmod.weapon;

import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModSounds;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * RPG bow (section 2B.2). Damage and arrow speed come from the tier, forging material, player stats and class.
 * Shift + right click starts a triple-shot volley (10 s cooldown).
 */
public class RpgBowItem extends BowItem implements IRpgWeapon {
    public static final String TAG_TRIPLE = "rpgmod:triple_shot";
    public static final int TRIPLE_SHOT_COOLDOWN = 200;

    private final WeaponTier tier;

    public RpgBowItem(WeaponTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.BOW;
    }

    @Override
    public WeaponTier getTier() {
        return tier;
    }

    /** Draw speed multiplier (gold material draws faster). */
    public float getDrawSpeedMultiplier(ItemStack stack) {
        WeaponMaterial material = RpgItemData.getMaterial(stack);
        return material == null ? 1.0F : material.getSpeedMultiplier();
    }

    public float getPower(ItemStack stack, int useTicks) {
        return getPowerForTime(Math.round(useTicks * getDrawSpeedMultiplier(stack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean triple = player.isShiftKeyDown() && RpgConfig.COMMON.bowTripleShotEnabled.get() && !player.getCooldowns().isOnCooldown(this);
        if (stack.getTag() != null && stack.getTag().contains(TAG_TRIPLE)) {
            stack.getTag().remove(TAG_TRIPLE);
        }
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (triple && result.getResult().consumesAction()) {
            stack.getOrCreateTag().putBoolean(TAG_TRIPLE, true);
        }
        return result;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        CompoundTag tag = stack.getTag();
        boolean triple = tag != null && tag.getBoolean(TAG_TRIPLE);
        if (tag != null) {
            tag.remove(TAG_TRIPLE);
        }

        boolean infinite = player.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
        ItemStack ammo = player.getProjectile(stack);
        int useTicks = this.getUseDuration(stack) - timeLeft;
        useTicks = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, level, player, useTicks, !ammo.isEmpty() || infinite);
        if (useTicks < 0) {
            return;
        }
        if (ammo.isEmpty() && !infinite) {
            return;
        }
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.ARROW);
        }
        float power = getPower(stack, useTicks);
        if (power < 0.1F) {
            return;
        }
        boolean freeAmmo = player.getAbilities().instabuild
                || (ammo.getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(ammo, stack, player));

        if (!level.isClientSide) {
            WeaponMaterial material = RpgItemData.getMaterial(stack);
            boolean archer = PlayerClass.of(player) == PlayerClass.ARCHER;
            float speedMult = tier.getBowArrowSpeed()
                    * (material == null ? 1.0F : material.getProjectileSpeedMultiplier() * material.getRangeMultiplier())
                    * (archer ? 1.2F : 1.0F);
            float velocity = power * 3.0F * speedMult;
            float damageMult = 1.0F
                    + (float) player.getAttributeValue(ModAttributes.RANGED_DAMAGE.get())
                    + (archer ? 0.15F : 0.0F);
            float tierDamage = tier.getBowDamage() * damageMult + (material == null ? 0.0F : material.getBonusDamage());
            int shots = triple ? 3 : 1;
            for (int i = 0; i < shots; i++) {
                ArrowItem arrowItem = (ArrowItem) (ammo.getItem() instanceof ArrowItem ? ammo.getItem() : Items.ARROW);
                AbstractArrow arrow = arrowItem.createArrow(level, ammo, player);
                arrow = customArrow(arrow);
                double effectBonus = arrow.getBaseDamage() - 2.0D;
                arrow.setBaseDamage((tierDamage + effectBonus) / (3.0D * speedMult));
                float yawOffset = shots == 1 ? 0.0F : (i - 1) * 10.0F;
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F, velocity, 1.0F);
                if (power >= 1.0F) {
                    arrow.setCritArrow(true);
                }
                int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                if (powerLevel > 0) {
                    arrow.setBaseDamage(arrow.getBaseDamage() + powerLevel * 0.5D + 0.5D);
                }
                int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
                if (punch > 0) {
                    arrow.setKnockback(punch);
                }
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
                    arrow.setSecondsOnFire(100);
                }
                if (material != null && arrow instanceof RpgArrowEntity rpgArrow) {
                    rpgArrow.setArmorPenetration(material.getArmorPenetration());
                }
                if (material != null && material.hasOnHitEffect() && punch == 0) {
                    arrow.setKnockback(1);
                }
                if (freeAmmo || i > 0 || (player.getAbilities().instabuild && (ammo.is(Items.SPECTRAL_ARROW) || ammo.is(Items.TIPPED_ARROW)))) {
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                level.addFreshEntity(arrow);
            }
            stack.hurtAndBreak(triple ? 3 : 1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            if (triple) {
                player.getCooldowns().addCooldown(this, TRIPLE_SHOT_COOLDOWN);
            }
            if (power >= 1.0F && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getEyeY() - 0.2D, player.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.05D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (!freeAmmo && !player.getAbilities().instabuild) {
            ammo.shrink(1);
            if (ammo.isEmpty()) {
                player.getInventory().removeItem(ammo);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int useTicks = getUseDuration(stack) - remaining;
        if (useTicks == 1 || (useTicks > 1 && getPower(stack, useTicks) >= 1.0F && getPower(stack, useTicks - 1) < 1.0F)) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.BOW_DRAW.get(), SoundSource.PLAYERS, 0.5F, useTicks == 1 ? 1.0F : 1.5F);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return WeaponAttributes.withRpgBonuses(stack, slot, super.getAttributeModifiers(slot, stack), false);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return WeaponAttributes.scaledMaxDamage(stack, tier.getUses());
    }

    @Override
    public int getDefaultProjectileRange() {
        return Math.round(15.0F * tier.getBowRange());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.rpgmod.bow.stats", Math.round(tier.getBowDamage()), String.format(java.util.Locale.ROOT, "%.1f", tier.getBowArrowSpeed())).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.rpgmod.bow.special").withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
