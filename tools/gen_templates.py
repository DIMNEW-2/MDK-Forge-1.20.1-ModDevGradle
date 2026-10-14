#!/usr/bin/env python3
"""Generates the jigsaw templates of the eight dungeons as gzip NBT files in data/rpgmod/structures/<dungeon>/.

This is a port of com.closky.rpgmod.worldgen.template.DungeonTemplateGenerator (same pieces, same palettes) so that
the shipped templates and the in-game "/rpgmod dungeon generate_templates" command produce compatible layouts.

Pieces per dungeon: start, corridor, room_a, room_b, treasure, end, boss_room.

Run from the repository root:  python3 tools/gen_templates.py
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import nbtlite  # noqa: E402
from nbtlite import Int, Long, Short  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "rpgmod", "structures")
DATA_VERSION = 3465  # 1.20.1
AIR = "minecraft:air"
CONNECTOR = "rpgmod:conn"

NORTH, SOUTH, WEST, EAST = "north", "south", "west", "east"


class DungeonInfo:
    def __init__(self, id_, level, boss, underground, walls, floor, light, decor, spawner_mobs, ruin_factor, roofed):
        self.id = id_
        self.level = level
        self.boss = boss
        self.underground = underground
        self.walls = walls
        self.floor = floor
        self.light = light
        self.decor = decor
        self.spawner_mobs = spawner_mobs
        self.ruin_factor = ruin_factor
        self.roofed = roofed

    @property
    def loot_table(self):
        return "rpgmod:chests/" + self.id


# mirrors DungeonRegistry.java
DUNGEONS = [
    DungeonInfo("catacombs_basic", 5, "skeleton_king", True,
                ["minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks"],
                ["minecraft:stone_bricks", "minecraft:cobblestone", "minecraft:gravel"],
                "minecraft:soul_lantern[hanging=true]",
                ["minecraft:cobweb", "minecraft:skeleton_skull[rotation=4]", "minecraft:bone_block", "minecraft:brown_mushroom"],
                ["minecraft:skeleton", "minecraft:zombie"], 0.0, True),
    DungeonInfo("bandit_camp", 6, "bandit_chief", False,
                ["minecraft:spruce_planks", "minecraft:spruce_log[axis=y]", "minecraft:stripped_spruce_log[axis=y]"],
                ["minecraft:coarse_dirt", "minecraft:dirt_path", "minecraft:spruce_planks"],
                "minecraft:torch",
                ["minecraft:hay_block", "minecraft:barrel[facing=up]", "minecraft:campfire[lit=true]", "minecraft:spruce_fence"],
                ["minecraft:pillager", "minecraft:vindicator"], 0.0, False),
    DungeonInfo("deep_mines", 7, "mine_overseer", True,
                ["minecraft:cobblestone", "minecraft:stone", "minecraft:andesite", "minecraft:coal_ore", "minecraft:iron_ore"],
                ["minecraft:cobblestone", "minecraft:gravel", "minecraft:stone"],
                "minecraft:lantern[hanging=true]",
                ["minecraft:oak_fence", "minecraft:rail", "minecraft:cobweb", "minecraft:oak_planks"],
                ["minecraft:cave_spider", "minecraft:zombie", "minecraft:husk"], 0.0, True),
    DungeonInfo("abandoned_manor", 8, "manor_ghost", False,
                ["minecraft:dark_oak_planks", "minecraft:dark_oak_planks", "minecraft:cobblestone", "minecraft:dark_oak_log[axis=y]"],
                ["minecraft:dark_oak_planks", "minecraft:spruce_planks"],
                "minecraft:candle[candles=3,lit=true]",
                ["minecraft:cobweb", "minecraft:bookshelf", "minecraft:flower_pot", "minecraft:dark_oak_stairs[facing=north]"],
                ["minecraft:zombie_villager", "minecraft:witch", "minecraft:skeleton"], 0.05, True),
    DungeonInfo("labyrinth_basic", 9, "labyrinth_minotaur", True,
                ["minecraft:deepslate_bricks", "minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks", "minecraft:tuff"],
                ["minecraft:deepslate_tiles", "minecraft:deepslate_bricks"],
                "minecraft:lantern[hanging=true]",
                ["minecraft:cobweb", "minecraft:deepslate_brick_wall", "minecraft:glow_lichen[down=true]", "minecraft:moss_carpet"],
                ["minecraft:zombie", "minecraft:spider", "minecraft:cave_spider"], 0.0, True),
    DungeonInfo("underground_fortress", 10, "fortress_lord", True,
                ["minecraft:polished_deepslate", "minecraft:deepslate_tiles", "minecraft:polished_blackstone_bricks"],
                ["minecraft:polished_deepslate", "minecraft:deepslate_tiles"],
                "minecraft:soul_lantern[hanging=true]",
                ["minecraft:iron_bars", "minecraft:chain[axis=y]", "minecraft:anvil[facing=north]", "minecraft:polished_blackstone_wall"],
                ["minecraft:skeleton", "minecraft:zombie", "minecraft:vindicator"], 0.0, True),
    DungeonInfo("ruined_castle", 12, "fallen_king", False,
                ["minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cobblestone", "minecraft:mossy_cobblestone"],
                ["minecraft:stone_bricks", "minecraft:cobblestone", "minecraft:grass_block"],
                "minecraft:torch",
                ["minecraft:cobweb", "minecraft:vine[north=true]", "minecraft:stone_brick_slab", "minecraft:fern"],
                ["minecraft:skeleton", "minecraft:zombie", "minecraft:stray"], 0.12, True),
    DungeonInfo("arcane_tower", 15, "archmage", False,
                ["minecraft:quartz_bricks", "minecraft:smooth_quartz", "minecraft:chiseled_quartz_block", "minecraft:amethyst_block"],
                ["minecraft:smooth_quartz", "minecraft:purpur_block"],
                "minecraft:sea_lantern",
                ["minecraft:bookshelf", "minecraft:enchanting_table", "minecraft:lectern[facing=north]", "minecraft:amethyst_cluster[facing=up]"],
                ["minecraft:witch", "minecraft:skeleton", "minecraft:blaze"], 0.0, True),
]


# ------------------------------------------------------------------------------------------ template builder
class TemplateBuilder:
    """Port of TemplateBuilder.java: sparse 3D block map -> structure template NBT."""

    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}       # (x,y,z) -> palette index (insertion ordered)
        self.block_nbt = {}
        self.palette = []
        self.palette_index = {}

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, state, nbt=None):
        if not self.inside(x, y, z):
            return
        idx = self.palette_index.get(state)
        if idx is None:
            idx = len(self.palette)
            self.palette.append(state)
            self.palette_index[state] = idx
        key = (x, y, z)
        self.blocks[key] = idx
        if nbt is not None:
            self.block_nbt[key] = nbt
        else:
            self.block_nbt.pop(key, None)

    def get(self, x, y, z):
        idx = self.blocks.get((x, y, z))
        return None if idx is None else self.palette[idx]

    def is_air(self, x, y, z):
        return self.get(x, y, z) == AIR

    def fill(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, state)

    @staticmethod
    def parse_state(state):
        bracket = state.find("[")
        if bracket < 0:
            return {"Name": state}
        props = {}
        for pair in state[bracket + 1:-1].split(","):
            if "=" in pair:
                k, v = pair.split("=", 1)
                props[k.strip()] = v.strip()
        return {"Name": state[:bracket], "Properties": props}

    def build(self):
        blocks = []
        for (x, y, z), idx in self.blocks.items():
            entry = {"pos": [Int(x), Int(y), Int(z)], "state": Int(idx)}
            nbt = self.block_nbt.get((x, y, z))
            if nbt is not None:
                entry["nbt"] = nbt
            blocks.append(entry)
        return {
            "DataVersion": Int(DATA_VERSION),
            "author": "rpgmod",
            "size": [Int(self.size[0]), Int(self.size[1]), Int(self.size[2])],
            "palette": [self.parse_state(s) for s in self.palette],
            "blocks": blocks,
            "entities": [],
        }


def jigsaw_nbt(name, target, pool, final_state, joint):
    return {"id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final_state, "joint": joint}


def loot_chest_nbt(loot_table):
    return {"id": "rpgmod:loot_chest", "LootTable": loot_table, "LootTableSeed": Long(0)}


def boss_spawner_nbt(boss_id):
    return {"id": "rpgmod:boss_spawner", "Boss": boss_id}


def mob_spawner_nbt(entity_id):
    spawn_data = {"entity": {"id": entity_id}}
    return {
        "id": "minecraft:mob_spawner",
        "SpawnData": spawn_data,
        "SpawnPotentials": [{"weight": Int(1), "data": {"entity": {"id": entity_id}}}],
        "Delay": Short(20), "MinSpawnDelay": Short(200), "MaxSpawnDelay": Short(800), "SpawnCount": Short(3),
        "MaxNearbyEntities": Short(6), "RequiredPlayerRange": Short(16), "SpawnRange": Short(4),
    }


# ------------------------------------------------------------------------------------------ pieces (port of the Java code)
def pick(options, rnd):
    return options[rnd.randrange(len(options))]


def wall(info, rnd):
    return info.walls[0] if rnd.random() < 0.55 else pick(info.walls, rnd)


def shell(b, info, rnd, x1, y1, z1, x2, y2, z2):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                boundary = x in (x1, x2) or y in (y1, y2) or z in (z1, z2)
                if not boundary:
                    b.set(x, y, z, AIR)
                    continue
                if y == y1:
                    b.set(x, y, z, pick(info.floor, rnd))
                elif y == y2:
                    if not info.roofed:
                        continue
                    if info.ruin_factor > 0 and rnd.random() < info.ruin_factor * 2.0:
                        continue
                    b.set(x, y, z, wall(info, rnd))
                else:
                    if info.ruin_factor > 0 and y > (y1 + y2) // 2 and rnd.random() < info.ruin_factor:
                        continue
                    if not info.roofed and y >= y1 + 3 and rnd.random() < 0.5:
                        continue
                    b.set(x, y, z, wall(info, rnd))


def doorway(b, info, side, pool, x1, y1, z1, x2, y2, z2):
    cx = (x1 + x2) // 2
    cz = (z1 + z2) // 2
    if side == NORTH:
        jx, jz = cx, z1
        b.fill(cx - 1, y1 + 1, z1, cx + 1, y1 + 3, z1, AIR)
    elif side == SOUTH:
        jx, jz = cx, z2
        b.fill(cx - 1, y1 + 1, z2, cx + 1, y1 + 3, z2, AIR)
    elif side == WEST:
        jx, jz = x1, cz
        b.fill(x1, y1 + 1, cz - 1, x1, y1 + 3, cz + 1, AIR)
    else:
        jx, jz = x2, cz
        b.fill(x2, y1 + 1, cz - 1, x2, y1 + 3, cz + 1, AIR)
    b.set(jx, y1 + 1, jz, "minecraft:jigsaw[orientation=%s_up]" % side, jigsaw_nbt(CONNECTOR, CONNECTOR, pool, AIR, "aligned"))
    if info.roofed and info.light.startswith("minecraft:torch"):
        torch = "minecraft:torch"
        if side == NORTH:
            b.set(cx - 2 if cx - 2 >= x1 + 1 else cx + 2, y1 + 2, z1 + 1, torch)
        elif side == SOUTH:
            b.set(cx - 2 if cx - 2 >= x1 + 1 else cx + 2, y1 + 2, z2 - 1, torch)
        elif side == WEST:
            b.set(x1 + 1, y1 + 2, cz - 2 if cz - 2 >= z1 + 1 else cz + 2, torch)
        else:
            b.set(x2 - 1, y1 + 2, cz - 2 if cz - 2 >= z1 + 1 else cz + 2, torch)


def lights(b, info, x1, y1, x2, y2, z2):
    hanging = "hanging=true" in info.light
    ceiling_block = info.light in ("minecraft:sea_lantern", "minecraft:glowstone")
    for cx, cz in ((x1, 1), (x2, 1), (x1, z2), (x2, z2)):
        if ceiling_block:
            b.set(cx, y2 + 1, cz, info.light)
        elif hanging:
            b.set(cx, y2, cz, info.light)
        else:
            b.set(cx, y1, cz, info.light)


def blocks_door(x, z, w, d, doors):
    cx = (w - 1) // 2
    cz = (d - 1) // 2
    if NORTH in doors and abs(x - cx) <= 1 and z <= 2:
        return True
    if SOUTH in doors and abs(x - cx) <= 1 and z >= d - 3:
        return True
    if WEST in doors and abs(z - cz) <= 1 and x <= 2:
        return True
    return EAST in doors and abs(z - cz) <= 1 and x >= w - 3


def decorate(b, info, rnd, w, h, d, doors):
    count = max(1, (w * d) // 14)
    for _ in range(count):
        x = 1 + rnd.randrange(w - 2)
        z = 1 + rnd.randrange(d - 2)
        if blocks_door(x, z, w, d, doors) or not b.is_air(x, 1, z):
            continue
        decor = pick(info.decor, rnd)
        if "cobweb" in decor and rnd.random() < 0.5:
            b.set(x, h - 2, z, decor)
        else:
            b.set(x, 1, z, decor)


def place_chests(b, info, rnd, w, d, count, rare, doors):
    spots = []
    for x in range(1, w - 1):
        spots.append((x, 1, 2))
        spots.append((x, d - 2, 0))
    for z in range(1, d - 1):
        spots.append((1, z, 3))
        spots.append((w - 2, z, 1))
    facing = ("north", "west", "south", "east")
    placed = 0
    attempts = 0
    while placed < count and attempts < 50 and spots:
        attempts += 1
        spot = spots.pop(rnd.randrange(len(spots)))
        if blocks_door(spot[0], spot[1], w, d, doors) or not b.is_air(spot[0], 1, spot[1]):
            continue
        this_rare = rare and placed == 0
        block = "rpgmod:loot_chest_rare" if this_rare else "rpgmod:loot_chest"
        b.set(spot[0], 1, spot[1], "%s[facing=%s,open=false]" % (block, facing[spot[2]]), loot_chest_nbt(info.loot_table))
        placed += 1


def place_spawners(b, info, rnd, w, d, count):
    placed = 0
    attempts = 0
    while placed < count and attempts < 40:
        attempts += 1
        x = 2 + rnd.randrange(max(1, w - 4))
        z = 2 + rnd.randrange(max(1, d - 4))
        if not b.is_air(x, 1, z):
            continue
        b.set(x, 1, z, "minecraft:spawner", mob_spawner_nbt(pick(info.spawner_mobs, rnd)))
        placed += 1


def room(info, rnd, w, h, d, connectors, chests, spawners, rare_chest, pillars):
    b = TemplateBuilder(w, h, d)
    shell(b, info, rnd, 0, 0, 0, w - 1, h - 1, d - 1)
    for side, pool in connectors.items():
        doorway(b, info, side, pool, 0, 0, 0, w - 1, h - 1, d - 1)
    lights(b, info, 1, 1, w - 2, h - 2, d - 2)
    if pillars:
        for x in (3, w - 4):
            for z in (2, d - 3):
                b.fill(x, 1, z, x, h - 2, z, info.walls[0])
    doors = set(connectors.keys())
    decorate(b, info, rnd, w, h, d, doors)
    place_chests(b, info, rnd, w, d, chests, rare_chest, doors)
    place_spawners(b, info, rnd, w, d, spawners)
    return b


def corridor(info, rnd, length, pool_base):
    w, h = 5, 5
    b = TemplateBuilder(w, h, length)
    shell(b, info, rnd, 0, 0, 0, w - 1, h - 1, length - 1)
    doorway(b, info, SOUTH, pool_base + "ends", 0, 0, 0, w - 1, h - 1, length - 1)
    doorway(b, info, NORTH, pool_base + "rooms", 0, 0, 0, w - 1, h - 1, length - 1)
    for z in range(2, length - 2, 4):
        b.set(1, 1, z, info.walls[0])
        b.set(1, 2, z, info.walls[0])
        b.set(w - 2, 1, z, info.walls[0])
        b.set(w - 2, 2, z, info.walls[0])
        if info.roofed:
            b.set(2, h - 2, z, info.light)
    if rnd.random() < 0.5:
        b.set(2 + rnd.randrange(2) - 1, 1, length // 2, pick(info.decor, rnd))
    return b


def end_plug(info, pool_base):
    b = TemplateBuilder(5, 5, 1)
    b.fill(0, 0, 0, 4, 4, 0, info.walls[0])
    b.set(2, 1, 0, "minecraft:jigsaw[orientation=south_up]", jigsaw_nbt(CONNECTOR, CONNECTOR, pool_base + "ends", info.walls[0], "aligned"))
    return b


def boss_room(info, rnd, pool_base):
    w, h, d = 17, 9, 17
    corridor_len = 7
    b = TemplateBuilder(w, h, d + corridor_len)
    shell(b, info, rnd, 0, 0, 0, w - 1, h - 1, d - 1)
    cx = w // 2
    shell(b, info, rnd, cx - 2, 0, d - 1, cx + 2, 4, d - 1 + corridor_len)
    b.fill(cx - 1, 1, d - 1, cx + 1, 3, d - 1, AIR)
    b.set(cx, 1, d - 1 + corridor_len, "minecraft:jigsaw[orientation=south_up]", jigsaw_nbt(CONNECTOR, CONNECTOR, pool_base + "ends", AIR, "aligned"))
    b.set(cx, 2, d - 1 + corridor_len, AIR)
    b.set(cx, 3, d - 1 + corridor_len, AIR)
    lights(b, info, 1, 1, w - 2, h - 2, d - 2)
    for x in (3, w - 4):
        for z in (3, d - 4):
            b.fill(x, 1, z, x, h - 2, z, info.walls[0])
    b.fill(cx - 2, 1, 1, cx + 2, 1, 3, info.floor[0])
    b.set(cx, 2, 1, "rpgmod:loot_chest_boss[facing=south,open=false]", loot_chest_nbt(info.loot_table + "_boss"))
    b.set(cx - 2, 2, 1, info.light)
    b.set(cx + 2, 2, 1, info.light)
    b.set(cx, 0, d // 2, "rpgmod:boss_spawner", boss_spawner_nbt(info.boss))
    decorate(b, info, rnd, w, h, d, {SOUTH})
    place_chests(b, info, rnd, w, d, 2, True, {SOUTH, NORTH})
    return b


def build_pieces(info, rnd):
    pool_base = "rpgmod:" + info.id + "/"
    pieces = {}
    pieces["start"] = room(info, rnd, 11, 7, 11, {NORTH: pool_base + "boss", EAST: pool_base + "corridors",
                                                  WEST: pool_base + "corridors", SOUTH: pool_base + "corridors"}, 1, 0, False, False)
    pieces["corridor"] = corridor(info, rnd, 9, pool_base)
    pieces["room_a"] = room(info, rnd, 9, 6, 9, {SOUTH: pool_base + "ends", EAST: pool_base + "corridors", WEST: pool_base + "corridors"},
                            1 + rnd.randrange(2), 1, False, False)
    pieces["room_b"] = room(info, rnd, 13, 7, 9, {SOUTH: pool_base + "ends", NORTH: pool_base + "corridors"}, 2, 2, False, True)
    pieces["treasure"] = room(info, rnd, 7, 5, 7, {SOUTH: pool_base + "ends"}, 3, 1, True, False)
    pieces["end"] = end_plug(info, pool_base)
    pieces["boss_room"] = boss_room(info, rnd, pool_base)
    return pieces


def verify(path):
    """Round-trips a template file and checks the invariants the game relies on."""
    tag = nbtlite.read_gzip(path)
    size = tag["size"]
    palette = tag["palette"]
    jigsaws = 0
    for blk in tag["blocks"]:
        x, y, z = blk["pos"]
        assert 0 <= x < size[0] and 0 <= y < size[1] and 0 <= z < size[2], (path, blk["pos"])
        assert 0 <= blk["state"] < len(palette), path
        if palette[blk["state"]]["Name"] == "minecraft:jigsaw":
            jigsaws += 1
            assert blk["nbt"]["pool"].startswith("rpgmod:"), path
    assert jigsaws >= 1, path
    return jigsaws


def main():
    total = 0
    for info in DUNGEONS:
        rnd = random.Random(info.id)
        folder = os.path.join(OUT, info.id)
        os.makedirs(folder, exist_ok=True)
        for name, builder in build_pieces(info, rnd).items():
            path = os.path.join(folder, name + ".nbt")
            nbtlite.write_gzip(path, builder.build())
            verify(path)
            total += 1
    print("templates written:", total)


if __name__ == "__main__":
    main()
