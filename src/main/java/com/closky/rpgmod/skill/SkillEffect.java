package com.closky.rpgmod.skill;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A single effect of a skill node. Data-driven; see {@code data/rpgmod/skill_tree/*.json}.
 *
 * <ul>
 *     <li>{@code attribute}: permanent attribute modifier ({@code target} = attribute id).</li>
 *     <li>{@code unlock_spell}: learns the spell {@code target} when unlocked.</li>
 *     <li>{@code mana_cost}: multiplies spell mana cost by {@code 1 - amount} (summed over nodes).</li>
 *     <li>{@code cooldown}: multiplies spell cooldown by {@code 1 - amount}.</li>
 *     <li>{@code spell_power}: adds {@code amount} to spell damage multiplier (optionally for one school via {@code target}).</li>
 *     <li>{@code flag}: enables a named gameplay flag ({@code target}) checked in code.</li>
 * </ul>
 */
public record SkillEffect(Type type, String target, double amount, AttributeModifier.Operation operation) {
    public static final int MAX_SKILL_MODIFIERS = 32;

    public enum Type {
        ATTRIBUTE, UNLOCK_SPELL, MANA_COST, COOLDOWN, SPELL_POWER, FLAG;

        public static Type byKey(String key) {
            for (Type t : values()) {
                if (t.name().equalsIgnoreCase(key)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unknown skill effect type: " + key);
        }
    }

    @Nullable
    public Attribute attribute() {
        if (type != Type.ATTRIBUTE) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(target);
        return id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
    }

    public void onUnlock(Player player, PlayerRpgData data) {
        if (type == Type.UNLOCK_SPELL) {
            ResourceLocation id = ResourceLocation.tryParse(target);
            if (id != null) {
                data.learnSpell(id);
            }
        }
    }

    /** Human readable line for the skill screen tooltip. */
    public Component describe() {
        return switch (type) {
            case ATTRIBUTE -> {
                Attribute attribute = attribute();
                String name = attribute == null ? target : Component.translatable(attribute.getDescriptionId()).getString();
                String value = operation == AttributeModifier.Operation.ADDITION
                        ? String.format(Locale.ROOT, "%+.2f", amount).replace(".00", "")
                        : String.format(Locale.ROOT, "%+.0f%%", amount * 100.0D);
                yield Component.translatable("skill_effect.rpgmod.attribute", value, name);
            }
            case UNLOCK_SPELL -> Component.translatable("skill_effect.rpgmod.unlock_spell", Component.translatable(spellDescriptionId(target)));
            case MANA_COST -> Component.translatable("skill_effect.rpgmod.mana_cost", (int) Math.round(amount * 100.0D));
            case COOLDOWN -> Component.translatable("skill_effect.rpgmod.cooldown", (int) Math.round(amount * 100.0D));
            case SPELL_POWER -> Component.translatable("skill_effect.rpgmod.spell_power", (int) Math.round(amount * 100.0D));
            case FLAG -> Component.translatable("skill_flag.rpgmod." + target);
        };
    }

    private static String spellDescriptionId(String spellId) {
        ResourceLocation id = ResourceLocation.tryParse(spellId);
        return id == null ? spellId : "spell." + id.getNamespace() + "." + id.getPath();
    }

    /** Attributes referenced by any loaded node, used to clean up stale modifiers. */
    public static Set<Attribute> knownAttributes() {
        Set<Attribute> set = new LinkedHashSet<>();
        for (SkillNode node : SkillTree.get().getNodes()) {
            for (SkillEffect effect : node.effects()) {
                Attribute attribute = effect.attribute();
                if (attribute != null) {
                    set.add(attribute);
                }
            }
        }
        return set;
    }

    // ------------------------------------------------------------------ serialization

    public static SkillEffect fromJson(JsonObject json) {
        Type type = Type.byKey(GsonHelper.getAsString(json, "type"));
        String target = switch (type) {
            case ATTRIBUTE -> GsonHelper.getAsString(json, "attribute");
            case UNLOCK_SPELL -> GsonHelper.getAsString(json, "spell");
            case SPELL_POWER -> GsonHelper.getAsString(json, "school", "");
            case FLAG -> GsonHelper.getAsString(json, "flag");
            default -> GsonHelper.getAsString(json, "target", "");
        };
        double amount = GsonHelper.getAsDouble(json, "amount", 0.0D);
        AttributeModifier.Operation operation = parseOperation(GsonHelper.getAsString(json, "operation", "addition"));
        return new SkillEffect(type, target, amount, operation);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name().toLowerCase(Locale.ROOT));
        switch (type) {
            case ATTRIBUTE -> json.addProperty("attribute", target);
            case UNLOCK_SPELL -> json.addProperty("spell", target);
            case SPELL_POWER -> json.addProperty("school", target);
            case FLAG -> json.addProperty("flag", target);
            default -> json.addProperty("target", target);
        }
        json.addProperty("amount", amount);
        json.addProperty("operation", operation.name().toLowerCase(Locale.ROOT));
        return json;
    }

    private static AttributeModifier.Operation parseOperation(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeUtf(target);
        buf.writeDouble(amount);
        buf.writeEnum(operation);
    }

    public static SkillEffect read(FriendlyByteBuf buf) {
        return new SkillEffect(buf.readEnum(Type.class), buf.readUtf(), buf.readDouble(), buf.readEnum(AttributeModifier.Operation.class));
    }
}
