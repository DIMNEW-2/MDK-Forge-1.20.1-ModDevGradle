package com.closky.rpgmod.worldgen;

import com.closky.rpgmod.registry.ModStructures;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.List;
import java.util.Optional;

/**
 * Jigsaw-based dungeon structure (section 5). Compared to {@code minecraft:jigsaw} it adds two placement checks:
 * underground dungeons must start well below the surface and surface dungeons refuse to start in water.
 * Everything else (pools, templates, processors) is plain data-pack JSON / NBT.
 */
public class DungeonStructure extends Structure {
    public static final Codec<DungeonStructure> CODEC = RecordCodecBuilder.<DungeonStructure>mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            Codec.intRange(0, 30).fieldOf("size").forGetter(s -> s.size),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
            Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
            Codec.BOOL.optionalFieldOf("underground", false).forGetter(s -> s.underground),
            Codec.intRange(0, 64).optionalFieldOf("min_depth_below_surface", 12).forGetter(s -> s.minDepthBelowSurface)
    ).apply(instance, DungeonStructure::new)).codec();

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final boolean underground;
    private final int minDepthBelowSurface;

    public DungeonStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName,
                            int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap,
                            int maxDistanceFromCenter, boolean underground, int minDepthBelowSurface) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.underground = underground;
        this.minDepthBelowSurface = minDepthBelowSurface;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMinBlockX() + 8;
        int z = chunkPos.getMinBlockZ() + 8;
        int y = startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (underground) {
            if (y > surface - minDepthBelowSurface) {
                y = surface - minDepthBelowSurface - context.random().nextInt(16);
            }
            if (y < context.heightAccessor().getMinBuildHeight() + 8) {
                return Optional.empty();
            }
        } else {
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            if (!column.getBlock(surface).getFluidState().isEmpty() || !column.getBlock(surface + 1).getFluidState().isEmpty()) {
                return Optional.empty();
            }
            if (surface < context.chunkGenerator().getSeaLevel()) {
                return Optional.empty();
            }
        }
        BlockPos pos = new BlockPos(x, y, z);
        Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, startJigsawName, size, pos, false, projectStartToHeightmap, maxDistanceFromCenter);
        if (!underground || stub.isEmpty()) {
            return stub;
        }
        // underground dungeons get a staircase from the start room up to the surface (plan 4.2)
        GenerationStub inner = stub.get();
        StructurePiecesBuilder builder = inner.getPiecesBuilder();
        List<StructurePiece> pieces = builder.build().pieces();
        if (pieces.isEmpty()) {
            return Optional.of(new GenerationStub(inner.position(), Either.right(builder)));
        }
        BoundingBox room = pieces.get(0).getBoundingBox();
        int centerX = (room.minX() + room.maxX()) / 2;
        int centerZ = (room.minZ() + room.maxZ()) / 2;
        // OCEAN_FLOOR_WG ignores water, so the block right above the ground tells whether the spot is flooded
        int surfaceAtCenter = context.chunkGenerator().getFirstOccupiedHeight(centerX, centerZ, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
        NoiseColumn column = context.chunkGenerator().getBaseColumn(centerX, centerZ, context.heightAccessor(), context.randomState());
        if (!column.getBlock(surfaceAtCenter + 1).getFluidState().isEmpty()) {
            return Optional.empty(); // no entrances under lakes and oceans
        }
        if (surfaceAtCenter > room.maxY() + 2) {
            builder.addPiece(new EntranceShaftPiece(centerX, centerZ, room.minY() + 1, room.maxY(), surfaceAtCenter, dungeonId()));
        }
        return Optional.of(new GenerationStub(inner.position(), Either.right(builder)));
    }

    /** Dungeon id derived from the start pool ({@code rpgmod:<id>/start}). */
    private String dungeonId() {
        String path = startPool.unwrapKey().map(key -> key.location().getPath()).orElse("");
        int slash = path.indexOf('/');
        return slash > 0 ? path.substring(0, slash) : path;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DUNGEON.get();
    }
}
