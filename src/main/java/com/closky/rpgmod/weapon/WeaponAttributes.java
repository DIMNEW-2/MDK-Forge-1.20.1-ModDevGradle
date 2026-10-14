package com.closky.rpgmod.weapon;

import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponMaterial;
import com.closky.rpgmod.registry.ModAttributes;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Shared attribute-modifier logic for forged materials and rolled bonuses.
 */
public final class WeaponAttributes {
    public static final UUID MATERIAL_DAMAGE_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0001");
    public static final UUID MATERIAL_SPEED_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0002");
    public static final UUID MATERIAL_MANA_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0003");
    public static final UUID MATERIAL_CRIT_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0004");
    public static final UUID MATERIAL_PEN_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0005");
    public static final UUID MATERIAL_MAGIC_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0006");
    public static final UUID STAFF_MAGIC_UUID = UUID.fromString("5d1f2a30-1b6a-4b7e-9c6a-0f2d7c1a0007");

    private WeaponAttributes() {
    }

    /**
     * Returns a mutable copy of {@code base} with material and bonus modifiers added for the main hand.
     *
     * @param melee whether the material damage / attack speed bonuses apply (false for ranged weapons & staffs)
     */
    public static Multimap<Attribute, AttributeModifier> withRpgBonuses(ItemStack stack, EquipmentSlot slot,
                                                                        Multimap<Attribute, AttributeModifier> base, boolean melee) {
        if (slot != EquipmentSlot.MAINHAND) {
            return base;
        }
        Multimap<Attribute, AttributeModifier> map = LinkedHashMultimap.create(base);
        WeaponMaterial material = RpgItemData.getMaterial(stack);
        if (material != null) {
            if (melee && material.getBonusDamage() > 0.0F) {
                map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(MATERIAL_DAMAGE_UUID, "rpgmod material damage", material.getBonusDamage(), AttributeModifier.Operation.ADDITION));
            }
            if (melee && material.getSpeedMultiplier() != 1.0F) {
                map.put(Attributes.ATTACK_SPEED, new AttributeModifier(MATERIAL_SPEED_UUID, "rpgmod material speed", material.getSpeedMultiplier() - 1.0F, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            if (material.getManaBonus() > 0.0F) {
                map.put(ModAttributes.MAX_MANA.get(), new AttributeModifier(MATERIAL_MANA_UUID, "rpgmod material mana", material.getManaBonus(), AttributeModifier.Operation.ADDITION));
            }
            if (material.getCritChance() > 0.0F) {
                map.put(ModAttributes.CRIT_CHANCE.get(), new AttributeModifier(MATERIAL_CRIT_UUID, "rpgmod material crit", material.getCritChance(), AttributeModifier.Operation.ADDITION));
            }
            if (material.getArmorPenetration() > 0.0F) {
                map.put(ModAttributes.ARMOR_PENETRATION.get(), new AttributeModifier(MATERIAL_PEN_UUID, "rpgmod material penetration", material.getArmorPenetration(), AttributeModifier.Operation.ADDITION));
            }
            if (!melee && material.getMagicDamage() > 0.0F) {
                map.put(ModAttributes.MAGIC_POWER.get(), new AttributeModifier(MATERIAL_MAGIC_UUID, "rpgmod material magic", 0.15D, AttributeModifier.Operation.ADDITION));
            }
        }
        RpgItemData.applyBonusAttributes(stack, slot, map);
        return map;
    }

    /** Max durability scaled by the material (diamond: +50%). */
    public static int scaledMaxDamage(ItemStack stack, int base) {
        WeaponMaterial material = RpgItemData.getMaterial(stack);
        return material == null ? base : Math.round(base * material.getDurabilityMultiplier());
    }
}
