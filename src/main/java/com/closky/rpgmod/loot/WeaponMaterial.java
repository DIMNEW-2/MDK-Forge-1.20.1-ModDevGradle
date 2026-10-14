package com.closky.rpgmod.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Forging material (section 2.3). A weapon can be forged once with a material through the smithing table
 * using a {@code rpgmod:forging_template}; the material is stored in the item NBT and grants a passive bonus.
 */
public enum WeaponMaterial {
    IRON(Items.IRON_INGOT, ChatFormatting.WHITE),           // + damage
    GOLD(Items.GOLD_INGOT, ChatFormatting.YELLOW),          // + attack speed / draw speed
    DIAMOND(Items.DIAMOND, ChatFormatting.AQUA),            // + durability
    EMERALD(Items.EMERALD, ChatFormatting.GREEN),           // + crit chance
    NETHERITE(Items.NETHERITE_INGOT, ChatFormatting.DARK_GRAY), // + armor penetration
    AMETHYST(Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE), // + magic damage
    LAPIS(Items.LAPIS_LAZULI, ChatFormatting.BLUE),         // + mana
    REDSTONE(Items.REDSTONE, ChatFormatting.RED),           // + reload / projectile speed
    QUARTZ(Items.QUARTZ, ChatFormatting.GRAY),              // + range
    COPPER(Items.COPPER_INGOT, ChatFormatting.GOLD);        // + on-hit effect

    private final Item ingredient;
    private final ChatFormatting color;
    private final String key = name().toLowerCase(Locale.ROOT);

    WeaponMaterial(Item ingredient, ChatFormatting color) {
        this.ingredient = ingredient;
        this.color = color;
    }

    public Item getIngredient() {
        return ingredient;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getSerializedName() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("material.rpgmod." + key).withStyle(color);
    }

    /** Short description of the passive bonus (shown in tooltips). */
    public Component getBonusDescription() {
        return Component.translatable("material.rpgmod." + key + ".bonus").withStyle(ChatFormatting.DARK_AQUA);
    }

    /** Flat bonus damage added to melee hits. */
    public float getBonusDamage() {
        return this == IRON ? 1.5F : 0.0F;
    }

    /** Multiplier applied to attack speed (melee) / draw speed (ranged). */
    public float getSpeedMultiplier() {
        return this == GOLD ? 1.15F : 1.0F;
    }

    /** Multiplier applied to max durability. */
    public float getDurabilityMultiplier() {
        return this == DIAMOND ? 1.5F : 1.0F;
    }

    /** Additional critical strike chance (0..1). */
    public float getCritChance() {
        return this == EMERALD ? 0.15F : 0.0F;
    }

    /** Fraction of the target's armor that is ignored (0..1). */
    public float getArmorPenetration() {
        return this == NETHERITE ? 0.3F : 0.0F;
    }

    /** Extra magic damage (flat) applied on hit / spell power bonus. */
    public float getMagicDamage() {
        return this == AMETHYST ? 2.0F : 0.0F;
    }

    /** Flat bonus to max mana while held. */
    public float getManaBonus() {
        return this == LAPIS ? 15.0F : 0.0F;
    }

    /** Multiplier for projectile speed / reload speed. */
    public float getProjectileSpeedMultiplier() {
        return this == REDSTONE ? 1.25F : 1.0F;
    }

    /** Multiplier for projectile range (less drag / further flight). */
    public float getRangeMultiplier() {
        return this == QUARTZ ? 1.3F : 1.0F;
    }

    /** Item level gained when the material is forged onto a weapon. */
    public int getItemLevelBonus() {
        return switch (this) {
            case NETHERITE -> 3;
            case DIAMOND, EMERALD, AMETHYST -> 2;
            default -> 1;
        };
    }

    /** Whether the material applies an on-hit status effect (copper: weakness). */
    public boolean hasOnHitEffect() {
        return this == COPPER;
    }

    @Nullable
    public static WeaponMaterial byName(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (WeaponMaterial m : values()) {
            if (m.key.equals(name)) {
                return m;
            }
        }
        return null;
    }

    @Nullable
    public static WeaponMaterial byIngredient(Item item) {
        for (WeaponMaterial m : values()) {
            if (m.ingredient == item) {
                return m;
            }
        }
        return null;
    }
}
