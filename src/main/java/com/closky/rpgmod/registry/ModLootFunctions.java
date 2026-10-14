package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.loot.SetRpgLevelFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootFunctions {
    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, RpgMod.MODID);

    public static final RegistryObject<LootItemFunctionType> SET_RPG_LEVEL = LOOT_FUNCTIONS.register("set_rpg_level",
            () -> new LootItemFunctionType(new SetRpgLevelFunction.Serializer()));

    private ModLootFunctions() {
    }
}
