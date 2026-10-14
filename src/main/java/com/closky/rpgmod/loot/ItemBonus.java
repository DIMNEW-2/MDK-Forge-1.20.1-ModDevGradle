package com.closky.rpgmod.loot;

import com.closky.rpgmod.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Random affixes stored in {@code rpgmod:bonuses}. Each maps onto an attribute modifier while the item is equipped.
 */
public enum ItemBonus {
    DAMAGE(() -> Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, false, 1.0F, 4.0F),
    ATTACK_SPEED(() -> Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, true, 0.05F, 0.20F),
    CRIT_CHANCE(ModAttributes.CRIT_CHANCE::get, AttributeModifier.Operation.ADDITION, true, 0.03F, 0.15F),
    MAX_HEALTH(() -> Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, false, 2.0F, 8.0F),
    MAX_MANA(ModAttributes.MAX_MANA::get, AttributeModifier.Operation.ADDITION, false, 5.0F, 25.0F),
    MAGIC_POWER(ModAttributes.MAGIC_POWER::get, AttributeModifier.Operation.ADDITION, true, 0.05F, 0.25F),
    ARMOR(() -> Attributes.ARMOR, AttributeModifier.Operation.ADDITION, false, 1.0F, 4.0F),
    MOVE_SPEED(() -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, true, 0.03F, 0.12F),
    ARMOR_PENETRATION(ModAttributes.ARMOR_PENETRATION::get, AttributeModifier.Operation.ADDITION, true, 0.05F, 0.25F),
    MANA_REGEN(ModAttributes.MANA_REGEN::get, AttributeModifier.Operation.ADDITION, false, 0.25F, 1.5F);

    private final Supplier<Attribute> attribute;
    private final AttributeModifier.Operation operation;
    private final boolean percent;
    private final float min;
    private final float max;
    private final String key = name().toLowerCase(Locale.ROOT);

    ItemBonus(Supplier<Attribute> attribute, AttributeModifier.Operation operation, boolean percent, float min, float max) {
        this.attribute = attribute;
        this.operation = operation;
        this.percent = percent;
        this.min = min;
        this.max = max;
    }

    public Attribute getAttribute() {
        return attribute.get();
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public boolean isPercent() {
        return percent;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public String getSerializedName() {
        return key;
    }

    /** Rolls a value for this bonus scaled by the item level (higher levels roll closer to the max). */
    public float roll(java.util.Random random, int itemLevel) {
        float t = Math.min(1.0F, itemLevel / 25.0F);
        float lo = min + (max - min) * t * 0.5F;
        float v = lo + random.nextFloat() * (max - lo);
        return percent ? Math.round(v * 100.0F) / 100.0F : Math.round(v * 2.0F) / 2.0F;
    }

    public Component format(float value) {
        String num = percent ? String.format(Locale.ROOT, "+%d%%", Math.round(value * 100.0F))
                : (value == Math.floor(value) ? String.format(Locale.ROOT, "+%d", (int) value) : String.format(Locale.ROOT, "+%.1f", value));
        return Component.literal(num + " ").withStyle(ChatFormatting.BLUE)
                .append(Component.translatable("bonus.rpgmod." + key).withStyle(ChatFormatting.BLUE));
    }

    public static ItemBonus byName(String name) {
        for (ItemBonus b : values()) {
            if (b.key.equals(name)) {
                return b;
            }
        }
        return null;
    }
}
