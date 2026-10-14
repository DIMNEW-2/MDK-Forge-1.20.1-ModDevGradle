package com.closky.rpgmod.worldgen;

import com.closky.rpgmod.registry.ModStructures;
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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

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
        return JigsawPlacement.addPieces(context, startPool, startJigsawName, size, pos, false, projectStartToHeightmap, maxDistanceFromCenter);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DUNGEON.get();
    }
}
