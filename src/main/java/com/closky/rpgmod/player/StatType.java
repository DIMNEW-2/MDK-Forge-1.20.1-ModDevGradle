package com.closky.rpgmod.player;

import com.closky.rpgmod.registry.ModAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Allocatable character stats (sections 7.2 and 19.3). Every point adds {@code perPoint} to the attribute.
 */
public enum StatType {
    STRENGTH(() -> Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 0.5D, "2b7f5e40-0f3f-4a61-9f4a-7d3c2a1b0001"),
    HEALTH(() -> Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 2.0D, "2b7f5e40-0f3f-4a61-9f4a-7d3c2a1b0002"),
    MANA(ModAttributes.MAX_MANA::get, AttributeModifier.Operation.ADDITION, 5.0D, "2b7f5e40-0f3f-4a61-9f4a-7d3c2a1b0003"),
    AGILITY(() -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.01D, "2b7f5e40-0f3f-4a61-9f4a-7d3c2a1b0004"),
    INTELLECT(ModAttributes.MAGIC_POWER::get, AttributeModifier.Operation.ADDITION, 0.03D, "2b7f5e40-0f3f-4a61-9f4a-7d3c2a1b0005"),
    LUCK(ModAttributes.CRIT_CHANCE::get, AttributeModifier.Operation.ADDITION, 0.01D, "2b7f5e40-0f3f-4a61-9f4a-7d3c2a1b0006");

    private final Supplier<Attribute> attribute;
    private final AttributeModifier.Operation operation;
    private final double perPoint;
    private final UUID uuid;
    private final String key = name().toLowerCase(Locale.ROOT);

    StatType(Supplier<Attribute> attribute, AttributeModifier.Operation operation, double perPoint, String uuid) {
        this.attribute = attribute;
        this.operation = operation;
        this.perPoint = perPoint;
        this.uuid = UUID.fromString(uuid);
    }

    public Attribute getAttribute() {
        return attribute.get();
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public double getPerPoint() {
        return perPoint;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getKey() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("stat.rpgmod." + key);
    }

    public Component getDescription() {
        return Component.translatable("stat.rpgmod." + key + ".desc");
    }

    public static StatType byKey(String key) {
        for (StatType t : values()) {
            if (t.key.equals(key)) {
                return t;
            }
        }
        return null;
    }
}
