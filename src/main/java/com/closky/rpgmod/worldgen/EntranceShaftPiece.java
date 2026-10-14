package com.closky.rpgmod.worldgen;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.registry.ModStructures;
import com.closky.rpgmod.worldgen.DungeonRegistry.DungeonInfo;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Entrance of the underground dungeons (plan 4.2: "the entrance is always accessible without digging").
 * A spiral staircase around a central pillar climbs from the floor of the start room to the surface, where a small
 * hut with a door marks the entrance. The piece is appended to the jigsaw pieces by {@link DungeonStructure}.
 */
public class EntranceShaftPiece extends StructurePiece {
    /** Ring cells around the core pillar in walking order. */
    private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    private final int centerX;
    private final int centerZ;
    private final int roomFloorY;
    private final int roomTopY;
    private final int surfaceY;
    private final String dungeonId;

    public EntranceShaftPiece(int centerX, int centerZ, int roomFloorY, int roomTopY, int surfaceY, String dungeonId) {
        super(ModStructures.ENTRANCE_SHAFT.get(), 0,
                new BoundingBox(centerX - 3, roomFloorY, centerZ - 3, centerX + 3, surfaceY + 3, centerZ + 3));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.roomFloorY = roomFloorY;
        this.roomTopY = roomTopY;
        this.surfaceY = surfaceY;
        this.dungeonId = dungeonId;
    }

    public EntranceShaftPiece(CompoundTag tag) {
        super(ModStructures.ENTRANCE_SHAFT.get(), tag);
        this.centerX = tag.getInt("CX");
        this.centerZ = tag.getInt("CZ");
        this.roomFloorY = tag.getInt("FloorY");
        this.roomTopY = tag.getInt("TopY");
        this.surfaceY = tag.getInt("SurfaceY");
        this.dungeonId = tag.getString("Dungeon");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", centerX);
        tag.putInt("CZ", centerZ);
        tag.putInt("FloorY", roomFloorY);
        tag.putInt("TopY", roomTopY);
        tag.putInt("SurfaceY", surfaceY);
        tag.putString("Dungeon", dungeonId);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        DungeonInfo info = DungeonRegistry.get(dungeonId);
        BlockState wall = info != null ? parse(info.walls().get(0), Blocks.STONE_BRICKS.defaultBlockState()) : Blocks.STONE_BRICKS.defaultBlockState();
        BlockState floor = info != null ? parse(info.floor().get(0), wall) : wall;
        BlockState torch = info != null && info.light().contains("soul") ? Blocks.SOUL_TORCH.defaultBlockState() : Blocks.TORCH.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int y = roomFloorY; y <= surfaceY; y++) {
            boolean insideRoom = y < roomTopY;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int ax = Math.abs(dx);
                    int az = Math.abs(dz);
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (ax <= 1 && az <= 1) {
                        // core pillar and the spiral steps
                        if (dx == 0 && dz == 0) {
                            placeBlock(level, wall, x, y, z, box);
                        } else {
                            int ring = ringIndex(dx, dz);
                            int rel = y - roomFloorY - ring / 2;
                            if (rel >= 0 && rel % 4 == 0) {
                                placeBlock(level, wall, x, y, z, box);
                                if (ring == 0 && y + 1 <= surfaceY) {
                                    placeBlock(level, torch, x, y + 1, z, box);
                                }
                            } else if (!(ring == 0 && rel > 0 && (rel - 1) % 4 == 0)) {
                                placeBlock(level, air, x, y, z, box); // keep the torch placed by the step below
                            }
                        }
                    } else if (ax <= 2 && az <= 2) {
                        // shaft wall above the room; inside the room the staircase stands free
                        if (y == surfaceY) {
                            placeBlock(level, floor, x, y, z, box);
                        } else if (!insideRoom) {
                            placeBlock(level, wall, x, y, z, box);
                        }
                    } else if (y == surfaceY) {
                        placeBlock(level, floor, x, y, z, box); // hut floor
                    }
                }
            }
        }
        // the hut: 7x7 walls with a door on the south side and a flat roof
        for (int y = surfaceY + 1; y <= surfaceY + 2; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean perimeter = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                    boolean door = dx == 0 && dz == 3;
                    boolean core = dx == 0 && dz == 0;
                    BlockState state = core || (perimeter && !door) ? wall : air;
                    placeBlock(level, state, centerX + dx, y, centerZ + dz, box);
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                placeBlock(level, wall, centerX + dx, surfaceY + 3, centerZ + dz, box);
            }
        }
        placeBlock(level, torch, centerX + 2, surfaceY + 1, centerZ + 2, box);
        placeBlock(level, torch, centerX - 2, surfaceY + 1, centerZ - 2, box);
    }

    private static int ringIndex(int dx, int dz) {
        for (int i = 0; i < RING.length; i++) {
            if (RING[i][0] == dx && RING[i][1] == dz) {
                return i;
            }
        }
        return 0;
    }

    private static BlockState parse(String state, BlockState fallback) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), state, false).blockState();
        } catch (CommandSyntaxException e) {
            RpgMod.LOGGER.warn("Bad block state '{}' in dungeon palette", state);
            return fallback;
        }
    }
}
