package com.closky.rpgmod.weapon;

import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.util.ModTags;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * RPG crossbow (section 2B.3). Reload time, damage and range come from the tier and forging material.
 * Accepts arrows, RPG bolts and fireworks. Shift + right click (when loaded) fires a fan of three bolts.
 */
public class RpgCrossbowItem extends CrossbowItem implements IRpgWeapon {
    private static final String TAG_CHARGED_PROJECTILES = "ChargedProjectiles";
    public static final int FAN_COOLDOWN = 240;
    public static final float ARROW_POWER = 3.15F;
    public static final float FIREWORK_POWER = 1.6F;

    private final WeaponTier tier;
    private boolean startSoundPlayed;
    private boolean midLoadSoundPlayed;

    public RpgCrossbowItem(WeaponTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.CROSSBOW;
    }

    @Override
    public WeaponTier getTier() {
        return tier;
    }

    // ------------------------------------------------------------------ ammunition

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return s -> s.is(ItemTags.ARROWS) || s.is(ModTags.Items.BOLTS) || s.is(Items.FIREWORK_ROCKET);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return s -> s.is(ItemTags.ARROWS) || s.is(ModTags.Items.BOLTS);
    }

    // ------------------------------------------------------------------ charging

    /** Reload duration in ticks: tier table, Quick Charge, redstone/gold material. */
    public int getRpgChargeDuration(ItemStack stack) {
        float ticks = tier.getCrossbowReloadTicks();
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        ticks -= 5 * quickCharge;
        WeaponMaterial material = RpgItemData.getMaterial(stack);
        if (material != null) {
            ticks /= material.getProjectileSpeedMultiplier() == 1.0F ? 1.0F : 1.25F; // redstone: -20%
            ticks /= material.getSpeedMultiplier();                                  // gold: faster
        }
        return Math.max(5, Math.round(ticks));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return getRpgChargeDuration(stack) + 3;
    }

    public float getChargeProgress(ItemStack stack, int useTicks) {
        return Math.min(1.0F, useTicks / (float) getRpgChargeDuration(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isCharged(stack)) {
            boolean fan = player.isShiftKeyDown() && RpgConfig.COMMON.crossbowMultiShotEnabled.get() && !player.getCooldowns().isOnCooldown(this);
            performRpgShooting(level, player, hand, stack, fan);
            setCharged(stack, false);
            if (fan) {
                player.getCooldowns().addCooldown(this, FAN_COOLDOWN);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (!player.getProjectile(stack).isEmpty()) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int useTicks = this.getUseDuration(stack) - timeLeft;
        float progress = getChargeProgress(stack, useTicks);
        if (progress >= 1.0F && !isCharged(stack) && tryLoadProjectiles(entity, stack)) {
            setCharged(stack, true);
            SoundSource source = entity instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_LOADING_END, source, 1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }

    private static boolean tryLoadProjectiles(LivingEntity shooter, ItemStack crossbow) {
        int multishot = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, crossbow);
        int count = multishot == 0 ? 1 : 3;
        boolean creative = shooter instanceof Player player && player.getAbilities().instabuild;
        ItemStack ammo = shooter.getProjectile(crossbow);
        ItemStack ammoCopy = ammo.copy();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                ammo = ammoCopy.copy();
            }
            if (ammo.isEmpty() && creative) {
                ammo = new ItemStack(Items.ARROW);
                ammoCopy = ammo.copy();
            }
            if (!loadProjectile(shooter, crossbow, ammo, i > 0, creative)) {
                return false;
            }
        }
        return true;
    }

    private static boolean loadProjectile(LivingEntity shooter, ItemStack crossbow, ItemStack ammo, boolean extra, boolean creative) {
        if (ammo.isEmpty()) {
            return false;
        }
        boolean freeArrow = creative && ammo.getItem() instanceof ArrowItem;
        ItemStack loaded;
        if (!freeArrow && !creative && !extra) {
            loaded = ammo.split(1);
            if (ammo.isEmpty() && shooter instanceof Player player) {
                player.getInventory().removeItem(ammo);
            }
        } else {
            loaded = ammo.copy();
        }
        addChargedProjectile(crossbow, loaded);
        return true;
    }

    private static void addChargedProjectile(ItemStack crossbow, ItemStack projectile) {
        CompoundTag tag = crossbow.getOrCreateTag();
        ListTag list = tag.contains(TAG_CHARGED_PROJECTILES, Tag.TAG_LIST) ? tag.getList(TAG_CHARGED_PROJECTILES, Tag.TAG_COMPOUND) : new ListTag();
        CompoundTag entry = new CompoundTag();
        projectile.save(entry);
        list.add(entry);
        tag.put(TAG_CHARGED_PROJECTILES, list);
    }

    public static List<ItemStack> getLoadedProjectiles(ItemStack crossbow) {
        List<ItemStack> list = new ArrayList<>();
        CompoundTag tag = crossbow.getTag();
        if (tag != null && tag.contains(TAG_CHARGED_PROJECTILES, Tag.TAG_LIST)) {
            ListTag entries = tag.getList(TAG_CHARGED_PROJECTILES, Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                list.add(ItemStack.of(entries.getCompound(i)));
            }
        }
        return list;
    }

    private static void clearLoadedProjectiles(ItemStack crossbow) {
        CompoundTag tag = crossbow.getTag();
        if (tag != null) {
            tag.put(TAG_CHARGED_PROJECTILES, new ListTag());
        }
    }

    public static boolean isLoadedWith(ItemStack crossbow, net.minecraft.world.item.Item item) {
        for (ItemStack s : getLoadedProjectiles(crossbow)) {
            if (s.is(item)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ shooting

    private void performRpgShooting(Level level, Player player, InteractionHand hand, ItemStack crossbow, boolean fan) {
        if (net.minecraftforge.event.ForgeEventFactory.onArrowLoose(crossbow, level, player, 1, true) < 0) {
            return;
        }
        List<ItemStack> loaded = getLoadedProjectiles(crossbow);
        boolean firework = !loaded.isEmpty() && loaded.get(0).is(Items.FIREWORK_ROCKET);
        float power = firework ? FIREWORK_POWER : ARROW_POWER;
        boolean rand = player.getRandom().nextBoolean();
        float[] pitches = {1.0F, randomPitch(rand, level), randomPitch(!rand, level)};

        if (fan && loaded.size() == 1 && !firework) {
            ItemStack single = loaded.get(0);
            loaded = List.of(single, single.copy(), single.copy());
        }
        for (int i = 0; i < loaded.size() && i < 3; i++) {
            ItemStack projectile = loaded.get(i);
            if (projectile.isEmpty()) {
                continue;
            }
            float angle = i == 0 ? 0.0F : (i == 1 ? -10.0F : 10.0F);
            shootProjectile(level, player, hand, crossbow, projectile, pitches[i], player.getAbilities().instabuild || i > 0, power, 1.0F, angle);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!level.isClientSide) {
                CriteriaTriggers.SHOT_CROSSBOW.trigger(serverPlayer, crossbow);
            }
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }
        clearLoadedProjectiles(crossbow);
    }

    private static float randomPitch(boolean high, Level level) {
        float f = high ? 0.63F : 0.43F;
        return 1.0F / (level.getRandom().nextFloat() * 0.5F + 1.8F) + f;
    }

    private void shootProjectile(Level level, Player shooter, InteractionHand hand, ItemStack crossbow, ItemStack ammo,
                                 float soundPitch, boolean creativeOnlyPickup, float velocity, float inaccuracy, float angle) {
        if (level.isClientSide) {
            return;
        }
        boolean firework = ammo.is(Items.FIREWORK_ROCKET);
        Projectile projectile;
        WeaponMaterial material = RpgItemData.getMaterial(crossbow);
        float speedMult = 1.0F;
        if (firework) {
            projectile = new FireworkRocketEntity(level, ammo, shooter, shooter.getX(), shooter.getEyeY() - 0.15D, shooter.getZ(), true);
        } else {
            boolean crossbowman = PlayerClass.of(shooter) == PlayerClass.CROSSBOWMAN;
            speedMult = tier.getCrossbowRange() * (material == null ? 1.0F : material.getRangeMultiplier());
            ArrowItem arrowItem = (ArrowItem) (ammo.getItem() instanceof ArrowItem ? ammo.getItem() : Items.ARROW);
            AbstractArrow arrow = arrowItem.createArrow(level, ammo, shooter);
            arrow.setCritArrow(true);
            arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
            arrow.setShotFromCrossbow(true);
            int pierce = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, crossbow);
            if (material != null && material.getArmorPenetration() > 0.0F) {
                pierce = Math.max(pierce, 1);
            }
            if (crossbowman) {
                pierce += 1;
            }
            if (pierce > 0) {
                arrow.setPierceLevel((byte) Math.max(arrow.getPierceLevel(), pierce));
            }
            float damageMult = 1.0F + (float) shooter.getAttributeValue(ModAttributes.RANGED_DAMAGE.get()) + (crossbowman ? 0.2F : 0.0F);
            double effectBonus = arrow.getBaseDamage() - 2.0D;
            float tierDamage = tier.getCrossbowDamage() * damageMult + (material == null ? 0.0F : material.getBonusDamage());
            arrow.setBaseDamage((tierDamage + effectBonus) / (velocity * speedMult));
            if (material != null && arrow instanceof RpgArrowEntity rpgArrow) {
                rpgArrow.setArmorPenetration(material.getArmorPenetration());
            }
            if (creativeOnlyPickup || angle != 0.0F) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            projectile = arrow;
        }
        Vec3 up = shooter.getUpVector(1.0F);
        Quaternionf rotation = new Quaternionf().setAngleAxis(angle * ((float) Math.PI / 180.0F), up.x, up.y, up.z);
        Vec3 view = shooter.getViewVector(1.0F);
        Vector3f dir = view.toVector3f().rotate(rotation);
        projectile.shoot(dir.x(), dir.y(), dir.z(), velocity * speedMult, inaccuracy);
        crossbow.hurtAndBreak(firework ? 3 : 1, shooter, p -> p.broadcastBreakEvent(hand));
        level.addFreshEntity(projectile);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, soundPitch);
    }

    // ------------------------------------------------------------------ sounds while loading

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide) {
            return;
        }
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        SoundEvent start = getStartSound(quickCharge);
        SoundEvent mid = quickCharge == 0 ? SoundEvents.CROSSBOW_LOADING_MIDDLE : null;
        float progress = (float) (stack.getUseDuration() - remaining) / (float) getRpgChargeDuration(stack);
        if (progress < 0.2F) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
        }
        if (progress >= 0.2F && !this.startSoundPlayed) {
            this.startSoundPlayed = true;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), start, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        if (progress >= 0.5F && mid != null && !this.midLoadSoundPlayed) {
            this.midLoadSoundPlayed = true;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), mid, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    private static SoundEvent getStartSound(int quickCharge) {
        return switch (quickCharge) {
            case 1 -> SoundEvents.CROSSBOW_QUICK_CHARGE_1;
            case 2 -> SoundEvents.CROSSBOW_QUICK_CHARGE_2;
            case 3 -> SoundEvents.CROSSBOW_QUICK_CHARGE_3;
            default -> SoundEvents.CROSSBOW_LOADING_START;
        };
    }

    // ------------------------------------------------------------------ misc

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return WeaponAttributes.withRpgBonuses(stack, slot, super.getAttributeModifiers(slot, stack), false);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return WeaponAttributes.scaledMaxDamage(stack, super.getMaxDamage(stack));
    }

    @Override
    public int getDefaultProjectileRange() {
        return Math.round(8.0F * tier.getCrossbowRange());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.rpgmod.crossbow.stats", Math.round(tier.getCrossbowDamage()),
                String.format(java.util.Locale.ROOT, "%.1f", getRpgChargeDuration(stack) / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.rpgmod.crossbow.special").withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
