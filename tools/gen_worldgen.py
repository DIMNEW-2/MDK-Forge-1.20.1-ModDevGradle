#!/usr/bin/env python3
"""Generates the worldgen JSON of RPG Mod (plan sections 3 and 4).

Vanilla overrides (data/minecraft):
  * dimension_type/overworld.json                    - taller overworld: min_y -64, height 704 (build limit 640)
  * worldgen/noise_settings/overworld.json            - Tectonic-like noise router: amplified mountains, larger
                                                        continents / climate zones, compact surface rule
  * worldgen/density_function/overworld/depth.json    - amplified mountain offset + gradient stretched to the new ceiling
  * worldgen/density_function/overworld/{continents,erosion}.json - point at the larger rpgmod noises
  * worldgen/structure_set/{mineshafts,...}.json       - vanilla dungeon-like structures disabled (empty sets)

Mod data (data/rpgmod):
  * worldgen/noise/*.json                              - continentalness / erosion / temperature / vegetation at 2x scale
  * worldgen/structure/<dungeon>.json, template_pool/<dungeon>/*.json, structure_set/<dungeon>.json
  * tags/worldgen/biome/has_structure/*.json

Run from the repository root:  python3 tools/gen_worldgen.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
DATA = os.path.join(RES, "data", "rpgmod")
MC = os.path.join(RES, "data", "minecraft")

MIN_Y = -64
HEIGHT = 704  # -64 .. 640
LOGICAL_HEIGHT = 384

DUNGEONS = {
    # id: (level, boss, underground, salt)
    "catacombs_basic": (5, "skeleton_king", True, 1180731),
    "bandit_camp": (6, "bandit_chief", False, 2290412),
    "deep_mines": (7, "mine_overseer", True, 3320517),
    "abandoned_manor": (8, "manor_ghost", False, 4410623),
    "labyrinth_basic": (9, "labyrinth_minotaur", True, 5519731),
    "underground_fortress": (10, "fortress_lord", True, 6602844),
    "ruined_castle": (12, "fallen_king", False, 7714950),
    "arcane_tower": (15, "archmage", False, 8807063),
}
SURFACE_BIOMES = {
    "bandit_camp": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:savanna_plateau",
                    "minecraft:windswept_savanna", "minecraft:sparse_jungle", "minecraft:forest", "minecraft:birch_forest"],
    "abandoned_manor": ["minecraft:dark_forest", "minecraft:forest", "minecraft:swamp", "minecraft:old_growth_birch_forest",
                        "minecraft:flower_forest", "minecraft:taiga"],
    "ruined_castle": ["minecraft:plains", "minecraft:meadow", "minecraft:forest", "minecraft:windswept_hills",
                      "minecraft:windswept_forest", "minecraft:stony_shore", "minecraft:cherry_grove", "minecraft:savanna_plateau"],
    "arcane_tower": ["minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:snowy_taiga",
                     "minecraft:snowy_plains", "minecraft:grove", "minecraft:snowy_slopes", "minecraft:jagged_peaks",
                     "minecraft:frozen_peaks", "minecraft:windswept_hills"],
}
# vanilla structure sets that are replaced by the mod dungeons (plan 3.4); strongholds stay (End access)
DISABLED_VANILLA_SETS = {
    "mineshafts": (1, 0, 0),
    "desert_pyramids": (32, 8, 14357617),
    "jungle_temples": (32, 8, 14357619),
    "swamp_huts": (32, 8, 14357620),
    "ocean_monuments": (32, 5, 10387313),
    "woodland_mansions": (80, 20, 10387319),
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


# =========================================================================================== dimension
def gen_dimension_type():
    write_json(os.path.join(MC, "dimension_type", "overworld.json"), {
        "ultrawarm": False, "natural": True, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": True,
        "has_raids": True, "has_skylight": True, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.0,
        "logical_height": LOGICAL_HEIGHT, "effects": "minecraft:overworld", "infiniburn": "#minecraft:infiniburn_overworld",
        "min_y": MIN_Y, "height": HEIGHT,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
        "monster_spawn_block_light_limit": 0,
    })


# =========================================================================================== density functions
def df_noise(noise, xz, y):
    return {"type": "minecraft:noise", "noise": noise, "xz_scale": xz, "y_scale": y}


def shifted(noise):
    return {"type": "minecraft:shifted_noise", "noise": noise, "xz_scale": 0.25, "y_scale": 0.0,
            "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z"}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def dmin(a, b):
    return {"type": "minecraft:min", "argument1": a, "argument2": b}


def dmax(a, b):
    return {"type": "minecraft:max", "argument1": a, "argument2": b}


def clamp(x, lo, hi):
    """clamp(x) - ``minecraft:clamp`` decodes ``input`` with DensityFunction.DIRECT_CODEC, which only accepts an inline
    function object or a number (no registry references), so references are clamped with min/max instead."""
    if isinstance(x, str):
        return dmin(dmax(x, lo), hi)
    return {"type": "minecraft:clamp", "input": x, "min": lo, "max": hi}


def gradient(from_y, to_y, from_value, to_value):
    return {"type": "minecraft:y_clamped_gradient", "from_y": from_y, "to_y": to_y, "from_value": from_value, "to_value": to_value}


def range_choice(inp, lo, hi, in_range, out_range):
    return {"type": "minecraft:range_choice", "input": inp, "min_inclusive": lo, "max_exclusive": hi,
            "when_in_range": in_range, "when_out_of_range": out_range}


def unary(kind, x):
    return {"type": "minecraft:" + kind, "argument": x}


def slide(density):
    """Vanilla overworld slide with the top slide moved up to the new world ceiling."""
    top_start = MIN_Y + HEIGHT - 80   # 560
    top_end = MIN_Y + HEIGHT - 64     # 576
    top = add(-0.078125, mul(gradient(top_start, top_end, 1.0, 0.0), add(0.078125, density)))
    return add(0.1171875, mul(gradient(MIN_Y, MIN_Y + 24, 0.0, 1.0), add(-0.1171875, top)))


def gen_noises():
    # vanilla: continentalness -9, erosion -9, temperature -10, vegetation -8 (ridge stays vanilla for normal rivers)
    noises = {
        "continentalness": (-10, [1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0]),
        "erosion": (-10, [1.0, 1.0, 0.0, 1.0, 1.0]),
        "temperature": (-11, [1.5, 0.0, 1.0, 0.0, 0.0, 0.0]),
        "vegetation": (-9, [1.0, 1.0, 0.0, 0.0, 0.0, 0.0]),
    }
    for name, (octave, amplitudes) in noises.items():
        write_json(os.path.join(DATA, "worldgen", "noise", name + ".json"), {"firstOctave": octave, "amplitudes": amplitudes})


def gen_density_functions():
    # larger continents / erosion bands: the vanilla offset, factor and jaggedness splines read these two functions
    write_json(os.path.join(MC, "worldgen", "density_function", "overworld", "continents.json"),
               {"type": "minecraft:flat_cache", "argument": shifted("rpgmod:continentalness")})
    write_json(os.path.join(MC, "worldgen", "density_function", "overworld", "erosion.json"),
               {"type": "minecraft:flat_cache", "argument": shifted("rpgmod:erosion")})
    # amplified depth: the offset is scaled up where it is positive (hills -> mountains), lowlands and oceans stay vanilla
    offset = "minecraft:overworld/offset"
    amplified_offset = mul(offset, add(1.0, mul(1.25, clamp(offset, 0.0, 1.0))))
    # same slope as vanilla (3 / 384 per block) but stretched to the new ceiling so tall terrain still closes off
    depth = add(gradient(MIN_Y, MIN_Y + 640, 1.5, -3.5), amplified_offset)
    write_json(os.path.join(MC, "worldgen", "density_function", "overworld", "depth.json"), depth)


# =========================================================================================== noise settings
def block(name, props=None):
    b = {"Name": name}
    if props:
        b["Properties"] = props
    return b


def surface_rule():
    """The vanilla 1.20.1 overworld surface rule, verbatim (tools/vanilla/overworld_surface_rule_1.20.1.json).

    The mod changes the terrain shape (noise router / density functions) but not the surface materials, and the rule
    references vanilla noises by id (``minecraft:surface_swamp`` etc.), so copying it is both simpler and safer than
    rebuilding it by hand."""
    with open(os.path.join(os.path.dirname(__file__), "vanilla", "overworld_surface_rule_1.20.1.json"), encoding="utf-8") as fh:
        return json.load(fh)


def noise_router():
    sloped_cheese = "minecraft:overworld/sloped_cheese"
    entrances = "minecraft:overworld/caves/entrances"
    caves = dmax(
        dmin(
            dmin(
                add(mul(4.0, unary("square", df_noise("minecraft:cave_layer", 1.0, 8.0))),
                    add(clamp(add(0.27, df_noise("minecraft:cave_cheese", 1.0, 0.6666666666666666)), -1.0, 1.0),
                        clamp(add(1.5, mul(-0.64, sloped_cheese)), 0.0, 0.5))),
                entrances),
            add("minecraft:overworld/caves/spaghetti_2d", "minecraft:overworld/caves/spaghetti_roughness_function")),
        range_choice("minecraft:overworld/caves/pillars", -1000000.0, 0.03, -1000000.0, "minecraft:overworld/caves/pillars"))
    terrain = range_choice(sloped_cheese, -1000000.0, 1.5625, dmin(sloped_cheese, mul(5.0, entrances)), caves)
    final_density = dmin(
        unary("squeeze", mul(0.64, unary("interpolated", unary("blend_density", slide(terrain))))),
        "minecraft:overworld/caves/noodle")
    initial = slide(clamp(add(-0.703125, mul(4.0, unary("quarter_negative",
                                                          mul("minecraft:overworld/depth", {"type": "minecraft:cache_2d", "argument": "minecraft:overworld/factor"})))),
                          -64.0, 64.0))
    vein_y = "minecraft:y"
    return {
        "barrier": df_noise("minecraft:aquifer_barrier", 1.0, 0.5),
        "fluid_level_floodedness": df_noise("minecraft:aquifer_fluid_level_floodedness", 1.0, 0.67),
        "fluid_level_spread": df_noise("minecraft:aquifer_fluid_level_spread", 1.0, 0.7142857142857143),
        "lava": df_noise("minecraft:aquifer_lava", 1.0, 1.0),
        "temperature": shifted("rpgmod:temperature"),
        "vegetation": shifted("rpgmod:vegetation"),
        "continents": "minecraft:overworld/continents",
        "erosion": "minecraft:overworld/erosion",
        "depth": "minecraft:overworld/depth",
        "ridges": "minecraft:overworld/ridges",
        "initial_density_without_jaggedness": initial,
        "final_density": final_density,
        "vein_toggle": unary("interpolated", range_choice(vein_y, -60.0, 51.0, df_noise("minecraft:ore_veininess", 1.5, 1.5), 0.0)),
        "vein_ridged": add(-0.07999999821186066, dmax(
            unary("abs", unary("interpolated", range_choice(vein_y, -60.0, 51.0, df_noise("minecraft:ore_vein_a", 4.0, 4.0), 0.0))),
            unary("abs", unary("interpolated", range_choice(vein_y, -60.0, 51.0, df_noise("minecraft:ore_vein_b", 4.0, 4.0), 0.0))))),
        "vein_gap": df_noise("minecraft:ore_gap", 1.0, 1.0),
    }


def gen_noise_settings():
    write_json(os.path.join(MC, "worldgen", "noise_settings", "overworld.json"), {
        "sea_level": 63,
        "disable_mob_generation": False,
        "aquifers_enabled": True,
        "ore_veins_enabled": True,
        "legacy_random_source": False,
        "default_block": block("minecraft:stone"),
        "default_fluid": block("minecraft:water", {"level": "0"}),
        "noise": {"min_y": MIN_Y, "height": HEIGHT, "size_horizontal": 1, "size_vertical": 2},
        "noise_router": noise_router(),
        "spawn_target": [
            {"temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-0.11, 1.0], "erosion": [-1.0, 1.0],
             "weirdness": [-1.0, -0.16], "depth": 0.0, "offset": 0.0},
            {"temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-0.11, 1.0], "erosion": [-1.0, 1.0],
             "weirdness": [0.16, 1.0], "depth": 0.0, "offset": 0.0},
        ],
        "surface_rule": surface_rule(),
    })


# =========================================================================================== dungeons
def pool_element(location, weight=1):
    return {"weight": weight, "element": {"element_type": "minecraft:single_pool_element", "location": location,
                                          "processors": "minecraft:empty", "projection": "rigid"}}


def gen_dungeons():
    for dungeon, (level, boss, underground, salt) in DUNGEONS.items():
        base = f"rpgmod:{dungeon}"
        pools = os.path.join(DATA, "worldgen", "template_pool", dungeon)
        write_json(os.path.join(pools, "start.json"), {"fallback": "minecraft:empty", "elements": [pool_element(f"{base}/start")]})
        write_json(os.path.join(pools, "corridors.json"), {"fallback": f"{base}/ends", "elements": [pool_element(f"{base}/corridor")]})
        write_json(os.path.join(pools, "rooms.json"), {"fallback": f"{base}/ends", "elements": [
            pool_element(f"{base}/room_a", 4), pool_element(f"{base}/room_b", 3), pool_element(f"{base}/treasure", 2), pool_element(f"{base}/corridor", 1)]})
        write_json(os.path.join(pools, "boss.json"), {"fallback": f"{base}/ends", "elements": [pool_element(f"{base}/boss_room")]})
        write_json(os.path.join(pools, "ends.json"), {"fallback": "minecraft:empty", "elements": [pool_element(f"{base}/end")]})

        structure = {
            "type": "rpgmod:dungeon",
            "biomes": f"#rpgmod:has_structure/{dungeon}",
            "step": "underground_structures" if underground else "surface_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "none" if underground else "beard_thin",
            "start_pool": f"{base}/start",
            "size": 7,
            "max_distance_from_center": 96,
            "underground": underground,
            "min_depth_below_surface": 14,
        }
        if underground:
            structure["start_height"] = {"type": "minecraft:uniform", "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 24}}
        else:
            structure["start_height"] = {"absolute": 0}
            structure["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
        write_json(os.path.join(DATA, "worldgen", "structure", dungeon + ".json"), structure)

        write_json(os.path.join(DATA, "worldgen", "structure_set", dungeon + ".json"), {
            "structures": [{"structure": base, "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": 22 if underground else 38,
                          "separation": 9 if underground else 14, "salt": salt},
        })
        biomes = ["#minecraft:is_overworld"] if underground else SURFACE_BIOMES[dungeon]
        write_json(os.path.join(DATA, "tags", "worldgen", "biome", "has_structure", dungeon + ".json"), {"replace": False, "values": biomes})
    write_json(os.path.join(DATA, "tags", "worldgen", "biome", "has_structure", "underground_dungeons.json"),
               {"replace": False, "values": ["#minecraft:is_overworld"]})
    surface = sorted({b for lst in SURFACE_BIOMES.values() for b in lst})
    write_json(os.path.join(DATA, "tags", "worldgen", "biome", "has_structure", "surface_dungeons.json"), {"replace": False, "values": surface})


def gen_disabled_vanilla_sets():
    for name, (spacing, separation, salt) in DISABLED_VANILLA_SETS.items():
        write_json(os.path.join(MC, "worldgen", "structure_set", name + ".json"), {
            "structures": [],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt},
        })


def main():
    gen_dimension_type()
    gen_noises()
    gen_density_functions()
    gen_noise_settings()
    gen_dungeons()
    gen_disabled_vanilla_sets()
    print("worldgen generated")


if __name__ == "__main__":
    main()
