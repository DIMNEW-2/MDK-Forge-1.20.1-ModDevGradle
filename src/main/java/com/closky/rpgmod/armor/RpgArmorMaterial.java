package com.closky.rpgmod.armor;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.player.PlayerClass;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;

/**
 * One armor material per player class (section 7.4). Layer textures live at
 * {@code assets/rpgmod/textures/models/armor/<class>_layer_{1,2}.png}.
 */
public enum RpgArmorMaterial implements ArmorMaterial {
    WARRIOR(PlayerClass.WARRIOR, 25, new int[]{3, 6, 8, 3}, 2.0F, 0.0F, 12, SoundEvents.ARMOR_EQUIP_IRON),
    MAGE(PlayerClass.MAGE, 12, new int[]{1, 3, 4, 1}, 0.0F, 0.0F, 25, SoundEvents.ARMOR_EQUIP_LEATHER),
    ARCHER(PlayerClass.ARCHER, 15, new int[]{2, 4, 5, 2}, 0.0F, 0.0F, 15, SoundEvents.ARMOR_EQUIP_LEATHER),
    CROSSBOWMAN(PlayerClass.CROSSBOWMAN, 18, new int[]{2, 5, 6, 2}, 1.0F, 0.0F, 12, SoundEvents.ARMOR_EQUIP_CHAIN),
    ROGUE(PlayerClass.ROGUE, 14, new int[]{2, 4, 5, 2}, 0.0F, 0.0F, 18, SoundEvents.ARMOR_EQUIP_LEATHER),
    DRUID(PlayerClass.DRUID, 14, new int[]{2, 4, 5, 2}, 0.0F, 0.0F, 20, SoundEvents.ARMOR_EQUIP_LEATHER),
    SPEARMAN(PlayerClass.SPEARMAN, 20, new int[]{2, 5, 6, 2}, 1.0F, 0.0F, 12, SoundEvents.ARMOR_EQUIP_CHAIN),
    BOMBER(PlayerClass.BOMBER, 18, new int[]{2, 5, 6, 2}, 1.0F, 0.1F, 12, SoundEvents.ARMOR_EQUIP_IRON);

    private static final EnumMap<ArmorItem.Type, Integer> BASE_DURABILITY = new EnumMap<>(ArmorItem.Type.class);

    static {
        BASE_DURABILITY.put(ArmorItem.Type.BOOTS, 13);
        BASE_DURABILITY.put(ArmorItem.Type.LEGGINGS, 15);
        BASE_DURABILITY.put(ArmorItem.Type.CHESTPLATE, 16);
        BASE_DURABILITY.put(ArmorItem.Type.HELMET, 11);
    }

    private final PlayerClass playerClass;
    private final int durabilityMultiplier;
    private final int[] defense; // boots, leggings, chestplate, helmet
    private final float toughness;
    private final float knockbackResistance;
    private final int enchantmentValue;
    private final SoundEvent equipSound;

    RpgArmorMaterial(PlayerClass playerClass, int durabilityMultiplier, int[] defense, float toughness, float knockbackResistance, int enchantmentValue, SoundEvent equipSound) {
        this.playerClass = playerClass;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY.get(type) * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> defense[0];
            case LEGGINGS -> defense[1];
            case CHESTPLATE -> defense[2];
            case HELMET -> defense[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.LEATHER, Items.IRON_INGOT);
    }

    @Override
    public String getName() {
        return RpgMod.MODID + ":" + playerClass.getKey();
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
