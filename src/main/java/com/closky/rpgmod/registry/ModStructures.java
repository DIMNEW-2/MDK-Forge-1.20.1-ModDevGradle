package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.worldgen.DungeonStructure;
import com.closky.rpgmod.worldgen.EntranceShaftPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, RpgMod.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, RpgMod.MODID);

    public static final RegistryObject<StructureType<DungeonStructure>> DUNGEON = STRUCTURE_TYPES.register("dungeon", () -> () -> DungeonStructure.CODEC);
    public static final RegistryObject<StructurePieceType> ENTRANCE_SHAFT = STRUCTURE_PIECES.register("entrance_shaft",
            () -> (StructurePieceType.ContextlessType) EntranceShaftPiece::new);

    private ModStructures() {
    }
}
