package com.closky.rpgmod.crafting;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.config.RpgConfig;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Recipe condition {@code {"type":"rpgmod:config","key":"lootOnlyFromChests","value":false}}: the recipe is only
 * loaded when the boolean config option has the given value. Used to re-enable the vanilla bow / crossbow / sword
 * recipes (which the mod replaces) when {@code lootOnlyFromChests} is switched off.
 */
public record ConfigCondition(String key, boolean value) implements ICondition {
    public static final ResourceLocation ID = RpgMod.id("config");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return RpgConfig.getBoolean(key) == value;
    }

    public static class Serializer implements IConditionSerializer<ConfigCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ConfigCondition value) {
            json.addProperty("key", value.key());
            json.addProperty("value", value.value());
        }

        @Override
        public ConfigCondition read(JsonObject json) {
            return new ConfigCondition(GsonHelper.getAsString(json, "key"), GsonHelper.getAsBoolean(json, "value", true));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
