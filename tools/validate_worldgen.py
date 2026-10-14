#!/usr/bin/env python3
"""Validates the worldgen data of RPG Mod against the Minecraft 1.20.1 codecs - without running Minecraft.

Registry loading errors ("Failed to load registries due to above errors") crash the game when a world is created,
so every rule below mirrors a real codec check of 1.20.1:

  * density functions: which arguments accept registry references (HOLDER_HELPER_CODEC) and which do not
    (``minecraft:clamp`` decodes ``input`` with DIRECT_CODEC: inline object or number only), numeric ranges, noise ids
  * noise_settings: NoiseSettings guards (multiples of 16, min_y + height <= 2032), the 15 noise router entries,
    spawn targets, the surface rule grammar and every noise / biome / block it references
  * dimension_type guards, structure (rpgmod:dungeon codec), structure_set (spacing > separation), template pools
  * NBT templates: size <= 48, palette block ids and properties, jigsaw blocks (pool exists, orientation matches the
    wall, final_state parses), mob spawners (entity ids), loot chests (loot tables exist), boss spawners (boss keys)
  * every ``minecraft:`` id used anywhere in data/ (loot tables, recipes, tags, spells) exists in the vanilla registries

Reference data: tools/vanilla/registries_1.20.1.json (extracted from misode/mcmeta, tags 1.20.1-data / 1.20.1-summary).
Run from the repository root:  python3 tools/validate_worldgen.py
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import nbtlite  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
DATA = os.path.join(RES, "data")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "closky", "rpgmod")

with open(os.path.join(os.path.dirname(__file__), "vanilla", "registries_1.20.1.json"), encoding="utf-8") as fh:
    REF = json.load(fh)

problems = []


def problem(where, msg):
    problems.append(f"{where}: {msg}")


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def walk(root, suffix):
    for dirpath, _, files in os.walk(root):
        for f in sorted(files):
            if f.endswith(suffix):
                yield os.path.join(dirpath, f)


def rel(path):
    return os.path.relpath(path, RES).replace(os.sep, "/")


def is_num(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


# ----------------------------------------------------------------------------------------- id universes
def data_ids(namespace, folder):
    base = os.path.join(DATA, namespace, folder)
    out = set()
    if os.path.isdir(base):
        for p in walk(base, ".json"):
            out.add(namespace + ":" + os.path.relpath(p, base)[:-5].replace(os.sep, "/"))
    return out


def nbt_ids(namespace):
    base = os.path.join(DATA, namespace, "structures")
    out = set()
    if os.path.isdir(base):
        for p in walk(base, ".nbt"):
            out.add(namespace + ":" + os.path.relpath(p, base)[:-4].replace(os.sep, "/"))
    return out


def java_registered(file, register_call):
    src = open(os.path.join(JAVA, *file), encoding="utf-8").read()
    return set(re.findall(register_call + r'\("([a-z0-9_/]+)"', src))


MOD_BLOCKS = {"rpgmod:" + b for b in java_registered(("registry", "ModBlocks.java"), r"BLOCKS\.register")}
MOD_BLOCK_PROPS = {
    "rpgmod:loot_chest": {"facing": ["north", "south", "west", "east"], "open": ["true", "false"]},
    "rpgmod:loot_chest_rare": {"facing": ["north", "south", "west", "east"], "open": ["true", "false"]},
    "rpgmod:loot_chest_boss": {"facing": ["north", "south", "west", "east"], "open": ["true", "false"]},
    "rpgmod:boss_spawner": {},
}
MOD_ENTITIES = {"rpgmod:" + e for e in java_registered(("registry", "ModEntities.java"), r"ENTITIES\.register")}
BOSS_KEYS = {m.lower() for m in re.findall(r"^\s+([A-Z_]+)\(\(\) -> EntityType", open(os.path.join(JAVA, "boss", "BossDefinition.java"), encoding="utf-8").read(), re.M)}

VANILLA_WG = {k: {"minecraft:" + i for i in v} for k, v in REF["worldgen"].items()}
NOISES = VANILLA_WG["noise"] | data_ids("rpgmod", "worldgen/noise")
DENSITY_FUNCTIONS = VANILLA_WG["density_function"] | data_ids("rpgmod", "worldgen/density_function")
BIOMES = VANILLA_WG["biome"]
BIOME_TAGS = {"minecraft:" + t for t in REF["tags"]["worldgen/biome"]} | data_ids("rpgmod", "tags/worldgen/biome")
POOLS = VANILLA_WG["template_pool"] | data_ids("rpgmod", "worldgen/template_pool")
PROCESSORS = VANILLA_WG["processor_list"] | data_ids("rpgmod", "worldgen/processor_list")
STRUCTURES = VANILLA_WG["structure"] | data_ids("rpgmod", "worldgen/structure")
TEMPLATES = nbt_ids("rpgmod")
LOOT_TABLES = {"minecraft:" + t for t in REF["loot_table"]} | data_ids("rpgmod", "loot_tables")
BLOCKS = {"minecraft:" + b: props for b, props in REF["block"].items()}
BLOCKS.update(MOD_BLOCK_PROPS)
for b in MOD_BLOCKS:
    BLOCKS.setdefault(b, {})
ENTITIES = {"minecraft:" + e for e in REF["entity_type"]} | MOD_ENTITIES

HEIGHTMAPS = {"WORLD_SURFACE_WG", "WORLD_SURFACE", "OCEAN_FLOOR_WG", "OCEAN_FLOOR", "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES"}
DECORATION_STEPS = {"raw_generation", "lakes", "local_modifications", "underground_structures", "surface_structures", "strongholds",
                    "underground_ores", "underground_decoration", "fluid_springs", "vegetal_decoration", "top_layer_modification"}
TERRAIN_ADAPTATION = {"none", "bury", "beard_thin", "beard_box"}
MOB_CATEGORIES = {"monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient", "misc"}
NOISE_VALUE = 1000000.0


# ----------------------------------------------------------------------------------------- block states
def check_block_state(where, state):
    """BlockState.CODEC: {"Name": id, "Properties": {name: value}}"""
    if not isinstance(state, dict) or "Name" not in state:
        problem(where, f"block state must be an object with Name: {state!r}")
        return
    check_block_and_props(where, state["Name"], state.get("Properties", {}))


def check_block_and_props(where, name, props):
    if name not in BLOCKS:
        problem(where, f"unknown block '{name}'")
        return
    allowed = BLOCKS[name]
    for k, v in props.items():
        if k not in allowed:
            problem(where, f"block '{name}' has no property '{k}'")
        elif str(v) not in allowed[k]:
            problem(where, f"block '{name}' property '{k}' cannot be '{v}' (allowed {allowed[k]})")


def check_block_state_string(where, text):
    """BlockStateParser: ns:name or ns:name[prop=value,...]"""
    m = re.fullmatch(r"([a-z0-9_.-]+:[a-z0-9_./-]+)(?:\[([^\]]*)\])?", text)
    if not m:
        problem(where, f"unparseable block state string '{text}'")
        return
    props = {}
    if m.group(2):
        for pair in m.group(2).split(","):
            if "=" not in pair:
                problem(where, f"bad property '{pair}' in '{text}'")
                continue
            k, v = pair.split("=", 1)
            props[k.strip()] = v.strip()
    check_block_and_props(where, m.group(1), props)


# ----------------------------------------------------------------------------------------- density functions
def check_number(where, v, lo=None, hi=None, integer=False):
    if not is_num(v) or (integer and not float(v).is_integer()):
        problem(where, f"expected a number, got {v!r}")
        return
    if lo is not None and v < lo or hi is not None and v > hi:
        problem(where, f"value {v} out of range [{lo}, {hi}]")


def check_noise_ref(where, v):
    """NormalNoise.NoiseParameters.CODEC: reference or inline {firstOctave, amplitudes}"""
    if isinstance(v, str):
        if v not in NOISES:
            problem(where, f"unknown noise '{v}'")
    elif isinstance(v, dict):
        if not isinstance(v.get("firstOctave"), int) or not isinstance(v.get("amplitudes"), list):
            problem(where, "inline noise needs firstOctave (int) and amplitudes (list)")
    else:
        problem(where, f"bad noise {v!r}")


def check_df_arg(where, v, allow_reference=True):
    """DensityFunction.HOLDER_HELPER_CODEC (number | reference | inline) or DIRECT_CODEC (number | inline)."""
    if is_num(v):
        check_number(where, v, -NOISE_VALUE, NOISE_VALUE)
    elif isinstance(v, str):
        if not allow_reference:
            problem(where, f"a registry reference ('{v}') is not allowed here - this argument is decoded with "
                           "DensityFunction.DIRECT_CODEC and must be an inline function object or a number")
        elif v not in DENSITY_FUNCTIONS:
            problem(where, f"unknown density function '{v}'")
    elif isinstance(v, dict):
        check_density_function(where, v)
    else:
        problem(where, f"bad density function argument {v!r}")


def check_spline(where, v):
    if is_num(v):
        return
    if not isinstance(v, dict):
        problem(where, f"bad spline {v!r}")
        return
    check_df_arg(where + "/coordinate", v.get("coordinate"))
    points = v.get("points")
    if not isinstance(points, list) or not points:
        problem(where, "spline needs a non-empty points list")
        return
    last = None
    for i, p in enumerate(points):
        pw = f"{where}/points[{i}]"
        check_number(pw + "/location", p.get("location"))
        check_number(pw + "/derivative", p.get("derivative"))
        check_spline(pw + "/value", p.get("value"))
        if last is not None and is_num(p.get("location")) and p["location"] <= last:
            problem(pw, "spline point locations must be strictly increasing")
        last = p.get("location") if is_num(p.get("location")) else last


def check_density_function(where, df):
    if not isinstance(df, dict) or "type" not in df:
        problem(where, f"density function must be an object with a type: {df!r}")
        return
    t = df["type"]
    keys = set(df) - {"type"}

    def need(*fields):
        for f in fields:
            if f not in df:
                problem(where, f"{t} is missing field '{f}'")
        extra = keys - set(fields)
        if extra:
            problem(where, f"{t} has unknown fields {sorted(extra)}")

    if t in ("minecraft:blend_alpha", "minecraft:blend_offset", "minecraft:beardifier", "minecraft:end_islands"):
        need()
    elif t == "minecraft:old_blended_noise":
        need("xz_scale", "y_scale", "xz_factor", "y_factor", "smear_scale_multiplier")
        for f in ("xz_scale", "y_scale", "xz_factor", "y_factor"):
            check_number(where + "/" + f, df.get(f), 0.001, 1000.0)
        check_number(where + "/smear_scale_multiplier", df.get("smear_scale_multiplier"), 1.0, 8.0)
    elif t in ("minecraft:interpolated", "minecraft:flat_cache", "minecraft:cache_2d", "minecraft:cache_once",
               "minecraft:cache_all_in_cell", "minecraft:blend_density", "minecraft:abs", "minecraft:square", "minecraft:cube",
               "minecraft:half_negative", "minecraft:quarter_negative", "minecraft:squeeze"):
        need("argument")
        check_df_arg(where + "/argument", df.get("argument"))
    elif t == "minecraft:noise":
        need("noise", "xz_scale", "y_scale")
        check_noise_ref(where + "/noise", df.get("noise"))
        check_number(where + "/xz_scale", df.get("xz_scale"))
        check_number(where + "/y_scale", df.get("y_scale"))
    elif t == "minecraft:weird_scaled_sampler":
        need("input", "noise", "rarity_value_mapper")
        check_df_arg(where + "/input", df.get("input"))
        check_noise_ref(where + "/noise", df.get("noise"))
        if df.get("rarity_value_mapper") not in ("type_1", "type_2"):
            problem(where, "rarity_value_mapper must be type_1 or type_2")
    elif t == "minecraft:shifted_noise":
        need("shift_x", "shift_y", "shift_z", "xz_scale", "y_scale", "noise")
        for f in ("shift_x", "shift_y", "shift_z"):
            check_df_arg(where + "/" + f, df.get(f))
        check_noise_ref(where + "/noise", df.get("noise"))
        check_number(where + "/xz_scale", df.get("xz_scale"))
        check_number(where + "/y_scale", df.get("y_scale"))
    elif t == "minecraft:range_choice":
        need("input", "min_inclusive", "max_exclusive", "when_in_range", "when_out_of_range")
        check_df_arg(where + "/input", df.get("input"))
        check_number(where + "/min_inclusive", df.get("min_inclusive"), -NOISE_VALUE, NOISE_VALUE)
        check_number(where + "/max_exclusive", df.get("max_exclusive"), -NOISE_VALUE, NOISE_VALUE)
        check_df_arg(where + "/when_in_range", df.get("when_in_range"))
        check_df_arg(where + "/when_out_of_range", df.get("when_out_of_range"))
    elif t in ("minecraft:shift_a", "minecraft:shift_b", "minecraft:shift"):
        need("argument")
        check_noise_ref(where + "/argument", df.get("argument"))
    elif t == "minecraft:clamp":
        need("input", "min", "max")
        check_df_arg(where + "/input", df.get("input"), allow_reference=False)
        check_number(where + "/min", df.get("min"), -NOISE_VALUE, NOISE_VALUE)
        check_number(where + "/max", df.get("max"), -NOISE_VALUE, NOISE_VALUE)
    elif t in ("minecraft:add", "minecraft:mul", "minecraft:min", "minecraft:max"):
        need("argument1", "argument2")
        check_df_arg(where + "/argument1", df.get("argument1"))
        check_df_arg(where + "/argument2", df.get("argument2"))
    elif t == "minecraft:spline":
        need("spline")
        check_spline(where + "/spline", df.get("spline"))
    elif t == "minecraft:constant":
        need("argument")
        check_number(where + "/argument", df.get("argument"), -NOISE_VALUE, NOISE_VALUE)
    elif t == "minecraft:y_clamped_gradient":
        need("from_y", "to_y", "from_value", "to_value")
        check_number(where + "/from_y", df.get("from_y"), -4064, 4062, integer=True)
        check_number(where + "/to_y", df.get("to_y"), -4064, 4062, integer=True)
        check_number(where + "/from_value", df.get("from_value"), -NOISE_VALUE, NOISE_VALUE)
        check_number(where + "/to_value", df.get("to_value"), -NOISE_VALUE, NOISE_VALUE)
    else:
        problem(where, f"unknown density function type '{t}'")


# ----------------------------------------------------------------------------------------- surface rules
def check_vertical_anchor(where, v):
    if not isinstance(v, dict) or len(v) != 1 or next(iter(v)) not in ("absolute", "above_bottom", "below_top") \
            or not isinstance(next(iter(v.values())), int):
        problem(where, f"bad vertical anchor {v!r} (expected {{absolute|above_bottom|below_top: int}})")


def check_condition(where, c):
    if not isinstance(c, dict) or "type" not in c:
        problem(where, f"surface condition must be an object with a type: {c!r}")
        return
    t = c["type"]
    if t == "minecraft:biome":
        for b in c.get("biome_is", []):
            if b not in BIOMES:
                problem(where, f"unknown biome '{b}'")
        if not isinstance(c.get("biome_is"), list):
            problem(where, "biome condition needs biome_is list")
    elif t == "minecraft:noise_threshold":
        if c.get("noise") not in NOISES:
            problem(where, f"unknown noise '{c.get('noise')}'")
        check_number(where + "/min_threshold", c.get("min_threshold"))
        check_number(where + "/max_threshold", c.get("max_threshold"))
    elif t == "minecraft:vertical_gradient":
        if not isinstance(c.get("random_name"), str):
            problem(where, "vertical_gradient needs random_name")
        check_vertical_anchor(where + "/true_at_and_below", c.get("true_at_and_below"))
        check_vertical_anchor(where + "/false_at_and_above", c.get("false_at_and_above"))
    elif t == "minecraft:y_above":
        check_vertical_anchor(where + "/anchor", c.get("anchor"))
        check_number(where + "/surface_depth_multiplier", c.get("surface_depth_multiplier"), -20, 20, integer=True)
        if not isinstance(c.get("add_stone_depth"), bool):
            problem(where, "y_above needs add_stone_depth (bool)")
    elif t == "minecraft:water":
        check_number(where + "/offset", c.get("offset"), integer=True)
        check_number(where + "/surface_depth_multiplier", c.get("surface_depth_multiplier"), -20, 20, integer=True)
        if not isinstance(c.get("add_stone_depth"), bool):
            problem(where, "water needs add_stone_depth (bool)")
    elif t == "minecraft:stone_depth":
        check_number(where + "/offset", c.get("offset"), integer=True)
        check_number(where + "/secondary_depth_range", c.get("secondary_depth_range"), integer=True)
        if not isinstance(c.get("add_surface_depth"), bool):
            problem(where, "stone_depth needs add_surface_depth (bool)")
        if c.get("surface_type") not in ("floor", "ceiling"):
            problem(where, "stone_depth surface_type must be floor or ceiling")
    elif t == "minecraft:not":
        check_condition(where + "/invert", c.get("invert"))
    elif t in ("minecraft:temperature", "minecraft:steep", "minecraft:hole", "minecraft:above_preliminary_surface"):
        pass
    else:
        problem(where, f"unknown surface condition type '{t}'")


def check_rule(where, r):
    if not isinstance(r, dict) or "type" not in r:
        problem(where, f"surface rule must be an object with a type: {r!r}")
        return
    t = r["type"]
    if t == "minecraft:block":
        check_block_state(where + "/result_state", r.get("result_state"))
    elif t == "minecraft:sequence":
        seq = r.get("sequence")
        if not isinstance(seq, list):
            problem(where, "sequence needs a list")
            return
        for i, x in enumerate(seq):
            check_rule(f"{where}/sequence[{i}]", x)
    elif t == "minecraft:condition":
        check_condition(where + "/if_true", r.get("if_true"))
        check_rule(where + "/then_run", r.get("then_run"))
    elif t == "minecraft:bandlands":
        pass
    else:
        problem(where, f"unknown surface rule type '{t}'")


# ----------------------------------------------------------------------------------------- noise settings / dimension
def check_noise_settings(path):
    where = rel(path)
    d = load(path)
    required = {"noise", "default_block", "default_fluid", "noise_router", "surface_rule", "spawn_target", "sea_level",
                "disable_mob_generation", "aquifers_enabled", "ore_veins_enabled", "legacy_random_source"}
    for k in required - set(d):
        problem(where, f"missing field '{k}'")
    n = d.get("noise", {})
    min_y, height = n.get("min_y"), n.get("height")
    check_number(where + "/noise/min_y", min_y, -2032, 2031, integer=True)
    check_number(where + "/noise/height", height, 0, 4064, integer=True)
    check_number(where + "/noise/size_horizontal", n.get("size_horizontal"), 1, 4, integer=True)
    check_number(where + "/noise/size_vertical", n.get("size_vertical"), 1, 4, integer=True)
    if isinstance(min_y, int) and isinstance(height, int):
        if min_y + height > 2032:
            problem(where, "noise: min_y + height cannot be higher than 2032")
        if height % 16:
            problem(where, "noise: height has to be a multiple of 16")
        if min_y % 16:
            problem(where, "noise: min_y has to be a multiple of 16")
    check_block_state(where + "/default_block", d.get("default_block"))
    check_block_state(where + "/default_fluid", d.get("default_fluid"))
    router = d.get("noise_router", {})
    router_keys = {"barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation", "continents",
                   "erosion", "depth", "ridges", "initial_density_without_jaggedness", "final_density", "vein_toggle", "vein_ridged",
                   "vein_gap"}
    for k in router_keys - set(router):
        problem(where, f"noise_router is missing '{k}'")
    for k in set(router) - router_keys:
        problem(where, f"noise_router has unknown entry '{k}'")
    for k, v in router.items():
        check_df_arg(f"{where}/noise_router/{k}", v)
    for i, target in enumerate(d.get("spawn_target", [])):
        tw = f"{where}/spawn_target[{i}]"
        for k in ("temperature", "humidity", "continentalness", "erosion", "depth", "weirdness"):
            v = target.get(k)
            if is_num(v):
                check_number(tw + "/" + k, v, -2.0, 2.0)
            elif isinstance(v, list) and len(v) == 2 and all(is_num(x) for x in v):
                if v[0] > v[1]:
                    problem(tw + "/" + k, "min > max")
            else:
                problem(tw + "/" + k, f"bad climate parameter {v!r}")
        check_number(tw + "/offset", target.get("offset"), 0.0, 1.0)
    check_rule(where + "/surface_rule", d.get("surface_rule"))
    check_number(where + "/sea_level", d.get("sea_level"), integer=True)
    return min_y, height


def check_dimension_type(path, noise_bounds):
    where = rel(path)
    d = load(path)
    required = {"ultrawarm", "natural", "coordinate_scale", "has_skylight", "has_ceiling", "ambient_light", "logical_height",
                "infiniburn", "min_y", "height", "piglin_safe", "bed_works", "respawn_anchor_works", "has_raids",
                "monster_spawn_light_level", "monster_spawn_block_light_limit"}
    for k in required - set(d):
        problem(where, f"missing field '{k}'")
    for k in set(d) - required - {"fixed_time", "effects"}:
        problem(where, f"unknown field '{k}'")
    min_y, height, logical = d.get("min_y"), d.get("height"), d.get("logical_height")
    check_number(where + "/min_y", min_y, -2032, 2031, integer=True)
    check_number(where + "/height", height, 16, 4064, integer=True)
    check_number(where + "/logical_height", logical, 0, 4064, integer=True)
    check_number(where + "/coordinate_scale", d.get("coordinate_scale"), 1.0e-5, 3.0e7)
    check_number(where + "/monster_spawn_block_light_limit", d.get("monster_spawn_block_light_limit"), 0, 15, integer=True)
    if all(isinstance(x, int) for x in (min_y, height, logical)):
        if min_y + height > 2032:
            problem(where, "min_y + height cannot be higher than 2032")
        if logical > height:
            problem(where, "logical_height cannot be higher than height")
        if height % 16:
            problem(where, "height has to be a multiple of 16")
        if min_y % 16:
            problem(where, "min_y has to be a multiple of 16")
        if noise_bounds and (noise_bounds[0] != min_y or noise_bounds[1] != height):
            problem(where, f"dimension height {min_y}..{min_y + height} differs from noise_settings {noise_bounds[0]}..{noise_bounds[0] + noise_bounds[1]}")
    light = d.get("monster_spawn_light_level")
    if isinstance(light, dict):
        if light.get("type") != "minecraft:uniform" or not isinstance(light.get("value"), dict):
            problem(where, "monster_spawn_light_level: expected {type: minecraft:uniform, value: {min_inclusive, max_inclusive}}")
    elif not isinstance(light, int):
        problem(where, "monster_spawn_light_level must be an int or an int provider")
    if isinstance(d.get("infiniburn"), str) and not d["infiniburn"].startswith("#"):
        problem(where, "infiniburn must be a block tag ('#ns:name')")


# ----------------------------------------------------------------------------------------- structures
def check_height_provider(where, v):
    if isinstance(v, dict) and len(v) == 1 and next(iter(v)) in ("absolute", "above_bottom", "below_top"):
        check_vertical_anchor(where, v)
        return
    if not isinstance(v, dict) or "type" not in v:
        problem(where, f"bad height provider {v!r}")
        return
    t = v["type"]
    if t in ("minecraft:uniform", "minecraft:biased_to_bottom", "minecraft:very_biased_to_bottom", "minecraft:trapezoid"):
        check_vertical_anchor(where + "/min_inclusive", v.get("min_inclusive"))
        check_vertical_anchor(where + "/max_inclusive", v.get("max_inclusive"))
    elif t == "minecraft:weighted_list":
        pass
    else:
        problem(where, f"unknown height provider type '{t}'")


def check_biome_holder_set(where, v):
    if isinstance(v, str):
        if v.startswith("#"):
            if v[1:] not in BIOME_TAGS:
                problem(where, f"unknown biome tag '{v}'")
        elif v not in BIOMES:
            problem(where, f"unknown biome '{v}'")
    elif isinstance(v, list):
        for b in v:
            check_biome_holder_set(where, b)
    else:
        problem(where, f"bad biome set {v!r}")


def check_structure(path):
    where = rel(path)
    d = load(path)
    if d.get("type") != "rpgmod:dungeon":
        problem(where, f"unexpected structure type {d.get('type')}")
        return
    required = {"type", "biomes", "step", "spawn_overrides", "start_pool", "size", "start_height", "max_distance_from_center"}
    optional = {"terrain_adaptation", "start_jigsaw_name", "project_start_to_heightmap", "underground", "min_depth_below_surface"}
    for k in required - set(d):
        problem(where, f"missing field '{k}'")
    for k in set(d) - required - optional:
        problem(where, f"unknown field '{k}' (not in DungeonStructure.CODEC)")
    check_biome_holder_set(where + "/biomes", d.get("biomes"))
    if d.get("step") not in DECORATION_STEPS:
        problem(where, f"bad step '{d.get('step')}'")
    if d.get("terrain_adaptation", "none") not in TERRAIN_ADAPTATION:
        problem(where, f"bad terrain_adaptation '{d.get('terrain_adaptation')}'")
    for cat in d.get("spawn_overrides", {}):
        if cat not in MOB_CATEGORIES:
            problem(where, f"bad spawn_overrides category '{cat}'")
    if d.get("start_pool") not in POOLS:
        problem(where, f"unknown start_pool '{d.get('start_pool')}'")
    check_number(where + "/size", d.get("size"), 0, 30, integer=True)
    check_number(where + "/max_distance_from_center", d.get("max_distance_from_center"), 1, 128, integer=True)
    check_height_provider(where + "/start_height", d.get("start_height"))
    if "project_start_to_heightmap" in d and d["project_start_to_heightmap"] not in HEIGHTMAPS:
        problem(where, f"bad heightmap '{d['project_start_to_heightmap']}'")
    if "underground" in d and not isinstance(d["underground"], bool):
        problem(where, "underground must be a boolean")
    if "min_depth_below_surface" in d:
        check_number(where + "/min_depth_below_surface", d["min_depth_below_surface"], 0, 64, integer=True)


def check_structure_set(path):
    where = rel(path)
    d = load(path)
    for i, e in enumerate(d.get("structures", [])):
        if e.get("structure") not in STRUCTURES:
            problem(f"{where}/structures[{i}]", f"unknown structure '{e.get('structure')}'")
        check_number(f"{where}/structures[{i}]/weight", e.get("weight"), 1, None, integer=True)
    p = d.get("placement", {})
    t = p.get("type")
    check_number(where + "/placement/salt", p.get("salt"), 0, None, integer=True)
    if "frequency" in p:
        check_number(where + "/placement/frequency", p["frequency"], 0.0, 1.0)
    if t == "minecraft:random_spread":
        check_number(where + "/placement/spacing", p.get("spacing"), 0, 4096, integer=True)
        check_number(where + "/placement/separation", p.get("separation"), 0, 4096, integer=True)
        if isinstance(p.get("spacing"), int) and isinstance(p.get("separation"), int) and p["spacing"] <= p["separation"]:
            problem(where, "placement: spacing has to be larger than separation")
        if p.get("spread_type", "linear") not in ("linear", "triangular"):
            problem(where, f"bad spread_type '{p.get('spread_type')}'")
    elif t == "minecraft:concentric_rings":
        check_number(where + "/placement/distance", p.get("distance"), 0, 1023, integer=True)
        check_number(where + "/placement/spread", p.get("spread"), 0, 1023, integer=True)
        check_number(where + "/placement/count", p.get("count"), 1, 4095, integer=True)
    else:
        problem(where, f"unknown placement type '{t}'")


def check_template_pool(path):
    where = rel(path)
    d = load(path)
    if d.get("fallback") not in POOLS:
        problem(where, f"unknown fallback pool '{d.get('fallback')}'")
    for k in set(d) - {"fallback", "elements"}:
        problem(where, f"unknown field '{k}' (1.20.1 pools have only fallback + elements)")
    for i, e in enumerate(d.get("elements", [])):
        ew = f"{where}/elements[{i}]"
        check_number(ew + "/weight", e.get("weight"), 1, 150, integer=True)
        el = e.get("element", {})
        et = el.get("element_type")
        if et in ("minecraft:single_pool_element", "minecraft:legacy_single_pool_element"):
            if el.get("location") not in TEMPLATES:
                problem(ew, f"template '{el.get('location')}' has no NBT file under data/<ns>/structures")
            proc = el.get("processors")
            if isinstance(proc, str):
                if proc not in PROCESSORS:
                    problem(ew, f"unknown processor list '{proc}'")
            elif not isinstance(proc, list):
                problem(ew, "processors must be a reference or an inline list")
            if el.get("projection") not in ("rigid", "terrain_matching"):
                problem(ew, f"bad projection '{el.get('projection')}'")
        elif et == "minecraft:empty_pool_element":
            pass
        else:
            problem(ew, f"unsupported element_type '{et}'")


def check_biome_tag(path):
    where = rel(path)
    d = load(path)
    for v in d.get("values", []):
        vid = v["id"] if isinstance(v, dict) else v
        check_biome_holder_set(where, vid)


# ----------------------------------------------------------------------------------------- NBT templates
ORIENTATION_FOR_WALL = {"north": "north_up", "south": "south_up", "west": "west_up", "east": "east_up"}


def check_template(path):
    where = rel(path)
    try:
        root = nbtlite.read_gzip(path)
    except Exception as e:  # noqa: BLE001
        problem(where, f"cannot read NBT: {e}")
        return
    for k in ("DataVersion", "size", "palette", "blocks", "entities"):
        if k not in root:
            problem(where, f"missing root tag '{k}'")
    size = root.get("size", [0, 0, 0])
    if len(size) != 3 or any(not 1 <= s <= 48 for s in size):
        problem(where, f"template size {size} must be 1..48 on every axis")
    palette = root.get("palette", [])
    for i, entry in enumerate(palette):
        check_block_state(f"{where}/palette[{i}]", entry)
    jigsaws = 0
    for i, b in enumerate(root.get("blocks", [])):
        bw = f"{where}/blocks[{i}]"
        pos = b.get("pos", [])
        if len(pos) != 3 or any(not 0 <= p < s for p, s in zip(pos, size)):
            problem(bw, f"position {pos} outside the template size {size}")
        state = b.get("state", -1)
        if not 0 <= state < len(palette):
            problem(bw, f"palette index {state} out of range")
            continue
        name = palette[state].get("Name")
        nbt = b.get("nbt")
        if name == "minecraft:jigsaw":
            jigsaws += 1
            if not nbt:
                problem(bw, "jigsaw block without block entity NBT")
                continue
            if nbt.get("id") != "minecraft:jigsaw":
                problem(bw, f"jigsaw NBT id must be minecraft:jigsaw, got {nbt.get('id')}")
            for k in ("name", "target", "pool", "final_state", "joint"):
                if k not in nbt:
                    problem(bw, f"jigsaw NBT is missing '{k}'")
            if nbt.get("pool") not in POOLS:
                problem(bw, f"jigsaw pool '{nbt.get('pool')}' does not exist")
            if nbt.get("joint") not in ("rollable", "aligned"):
                problem(bw, f"jigsaw joint must be rollable or aligned, got {nbt.get('joint')}")
            if "final_state" in nbt:
                check_block_state_string(bw + "/final_state", nbt["final_state"])
            orientation = palette[state].get("Properties", {}).get("orientation")
            x, y, z = pos
            walls = [w for w, on in (("north", z == 0), ("south", z == size[2] - 1), ("west", x == 0), ("east", x == size[0] - 1)) if on]
            if walls and orientation not in {ORIENTATION_FOR_WALL[w] for w in walls}:
                problem(bw, f"jigsaw on the {'/'.join(walls)} wall should face {[ORIENTATION_FOR_WALL[w] for w in walls]}, has {orientation}")
            if not walls and orientation not in ("up_north", "up_south", "up_east", "up_west", "down_north", "down_south", "down_east", "down_west"):
                problem(bw, f"jigsaw at {pos} is not on a wall of the template")
        elif name == "minecraft:spawner":
            if not nbt or nbt.get("id") != "minecraft:mob_spawner":
                problem(bw, "spawner needs NBT with id minecraft:mob_spawner")
                continue
            entity = nbt.get("SpawnData", {}).get("entity", {}).get("id")
            if entity not in ENTITIES:
                problem(bw, f"spawner entity '{entity}' does not exist")
            for j, pot in enumerate(nbt.get("SpawnPotentials", [])):
                eid = pot.get("data", {}).get("entity", {}).get("id")
                if eid not in ENTITIES:
                    problem(bw, f"SpawnPotentials[{j}] entity '{eid}' does not exist")
        elif name in ("rpgmod:loot_chest", "rpgmod:loot_chest_rare", "rpgmod:loot_chest_boss"):
            if not nbt or nbt.get("id") != "rpgmod:loot_chest":
                problem(bw, "loot chest needs NBT with id rpgmod:loot_chest")
                continue
            if nbt.get("LootTable") not in LOOT_TABLES:
                problem(bw, f"loot table '{nbt.get('LootTable')}' does not exist")
        elif name == "rpgmod:boss_spawner":
            if not nbt or nbt.get("id") != "rpgmod:boss_spawner":
                problem(bw, "boss spawner needs NBT with id rpgmod:boss_spawner")
                continue
            if nbt.get("Boss") not in BOSS_KEYS:
                problem(bw, f"boss '{nbt.get('Boss')}' is not a BossDefinition key {sorted(BOSS_KEYS)}")
        elif nbt is not None and isinstance(nbt, dict) and "id" in nbt and nbt["id"] not in {"minecraft:" + b for b in REF["block_entity_type"]}:
            problem(bw, f"block entity id '{nbt['id']}' does not exist")
    if jigsaws == 0 and not path.endswith(os.sep + "end.nbt"):
        problem(where, "template has no jigsaw blocks - it can never connect to anything")
    return root


def check_pool_connectivity():
    """Every jigsaw target must have a matching jigsaw name in the templates of the pool it points to."""
    pool_elements = {}
    for path in walk(os.path.join(DATA, "rpgmod", "worldgen", "template_pool"), ".json"):
        pid = "rpgmod:" + os.path.relpath(path, os.path.join(DATA, "rpgmod", "worldgen", "template_pool"))[:-5].replace(os.sep, "/")
        pool_elements[pid] = [e["element"].get("location") for e in load(path).get("elements", [])]
    names_by_template = {}
    for path in walk(os.path.join(DATA, "rpgmod", "structures"), ".nbt"):
        tid = "rpgmod:" + os.path.relpath(path, os.path.join(DATA, "rpgmod", "structures"))[:-4].replace(os.sep, "/")
        root = nbtlite.read_gzip(path)
        palette = root["palette"]
        names = set()
        targets = []
        for b in root["blocks"]:
            if palette[b["state"]]["Name"] == "minecraft:jigsaw" and b.get("nbt"):
                names.add(b["nbt"].get("name"))
                targets.append((b["nbt"].get("pool"), b["nbt"].get("target")))
        names_by_template[tid] = (names, targets)
    for tid, (_, targets) in names_by_template.items():
        for pool, target in targets:
            if pool not in pool_elements:
                continue  # vanilla pool, checked elsewhere
            if not any(target in names_by_template.get(loc, (set(), []))[0] for loc in pool_elements[pool]):
                problem(tid, f"jigsaw target '{target}' has no matching jigsaw name in any template of pool '{pool}'")


# ----------------------------------------------------------------------------------------- generic vanilla id check
ID_FIELDS = {
    "item": {"minecraft:" + i for i in REF["item"]},
    "block": set(BLOCKS),
    "entity_type": ENTITIES,
    "mob_effect": {"minecraft:" + e for e in REF["mob_effect"]},
    "enchantment": {"minecraft:" + e for e in REF["enchantment"]},
    "attribute": {"minecraft:" + a for a in REF["attribute"]},
    "potion": {"minecraft:" + p for p in REF["potion"]},
    "sound_event": {"minecraft:" + s for s in REF["sound_event"]},
    "loot_table": LOOT_TABLES,
    "damage_type": {"minecraft:" + d for d in REF["damage_type"]},
}
TAG_FILES = {"blocks": "block", "items": "item", "entity_types": "entity_type", "damage_type": "damage_type"}


def check_vanilla_ids():
    """Loot tables, recipes and tags: every minecraft: id must exist (mod ids are checked by validate_data.py)."""
    mod_items = {"rpgmod:" + i for i in java_registered(("registry", "ModItems.java"), r"ITEMS\.register")}
    vanilla_tags = {k: {"minecraft:" + t for t in v} for k, v in REF["tags"].items()}

    def check_id(where, kind, value):
        if not isinstance(value, str) or ":" not in value:
            return
        if value.startswith("#"):
            name = value[1:]
            if name.startswith("minecraft:"):
                folder = {"item": "items", "block": "blocks", "entity_type": "entity_types"}.get(kind)
                if folder and name not in vanilla_tags[folder]:
                    problem(where, f"unknown vanilla {kind} tag '{value}'")
            return
        if value.startswith("minecraft:") and value not in ID_FIELDS[kind]:
            problem(where, f"unknown vanilla {kind} '{value}'")

    def scan(where, obj):
        if isinstance(obj, dict):
            t = obj.get("type", "")
            if t == "minecraft:item" and "name" in obj:
                check_id(where, "item", obj["name"])
            if t == "minecraft:loot_table" and "name" in obj:
                check_id(where, "loot_table", obj["name"])
            if "item" in obj and isinstance(obj["item"], str):
                check_id(where, "item", obj["item"])
            if "tag" in obj and isinstance(obj["tag"], str) and "function" not in obj:
                check_id(where, "item", "#" + obj["tag"])
            if t in ("minecraft:set_potion",) and "id" in obj:
                check_id(where, "potion", obj["id"])
            if "enchantment" in obj and isinstance(obj["enchantment"], str):
                check_id(where, "enchantment", obj["enchantment"])
            if "effect" in obj and isinstance(obj["effect"], str):
                check_id(where, "mob_effect", obj["effect"])
            for k, v in obj.items():
                scan(where + "/" + k, v)
        elif isinstance(obj, list):
            for i, v in enumerate(obj):
                scan(f"{where}[{i}]", v)

    for ns in sorted(os.listdir(DATA)):
        for folder in ("loot_tables", "recipes", "spells"):
            base = os.path.join(DATA, ns, folder)
            if os.path.isdir(base):
                for path in walk(base, ".json"):
                    scan(rel(path), load(path))
                    d = load(path)
                    if folder == "recipes" and isinstance(d.get("result"), dict):
                        check_id(rel(path), "item", d["result"].get("item"))
                    if folder == "recipes" and isinstance(d.get("result"), str):
                        check_id(rel(path), "item", d["result"])
        tags_base = os.path.join(DATA, ns, "tags")
        for folder, kind in TAG_FILES.items():
            base = os.path.join(tags_base, folder)
            if os.path.isdir(base):
                for path in walk(base, ".json"):
                    for v in load(path).get("values", []):
                        vid = v["id"] if isinstance(v, dict) else v
                        if vid.startswith("#"):
                            if vid[1:].startswith("minecraft:") and vid[1:] not in vanilla_tags[folder]:
                                problem(rel(path), f"unknown vanilla tag '{vid}'")
                        else:
                            check_id(rel(path), kind, vid)
    # mod ids referenced by vanilla-namespace data (recipe overrides) must be registered
    for path in walk(os.path.join(DATA, "minecraft", "recipes"), ".json"):
        text = open(path, encoding="utf-8").read()
        for m in re.findall(r'"rpgmod:([a-z0-9_/]+)"', text):
            if "rpgmod:" + m not in mod_items and not m.startswith("config"):
                problem(rel(path), f"unregistered mod item 'rpgmod:{m}'")


def check_biome_modifiers():
    placed = VANILLA_WG["placed_feature"] | data_ids("rpgmod", "worldgen/placed_feature")
    for path in walk(os.path.join(DATA, "rpgmod", "forge", "biome_modifier"), ".json"):
        where = rel(path)
        d = load(path)
        if d.get("type") != "forge:remove_features":
            continue
        check_biome_holder_set(where + "/biomes", d.get("biomes"))
        feats = d.get("features", [])
        for f in feats if isinstance(feats, list) else [feats]:
            if f not in placed:
                problem(where, f"unknown placed feature '{f}'")
        for s in d.get("steps", []):
            if s not in DECORATION_STEPS:
                problem(where, f"bad step '{s}'")


def main():
    bounds = None
    for path in walk(os.path.join(DATA, "minecraft", "worldgen", "density_function"), ".json"):
        check_density_function(rel(path), load(path))
    for path in walk(os.path.join(DATA, "rpgmod", "worldgen", "density_function"), ".json"):
        check_density_function(rel(path), load(path))
    for path in walk(os.path.join(DATA, "minecraft", "worldgen", "noise_settings"), ".json"):
        bounds = check_noise_settings(path)
    for path in walk(os.path.join(DATA, "minecraft", "dimension_type"), ".json"):
        check_dimension_type(path, bounds)
    for path in walk(os.path.join(DATA, "rpgmod", "worldgen", "structure"), ".json"):
        check_structure(path)
    for ns in ("rpgmod", "minecraft"):
        for path in walk(os.path.join(DATA, ns, "worldgen", "structure_set"), ".json"):
            check_structure_set(path)
    for path in walk(os.path.join(DATA, "rpgmod", "worldgen", "template_pool"), ".json"):
        check_template_pool(path)
    for path in walk(os.path.join(DATA, "rpgmod", "tags", "worldgen", "biome"), ".json"):
        check_biome_tag(path)
    templates = 0
    for path in walk(os.path.join(DATA, "rpgmod", "structures"), ".nbt"):
        check_template(path)
        templates += 1
    check_pool_connectivity()
    check_biome_modifiers()
    check_vanilla_ids()
    for p in problems:
        print("PROBLEM", p)
    print(f"worldgen: {len(DENSITY_FUNCTIONS - VANILLA_WG['density_function']) + 3} density functions, {templates} templates, "
          f"{len(POOLS - VANILLA_WG['template_pool'])} pools, {len(STRUCTURES - VANILLA_WG['structure'])} structures checked")
    if problems:
        print(f"{len(problems)} problem(s)")
        sys.exit(1)
    print("worldgen OK")


if __name__ == "__main__":
    main()
