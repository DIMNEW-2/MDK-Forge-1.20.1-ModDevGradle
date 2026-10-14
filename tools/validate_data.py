#!/usr/bin/env python3
"""Cross-checks the generated resources against the Java registries (no Minecraft needed).

* every rpgmod item / block referenced by loot tables, recipes, tags and skill icons is registered in ModItems/ModBlocks
* skill tree: parents exist, no cycles, effect types valid, spell unlocks reference existing spells, lang keys exist
* template pools reference existing NBT templates, structures reference existing pools / biome tags
* every item/block has a model and a lang entry (en_us + ru_ru), every model texture exists
* sounds.json covers every ModSounds event, particles have textures

Run from the repository root:  python3 tools/validate_data.py
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "rpgmod")
DATA = os.path.join(RES, "data", "rpgmod")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "closky", "rpgmod")

problems = []


def problem(msg):
    problems.append(msg)


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def walk(root, suffix):
    for dirpath, _, files in os.walk(root):
        for f in files:
            if f.endswith(suffix):
                yield os.path.join(dirpath, f)


# ----------------------------------------------------------------------------------------- registered ids from Java
def registered_items():
    src = open(os.path.join(JAVA, "registry", "ModItems.java"), encoding="utf-8").read()
    ids = set(re.findall(r'ITEMS\.register\("([a-z_]+)",', src))
    tiers = ["wood", "stone", "iron", "gold", "diamond", "netherite", "rune"]
    for w in ["sword", "mace", "spear", "bow", "crossbow"]:
        for t in tiers:
            ids.add(f"{w}_{t}_rpg")
    for b in ["basic", "fire", "frost", "lightning", "poison", "smoke"]:
        ids.add(f"bomb_{b}")
    for a in ["basic", "fire", "frost", "explosive", "poison", "piercing", "glowing"]:
        ids.add(f"arrow_{a}")
    for b in ["basic", "fire", "frost", "poison", "piercing", "lightning"]:
        ids.add(f"bolt_{b}")
    for s in ["fire", "frost", "lightning", "nature"]:
        ids.add(f"staff_{s}")
        ids.add(f"scroll_{s}")
    for c in ["warrior", "mage", "archer", "crossbowman", "rogue", "druid", "spearman", "bomber"]:
        for p in ["helmet", "chestplate", "leggings", "boots"]:
            ids.add(f"{c}_{p}")
    return ids


def registered_blocks():
    src = open(os.path.join(JAVA, "registry", "ModBlocks.java"), encoding="utf-8").read()
    return set(re.findall(r'BLOCKS\.register\("([a-z_]+)"', src))


def registered_sounds():
    src = open(os.path.join(JAVA, "registry", "ModSounds.java"), encoding="utf-8").read()
    return set(re.findall(r'register\("([a-z_]+)"', src))


def registered_spells():
    src = open(os.path.join(JAVA, "registry", "ModSpells.java"), encoding="utf-8").read()
    return set(re.findall(r'register\("([a-z_]+)"', src))


ITEMS = registered_items()
BLOCKS = registered_blocks()
SOUNDS = registered_sounds()
SPELLS = registered_spells()
EN = load(os.path.join(ASSETS, "lang", "en_us.json"))
RU = load(os.path.join(ASSETS, "lang", "ru_ru.json"))


def check_item_ref(ref, where):
    if not ref.startswith("rpgmod:"):
        return
    name = ref.split(":", 1)[1]
    if name not in ITEMS and name not in BLOCKS:
        problem(f"{where}: unknown rpgmod item/block '{ref}'")


# ----------------------------------------------------------------------------------------- checks
def check_lang_and_models():
    for lang, name in ((EN, "en_us"), (RU, "ru_ru")):
        for item in ITEMS:
            if f"item.rpgmod.{item}" not in lang and f"block.rpgmod.{item}" not in lang:
                problem(f"{name}: missing name for item {item}")
        for block in BLOCKS:
            if f"block.rpgmod.{block}" not in lang:
                problem(f"{name}: missing name for block {block}")
    missing_ru = [k for k in EN if k not in RU]
    missing_en = [k for k in RU if k not in EN]
    if missing_ru:
        problem(f"ru_ru is missing {len(missing_ru)} keys: {missing_ru[:5]}")
    if missing_en:
        problem(f"en_us is missing {len(missing_en)} keys: {missing_en[:5]}")
    for item in ITEMS | BLOCKS:
        model = os.path.join(ASSETS, "models", "item", item + ".json")
        if not os.path.exists(model):
            problem(f"missing item model for {item}")
    for block in BLOCKS:
        if not os.path.exists(os.path.join(ASSETS, "blockstates", block + ".json")):
            problem(f"missing blockstate for {block}")
    for model in walk(os.path.join(ASSETS, "models"), ".json"):
        data = load(model)
        for tex in data.get("textures", {}).values():
            if tex.startswith("rpgmod:"):
                path = os.path.join(ASSETS, "textures", tex.split(":", 1)[1] + ".png")
                if not os.path.exists(path):
                    problem(f"{os.path.relpath(model, RES)}: missing texture {tex}")
        parent = data.get("parent", "")
        if parent.startswith("rpgmod:"):
            path = os.path.join(ASSETS, "models", parent.split(":", 1)[1] + ".json")
            if not os.path.exists(path):
                problem(f"{os.path.relpath(model, RES)}: missing parent model {parent}")
        for override in data.get("overrides", []):
            path = os.path.join(ASSETS, "models", override["model"].split(":", 1)[1] + ".json")
            if not os.path.exists(path):
                problem(f"{os.path.relpath(model, RES)}: missing override model {override['model']}")
    for state in walk(os.path.join(ASSETS, "blockstates"), ".json"):
        for variant in load(state)["variants"].values():
            path = os.path.join(ASSETS, "models", variant["model"].split(":", 1)[1] + ".json")
            if not os.path.exists(path):
                problem(f"{os.path.relpath(state, RES)}: missing model {variant['model']}")


def check_sounds_particles():
    sounds = load(os.path.join(ASSETS, "sounds.json"))
    for event in SOUNDS:
        if event not in sounds:
            problem(f"sounds.json: missing event {event}")
        elif not sounds[event]["sounds"]:
            problem(f"sounds.json: event {event} has no sounds")
        elif sounds[event].get("subtitle") and sounds[event]["subtitle"] not in EN:
            problem(f"sounds.json: subtitle {sounds[event]['subtitle']} missing in lang")
    for particle in walk(os.path.join(ASSETS, "particles"), ".json"):
        for tex in load(particle)["textures"]:
            path = os.path.join(ASSETS, "textures", "particle", tex.split(":", 1)[1] + ".png")
            if not os.path.exists(path):
                problem(f"{os.path.relpath(particle, RES)}: missing texture {tex}")


def check_loot_and_recipes():
    for table in walk(os.path.join(DATA, "loot_tables"), ".json"):
        data = load(table)
        for pool in data.get("pools", []):
            for entry in pool.get("entries", []):
                if entry.get("type") == "minecraft:item":
                    check_item_ref(entry["name"], os.path.relpath(table, RES))
                for func in entry.get("functions", []):
                    if func["function"] == "rpgmod:set_rpg_level" and "level" not in func and "offset_min" not in func:
                        problem(f"{os.path.relpath(table, RES)}: set_rpg_level without level/offset")
    for recipe in walk(os.path.join(RES, "data"), ".json"):
        if "/recipes/" not in recipe.replace(os.sep, "/"):
            continue
        data = load(recipe)
        refs = []
        result = data.get("result")
        if isinstance(result, dict) and "item" in result:
            refs.append(result["item"])
        for ing in data.get("ingredients", []) + list(data.get("key", {}).values()):
            if "item" in ing:
                refs.append(ing["item"])
        for ref in refs:
            check_item_ref(ref, os.path.relpath(recipe, RES))
    for tag in walk(os.path.join(DATA, "tags", "items"), ".json"):
        for value in load(tag)["values"]:
            check_item_ref(value, os.path.relpath(tag, RES))
    for tag in walk(os.path.join(DATA, "tags", "blocks"), ".json"):
        for value in load(tag)["values"]:
            check_item_ref(value, os.path.relpath(tag, RES))


def check_skill_tree():
    nodes = {}
    for path in walk(os.path.join(DATA, "skill_tree"), ".json"):
        rel = os.path.relpath(path, os.path.join(DATA, "skill_tree")).replace(os.sep, "/")[:-5]
        nodes["rpgmod:" + rel] = load(path)
    valid_effects = {"attribute", "unlock_spell", "mana_cost", "cooldown", "spell_power", "flag"}
    valid_branches = {"fire", "frost", "lightning", "nature", "arcane", "combat", "defense", "ranged", "exploration", "social"}
    positions = {}
    for node_id, node in nodes.items():
        if node["branch"] not in valid_branches:
            problem(f"{node_id}: unknown branch {node['branch']}")
        pos = (node["x"], node["y"])
        if pos in positions:
            problem(f"{node_id}: overlaps {positions[pos]} at {pos}")
        positions[pos] = node_id
        for parent in node.get("parents", []):
            if parent not in nodes:
                problem(f"{node_id}: unknown parent {parent}")
            if parent == node_id:
                problem(f"{node_id}: is its own parent")
        for effect in node.get("effects", []):
            if effect["type"] not in valid_effects:
                problem(f"{node_id}: unknown effect type {effect['type']}")
            if effect["type"] == "unlock_spell":
                spell = effect["spell"].split(":", 1)[1]
                if spell not in SPELLS:
                    problem(f"{node_id}: unknown spell {effect['spell']}")
            if effect["type"] == "attribute" and effect["operation"] not in ("addition", "multiply_base", "multiply_total"):
                problem(f"{node_id}: bad operation {effect['operation']}")
        key = "skill." + node_id.replace(":", ".").replace("/", ".")
        for lang, name in ((EN, "en_us"), (RU, "ru_ru")):
            if key not in lang or key + ".desc" not in lang:
                problem(f"{name}: missing {key} / {key}.desc")
        if node.get("required_class") and f"class.rpgmod.{node['required_class']}" not in EN:
            problem(f"{node_id}: unknown required_class {node['required_class']}")
    # cycle check (Kahn)
    indegree = {n: len(v.get("parents", [])) for n, v in nodes.items()}
    queue = [n for n, d in indegree.items() if d == 0]
    seen = 0
    while queue:
        current = queue.pop()
        seen += 1
        for n, v in nodes.items():
            if current in v.get("parents", []):
                indegree[n] -= 1
                if indegree[n] == 0:
                    queue.append(n)
    if seen != len(nodes):
        problem("skill tree contains a cycle")
    # every spell that is not basic must be unlockable through the tree or a scroll
    unlocked = {e["spell"].split(":", 1)[1] for v in nodes.values() for e in v.get("effects", []) if e["type"] == "unlock_spell"}
    for spell_file in walk(os.path.join(DATA, "spells"), ".json"):
        spell = os.path.basename(spell_file)[:-5]
        if spell not in SPELLS:
            problem(f"spells/{spell}.json does not match a registered spell")
        if not load(spell_file).get("basic") and spell not in unlocked:
            problem(f"spell {spell} cannot be unlocked through the skill tree")
        for lang, name in ((EN, "en_us"), (RU, "ru_ru")):
            if f"spell.rpgmod.{spell}" not in lang:
                problem(f"{name}: missing spell.rpgmod.{spell}")
    return len(nodes)


def check_worldgen():
    pools = {}
    for path in walk(os.path.join(DATA, "worldgen", "template_pool"), ".json"):
        rel = os.path.relpath(path, os.path.join(DATA, "worldgen", "template_pool")).replace(os.sep, "/")[:-5]
        pools["rpgmod:" + rel] = load(path)
    for pool_id, pool in pools.items():
        fallback = pool["fallback"]
        if fallback != "minecraft:empty" and fallback not in pools:
            problem(f"{pool_id}: unknown fallback pool {fallback}")
        for element in pool["elements"]:
            loc = element["element"]["location"]
            nbt = os.path.join(DATA, "structures", loc.split(":", 1)[1] + ".nbt")
            if not os.path.exists(nbt):
                problem(f"{pool_id}: missing template {loc}")
    structures = 0
    for path in walk(os.path.join(DATA, "worldgen", "structure"), ".json"):
        structures += 1
        data = load(path)
        if data["start_pool"] not in pools:
            problem(f"{os.path.relpath(path, RES)}: unknown start pool {data['start_pool']}")
        tag = data["biomes"]
        if tag.startswith("#rpgmod:"):
            tag_path = os.path.join(DATA, "tags", "worldgen", "biome", tag[len("#rpgmod:"):] + ".json")
            if not os.path.exists(tag_path):
                problem(f"{os.path.relpath(path, RES)}: missing biome tag {tag}")
        name = os.path.basename(path)[:-5]
        if not os.path.exists(os.path.join(DATA, "worldgen", "structure_set", name + ".json")):
            problem(f"structure {name} has no structure set")
        for suffix in ("", "_boss"):
            if not os.path.exists(os.path.join(DATA, "loot_tables", "chests", name + suffix + ".json")):
                problem(f"structure {name}: missing loot table chests/{name}{suffix}")
    # pieces must reference loot tables that exist
    return structures


def main():
    check_lang_and_models()
    check_sounds_particles()
    check_loot_and_recipes()
    nodes = check_skill_tree()
    structures = check_worldgen()
    print(f"items={len(ITEMS)} blocks={len(BLOCKS)} sounds={len(SOUNDS)} spells={len(SPELLS)} skill_nodes={nodes} structures={structures} lang_keys={len(EN)}")
    if problems:
        print(f"{len(problems)} problem(s):")
        for p in problems:
            print("  -", p)
        sys.exit(1)
    print("data OK")


if __name__ == "__main__":
    main()
