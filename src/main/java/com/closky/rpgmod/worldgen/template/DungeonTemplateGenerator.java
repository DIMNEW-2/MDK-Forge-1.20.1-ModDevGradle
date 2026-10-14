package com.closky.rpgmod.worldgen.template;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.worldgen.DungeonRegistry;
import com.closky.rpgmod.worldgen.DungeonRegistry.DungeonInfo;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Procedural generator of the dungeon jigsaw pieces (section 5.4). Each dungeon gets seven pieces:
 * {@code start, corridor, room_a, room_b, treasure, end, boss_room}. The algorithm mirrors
 * {@code tools/gen_templates.py}, which produced the templates shipped in {@code data/rpgmod/structures}; the
 * in-game command writes fresh variants to the world's {@code generated/rpgmod/structures} folder where they
 * override the shipped ones (handy for iterating on dungeon layouts without rebuilding the mod).
 */
public final class DungeonTemplateGenerator {
    public static final String CONNECTOR = "rpgmod:conn";

    private DungeonTemplateGenerator() {
    }

    /** Generates and saves all pieces of all dungeons. Returns the number of templates written. */
    public static int generateAll(ServerLevel level) {
        StructureTemplateManager manager = level.getStructureManager();
        int count = 0;
        for (DungeonInfo info : DungeonRegistry.all()) {
            Random random = new Random(info.id().hashCode() ^ level.getSeed());
            for (Map.Entry<String, TemplateBuilder> entry : buildPieces(info, random).entrySet()) {
                ResourceLocation id = RpgMod.id(info.id() + "/" + entry.getKey());
                StructureTemplate template = manager.getOrCreate(id);
                template.load(level.holderLookup(Registries.BLOCK), entry.getValue().build());
                template.setAuthor("rpgmod");
                if (manager.save(id)) {
                    count++;
                } else {
                    RpgMod.LOGGER.warn("Could not save template {}", id);
                }
            }
        }
        return count;
    }

    public static Map<String, TemplateBuilder> buildPieces(DungeonInfo info, Random random) {
        Map<String, TemplateBuilder> pieces = new java.util.LinkedHashMap<>();
        String poolBase = "rpgmod:" + info.id() + "/";

        Map<Direction, String> startConnectors = new EnumMap<>(Direction.class);
        startConnectors.put(Direction.NORTH, poolBase + "boss");
        startConnectors.put(Direction.EAST, poolBase + "corridors");
        startConnectors.put(Direction.WEST, poolBase + "corridors");
        startConnectors.put(Direction.SOUTH, poolBase + "corridors");
        pieces.put("start", room(info, random, 11, 7, 11, startConnectors, 1, 0, false, false));

        pieces.put("corridor", corridor(info, random, 9, poolBase));

        Map<Direction, String> roomAConnectors = new EnumMap<>(Direction.class);
        roomAConnectors.put(Direction.SOUTH, poolBase + "ends");
        roomAConnectors.put(Direction.EAST, poolBase + "corridors");
        roomAConnectors.put(Direction.WEST, poolBase + "corridors");
        pieces.put("room_a", room(info, random, 9, 6, 9, roomAConnectors, 1 + random.nextInt(2), 1, false, false));

        Map<Direction, String> roomBConnectors = new EnumMap<>(Direction.class);
        roomBConnectors.put(Direction.SOUTH, poolBase + "ends");
        roomBConnectors.put(Direction.NORTH, poolBase + "corridors");
        pieces.put("room_b", room(info, random, 13, 7, 9, roomBConnectors, 2, 2, false, true));

        Map<Direction, String> treasureConnectors = new EnumMap<>(Direction.class);
        treasureConnectors.put(Direction.SOUTH, poolBase + "ends");
        pieces.put("treasure", room(info, random, 7, 5, 7, treasureConnectors, 3, 1, true, false));

        pieces.put("end", endPlug(info, poolBase));
        pieces.put("boss_room", bossRoom(info, random, poolBase));
        return pieces;
    }

    // ------------------------------------------------------------------ pieces

    static String pick(List<String> options, Random random) {
        return options.get(random.nextInt(options.size()));
    }

    static String wall(DungeonInfo info, Random random) {
        // first entry is the primary block: make it dominate
        return random.nextFloat() < 0.55F ? info.walls().get(0) : pick(info.walls(), random);
    }

    /** Generic room piece. Connectors are doorways with jigsaw blocks; SOUTH is the "back" by convention. */
    static TemplateBuilder room(DungeonInfo info, Random random, int w, int h, int d, Map<Direction, String> connectors,
                                int chests, int spawners, boolean rareChest, boolean pillars) {
        TemplateBuilder b = new TemplateBuilder(w, h, d);
        shell(b, info, random, 0, 0, 0, w - 1, h - 1, d - 1);
        for (Map.Entry<Direction, String> entry : connectors.entrySet()) {
            doorway(b, info, entry.getKey(), entry.getValue(), 0, 0, 0, w - 1, h - 1, d - 1);
        }
        lights(b, info, 1, 1, w - 2, h - 2, d - 2);
        if (pillars) {
            for (int x : new int[]{3, w - 4}) {
                for (int z : new int[]{2, d - 3}) {
                    b.fill(x, 1, z, x, h - 2, z, info.walls().get(0));
                }
            }
        }
        decorate(b, info, random, w, h, d, connectors.keySet());
        placeChests(b, info, random, w, d, chests, rareChest, connectors.keySet());
        placeSpawners(b, info, random, w, d, spawners);
        return b;
    }

    static TemplateBuilder corridor(DungeonInfo info, Random random, int length, String poolBase) {
        int w = 5;
        int h = 5;
        TemplateBuilder b = new TemplateBuilder(w, h, length);
        shell(b, info, random, 0, 0, 0, w - 1, h - 1, length - 1);
        doorway(b, info, Direction.SOUTH, poolBase + "ends", 0, 0, 0, w - 1, h - 1, length - 1);
        doorway(b, info, Direction.NORTH, poolBase + "rooms", 0, 0, 0, w - 1, h - 1, length - 1);
        // supports every 4 blocks + a light in the middle
        for (int z = 2; z < length - 2; z += 4) {
            b.set(1, 1, z, info.walls().get(0));
            b.set(1, 2, z, info.walls().get(0));
            b.set(w - 2, 1, z, info.walls().get(0));
            b.set(w - 2, 2, z, info.walls().get(0));
            if (info.roofed()) {
                b.set(2, h - 2, z, info.light());
            }
        }
        if (random.nextFloat() < 0.5F) {
            b.set(2 + random.nextInt(2) - 1, 1, length / 2, pick(info.decor(), random));
        }
        return b;
    }

    static TemplateBuilder endPlug(DungeonInfo info, String poolBase) {
        TemplateBuilder b = new TemplateBuilder(5, 5, 1);
        b.fill(0, 0, 0, 4, 4, 0, info.walls().get(0));
        b.setNbt(2, 1, 0, "minecraft:jigsaw[orientation=south_up]",
                TemplateBuilder.jigsawNbt(CONNECTOR, CONNECTOR, poolBase + "ends", info.walls().get(0), "aligned"));
        return b;
    }

    static TemplateBuilder bossRoom(DungeonInfo info, Random random, String poolBase) {
        int w = 17;
        int h = 9;
        int d = 17;
        int corridorLen = 7;
        TemplateBuilder b = new TemplateBuilder(w, h, d + corridorLen);
        shell(b, info, random, 0, 0, 0, w - 1, h - 1, d - 1);
        // corridor section attached to the south wall
        int cx = w / 2;
        shell(b, info, random, cx - 2, 0, d - 1, cx + 2, 4, d - 1 + corridorLen);
        // opening between room and corridor
        b.fill(cx - 1, 1, d - 1, cx + 1, 3, d - 1, TemplateBuilder.AIR);
        // back connector at the end of the corridor
        b.setNbt(cx, 1, d - 1 + corridorLen, "minecraft:jigsaw[orientation=south_up]",
                TemplateBuilder.jigsawNbt(CONNECTOR, CONNECTOR, poolBase + "ends", TemplateBuilder.AIR, "aligned"));
        b.set(cx, 2, d - 1 + corridorLen, TemplateBuilder.AIR);
        b.set(cx, 3, d - 1 + corridorLen, TemplateBuilder.AIR);
        lights(b, info, 1, 1, w - 2, h - 2, d - 2);
        // pillars
        for (int x : new int[]{3, w - 4}) {
            for (int z : new int[]{3, d - 4}) {
                b.fill(x, 1, z, x, h - 2, z, info.walls().get(0));
            }
        }
        // throne dais at the north end with the boss chest
        b.fill(cx - 2, 1, 1, cx + 2, 1, 3, info.floor().get(0));
        b.setNbt(cx, 2, 1, "rpgmod:loot_chest_boss[facing=south,open=false]", TemplateBuilder.lootChestNbt(info.lootTable() + "_boss"));
        b.set(cx - 2, 2, 1, info.light());
        b.set(cx + 2, 2, 1, info.light());
        // boss spawner in the floor at the centre
        b.setNbt(cx, 0, d / 2, "rpgmod:boss_spawner", TemplateBuilder.bossSpawnerNbt(info.boss().getKey()));
        decorate(b, info, random, w, h, d, java.util.Set.of(Direction.SOUTH));
        placeChests(b, info, random, w, d, 2, true, java.util.Set.of(Direction.SOUTH, Direction.NORTH));
        return b;
    }

    // ------------------------------------------------------------------ primitives

    /** Hollow box with walls, floor and ceiling; interior filled with air. */
    static void shell(TemplateBuilder b, DungeonInfo info, Random random, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    boolean boundary = x == x1 || x == x2 || y == y1 || y == y2 || z == z1 || z == z2;
                    if (!boundary) {
                        b.set(x, y, z, TemplateBuilder.AIR);
                        continue;
                    }
                    if (y == y1) {
                        b.set(x, y, z, pick(info.floor(), random));
                    } else if (y == y2) {
                        if (!info.roofed()) {
                            continue; // open to the sky (camps)
                        }
                        if (info.ruinFactor() > 0 && random.nextFloat() < info.ruinFactor() * 2.0F) {
                            continue; // collapsed roof
                        }
                        b.set(x, y, z, wall(info, random));
                    } else {
                        if (info.ruinFactor() > 0 && y > (y1 + y2) / 2 && random.nextFloat() < info.ruinFactor()) {
                            continue; // crumbled upper wall
                        }
                        if (!info.roofed() && y >= y1 + 3 && random.nextFloat() < 0.5F) {
                            continue; // palisade: low, uneven walls
                        }
                        b.set(x, y, z, wall(info, random));
                    }
                }
            }
        }
    }

    /** 3-wide, 3-high doorway centred on the given wall, with a jigsaw block at floor level. */
    static void doorway(TemplateBuilder b, DungeonInfo info, Direction side, String pool, int x1, int y1, int z1, int x2, int y2, int z2) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        int jx;
        int jz;
        switch (side) {
            case NORTH -> {
                jx = cx;
                jz = z1;
                b.fill(cx - 1, y1 + 1, z1, cx + 1, y1 + 3, z1, TemplateBuilder.AIR);
            }
            case SOUTH -> {
                jx = cx;
                jz = z2;
                b.fill(cx - 1, y1 + 1, z2, cx + 1, y1 + 3, z2, TemplateBuilder.AIR);
            }
            case WEST -> {
                jx = x1;
                jz = cz;
                b.fill(x1, y1 + 1, cz - 1, x1, y1 + 3, cz + 1, TemplateBuilder.AIR);
            }
            default -> {
                jx = x2;
                jz = cz;
                b.fill(x2, y1 + 1, cz - 1, x2, y1 + 3, cz + 1, TemplateBuilder.AIR);
            }
        }
        String orientation = side.getName() + "_up";
        b.setNbt(jx, y1 + 1, jz, "minecraft:jigsaw[orientation=" + orientation + "]",
                TemplateBuilder.jigsawNbt(CONNECTOR, CONNECTOR, pool, TemplateBuilder.AIR, "aligned"));
        // lintel lights beside the door
        if (info.roofed()) {
            String torch = info.light().startsWith("minecraft:torch") ? "minecraft:torch" : null;
            if (torch != null) {
                switch (side) {
                    case NORTH -> b.set(cx - 2 >= x1 + 1 ? cx - 2 : cx + 2, y1 + 2, z1 + 1, torch);
                    case SOUTH -> b.set(cx - 2 >= x1 + 1 ? cx - 2 : cx + 2, y1 + 2, z2 - 1, torch);
                    case WEST -> b.set(x1 + 1, y1 + 2, cz - 2 >= z1 + 1 ? cz - 2 : cz + 2, torch);
                    default -> b.set(x2 - 1, y1 + 2, cz - 2 >= z1 + 1 ? cz - 2 : cz + 2, torch);
                }
            }
        }
    }

    /** Lights in the four interior corners (hanging from the ceiling or standing on the floor). */
    static void lights(TemplateBuilder b, DungeonInfo info, int x1, int y1, int x2, int y2, int z2) {
        boolean hanging = info.light().contains("hanging=true");
        boolean ceilingBlock = info.light().equals("minecraft:sea_lantern") || info.light().equals("minecraft:glowstone");
        int[][] corners = {{x1, 1}, {x2, 1}, {x1, z2}, {x2, z2}};
        for (int[] c : corners) {
            if (ceilingBlock) {
                b.set(c[0], y2 + 1, c[1], info.light());
            } else if (hanging) {
                b.set(c[0], y2, c[1], info.light());
            } else {
                b.set(c[0], y1, c[1], info.light());
            }
        }
    }

    static void decorate(TemplateBuilder b, DungeonInfo info, Random random, int w, int h, int d, java.util.Set<Direction> doors) {
        int count = Math.max(1, (w * d) / 14);
        for (int i = 0; i < count; i++) {
            int x = 1 + random.nextInt(w - 2);
            int z = 1 + random.nextInt(d - 2);
            if (blocksDoor(x, z, w, d, doors) || !b.isAir(x, 1, z)) {
                continue;
            }
            String decor = pick(info.decor(), random);
            if (decor.contains("cobweb") && random.nextBoolean()) {
                b.set(x, h - 2, z, decor); // webs prefer ceilings
            } else {
                b.set(x, 1, z, decor);
            }
        }
    }

    static boolean blocksDoor(int x, int z, int w, int d, java.util.Set<Direction> doors) {
        int cx = (w - 1) / 2;
        int cz = (d - 1) / 2;
        if (doors.contains(Direction.NORTH) && Math.abs(x - cx) <= 1 && z <= 2) {
            return true;
        }
        if (doors.contains(Direction.SOUTH) && Math.abs(x - cx) <= 1 && z >= d - 3) {
            return true;
        }
        if (doors.contains(Direction.WEST) && Math.abs(z - cz) <= 1 && x <= 2) {
            return true;
        }
        return doors.contains(Direction.EAST) && Math.abs(z - cz) <= 1 && x >= w - 3;
    }

    static void placeChests(TemplateBuilder b, DungeonInfo info, Random random, int w, int d, int count, boolean rare, java.util.Set<Direction> doors) {
        List<int[]> spots = new ArrayList<>();
        for (int x = 1; x < w - 1; x++) {
            spots.add(new int[]{x, 1, 2}); // against north wall, facing south
            spots.add(new int[]{x, d - 2, 0}); // against south wall, facing north
        }
        for (int z = 1; z < d - 1; z++) {
            spots.add(new int[]{1, z, 3}); // west wall, facing east
            spots.add(new int[]{w - 2, z, 1}); // east wall, facing west
        }
        String[] facing = {"north", "west", "south", "east"};
        int placed = 0;
        int attempts = 0;
        while (placed < count && attempts++ < 50 && !spots.isEmpty()) {
            int[] spot = spots.remove(random.nextInt(spots.size()));
            if (blocksDoor(spot[0], spot[1], w, d, doors) || !b.isAir(spot[0], 1, spot[1])) {
                continue;
            }
            boolean thisRare = rare && placed == 0;
            String block = thisRare ? "rpgmod:loot_chest_rare" : "rpgmod:loot_chest";
            b.setNbt(spot[0], 1, spot[1], block + "[facing=" + facing[spot[2]] + ",open=false]", TemplateBuilder.lootChestNbt(info.lootTable()));
            placed++;
        }
    }

    static void placeSpawners(TemplateBuilder b, DungeonInfo info, Random random, int w, int d, int count) {
        int placed = 0;
        int attempts = 0;
        while (placed < count && attempts++ < 40) {
            int x = 2 + random.nextInt(Math.max(1, w - 4));
            int z = 2 + random.nextInt(Math.max(1, d - 4));
            if (!b.isAir(x, 1, z)) {
                continue;
            }
            b.setNbt(x, 1, z, "minecraft:spawner", TemplateBuilder.mobSpawnerNbt(pick(info.spawnerMobs(), random)));
            placed++;
        }
    }
}
