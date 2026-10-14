package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.worldgen.DungeonStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, RpgMod.MODID);

    public static final RegistryObject<StructureType<DungeonStructure>> DUNGEON = STRUCTURE_TYPES.register("dungeon", () -> () -> DungeonStructure.CODEC);

    private ModStructures() {
    }
}
