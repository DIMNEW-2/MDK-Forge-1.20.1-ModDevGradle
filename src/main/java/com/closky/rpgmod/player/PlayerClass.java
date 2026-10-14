package com.closky.rpgmod.player;

import com.closky.rpgmod.armor.RpgArmorItem;
import com.closky.rpgmod.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Player classes (section 7.3). A class is never chosen explicitly: it is determined by wearing the full
 * four-piece armor set of that class. Weapon-specific set bonuses are applied in the weapon code.
 */
public enum PlayerClass {
    NONE(ChatFormatting.GRAY),
    WARRIOR(ChatFormatting.RED,
            mod(() -> Attributes.ARMOR, 4.0D, AttributeModifier.Operation.ADDITION),
            mod(() -> Attributes.ATTACK_DAMAGE, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL)),
    MAGE(ChatFormatting.LIGHT_PURPLE,
            mod(ModAttributes.MAX_MANA::get, 20.0D, AttributeModifier.Operation.ADDITION),
            mod(ModAttributes.MAGIC_POWER::get, 0.15D, AttributeModifier.Operation.ADDITION)),
    ARCHER(ChatFormatting.GREEN,
            mod(() -> Attributes.MOVEMENT_SPEED, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL)),
    CROSSBOWMAN(ChatFormatting.DARK_GREEN,
            mod(() -> Attributes.ARMOR, 2.0D, AttributeModifier.Operation.ADDITION)),
    ROGUE(ChatFormatting.DARK_PURPLE,
            mod(ModAttributes.CRIT_CHANCE::get, 0.10D, AttributeModifier.Operation.ADDITION),
            mod(() -> Attributes.ATTACK_SPEED, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL)),
    DRUID(ChatFormatting.DARK_AQUA,
            mod(ModAttributes.HEALING_POWER::get, 0.25D, AttributeModifier.Operation.ADDITION),
            mod(ModAttributes.MANA_REGEN::get, 1.0D, AttributeModifier.Operation.ADDITION)),
    SPEARMAN(ChatFormatting.GOLD,
            mod(ForgeMod.ENTITY_REACH::get, 0.5D, AttributeModifier.Operation.ADDITION),
            mod(() -> Attributes.MOVEMENT_SPEED, 0.05D, AttributeModifier.Operation.MULTIPLY_TOTAL)),
    BOMBER(ChatFormatting.DARK_RED,
            mod(() -> Attributes.ARMOR, 2.0D, AttributeModifier.Operation.ADDITION));

    public record ClassModifier(Supplier<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    }

    private static final UUID[] UUIDS = {
            UUID.fromString("7c1d0f3a-5b2e-4c9d-8e1f-3a2b1c0d0001"),
            UUID.fromString("7c1d0f3a-5b2e-4c9d-8e1f-3a2b1c0d0002"),
            UUID.fromString("7c1d0f3a-5b2e-4c9d-8e1f-3a2b1c0d0003")
    };

    private final ChatFormatting color;
    private final List<ClassModifier> modifiers;
    private final String key = name().toLowerCase(Locale.ROOT);

    PlayerClass(ChatFormatting color, ClassModifier... modifiers) {
        this.color = color;
        this.modifiers = List.of(modifiers);
    }

    private static ClassModifier mod(Supplier<Attribute> attribute, double amount, AttributeModifier.Operation op) {
        return new ClassModifier(attribute, amount, op);
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getKey() {
        return key;
    }

    public List<ClassModifier> getModifiers() {
        return modifiers;
    }

    /** UUID used for the i-th modifier of any class (classes are mutually exclusive so they may share UUIDs). */
    public static UUID modifierUuid(int index) {
        return UUIDS[index];
    }

    public Component getDisplayName() {
        return Component.translatable("class.rpgmod." + key).withStyle(color);
    }

    public Component getBonusDescription() {
        return Component.translatable("class.rpgmod." + key + ".bonus");
    }

    public static PlayerClass byKey(String key) {
        for (PlayerClass c : values()) {
            if (c.key.equals(key)) {
                return c;
            }
        }
        return NONE;
    }

    /** Determines the class from the worn armor: all four pieces must belong to the same class set. */
    public static PlayerClass of(LivingEntity entity) {
        PlayerClass result = null;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof RpgArmorItem armor)) {
                return NONE;
            }
            if (result == null) {
                result = armor.getPlayerClass();
            } else if (result != armor.getPlayerClass()) {
                return NONE;
            }
        }
        return result == null ? NONE : result;
    }
}
