package com.closky.rpgmod.loot;

import com.closky.rpgmod.armor.RpgArmorItem;
import com.closky.rpgmod.weapon.IRpgWeapon;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * NBT helpers for RPG item data (section 2.4):
 * <ul>
 *     <li>{@code rpgmod:item_level} - int</li>
 *     <li>{@code rpgmod:weapon_class} - string (informational, usually derived from the item)</li>
 *     <li>{@code rpgmod:material} - forging material</li>
 *     <li>{@code rpgmod:bonuses} - list of {id, value}</li>
 * </ul>
 */
public final class RpgItemData {
    public static final String KEY_LEVEL = "rpgmod:item_level";
    public static final String KEY_CLASS = "rpgmod:weapon_class";
    public static final String KEY_MATERIAL = "rpgmod:material";
    public static final String KEY_BONUSES = "rpgmod:bonuses";
    public static final String KEY_DESCRIPTION = "rpgmod:description";

    private RpgItemData() {
    }

    public static boolean isRpgItem(ItemStack stack) {
        return stack.getItem() instanceof IRpgWeapon || stack.getItem() instanceof RpgArmorItem;
    }

    // ------------------------------------------------------------------ level

    public static int getItemLevel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY_LEVEL, Tag.TAG_ANY_NUMERIC)) {
            return tag.getInt(KEY_LEVEL);
        }
        if (stack.getItem() instanceof IRpgWeapon weapon) {
            return weapon.getTier().getBaseItemLevel();
        }
        if (stack.getItem() instanceof RpgArmorItem armor) {
            return armor.getBaseItemLevel();
        }
        return 1;
    }

    public static void setItemLevel(ItemStack stack, int level) {
        stack.getOrCreateTag().putInt(KEY_LEVEL, Math.max(1, level));
    }

    public static boolean hasExplicitLevel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(KEY_LEVEL, Tag.TAG_ANY_NUMERIC);
    }

    // ------------------------------------------------------------------ class

    @Nullable
    public static WeaponClass getWeaponClass(ItemStack stack) {
        if (stack.getItem() instanceof IRpgWeapon weapon) {
            return weapon.getWeaponClass();
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY_CLASS, Tag.TAG_STRING)) {
            return WeaponClass.byName(tag.getString(KEY_CLASS));
        }
        return null;
    }

    public static void setWeaponClass(ItemStack stack, WeaponClass weaponClass) {
        stack.getOrCreateTag().putString(KEY_CLASS, weaponClass.getSerializedName());
    }

    // ------------------------------------------------------------------ material

    @Nullable
    public static WeaponMaterial getMaterial(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY_MATERIAL, Tag.TAG_STRING)) {
            return WeaponMaterial.byName(tag.getString(KEY_MATERIAL));
        }
        return null;
    }

    public static void setMaterial(ItemStack stack, @Nullable WeaponMaterial material) {
        if (material == null) {
            if (stack.getTag() != null) {
                stack.getTag().remove(KEY_MATERIAL);
            }
        } else {
            stack.getOrCreateTag().putString(KEY_MATERIAL, material.getSerializedName());
        }
    }

    /** Convenience accessor that never returns null-dependent values. */
    public static float materialValue(ItemStack stack, java.util.function.Function<WeaponMaterial, Float> getter, float fallback) {
        WeaponMaterial m = getMaterial(stack);
        return m == null ? fallback : getter.apply(m);
    }

    // ------------------------------------------------------------------ bonuses

    public record BonusInstance(ItemBonus bonus, float value) {
    }

    public static List<BonusInstance> getBonuses(ItemStack stack) {
        List<BonusInstance> list = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_BONUSES, Tag.TAG_LIST)) {
            return list;
        }
        ListTag bonuses = tag.getList(KEY_BONUSES, Tag.TAG_COMPOUND);
        for (int i = 0; i < bonuses.size(); i++) {
            CompoundTag entry = bonuses.getCompound(i);
            ItemBonus bonus = ItemBonus.byName(entry.getString("id"));
            if (bonus != null) {
                list.add(new BonusInstance(bonus, entry.getFloat("value")));
            }
        }
        return list;
    }

    public static void setBonuses(ItemStack stack, List<BonusInstance> bonuses) {
        ListTag list = new ListTag();
        for (BonusInstance b : bonuses) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", b.bonus().getSerializedName());
            entry.putFloat("value", b.value());
            list.add(entry);
        }
        stack.getOrCreateTag().put(KEY_BONUSES, list);
    }

    public static void addBonus(ItemStack stack, ItemBonus bonus, float value) {
        List<BonusInstance> list = getBonuses(stack);
        list.removeIf(b -> b.bonus() == bonus);
        list.add(new BonusInstance(bonus, value));
        setBonuses(stack, list);
    }

    /** Rolls {@code count} distinct random bonuses scaled by the item level. */
    public static void rollBonuses(ItemStack stack, Random random, int count) {
        int level = getItemLevel(stack);
        List<ItemBonus> pool = new ArrayList<>(List.of(ItemBonus.values()));
        List<BonusInstance> result = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            ItemBonus bonus = pool.remove(random.nextInt(pool.size()));
            result.add(new BonusInstance(bonus, bonus.roll(random, level)));
        }
        setBonuses(stack, result);
    }

    /**
     * Adds the attribute modifiers of the rolled bonuses to {@code map}. Called from the items'
     * {@code getAttributeModifiers}. Modifier UUIDs are deterministic per bonus and slot so they never stack.
     */
    public static void applyBonusAttributes(ItemStack stack, EquipmentSlot slot, Multimap<Attribute, AttributeModifier> map) {
        for (BonusInstance b : getBonuses(stack)) {
            UUID uuid = UUID.nameUUIDFromBytes(("rpgmod_bonus_" + b.bonus().getSerializedName() + "_" + slot.getName()).getBytes());
            map.put(b.bonus().getAttribute(), new AttributeModifier(uuid, "rpgmod bonus " + b.bonus().getSerializedName(), b.value(), b.bonus().getOperation()));
        }
    }

    // ------------------------------------------------------------------ rarity

    public static ItemRarity getRarity(ItemStack stack) {
        if (stack.getItem() instanceof IRpgWeapon weapon) {
            ItemRarity tierRarity = weapon.getTier().getRarity();
            ItemRarity levelRarity = ItemRarity.byItemLevel(getItemLevel(stack));
            return tierRarity.ordinal() >= levelRarity.ordinal() ? tierRarity : levelRarity;
        }
        return ItemRarity.byItemLevel(getItemLevel(stack));
    }

    // ------------------------------------------------------------------ tooltip

    /**
     * Builds the dynamic tooltip: rarity + class, level, material, bonuses and description.
     */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        ItemRarity rarity = getRarity(stack);
        WeaponClass weaponClass = getWeaponClass(stack);
        Component header = weaponClass != null
                ? Component.empty().append(rarity.getDisplayName()).append(Component.literal(" ").withStyle(ChatFormatting.GRAY)).append(weaponClass.getDisplayName().copy().withStyle(ChatFormatting.GRAY))
                : rarity.getDisplayName();
        tooltip.add(header);
        tooltip.add(Component.translatable("tooltip.rpgmod.item_level", getItemLevel(stack)).withStyle(ChatFormatting.GRAY));

        WeaponMaterial material = getMaterial(stack);
        if (material != null) {
            tooltip.add(Component.translatable("tooltip.rpgmod.material", material.getDisplayName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  ").append(material.getBonusDescription()));
        } else if (weaponClass != null) {
            tooltip.add(Component.translatable("tooltip.rpgmod.material.none").withStyle(ChatFormatting.DARK_GRAY));
        }

        List<BonusInstance> bonuses = getBonuses(stack);
        if (!bonuses.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.rpgmod.bonuses").withStyle(ChatFormatting.GRAY));
            for (BonusInstance b : bonuses) {
                tooltip.add(Component.literal("  ").append(b.bonus().format(b.value())));
            }
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY_DESCRIPTION, Tag.TAG_STRING)) {
            tooltip.add(Component.translatable(tag.getString(KEY_DESCRIPTION)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE));
        } else {
            String descKey = stack.getItem().getDescriptionId() + ".desc";
            Component desc = Component.translatable(descKey);
            if (!desc.getString().equals(descKey)) {
                tooltip.add(desc.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE));
            }
        }
    }
}
