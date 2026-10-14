package com.closky.rpgmod.weapon;

import com.closky.rpgmod.loot.ItemRarity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Weapon tiers with the balance tables of sections 2A/2B.
 *
 * <p>The vanilla {@link Tier} damage bonus is kept at 0 so that each weapon class can define the exact attack damage
 * shown in the design tables.</p>
 */
public enum WeaponTier implements Tier {
    //    name       uses  mine  ench  swordDmg swordSpd  maceDmg maceSpd stun  spearMelee spearThrow bowDmg bowSpd bowRange xbowDmg xbowReload xbowRange baseLvl rarity
    WOOD("wood", 60, 0, 15, 4.0F, 1.6F, 6.0F, 0.8F, 0.05F, 3.0F, 5.0F, 4.0F, 1.0F, 1.0F, 5.0F, 36, 1.2F, 1, ItemRarity.COMMON, () -> Ingredient.of(ItemTags.PLANKS)),
    STONE("stone", 130, 1, 5, 5.0F, 1.6F, 7.0F, 0.8F, 0.05F, 4.0F, 6.0F, 5.0F, 1.1F, 1.1F, 6.0F, 34, 1.3F, 3, ItemRarity.COMMON, () -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS)),
    IRON("iron", 250, 2, 14, 6.0F, 1.6F, 9.0F, 0.8F, 0.10F, 5.0F, 8.0F, 6.0F, 1.2F, 1.2F, 8.0F, 30, 1.4F, 6, ItemRarity.UNCOMMON, () -> Ingredient.of(Items.IRON_INGOT)),
    GOLD("gold", 32, 0, 22, 4.0F, 2.0F, 6.0F, 1.0F, 0.15F, 4.0F, 7.0F, 5.0F, 1.4F, 1.0F, 7.0F, 24, 1.2F, 5, ItemRarity.UNCOMMON, () -> Ingredient.of(Items.GOLD_INGOT)),
    DIAMOND("diamond", 1500, 3, 10, 7.0F, 1.6F, 11.0F, 0.8F, 0.15F, 6.0F, 10.0F, 7.0F, 1.3F, 1.3F, 10.0F, 28, 1.5F, 10, ItemRarity.RARE, () -> Ingredient.of(Items.DIAMOND)),
    NETHERITE("netherite", 2000, 4, 15, 8.0F, 1.6F, 13.0F, 0.8F, 0.20F, 7.0F, 12.0F, 8.0F, 1.4F, 1.4F, 12.0F, 26, 1.6F, 14, ItemRarity.EPIC, () -> Ingredient.of(Items.NETHERITE_INGOT)),
    RUNE("rune", 3000, 4, 25, 10.0F, 1.8F, 15.0F, 0.9F, 0.25F, 8.0F, 15.0F, 10.0F, 1.6F, 1.6F, 15.0F, 22, 1.8F, 20, ItemRarity.LEGENDARY, () -> Ingredient.of(Items.AMETHYST_SHARD));

    private final String key;
    private final int uses;
    private final int miningLevel;
    private final int enchantmentValue;
    private final float swordDamage;
    private final float swordSpeed;
    private final float maceDamage;
    private final float maceSpeed;
    private final float maceStunChance;
    private final float spearMeleeDamage;
    private final float spearThrowDamage;
    private final float bowDamage;
    private final float bowArrowSpeed;
    private final float bowRange;
    private final float crossbowDamage;
    private final int crossbowReloadTicks;
    private final float crossbowRange;
    private final int baseItemLevel;
    private final ItemRarity rarity;
    private final Supplier<Ingredient> repairIngredient;
    private Ingredient cachedRepair;

    WeaponTier(String key, int uses, int miningLevel, int enchantmentValue,
               float swordDamage, float swordSpeed,
               float maceDamage, float maceSpeed, float maceStunChance,
               float spearMeleeDamage, float spearThrowDamage,
               float bowDamage, float bowArrowSpeed, float bowRange,
               float crossbowDamage, int crossbowReloadTicks, float crossbowRange,
               int baseItemLevel, ItemRarity rarity, Supplier<Ingredient> repairIngredient) {
        this.key = key;
        this.uses = uses;
        this.miningLevel = miningLevel;
        this.enchantmentValue = enchantmentValue;
        this.swordDamage = swordDamage;
        this.swordSpeed = swordSpeed;
        this.maceDamage = maceDamage;
        this.maceSpeed = maceSpeed;
        this.maceStunChance = maceStunChance;
        this.spearMeleeDamage = spearMeleeDamage;
        this.spearThrowDamage = spearThrowDamage;
        this.bowDamage = bowDamage;
        this.bowArrowSpeed = bowArrowSpeed;
        this.bowRange = bowRange;
        this.crossbowDamage = crossbowDamage;
        this.crossbowReloadTicks = crossbowReloadTicks;
        this.crossbowRange = crossbowRange;
        this.baseItemLevel = baseItemLevel;
        this.rarity = rarity;
        this.repairIngredient = repairIngredient;
    }

    public String getKey() {
        return key;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return 4.0F + miningLevel;
    }

    @Override
    public float getAttackDamageBonus() {
        return 0.0F;
    }

    @Override
    public int getLevel() {
        return miningLevel;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        if (cachedRepair == null) {
            cachedRepair = repairIngredient.get();
        }
        return cachedRepair;
    }

    public float getSwordDamage() {
        return swordDamage;
    }

    public float getSwordSpeed() {
        return swordSpeed;
    }

    public float getMaceDamage() {
        return maceDamage;
    }

    public float getMaceSpeed() {
        return maceSpeed;
    }

    public float getMaceStunChance() {
        return maceStunChance;
    }

    public float getSpearMeleeDamage() {
        return spearMeleeDamage;
    }

    public float getSpearThrowDamage() {
        return spearThrowDamage;
    }

    public float getBowDamage() {
        return bowDamage;
    }

    public float getBowArrowSpeed() {
        return bowArrowSpeed;
    }

    public float getBowRange() {
        return bowRange;
    }

    public float getCrossbowDamage() {
        return crossbowDamage;
    }

    public int getCrossbowReloadTicks() {
        return crossbowReloadTicks;
    }

    public float getCrossbowRange() {
        return crossbowRange;
    }

    public int getBaseItemLevel() {
        return baseItemLevel;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public static WeaponTier byKey(String key) {
        for (WeaponTier t : values()) {
            if (t.key.equals(key)) {
                return t;
            }
        }
        return WOOD;
    }

    @Override
    public String toString() {
        return key.toUpperCase(Locale.ROOT);
    }
}
