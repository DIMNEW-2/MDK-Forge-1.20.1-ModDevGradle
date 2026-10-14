package com.closky.rpgmod.loot;

import com.closky.rpgmod.enemy.MobLevel;
import com.closky.rpgmod.registry.ModLootFunctions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Loot function {@code rpgmod:set_rpg_level}: assigns an item level (fixed via {@code level}, or derived from the
 * zone level at the loot origin plus {@code offset_min..offset_max}) and optionally rolls random bonuses.
 *
 * <pre>
 * {"function":"rpgmod:set_rpg_level","level":{"min":5,"max":8},"bonuses":{"min":0,"max":2}}
 * {"function":"rpgmod:set_rpg_level","offset_min":2,"offset_max":5}
 * </pre>
 */
public class SetRpgLevelFunction extends LootItemConditionalFunction {
    @Nullable
    private final NumberProvider level;
    private final int offsetMin;
    private final int offsetMax;
    @Nullable
    private final NumberProvider bonuses;

    public SetRpgLevelFunction(LootItemCondition[] conditions, @Nullable NumberProvider level, int offsetMin, int offsetMax, @Nullable NumberProvider bonuses) {
        super(conditions);
        this.level = level;
        this.offsetMin = offsetMin;
        this.offsetMax = offsetMax;
        this.bonuses = bonuses;
    }

    @Override
    public LootItemFunctionType getType() {
        return ModLootFunctions.SET_RPG_LEVEL.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!RpgItemData.isRpgItem(stack)) {
            return stack;
        }
        int base;
        if (level != null) {
            base = level.getInt(context);
        } else {
            Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            base = origin == null ? 1 : MobLevel.zoneLevel(context.getLevel(), BlockPos.containing(origin));
        }
        int offset = offsetMax <= offsetMin ? offsetMin : offsetMin + context.getRandom().nextInt(offsetMax - offsetMin + 1);
        int itemLevel = Math.max(1, base + offset);
        itemLevel = Math.max(itemLevel, RpgItemData.getItemLevel(stack.copy()) - 2);
        RpgItemData.setItemLevel(stack, itemLevel);
        int count = bonuses != null ? bonuses.getInt(context) : LootItemHelper.bonusCount(itemLevel, context.getRandom());
        if (count > 0) {
            RpgItemData.rollBonuses(stack, new Random(context.getRandom().nextLong()), count);
        }
        return stack;
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetRpgLevelFunction> {
        @Override
        public void serialize(JsonObject json, SetRpgLevelFunction function, JsonSerializationContext context) {
            super.serialize(json, function, context);
            if (function.level != null) {
                json.add("level", context.serialize(function.level));
            }
            json.addProperty("offset_min", function.offsetMin);
            json.addProperty("offset_max", function.offsetMax);
            if (function.bonuses != null) {
                json.add("bonuses", context.serialize(function.bonuses));
            }
        }

        @Override
        public SetRpgLevelFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
            NumberProvider level = json.has("level") ? GsonHelper.getAsObject(json, "level", context, NumberProvider.class) : null;
            int offsetMin = GsonHelper.getAsInt(json, "offset_min", 0);
            int offsetMax = GsonHelper.getAsInt(json, "offset_max", offsetMin);
            NumberProvider bonuses = json.has("bonuses") ? GsonHelper.getAsObject(json, "bonuses", context, NumberProvider.class) : null;
            return new SetRpgLevelFunction(conditions, level, offsetMin, offsetMax, bonuses);
        }
    }
}
