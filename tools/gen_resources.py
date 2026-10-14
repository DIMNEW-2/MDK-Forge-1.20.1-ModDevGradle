#!/usr/bin/env python3
"""Generates the JSON resources of RPG Mod: models, blockstates, lang (en_us + ru_ru), sounds, particles, tags,
loot tables, recipes, the skill tree and spell overrides. Worldgen JSON lives in gen_worldgen.py and the
structure NBT templates in gen_templates.py.

Run from the repository root:  python3 tools/gen_resources.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "rpgmod")
DATA = os.path.join(RES, "data", "rpgmod")
MC_DATA = os.path.join(RES, "data", "minecraft")

TIERS = ["wood", "stone", "iron", "gold", "diamond", "netherite", "rune"]
WEAPONS = ["sword", "mace", "spear", "bow", "crossbow"]
BOMBS = ["basic", "fire", "frost", "lightning", "poison", "smoke"]
ARROWS = ["basic", "fire", "frost", "explosive", "poison", "piercing", "glowing"]
BOLTS = ["basic", "fire", "frost", "poison", "piercing", "lightning"]
SCHOOLS = ["fire", "frost", "lightning", "nature"]
CLASSES = ["warrior", "mage", "archer", "crossbowman", "rogue", "druid", "spearman", "bomber"]
PIECES = ["helmet", "chestplate", "leggings", "boots"]
CHEST_TIERS = ["common", "rare", "boss"]
DUNGEONS = {
    # id: (level, boss, underground)
    "catacombs_basic": (5, "skeleton_king", True),
    "bandit_camp": (6, "bandit_chief", False),
    "deep_mines": (7, "mine_overseer", True),
    "abandoned_manor": (8, "manor_ghost", False),
    "labyrinth_basic": (9, "labyrinth_minotaur", True),
    "underground_fortress": (10, "fortress_lord", True),
    "ruined_castle": (12, "fallen_king", False),
    "arcane_tower": (15, "archmage", False),
}
SPELLS = {
    # id: (school, mana, cooldown ticks, level, basic)
    "fireball": ("fire", 10, 40, 1, True), "fire_nova": ("fire", 20, 200, 5, False), "flame_burst": ("fire", 18, 120, 8, False), "inferno": ("fire", 30, 300, 12, False),
    "ice_shard": ("frost", 8, 30, 1, True), "frost_nova": ("frost", 18, 200, 5, False), "ice_armor": ("frost", 15, 400, 8, False), "blizzard": ("frost", 25, 240, 12, False),
    "spark": ("lightning", 6, 20, 1, True), "lightning_bolt": ("lightning", 15, 100, 5, False), "blink": ("lightning", 14, 160, 6, False), "static_charge": ("lightning", 12, 400, 8, False), "chain_lightning": ("lightning", 25, 240, 10, False),
    "heal": ("nature", 10, 60, 1, True), "thorn_bolt": ("nature", 8, 30, 3, False), "entangle": ("nature", 16, 200, 4, False), "regrowth": ("nature", 20, 300, 6, False), "bark_skin": ("nature", 15, 500, 8, False),
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


# =========================================================================================== models
BOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
CROSSBOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [-90, 0, -60], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [-90, 0, 30], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [-90, 0, -55], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [-90, 0, 35], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
SPEAR_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}


def item_model(name, parent="item/generated", texture=None, extra=None):
    model = {"parent": parent, "textures": {"layer0": "rpgmod:item/" + (texture or name)}}
    if extra:
        model.update(extra)
    write_json(os.path.join(ASSETS, "models", "item", name + ".json"), model)


def gen_models():
    for tier in TIERS:
        item_model(f"sword_{tier}_rpg", "item/handheld")
        item_model(f"mace_{tier}_rpg", "item/handheld")
        item_model(f"spear_{tier}_rpg", "item/handheld", extra={"display": SPEAR_DISPLAY})
        bow = f"bow_{tier}_rpg"
        item_model(bow, "item/generated", extra={
            "display": BOW_DISPLAY,
            "overrides": [
                {"predicate": {"pulling": 1}, "model": f"rpgmod:item/{bow}_pulling_0"},
                {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"rpgmod:item/{bow}_pulling_1"},
                {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"rpgmod:item/{bow}_pulling_2"},
            ],
        })
        for p in range(3):
            write_json(os.path.join(ASSETS, "models", "item", f"{bow}_pulling_{p}.json"),
                       {"parent": f"rpgmod:item/{bow}", "textures": {"layer0": f"rpgmod:item/{bow}_pulling_{p}"}})
        cb = f"crossbow_{tier}_rpg"
        item_model(cb, "item/generated", extra={
            "display": CROSSBOW_DISPLAY,
            "overrides": [
                {"predicate": {"pulling": 1}, "model": f"rpgmod:item/{cb}_pulling_0"},
                {"predicate": {"pulling": 1, "pull": 0.58}, "model": f"rpgmod:item/{cb}_pulling_1"},
                {"predicate": {"pulling": 1, "pull": 1.0}, "model": f"rpgmod:item/{cb}_pulling_2"},
                {"predicate": {"charged": 1}, "model": f"rpgmod:item/{cb}_arrow"},
                {"predicate": {"charged": 1, "firework": 1}, "model": f"rpgmod:item/{cb}_firework"},
            ],
        })
        for suffix in ["pulling_0", "pulling_1", "pulling_2", "arrow", "firework"]:
            write_json(os.path.join(ASSETS, "models", "item", f"{cb}_{suffix}.json"),
                       {"parent": f"rpgmod:item/{cb}", "textures": {"layer0": f"rpgmod:item/{cb}_{suffix}"}})
    for b in BOMBS:
        item_model(f"bomb_{b}")
    for a in ARROWS:
        item_model(f"arrow_{a}")
    for b in BOLTS:
        item_model(f"bolt_{b}")
    for s in SCHOOLS:
        item_model(f"staff_{s}", "item/handheld")
        item_model(f"scroll_{s}")
    for c in CLASSES:
        for p in PIECES:
            item_model(f"{c}_{p}")
    for name in ["forging_template", "mana_potion", "relations_map", "skill_book"]:
        item_model(name)
    # blocks
    for tier in CHEST_TIERS:
        block = "loot_chest" if tier == "common" else f"loot_chest_{tier}"
        write_json(os.path.join(ASSETS, "models", "block", block + ".json"), {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": f"rpgmod:block/loot_chest_{tier}_top",
                "front": f"rpgmod:block/loot_chest_{tier}_front",
                "side": f"rpgmod:block/loot_chest_{tier}_side",
            },
        })
        write_json(os.path.join(ASSETS, "models", "item", block + ".json"), {"parent": f"rpgmod:block/{block}"})
        write_json(os.path.join(ASSETS, "blockstates", block + ".json"), {"variants": {
            "facing=north": {"model": f"rpgmod:block/{block}"},
            "facing=east": {"model": f"rpgmod:block/{block}", "y": 90},
            "facing=south": {"model": f"rpgmod:block/{block}", "y": 180},
            "facing=west": {"model": f"rpgmod:block/{block}", "y": 270},
        }})
    write_json(os.path.join(ASSETS, "models", "block", "boss_spawner.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": "rpgmod:block/boss_spawner_top", "bottom": "rpgmod:block/boss_spawner_side", "side": "rpgmod:block/boss_spawner_side"},
    })
    write_json(os.path.join(ASSETS, "models", "item", "boss_spawner.json"), {"parent": "rpgmod:block/boss_spawner"})
    write_json(os.path.join(ASSETS, "blockstates", "boss_spawner.json"), {"variants": {"": {"model": "rpgmod:block/boss_spawner"}}})


# =========================================================================================== sounds / particles
SOUND_EVENTS = {
    "casino_spin": ["minecraft:block.note_block.hat", "minecraft:ui.loom.select_pattern"],
    "casino_stop": ["minecraft:block.note_block.bit"],
    "casino_win": ["minecraft:entity.player.levelup"],
    "casino_jackpot": ["minecraft:ui.toast.challenge_complete", "minecraft:entity.firework_rocket.twinkle"],
    "spell_cast": ["minecraft:entity.evoker.cast_spell"],
    "spell_fail": ["minecraft:block.fire.extinguish"],
    "spell_learn": ["minecraft:block.enchantment_table.use"],
    "level_up": ["minecraft:entity.player.levelup"],
    "skill_unlock": ["minecraft:ui.toast.in"],
    "forge": ["minecraft:block.anvil.use"],
    "spear_throw": ["minecraft:item.trident.throw"],
    "spear_hit": ["minecraft:item.trident.hit"],
    "bomb_fuse": ["minecraft:entity.tnt.primed"],
    "bomb_explode": ["minecraft:entity.generic.explode"],
    "mace_smash": ["minecraft:entity.iron_golem.attack", "minecraft:block.anvil.land"],
    "sword_dash": ["minecraft:entity.breeze.jump", "minecraft:item.trident.riptide_1"],
    "bow_draw": ["minecraft:item.crossbow.loading_start"],
    "crossbow_reload": ["minecraft:item.crossbow.loading_middle"],
    "boss_spawn": ["minecraft:entity.wither.spawn"],
    "spell_impact": ["minecraft:entity.blaze.hurt"],
    "heal": ["minecraft:entity.player.levelup"],
    "chest_open": ["minecraft:block.chest.open"],
}
# sounds that do not exist in 1.20.1 are filtered out here
MISSING_IN_1_20_1 = {"minecraft:entity.breeze.jump"}


def gen_sounds():
    sounds = {}
    for name, events in SOUND_EVENTS.items():
        entries = [{"name": e, "type": "event"} for e in events if e not in MISSING_IN_1_20_1]
        sounds[name] = {"subtitle": f"subtitles.rpgmod.{name}", "sounds": entries}
    write_json(os.path.join(ASSETS, "sounds.json"), sounds)
    for p in ["mana_spark", "casino_sparkle", "nature_leaf"]:
        write_json(os.path.join(ASSETS, "particles", p + ".json"), {"textures": [f"rpgmod:{p}"]})


# =========================================================================================== lang
TIER_NAMES = {
    "wood": ("Wooden", "Деревянный"), "stone": ("Stone", "Каменный"), "iron": ("Iron", "Железный"), "gold": ("Golden", "Золотой"),
    "diamond": ("Diamond", "Алмазный"), "netherite": ("Netherite", "Незеритовый"), "rune": ("Runic", "Рунический"),
}
TIER_NAMES_F = {  # feminine (булава, пика? -> "копьё" neuter) — Russian agreement per weapon
    "wood": "Деревянная", "stone": "Каменная", "iron": "Железная", "gold": "Золотая", "diamond": "Алмазная", "netherite": "Незеритовая", "rune": "Руническая",
}
TIER_NAMES_N = {
    "wood": "Деревянное", "stone": "Каменное", "iron": "Железное", "gold": "Золотое", "diamond": "Алмазное", "netherite": "Незеритовое", "rune": "Руническое",
}
WEAPON_NAMES = {
    "sword": ("Sword", "меч", "m"), "mace": ("Mace", "булава", "f"), "spear": ("Spear", "копьё", "n"),
    "bow": ("Bow", "лук", "m"), "crossbow": ("Crossbow", "арбалет", "m"),
}
BOMB_NAMES = {"basic": ("Bomb", "Бомба"), "fire": ("Fire Bomb", "Огненная бомба"), "frost": ("Frost Bomb", "Морозная бомба"),
              "lightning": ("Lightning Bomb", "Грозовая бомба"), "poison": ("Poison Bomb", "Ядовитая бомба"), "smoke": ("Smoke Bomb", "Дымовая бомба")}
EFFECT_NAMES = {"basic": ("", ""), "fire": ("Fire", "Огненн"), "frost": ("Frost", "Морозн"), "explosive": ("Explosive", "Взрывн"), "poison": ("Poison", "Ядовит"),
                "piercing": ("Piercing", "Бронебойн"), "glowing": ("Glowing", "Светящ"), "lightning": ("Lightning", "Грозов")}
SCHOOL_NAMES = {"fire": ("Fire", "Огонь", "Огненный"), "frost": ("Frost", "Лёд", "Ледяной"), "lightning": ("Lightning", "Молния", "Грозовой"), "nature": ("Nature", "Природа", "Природный")}
CLASS_NAMES = {"none": ("None", "Нет"), "warrior": ("Warrior", "Воин"), "mage": ("Mage", "Маг"), "archer": ("Archer", "Лучник"), "crossbowman": ("Crossbowman", "Арбалетчик"),
               "rogue": ("Rogue", "Разбойник"), "druid": ("Druid", "Друид"), "spearman": ("Spearman", "Копейщик"), "bomber": ("Bomber", "Подрывник")}
CLASS_GEN = {"warrior": "воина", "mage": "мага", "archer": "лучника", "crossbowman": "арбалетчика", "rogue": "разбойника", "druid": "друида", "spearman": "копейщика", "bomber": "подрывника"}
PIECE_NAMES = {"helmet": ("Helmet", "Шлем"), "chestplate": ("Chestplate", "Нагрудник"), "leggings": ("Leggings", "Поножи"), "boots": ("Boots", "Ботинки")}
CLASS_BONUS = {
    "none": ("No class bonus", "Нет классового бонуса"),
    "warrior": ("+10% sword damage, +15% mace damage, +2 armor", "+10% урона мечом, +15% булавой, +2 брони"),
    "mage": ("+25% magic power, +20 max mana, -10% mana cost, -20% magic damage taken", "+25% силы магии, +20 маны, -10% стоимости заклинаний, -20% урона от магии"),
    "archer": ("+20% bow damage, +15% movement speed", "+20% урона из лука, +15% скорости"),
    "crossbowman": ("+25% bolt damage, +10% armor penetration", "+25% урона болтами, +10% пробития брони"),
    "rogue": ("+30% backstab damage, +10% crit chance, +10% speed", "+30% урона в спину, +10% шанса крита, +10% скорости"),
    "druid": ("+20% healing, +5 mana regen, nature damage halved", "+20% исцеления, +5 реген. маны, урон от природы вдвое меньше"),
    "spearman": ("+15% spear damage, +1 reach, +1 armor", "+15% урона копьём, +1 дальность, +1 брони"),
    "bomber": ("+25% bomb damage, explosion damage taken halved", "+25% урона бомбами, урон от взрывов вдвое меньше"),
}
MATERIALS = {"iron": ("Iron", "Железо"), "gold": ("Gold", "Золото"), "diamond": ("Diamond", "Алмаз"), "emerald": ("Emerald", "Изумруд"), "netherite": ("Netherite", "Незерит"),
             "amethyst": ("Amethyst", "Аметист"), "lapis": ("Lapis", "Лазурит"), "redstone": ("Redstone", "Редстоун"), "quartz": ("Quartz", "Кварц"), "copper": ("Copper", "Медь")}
RARITIES = {"common": ("Common", "Обычный"), "uncommon": ("Uncommon", "Необычный"), "rare": ("Rare", "Редкий"), "epic": ("Epic", "Эпический"), "legendary": ("Legendary", "Легендарный")}
BONUSES = {"damage": ("Damage", "Урон"), "attack_speed": ("Attack Speed", "Скорость атаки"), "crit_chance": ("Critical Chance", "Шанс крита"), "armor_penetration": ("Armor Penetration", "Пробитие брони"),
           "max_health": ("Max Health", "Макс. здоровье"), "armor": ("Armor", "Броня"), "move_speed": ("Movement Speed", "Скорость"), "magic_power": ("Magic Power", "Сила магии"),
           "max_mana": ("Max Mana", "Макс. мана"), "mana_regen": ("Mana Regeneration", "Восстановление маны")}
ATTRIBUTES = {"crit_chance": ("Critical Chance", "Шанс критического удара"), "armor_penetration": ("Armor Penetration", "Пробитие брони"), "magic_power": ("Magic Power", "Сила магии"),
              "max_mana": ("Max Mana", "Максимум маны"), "mana_regen": ("Mana Regeneration", "Восстановление маны"), "healing_power": ("Healing Power", "Сила исцеления"), "ranged_damage": ("Ranged Damage", "Дальний урон")}
STATS = {"strength": ("Strength", "Сила", "+0.5 melee damage per point", "+0.5 урона в ближнем бою за очко"),
         "agility": ("Agility", "Ловкость", "+1% movement speed and +0.5% crit chance per point", "+1% скорости и +0.5% шанса крита за очко"),
         "intellect": ("Intellect", "Интеллект", "+2% magic power per point", "+2% силы магии за очко"),
         "health": ("Vitality", "Живучесть", "+1 max health per point", "+1 здоровья за очко"),
         "mana": ("Spirit", "Дух", "+5 max mana per point", "+5 маны за очко"),
         "luck": ("Luck", "Удача", "+0.25 luck per point (better loot)", "+0.25 удачи за очко (лучше добыча)")}
WEAPON_CLASSES = {"sword": ("Sword", "Меч"), "mace": ("Mace", "Булава"), "spear": ("Spear", "Копьё"), "bomb": ("Bomb", "Бомба"), "bow": ("Bow", "Лук"), "crossbow": ("Crossbow", "Арбалет"),
                  "axe": ("Axe", "Топор"), "dagger": ("Dagger", "Кинжал"), "staff": ("Staff", "Посох"), "tool": ("Tool", "Инструмент")}
VILLAGE_CLASSES = {"farming": ("Farming village", "Аграрная деревня"), "mining": ("Mining village", "Шахтёрская деревня"), "military": ("Military village", "Военная деревня"),
                   "magic": ("Magic village", "Магическая деревня"), "trading": ("Trading village", "Торговая деревня")}
RELATIONS = {"war": ("War", "Война"), "hostile": ("Hostile", "Враждебные"), "tense": ("Tense", "Напряжённые"), "neutral": ("Neutral", "Нейтральные"), "friendly": ("Friendly", "Дружеские"), "allied": ("Allied", "Союз")}
BOSSES = {"skeleton_king": ("Skeleton King", "Король скелетов"), "bandit_chief": ("Bandit Chief", "Главарь бандитов"), "mine_overseer": ("Mine Overseer", "Надзиратель шахт"),
          "manor_ghost": ("Ghost of the Manor", "Призрак поместья"), "labyrinth_minotaur": ("Minotaur of the Labyrinth", "Минотавр лабиринта"), "fortress_lord": ("Fortress Lord", "Владыка крепости"),
          "fallen_king": ("Fallen King", "Павший король"), "archmage": ("Archmage", "Архимаг")}
SKILL_BRANCHES = {"fire": ("Fire", "Огонь"), "frost": ("Frost", "Лёд"), "lightning": ("Lightning", "Молния"), "nature": ("Nature", "Природа"), "arcane": ("Arcane", "Тайная магия"),
                  "combat": ("Combat", "Бой"), "defense": ("Defense", "Защита"), "ranged": ("Ranged", "Стрельба"), "exploration": ("Exploration", "Исследование"), "social": ("Social", "Общество")}
SPELL_NAMES = {
    "fireball": ("Fireball", "Огненный шар", "Hurls a ball of fire that explodes on impact.", "Запускает огненный шар, взрывающийся при попадании."),
    "fire_nova": ("Fire Nova", "Огненная нова", "Burst of flames around the caster.", "Вспышка пламени вокруг заклинателя."),
    "flame_burst": ("Flame Burst", "Всплеск пламени", "Cone of fire in front of the caster.", "Конус огня перед заклинателем."),
    "inferno": ("Inferno", "Инферно", "Rains fire on a wide area.", "Обрушивает огонь на большую область."),
    "ice_shard": ("Ice Shard", "Ледяной осколок", "Fast icy projectile that slows the target.", "Быстрый ледяной снаряд, замедляющий цель."),
    "frost_nova": ("Frost Nova", "Ледяная нова", "Freezes everything around the caster.", "Замораживает всё вокруг заклинателя."),
    "ice_armor": ("Ice Armor", "Ледяная броня", "Protective frost shell: resistance and slowness for attackers.", "Защитная ледяная оболочка: сопротивление урону."),
    "blizzard": ("Blizzard", "Метель", "Storm of ice shards over an area.", "Шторм ледяных осколков над областью."),
    "spark": ("Spark", "Искра", "Quick electric bolt.", "Быстрый электрический разряд."),
    "lightning_bolt": ("Lightning Bolt", "Удар молнии", "Calls lightning onto the targeted block.", "Призывает молнию в точку прицела."),
    "blink": ("Blink", "Скачок", "Teleports a short distance forward.", "Телепортирует на небольшое расстояние вперёд."),
    "static_charge": ("Static Charge", "Статический заряд", "Haste and mana surge for a short time.", "Спешка и прилив маны на короткое время."),
    "chain_lightning": ("Chain Lightning", "Цепная молния", "Lightning that jumps between nearby enemies.", "Молния, перескакивающая между врагами."),
    "heal": ("Heal", "Исцеление", "Restores health to the caster.", "Восстанавливает здоровье заклинателю."),
    "thorn_bolt": ("Thorn Bolt", "Шип", "Nature projectile that poisons.", "Природный снаряд, отравляющий цель."),
    "entangle": ("Entangle", "Опутывание", "Roots enemies around the caster.", "Обездвиживает врагов вокруг заклинателя."),
    "regrowth": ("Regrowth", "Возрождение", "Long regeneration for the caster and nearby allies.", "Долгая регенерация для заклинателя и союзников рядом."),
    "bark_skin": ("Bark Skin", "Дубовая кожа", "Thick bark: strong damage reduction for a while.", "Толстая кора: сильное снижение урона на время."),
}


def armor_name_ru(cls, piece):
    base = PIECE_NAMES[piece][1]
    return f"{base} {CLASS_GEN[cls]}"


def build_lang():
    en = {}
    ru = {}

    def add(key, e, r):
        en[key] = e
        ru[key] = r

    # items
    for tier in TIERS:
        for w, (we, wr, gender) in WEAPON_NAMES.items():
            ten = TIER_NAMES[tier][0]
            if gender == "m":
                tru = TIER_NAMES[tier][1]
            elif gender == "f":
                tru = TIER_NAMES_F[tier]
            else:
                tru = TIER_NAMES_N[tier]
            add(f"item.rpgmod.{w}_{tier}_rpg", f"{ten} {we}", f"{tru} {wr}")
    for b, (e, r) in BOMB_NAMES.items():
        add(f"item.rpgmod.bomb_{b}", e, r)
    for a in ARROWS:
        e, r = EFFECT_NAMES[a]
        add(f"item.rpgmod.arrow_{a}", (e + " Arrow").strip() if a != "basic" else "Hunter's Arrow", (r + "ая стрела") if a != "basic" else "Охотничья стрела")
    for b in BOLTS:
        e, r = EFFECT_NAMES[b]
        add(f"item.rpgmod.bolt_{b}", (e + " Bolt").strip() if b != "basic" else "Steel Bolt", (r + "ый болт") if b != "basic" else "Стальной болт")
    for s, (e, r, radj) in SCHOOL_NAMES.items():
        add(f"item.rpgmod.staff_{s}", f"Staff of {e}", f"{radj} посох")
        add(f"item.rpgmod.scroll_{s}", f"{e} Scroll", f"{radj} свиток")
    for c in CLASSES:
        for p in PIECES:
            add(f"item.rpgmod.{c}_{p}", f"{CLASS_NAMES[c][0]}'s {PIECE_NAMES[p][0]}", armor_name_ru(c, p))
    add("item.rpgmod.forging_template", "Forging Template", "Шаблон ковки")
    add("item.rpgmod.mana_potion", "Mana Potion", "Зелье маны")
    add("item.rpgmod.relations_map", "Map of Relations", "Карта отношений")
    add("item.rpgmod.skill_book", "Skill Book", "Книга навыков")
    add("block.rpgmod.loot_chest", "Loot Chest", "Сундук с добычей")
    add("block.rpgmod.loot_chest_rare", "Rare Loot Chest", "Редкий сундук с добычей")
    add("block.rpgmod.loot_chest_boss", "Boss Chest", "Сундук босса")
    add("block.rpgmod.boss_spawner", "Boss Altar", "Алтарь босса")
    add("entity.rpgmod.thrown_spear", "Spear", "Копьё")
    add("entity.rpgmod.thrown_bomb", "Bomb", "Бомба")
    add("entity.rpgmod.rpg_arrow", "Arrow", "Стрела")
    add("entity.rpgmod.rpg_bolt", "Bolt", "Болт")
    add("entity.rpgmod.spell_projectile", "Spell", "Заклинание")
    add("entity.rpgmod.raider", "Raider", "Налётчик")
    add("itemGroup.rpgmod.weapons", "RPG Mod: Weapons", "RPG Mod: Оружие")
    add("itemGroup.rpgmod.magic", "RPG Mod: Magic", "RPG Mod: Магия")
    add("itemGroup.rpgmod.armor", "RPG Mod: Armor", "RPG Mod: Броня")
    add("itemGroup.rpgmod.world", "RPG Mod: World", "RPG Mod: Мир")
    add("effect.rpgmod.stun", "Stunned", "Оглушение")
    add("effect.rpgmod.root", "Rooted", "Обездвиживание")
    add("effect.rpgmod.mana_surge", "Mana Surge", "Прилив маны")
    add("effect.rpgmod.bark_skin", "Bark Skin", "Дубовая кожа")
    # enums
    for k, (e, r) in WEAPON_CLASSES.items():
        add(f"weapon_class.rpgmod.{k}", e, r)
    for k, (e, r) in MATERIALS.items():
        add(f"material.rpgmod.{k}", e, r)
    for k, (e, r) in RARITIES.items():
        add(f"rarity.rpgmod.{k}", e, r)
    for k, (e, r) in BONUSES.items():
        add(f"bonus.rpgmod.{k}", e, r)
    for k, (e, r) in ATTRIBUTES.items():
        add(f"attribute.rpgmod.{k}", e, r)
    for k, (e, r, de, dr) in STATS.items():
        add(f"stat.rpgmod.{k}", e, r)
        add(f"stat.rpgmod.{k}.desc", de, dr)
    for k, (e, r) in CLASS_NAMES.items():
        add(f"class.rpgmod.{k}", e, r)
        add(f"class.rpgmod.{k}.bonus", CLASS_BONUS[k][0], CLASS_BONUS[k][1])
    for k, (e, r, _) in SCHOOL_NAMES.items():
        add(f"spell_school.rpgmod.{k}", e, r)
    for k, (e, r) in VILLAGE_CLASSES.items():
        add(f"village_class.rpgmod.{k}", e, r)
    for k, (e, r) in RELATIONS.items():
        add(f"relation.rpgmod.{k}", e, r)
    for k, (e, r) in BOSSES.items():
        add(f"boss.rpgmod.{k}", e, r)
    for k, (e, r) in SKILL_BRANCHES.items():
        add(f"skill_branch.rpgmod.{k}", e, r)
    for k, (e, r, de, dr) in SPELL_NAMES.items():
        add(f"spell.rpgmod.{k}", e, r)
        add(f"spell.rpgmod.{k}.desc", de, dr)
    add("spell.rpgmod.unknown", "Unknown spell", "Неизвестное заклинание")
    for k, (e, r) in {"common": ("Common chest", "Обычный сундук"), "rare": ("Rare chest", "Редкий сундук"), "boss": ("Boss chest", "Сундук босса")}.items():
        add(f"container.rpgmod.loot_chest.{k}", e, r)
        add(f"tooltip.rpgmod.loot_chest.{k}", e, r)
    for k, (e, r) in BOMB_NAMES.items():
        pass
    bomb_desc = {"basic": ("Plain explosion", "Обычный взрыв"), "fire": ("Sets targets on fire", "Поджигает цели"), "frost": ("Slows and freezes", "Замедляет и замораживает"),
                 "lightning": ("Strikes with lightning", "Бьёт молнией"), "poison": ("Poison cloud", "Ядовитое облако"), "smoke": ("Blinding smoke screen, no damage", "Дымовая завеса, без урона")}
    for k, (e, r) in bomb_desc.items():
        add(f"tooltip.rpgmod.bomb.{k}", e, r)
    proj_desc = {"basic": ("Standard projectile", "Обычный снаряд"), "fire": ("Ignites the target", "Поджигает цель"), "frost": ("Slows the target", "Замедляет цель"),
                 "explosive": ("Explodes on impact", "Взрывается при попадании"), "poison": ("Poisons the target", "Отравляет цель"), "piercing": ("Ignores part of the armor", "Игнорирует часть брони"),
                 "glowing": ("Marks the target", "Подсвечивает цель"), "lightning": ("Calls lightning on impact", "Призывает молнию при попадании")}
    for k, (e, r) in proj_desc.items():
        add(f"tooltip.rpgmod.projectile.{k}", e, r)
    # skill effects / flags
    add("skill_effect.rpgmod.attribute", "%s %s", "%s %s")
    add("skill_effect.rpgmod.unlock_spell", "Unlocks spell: %s", "Открывает заклинание: %s")
    add("skill_effect.rpgmod.mana_cost", "-%s%% mana cost", "-%s%% стоимости маны")
    add("skill_effect.rpgmod.cooldown", "-%s%% spell cooldown", "-%s%% перезарядки заклинаний")
    add("skill_effect.rpgmod.spell_power", "+%s%% spell power", "+%s%% силы заклинаний")
    add("skill_flag.rpgmod.executioner", "Executioner: +25% damage to enemies below 25% health", "Палач: +25% урона по врагам с здоровьем ниже 25%")
    add("skill_flag.rpgmod.iron_will", "Iron Will: a single hit cannot take more than half of your health", "Железная воля: один удар не может снять больше половины здоровья")
    add("skill_flag.rpgmod.diplomat", "Diplomat: reputation gains doubled, losses halved", "Дипломат: прирост репутации удвоен, потери вдвое меньше")
    # tooltips
    add("tooltip.rpgmod.item_level", "Item Level: %s", "Уровень предмета: %s")
    add("tooltip.rpgmod.material", "Material: %s", "Материал: %s")
    add("tooltip.rpgmod.material.none", "Material: none (forge to add)", "Материал: нет (добавьте ковкой)")
    add("tooltip.rpgmod.bonuses", "Bonuses:", "Бонусы:")
    add("tooltip.rpgmod.sword.special", "Right click: dash forward and empower the next strike (5 s)", "ПКМ: рывок вперёд и усиленный следующий удар (5 с)")
    add("tooltip.rpgmod.mace.special", "Shift + right click: ground slam, stuns nearby enemies (8 s)", "Shift + ПКМ: удар по земле, оглушает врагов рядом (8 с)")
    add("tooltip.rpgmod.mace.stun", "Stun chance on hit: %s%%", "Шанс оглушения при ударе: %s%%")
    add("tooltip.rpgmod.spear.special", "Extended reach. Hold right click to throw; the spear can be picked up", "Увеличенная дальность. Удерживайте ПКМ для броска; копьё можно подобрать")
    add("tooltip.rpgmod.spear.throw_damage", "Throw damage: %s", "Урон при броске: %s")
    add("tooltip.rpgmod.bow.special", "Shift + right click: triple shot (10 s)", "Shift + ПКМ: тройной выстрел (10 с)")
    add("tooltip.rpgmod.bow.stats", "Arrow damage: %s, speed x%s", "Урон стрелы: %s, скорость x%s")
    add("tooltip.rpgmod.crossbow.special", "Shift + right click when loaded: fan of three bolts (12 s)", "Shift + ПКМ при заряде: веер из трёх болтов (12 с)")
    add("tooltip.rpgmod.crossbow.stats", "Bolt damage: %s, reload %s t", "Урон болта: %s, перезарядка %s т")
    add("tooltip.rpgmod.bomb.stats", "Damage: %s, radius: %s", "Урон: %s, радиус: %s")
    add("tooltip.rpgmod.armor.set", "Set: %s (wear all four pieces to activate)", "Комплект: %s (наденьте все четыре части)")
    add("tooltip.rpgmod.forging_template.1", "Smithing table: template + RPG weapon + material", "Кузнечный стол: шаблон + RPG-оружие + материал")
    add("tooltip.rpgmod.forging_template.2", "Adds a material bonus to the weapon", "Добавляет оружию бонус материала")
    add("tooltip.rpgmod.mana_potion", "Restores %s mana", "Восстанавливает %s маны")
    add("tooltip.rpgmod.relations_map", "Right click: open the map of village relations", "ПКМ: открыть карту отношений деревень")
    add("tooltip.rpgmod.skill_book", "Right click: open the skill tree", "ПКМ: открыть древо навыков")
    add("tooltip.rpgmod.boss_spawner", "Summons a dungeon boss when a player approaches", "Призывает босса подземелья, когда игрок подходит")
    add("tooltip.rpgmod.staff.school", "School: %s", "Школа: %s")
    add("tooltip.rpgmod.staff.spell", "Selected spell: %s", "Выбранное заклинание: %s")
    add("tooltip.rpgmod.staff.hint", "Right click: cast. Shift + right click: next spell", "ПКМ: применить. Shift + ПКМ: следующее заклинание")
    add("tooltip.rpgmod.scroll.hint", "Right click: learn the spell (or cast once without mana)", "ПКМ: выучить заклинание (или применить один раз без маны)")
    add("tooltip.rpgmod.spell.cost", "Mana: %s, cooldown: %s s", "Мана: %s, перезарядка: %s с")
    add("tooltip.rpgmod.spell.damage", "Damage: %s", "Урон: %s")
    add("tooltip.rpgmod.spell.level", "Requires level %s", "Требуется уровень %s")
    add("tooltip.rpgmod.mob_level", "[Lv. %s]", "[Ур. %s]")
    # messages
    add("message.rpgmod.welcome", "Welcome, adventurer! Press K for skills, O for your character and R for the map of relations.", "Добро пожаловать, искатель приключений! K — навыки, O — персонаж, R — карта отношений.")
    add("message.rpgmod.level_up", "Level up! You are now level %s.", "Новый уровень! Теперь вы %s уровня.")
    add("message.rpgmod.class_activated", "Class set activated: %s", "Активирован классовый комплект: %s")
    add("message.rpgmod.biome_discovered", "New biome discovered: %s", "Открыт новый биом: %s")
    add("message.rpgmod.settlement_discovered", "You discovered the settlement %s (%s)", "Вы обнаружили поселение %s (%s)")
    add("message.rpgmod.reputation_changed", "Reputation with %s: %s", "Репутация с %s: %s")
    add("message.rpgmod.alliance_formed", "%s and %s have formed an alliance!", "%s и %s заключили союз!")
    add("message.rpgmod.war_declared", "%s declared war on %s!", "%s объявила войну %s!")
    add("message.rpgmod.war_ended", "The war is over: %s defeated %s.", "Война окончена: %s победила %s.")
    add("message.rpgmod.raid_incoming", "Raiders from %s are attacking %s!", "Налётчики из %s атакуют %s!")
    add("message.rpgmod.diplomacy.trade_agreement", "%s and %s signed a trade agreement.", "%s и %s подписали торговое соглашение.")
    add("message.rpgmod.diplomacy.border_dispute", "A border dispute broke out between %s and %s.", "Между %s и %s вспыхнул пограничный спор.")
    add("message.rpgmod.diplomacy.festival", "%s invited %s to a festival.", "%s пригласила %s на праздник.")
    add("message.rpgmod.diplomacy.raid_blamed", "%s blames %s for a raid.", "%s обвиняет %s в набеге.")
    add("message.rpgmod.boss_awakens", "%s awakens!", "%s пробуждается!")
    add("message.rpgmod.boss_defeated", "%s has been defeated!", "%s повержен!")
    add("message.rpgmod.chest_empty", "The chest is empty.", "Сундук пуст.")
    add("message.rpgmod.chest_taken", "Another player is already opening this chest.", "Этот сундук уже открывает другой игрок.")
    add("message.rpgmod.respec_cost", "Respec costs one block of emerald.", "Сброс навыков стоит один блок изумруда.")
    add("message.rpgmod.respec_done", "Skills reset, %s points refunded.", "Навыки сброшены, возвращено очков: %s.")
    add("message.rpgmod.magic_disabled", "Magic is disabled on this server.", "Магия отключена на этом сервере.")
    add("message.rpgmod.no_spells", "No spells available for this staff.", "Для этого посоха нет доступных заклинаний.")
    add("message.rpgmod.not_enough_mana", "Not enough mana!", "Недостаточно маны!")
    add("message.rpgmod.spell_cooldown", "Spell is on cooldown (%.1f s)", "Заклинание перезаряжается (%.1f с)")
    add("message.rpgmod.spell_learned", "You learned %s!", "Вы выучили %s!")
    add("message.rpgmod.spell_level", "Requires character level %s", "Требуется уровень персонажа %s")
    add("message.rpgmod.spell_selected", "Spell: %s", "Заклинание: %s")
    add("message.rpgmod.spell_unknown", "You do not know %s yet.", "Вы ещё не знаете %s.")
    # commands
    add("command.rpgmod.level_set", "%s is now level %s", "%s теперь %s уровня")
    add("command.rpgmod.level_get", "%s: level %s (%s/%s XP), class %s", "%s: уровень %s (%s/%s опыта), класс %s")
    add("command.rpgmod.xp_added", "Added %s XP to %s", "Добавлено %s опыта игроку %s")
    add("command.rpgmod.points_added", "Added %s points to %s", "Добавлено %s очков игроку %s")
    add("command.rpgmod.not_rpg_item", "Hold an RPG item in your main hand", "Возьмите RPG-предмет в основную руку")
    add("command.rpgmod.unknown_boss", "Unknown boss", "Неизвестный босс")
    add("command.rpgmod.no_settlements", "No settlements discovered yet", "Поселений пока не обнаружено")
    add("command.rpgmod.unknown_settlement", "Unknown settlement", "Неизвестное поселение")
    add("command.rpgmod.template_too_big", "Templates are limited to 48 blocks per axis", "Размер шаблона ограничен 48 блоками по каждой оси")
    add("command.rpgmod.template_saved", "Template %s saved to generated/", "Шаблон %s сохранён в generated/")
    add("command.rpgmod.template_save_failed", "Could not save template %s", "Не удалось сохранить шаблон %s")
    add("command.rpgmod.template_not_found", "Template %s not found", "Шаблон %s не найден")
    add("command.rpgmod.template_placed", "Template %s placed", "Шаблон %s размещён")
    add("command.rpgmod.unknown_dungeon", "Unknown dungeon %s", "Неизвестное подземелье %s")
    add("command.rpgmod.templates_generated", "%s dungeon templates generated into generated/rpgmod/structures", "Сгенерировано шаблонов подземелий: %s (generated/rpgmod/structures)")
    # screens / hud / keys
    add("screen.rpgmod.skill_tree", "Skill Tree", "Древо навыков")
    add("screen.rpgmod.skill_tree.points", "Skill points: %s (level %s)", "Очки навыков: %s (уровень %s)")
    add("screen.rpgmod.skill_tree.class", "Class: %s", "Класс: %s")
    add("screen.rpgmod.skill_tree.filter", "Branch: %s", "Ветка: %s")
    add("screen.rpgmod.skill_tree.branch", "Branch: %s", "Ветка: %s")
    add("screen.rpgmod.skill_tree.cost", "Cost: %s point(s), requires level %s", "Стоимость: %s очк., требуется уровень %s")
    add("screen.rpgmod.skill_tree.requires_class", "Requires class: %s", "Требуется класс: %s")
    add("screen.rpgmod.skill_tree.unlocked", "Unlocked", "Изучено")
    add("screen.rpgmod.skill_tree.click_to_unlock", "Click to unlock", "Нажмите, чтобы изучить")
    add("screen.rpgmod.skill_tree.locked", "Locked", "Недоступно")
    add("screen.rpgmod.skill_tree.respec", "Respec", "Сброс")
    add("screen.rpgmod.character", "Character", "Персонаж")
    add("screen.rpgmod.character.level", "Level %s", "Уровень %s")
    add("screen.rpgmod.character.xp", "XP: %s / %s", "Опыт: %s / %s")
    add("screen.rpgmod.character.class", "Class: %s", "Класс: %s")
    add("screen.rpgmod.character.mana", "Mana: %s / %s", "Мана: %s / %s")
    add("screen.rpgmod.character.stat_points", "Stat points: %s", "Очки характеристик: %s")
    add("screen.rpgmod.character.stat_line", "%s: %s", "%s: %s")
    add("screen.rpgmod.character.spells", "Known spells", "Известные заклинания")
    add("screen.rpgmod.character.skills", "Skills", "Навыки")
    add("screen.rpgmod.relations", "Map of Relations", "Карта отношений")
    add("screen.rpgmod.relations.empty", "No settlements discovered yet. Explore villages!", "Поселений пока не обнаружено. Исследуйте деревни!")
    add("screen.rpgmod.relations.hint", "Click a settlement to see its details. Drag to pan, scroll to zoom.", "Нажмите на поселение, чтобы увидеть детали. Перетаскивайте карту, колесо — масштаб.")
    add("screen.rpgmod.relations.you", "You", "Вы")
    add("screen.rpgmod.relations.tier", "Development tier: %s", "Уровень развития: %s")
    add("screen.rpgmod.relations.population", "Population: %s", "Население: %s")
    add("screen.rpgmod.relations.prosperity", "Prosperity: %s", "Процветание: %s")
    add("screen.rpgmod.relations.military", "Military: %s", "Военная мощь: %s")
    add("screen.rpgmod.relations.position", "Position: %s, %s", "Координаты: %s, %s")
    add("screen.rpgmod.relations.reputation", "Your reputation: %s", "Ваша репутация: %s")
    add("screen.rpgmod.relations.relations", "Relations", "Отношения")
    add("screen.rpgmod.spell_select", "Select spell", "Выбор заклинания")
    add("screen.rpgmod.spell_select.header", "Spells of %s", "Заклинания школы: %s")
    add("screen.rpgmod.spell_select.cost", "(%s mana, %s s)", "(%s маны, %s с)")
    add("container.rpgmod.loot_chest.claim", "Claim", "Забрать")
    add("container.rpgmod.loot_chest.skip", "Skip", "Пропустить")
    add("container.rpgmod.loot_chest.rolling", "Rolling...", "Бросаем кости...")
    add("container.rpgmod.loot_chest.spinning", "Spinning!", "Крутим!")
    add("container.rpgmod.loot_chest.jackpot", "JACKPOT!", "ДЖЕКПОТ!")
    add("container.rpgmod.loot_chest.nothing", "Nothing this time...", "В этот раз пусто...")
    add("container.rpgmod.loot_chest.rewards", "Rewards: %s", "Награды: %s")
    add("container.rpgmod.loot_chest.wait", "Claim available in %s s", "Забрать можно через %s с")
    add("container.rpgmod.loot_chest.zone_level", "Zone level %s", "Уровень зоны %s")
    add("hud.rpgmod.level", "Lv. %s", "Ур. %s")
    add("hud.rpgmod.spell_ready", "%s (%s mana)", "%s (%s маны)")
    add("hud.rpgmod.spell_cooldown", "%s: %s s", "%s: %s с")
    add("key.categories.rpgmod", "RPG Mod", "RPG Mod")
    add("key.rpgmod.skills", "Skill tree", "Древо навыков")
    add("key.rpgmod.character", "Character sheet", "Персонаж")
    add("key.rpgmod.relations", "Map of relations", "Карта отношений")
    add("key.rpgmod.cast", "Cast selected spell", "Применить заклинание")
    add("key.rpgmod.select_spell", "Select spell", "Выбрать заклинание")
    # subtitles
    subtitles = {"casino_spin": ("Reels spin", "Барабаны крутятся"), "casino_stop": ("Reel stops", "Барабан останавливается"), "casino_win": ("Reward", "Награда"),
                 "casino_jackpot": ("Jackpot!", "Джекпот!"), "spell_cast": ("Spell cast", "Заклинание"), "spell_fail": ("Spell fizzles", "Заклинание не удалось"),
                 "spell_learn": ("Spell learned", "Заклинание выучено"), "level_up": ("Level up", "Новый уровень"), "skill_unlock": ("Skill unlocked", "Навык изучен"),
                 "forge": ("Forging", "Ковка"), "spear_throw": ("Spear thrown", "Бросок копья"), "spear_hit": ("Spear hits", "Копьё попадает"), "bomb_fuse": ("Fuse hisses", "Шипит фитиль"),
                 "bomb_explode": ("Bomb explodes", "Взрыв бомбы"), "mace_smash": ("Mace smashes", "Удар булавы"), "sword_dash": ("Dash", "Рывок"), "bow_draw": ("Bow drawn", "Натяжение лука"),
                 "crossbow_reload": ("Crossbow reloads", "Перезарядка арбалета"), "boss_spawn": ("Boss awakens", "Босс пробуждается"), "spell_impact": ("Spell impact", "Попадание заклинания"),
                 "heal": ("Healing", "Исцеление"), "chest_open": ("Loot chest opens", "Сундук открывается")}
    for k, (e, r) in subtitles.items():
        add(f"subtitles.rpgmod.{k}", e, r)
    # skills
    for node in SKILL_NODES:
        key = "skill.rpgmod." + node["id"].replace("/", ".")
        add(key, node["name"][0], node["name"][1])
        add(key + ".desc", node["desc"][0], node["desc"][1])
    # config comments / misc
    add("rpgmod.configuration.title", "RPG Mod configuration", "Настройки RPG Mod")
    return en, ru


# =========================================================================================== skill tree
def attr(name, amount, operation="addition"):
    return {"type": "attribute", "attribute": name, "amount": amount, "operation": operation}


def spell_unlock(spell):
    return {"type": "unlock_spell", "spell": "rpgmod:" + spell}


def flag(name):
    return {"type": "flag", "flag": name}


def node(id_, branch, x, y, level, name, desc, effects, parents=(), cost=1, icon="minecraft:book", required_class=None):
    n = {"id": id_, "branch": branch, "x": x, "y": y, "required_level": level, "name": name, "desc": desc, "effects": effects,
         "parents": ["rpgmod:" + p for p in parents], "cost": cost, "icon": icon}
    if required_class:
        n["required_class"] = required_class
    return n


SKILL_NODES = [
    # combat (x 0-1)
    node("combat/strength_1", "combat", 0, 0, 2, ("Strength I", "Сила I"), ("+1 attack damage.", "+1 к урону."), [attr("minecraft:generic.attack_damage", 1)], icon="minecraft:iron_sword"),
    node("combat/strength_2", "combat", 0, 1, 5, ("Strength II", "Сила II"), ("+1 attack damage.", "+1 к урону."), [attr("minecraft:generic.attack_damage", 1)], ["combat/strength_1"], icon="minecraft:diamond_sword"),
    node("combat/swift_strikes", "combat", 1, 1, 6, ("Swift Strikes", "Быстрые удары"), ("+10% attack speed.", "+10% скорости атаки."), [attr("minecraft:generic.attack_speed", 0.10, "multiply_base")], ["combat/strength_1"], icon="minecraft:feather"),
    node("combat/executioner", "combat", 0, 2, 10, ("Executioner", "Палач"), ("+25% damage to enemies below 25% health.", "+25% урона по врагам ниже 25% здоровья."), [flag("executioner")], ["combat/strength_2"], cost=2, icon="minecraft:netherite_axe"),
    node("combat/critical_eye", "combat", 1, 2, 8, ("Critical Eye", "Меткий глаз"), ("+5% critical chance.", "+5% шанса критического удара."), [attr("rpgmod:crit_chance", 0.05)], ["combat/swift_strikes"], icon="minecraft:spyglass"),
    node("combat/armor_breaker", "combat", 0, 3, 14, ("Armor Breaker", "Сокрушитель брони"), ("+10% armor penetration.", "+10% пробития брони."), [attr("rpgmod:armor_penetration", 0.10)], ["combat/executioner"], cost=2, icon="minecraft:iron_pickaxe"),
    node("combat/berserk", "combat", 1, 3, 18, ("Berserk", "Берсерк"), ("+10% total attack damage.", "+10% общего урона."), [attr("minecraft:generic.attack_damage", 0.10, "multiply_total")], ["combat/critical_eye"], cost=3, icon="minecraft:blaze_powder"),
    # defense (x 2-3)
    node("defense/vitality_1", "defense", 2, 0, 2, ("Vitality I", "Живучесть I"), ("+2 max health.", "+2 к здоровью."), [attr("minecraft:generic.max_health", 2)], icon="minecraft:apple"),
    node("defense/vitality_2", "defense", 2, 1, 5, ("Vitality II", "Живучесть II"), ("+2 max health.", "+2 к здоровью."), [attr("minecraft:generic.max_health", 2)], ["defense/vitality_1"], icon="minecraft:golden_apple"),
    node("defense/thick_skin", "defense", 3, 1, 6, ("Thick Skin", "Толстая кожа"), ("+1 armor.", "+1 брони."), [attr("minecraft:generic.armor", 1)], ["defense/vitality_1"], icon="minecraft:leather_chestplate"),
    node("defense/iron_will", "defense", 2, 2, 12, ("Iron Will", "Железная воля"), ("A single hit cannot take more than half of your health.", "Один удар не снимет больше половины здоровья."), [flag("iron_will")], ["defense/vitality_2"], cost=2, icon="minecraft:shield"),
    node("defense/toughness", "defense", 3, 2, 9, ("Toughness", "Стойкость"), ("+1 armor toughness.", "+1 прочности брони."), [attr("minecraft:generic.armor_toughness", 1)], ["defense/thick_skin"], icon="minecraft:iron_chestplate"),
    node("defense/stand_firm", "defense", 3, 3, 14, ("Stand Firm", "Твёрдая стойка"), ("+10% knockback resistance.", "+10% сопротивления отбрасыванию."), [attr("minecraft:generic.knockback_resistance", 0.1)], ["defense/toughness"], icon="minecraft:anvil"),
    node("defense/juggernaut", "defense", 2, 3, 20, ("Juggernaut", "Джаггернаут"), ("+4 max health.", "+4 к здоровью."), [attr("minecraft:generic.max_health", 4)], ["defense/iron_will"], cost=3, icon="minecraft:netherite_chestplate"),
    # ranged (x 4-5)
    node("ranged/steady_aim", "ranged", 4, 0, 2, ("Steady Aim", "Твёрдая рука"), ("+5% ranged damage.", "+5% дальнего урона."), [attr("rpgmod:ranged_damage", 0.05)], icon="minecraft:bow"),
    node("ranged/eagle_eye", "ranged", 5, 1, 7, ("Eagle Eye", "Орлиный глаз"), ("+5% critical chance.", "+5% шанса критического удара."), [attr("rpgmod:crit_chance", 0.05)], ["ranged/steady_aim"], icon="minecraft:ender_eye"),
    node("ranged/sharpshooter", "ranged", 4, 1, 6, ("Sharpshooter", "Снайпер"), ("+5% ranged damage.", "+5% дальнего урона."), [attr("rpgmod:ranged_damage", 0.05)], ["ranged/steady_aim"], icon="minecraft:arrow"),
    node("ranged/piercing_focus", "ranged", 4, 2, 11, ("Piercing Focus", "Пробивающий фокус"), ("+10% armor penetration.", "+10% пробития брони."), [attr("rpgmod:armor_penetration", 0.10)], ["ranged/sharpshooter"], cost=2, icon="minecraft:crossbow"),
    node("ranged/light_step", "ranged", 5, 2, 9, ("Light Step", "Лёгкий шаг"), ("+5% movement speed.", "+5% скорости передвижения."), [attr("minecraft:generic.movement_speed", 0.05, "multiply_base")], ["ranged/eagle_eye"], icon="minecraft:rabbit_foot"),
    node("ranged/master_marksman", "ranged", 4, 3, 16, ("Master Marksman", "Мастер стрельбы"), ("+15% ranged damage.", "+15% дальнего урона."), [attr("rpgmod:ranged_damage", 0.15)], ["ranged/piercing_focus"], cost=3, icon="minecraft:spectral_arrow"),
    # exploration (x 6-7)
    node("exploration/fleet_foot", "exploration", 6, 0, 2, ("Fleet Foot", "Быстрые ноги"), ("+5% movement speed.", "+5% скорости."), [attr("minecraft:generic.movement_speed", 0.05, "multiply_base")], icon="minecraft:leather_boots"),
    node("exploration/lucky_finder", "exploration", 6, 1, 5, ("Lucky Finder", "Везунчик"), ("+1 luck (better loot).", "+1 удачи (лучше добыча)."), [attr("minecraft:generic.luck", 1)], ["exploration/fleet_foot"], icon="minecraft:emerald"),
    node("exploration/mountaineer", "exploration", 7, 1, 8, ("Mountaineer", "Скалолаз"), ("Step over one block high obstacles.", "Перешагивает препятствия высотой в блок."), [attr("forge:step_height_addition", 0.5)], ["exploration/fleet_foot"], icon="minecraft:stone"),
    node("exploration/treasure_hunter", "exploration", 6, 2, 10, ("Treasure Hunter", "Охотник за сокровищами"), ("+1 luck.", "+1 удачи."), [attr("minecraft:generic.luck", 1)], ["exploration/lucky_finder"], cost=2, icon="minecraft:chest"),
    node("exploration/far_reach", "exploration", 7, 2, 12, ("Far Reach", "Длинные руки"), ("+1 block reach.", "+1 к дальности взаимодействия."), [attr("forge:block_reach", 1)], ["exploration/mountaineer"], icon="minecraft:fishing_rod"),
    node("exploration/swimmer", "exploration", 7, 3, 10, ("Swimmer", "Пловец"), ("+30% swim speed.", "+30% скорости плавания."), [attr("forge:swim_speed", 0.3, "multiply_base")], ["exploration/far_reach"], icon="minecraft:cod"),
    # social (x 8-9)
    node("social/friendly_face", "social", 8, 0, 2, ("Friendly Face", "Приветливое лицо"), ("+1 luck.", "+1 удачи."), [attr("minecraft:generic.luck", 1)], icon="minecraft:cake"),
    node("social/diplomat", "social", 8, 1, 6, ("Diplomat", "Дипломат"), ("Reputation gains doubled, losses halved.", "Прирост репутации удвоен, потери вдвое меньше."), [flag("diplomat")], ["social/friendly_face"], cost=2, icon="minecraft:writable_book"),
    node("social/mana_affinity", "social", 9, 1, 8, ("Mana Affinity", "Сродство с маной"), ("+10 max mana.", "+10 к максимуму маны."), [attr("rpgmod:max_mana", 10)], ["social/friendly_face"], icon="minecraft:lapis_lazuli"),
    node("social/scholar", "social", 9, 2, 12, ("Scholar", "Учёный"), ("+1 mana regeneration.", "+1 к восстановлению маны."), [attr("rpgmod:mana_regen", 1)], ["social/mana_affinity"], icon="minecraft:enchanted_book"),
    # fire (x 11-12)
    node("fire/kindling", "fire", 11, 0, 3, ("Kindling", "Растопка"), ("+5% spell power.", "+5% силы заклинаний."), [{"type": "spell_power", "school": "fire", "amount": 0.05}], icon="minecraft:blaze_powder"),
    node("fire/fire_nova", "fire", 11, 1, 5, ("Fire Nova", "Огненная нова"), ("Unlocks Fire Nova.", "Открывает Огненную нову."), [spell_unlock("fire_nova")], ["fire/kindling"], cost=2, icon="minecraft:fire_charge"),
    node("fire/flame_burst", "fire", 12, 1, 8, ("Flame Burst", "Всплеск пламени"), ("Unlocks Flame Burst.", "Открывает Всплеск пламени."), [spell_unlock("flame_burst")], ["fire/kindling"], cost=2, icon="minecraft:magma_cream"),
    node("fire/inferno", "fire", 11, 2, 12, ("Inferno", "Инферно"), ("Unlocks Inferno.", "Открывает Инферно."), [spell_unlock("inferno")], ["fire/fire_nova"], cost=3, icon="minecraft:lava_bucket"),
    node("fire/pyromancer", "fire", 12, 2, 14, ("Pyromancer", "Пиромант"), ("+10% spell power.", "+10% силы заклинаний."), [{"type": "spell_power", "school": "fire", "amount": 0.10}], ["fire/flame_burst"], cost=2, icon="minecraft:blaze_rod"),
    # frost (x 13-14)
    node("frost/cold_focus", "frost", 13, 0, 3, ("Cold Focus", "Холодный фокус"), ("-5% mana cost.", "-5% стоимости маны."), [{"type": "mana_cost", "amount": 0.05}], icon="minecraft:snowball"),
    node("frost/frost_nova", "frost", 13, 1, 5, ("Frost Nova", "Ледяная нова"), ("Unlocks Frost Nova.", "Открывает Ледяную нову."), [spell_unlock("frost_nova")], ["frost/cold_focus"], cost=2, icon="minecraft:packed_ice"),
    node("frost/ice_armor", "frost", 14, 1, 8, ("Ice Armor", "Ледяная броня"), ("Unlocks Ice Armor.", "Открывает Ледяную броню."), [spell_unlock("ice_armor")], ["frost/cold_focus"], cost=2, icon="minecraft:blue_ice"),
    node("frost/blizzard", "frost", 13, 2, 12, ("Blizzard", "Метель"), ("Unlocks Blizzard.", "Открывает Метель."), [spell_unlock("blizzard")], ["frost/frost_nova"], cost=3, icon="minecraft:powder_snow_bucket"),
    node("frost/deep_freeze", "frost", 14, 2, 14, ("Deep Freeze", "Глубокая заморозка"), ("-10% mana cost.", "-10% стоимости маны."), [{"type": "mana_cost", "amount": 0.10}], ["frost/ice_armor"], cost=2, icon="minecraft:ice"),
    # lightning (x 15-16)
    node("lightning/quick_mind", "lightning", 15, 0, 3, ("Quick Mind", "Быстрый ум"), ("-5% spell cooldowns.", "-5% перезарядки заклинаний."), [{"type": "cooldown", "amount": 0.05}], icon="minecraft:lightning_rod"),
    node("lightning/lightning_bolt", "lightning", 15, 1, 5, ("Lightning Bolt", "Удар молнии"), ("Unlocks Lightning Bolt.", "Открывает Удар молнии."), [spell_unlock("lightning_bolt")], ["lightning/quick_mind"], cost=2, icon="minecraft:copper_ingot"),
    node("lightning/blink", "lightning", 16, 1, 6, ("Blink", "Скачок"), ("Unlocks Blink.", "Открывает Скачок."), [spell_unlock("blink")], ["lightning/quick_mind"], cost=2, icon="minecraft:ender_pearl"),
    node("lightning/static_charge", "lightning", 16, 2, 8, ("Static Charge", "Статический заряд"), ("Unlocks Static Charge.", "Открывает Статический заряд."), [spell_unlock("static_charge")], ["lightning/blink"], cost=2, icon="minecraft:redstone"),
    node("lightning/chain_lightning", "lightning", 15, 2, 10, ("Chain Lightning", "Цепная молния"), ("Unlocks Chain Lightning.", "Открывает Цепную молнию."), [spell_unlock("chain_lightning")], ["lightning/lightning_bolt"], cost=3, icon="minecraft:chain"),
    node("lightning/storm_caller", "lightning", 15, 3, 15, ("Storm Caller", "Повелитель бурь"), ("-10% spell cooldowns.", "-10% перезарядки заклинаний."), [{"type": "cooldown", "amount": 0.10}], ["lightning/chain_lightning"], cost=2, icon="minecraft:trident"),
    # nature (x 17-18)
    node("nature/green_thumb", "nature", 17, 0, 3, ("Green Thumb", "Зелёный палец"), ("+10% healing power.", "+10% силы исцеления."), [attr("rpgmod:healing_power", 0.10)], icon="minecraft:oak_sapling"),
    node("nature/thorn_bolt", "nature", 17, 1, 3, ("Thorn Bolt", "Шип"), ("Unlocks Thorn Bolt.", "Открывает Шип."), [spell_unlock("thorn_bolt")], ["nature/green_thumb"], icon="minecraft:sweet_berries"),
    node("nature/entangle", "nature", 18, 1, 4, ("Entangle", "Опутывание"), ("Unlocks Entangle.", "Открывает Опутывание."), [spell_unlock("entangle")], ["nature/green_thumb"], cost=2, icon="minecraft:vine"),
    node("nature/regrowth", "nature", 17, 2, 6, ("Regrowth", "Возрождение"), ("Unlocks Regrowth.", "Открывает Возрождение."), [spell_unlock("regrowth")], ["nature/thorn_bolt"], cost=2, icon="minecraft:glow_berries"),
    node("nature/bark_skin", "nature", 18, 2, 8, ("Bark Skin", "Дубовая кожа"), ("Unlocks Bark Skin.", "Открывает Дубовую кожу."), [spell_unlock("bark_skin")], ["nature/entangle"], cost=2, icon="minecraft:oak_log"),
    node("nature/druidic_wisdom", "nature", 17, 3, 12, ("Druidic Wisdom", "Мудрость друида"), ("+20% healing power, +10 max mana.", "+20% силы исцеления, +10 маны."), [attr("rpgmod:healing_power", 0.20), attr("rpgmod:max_mana", 10)], ["nature/regrowth"], cost=3, icon="minecraft:azalea"),
    # arcane (x 19-20): generic magic
    node("arcane/mana_well", "arcane", 19, 0, 3, ("Mana Well", "Источник маны"), ("+15 max mana.", "+15 к максимуму маны."), [attr("rpgmod:max_mana", 15)], icon="minecraft:amethyst_shard"),
    node("arcane/meditation", "arcane", 19, 1, 6, ("Meditation", "Медитация"), ("+1 mana regeneration.", "+1 к восстановлению маны."), [attr("rpgmod:mana_regen", 1)], ["arcane/mana_well"], icon="minecraft:glass_bottle"),
    node("arcane/arcane_power", "arcane", 20, 1, 9, ("Arcane Power", "Тайная сила"), ("+10% magic power.", "+10% силы магии."), [attr("rpgmod:magic_power", 0.10)], ["arcane/mana_well"], cost=2, icon="minecraft:end_crystal"),
    node("arcane/archmage", "arcane", 19, 2, 16, ("Archmage", "Архимаг"), ("+20% magic power, -10% mana cost. Mage set required.", "+20% силы магии, -10% стоимости маны. Нужен комплект мага."), [attr("rpgmod:magic_power", 0.20), {"type": "mana_cost", "amount": 0.10}], ["arcane/meditation", "arcane/arcane_power"], cost=3, icon="minecraft:nether_star", required_class="mage"),
]


def gen_skill_tree():
    for n in SKILL_NODES:
        out = {"branch": n["branch"], "x": n["x"], "y": n["y"], "cost": n["cost"], "required_level": n["required_level"],
               "parents": n["parents"], "icon": n["icon"], "effects": n["effects"]}
        if "required_class" in n:
            out["required_class"] = n["required_class"]
        write_json(os.path.join(DATA, "skill_tree", n["id"] + ".json"), out)


# =========================================================================================== spells
def gen_spells():
    for sid, (school, mana, cd, level, basic) in SPELLS.items():
        write_json(os.path.join(DATA, "spells", sid + ".json"), {"mana_cost": mana, "cooldown": cd, "required_level": level, "basic": basic})


# =========================================================================================== tags
def gen_tags():
    weapons = [f"rpgmod:{w}_{t}_rpg" for t in TIERS for w in WEAPONS] + [f"rpgmod:staff_{s}" for s in SCHOOLS]
    write_json(os.path.join(DATA, "tags", "items", "rpg_weapons.json"), {"replace": False, "values": weapons})
    write_json(os.path.join(DATA, "tags", "items", "staffs.json"), {"replace": False, "values": [f"rpgmod:staff_{s}" for s in SCHOOLS]})
    write_json(os.path.join(DATA, "tags", "items", "bolts.json"), {"replace": False, "values": [f"rpgmod:bolt_{b}" for b in BOLTS]})
    write_json(os.path.join(DATA, "tags", "items", "forging_materials.json"), {"replace": False, "values": [
        "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald", "minecraft:netherite_ingot",
        "minecraft:amethyst_shard", "minecraft:lapis_lazuli", "minecraft:redstone", "minecraft:quartz", "minecraft:copper_ingot"]})
    write_json(os.path.join(DATA, "tags", "blocks", "loot_chests.json"), {"replace": False, "values": ["rpgmod:loot_chest", "rpgmod:loot_chest_rare", "rpgmod:loot_chest_boss"]})
    # mineable
    write_json(os.path.join(MC_DATA, "tags", "blocks", "mineable", "axe.json"), {"replace": False, "values": ["rpgmod:loot_chest", "rpgmod:loot_chest_rare", "rpgmod:loot_chest_boss"]})
    write_json(os.path.join(MC_DATA, "tags", "blocks", "mineable", "pickaxe.json"), {"replace": False, "values": ["rpgmod:boss_spawner"]})
    # structure tag used by locate / documentation
    write_json(os.path.join(DATA, "tags", "worldgen", "structure", "dungeons.json"), {"replace": False, "values": [f"rpgmod:{d}" for d in DUNGEONS]})


# =========================================================================================== loot tables
def item_entry(name, weight=1, count=None, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    funcs = list(functions or [])
    if count:
        funcs.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
    if funcs:
        e["functions"] = funcs
    return e


def rpg_level(offset_min, offset_max, bonuses=None):
    f = {"function": "rpgmod:set_rpg_level", "offset_min": offset_min, "offset_max": offset_max}
    if bonuses:
        f["bonuses"] = {"type": "minecraft:uniform", "min": bonuses[0], "max": bonuses[1]}
    return f


def weapon_entries(tiers, weight_per_tier, offset=(0, 2)):
    entries = []
    for tier, weight in zip(tiers, weight_per_tier):
        for w in WEAPONS:
            entries.append(item_entry(f"rpgmod:{w}_{tier}_rpg", weight, functions=[rpg_level(*offset)]))
    return entries


def armor_entries(weight=1):
    return [item_entry(f"rpgmod:{c}_{p}", weight) for c in CLASSES for p in PIECES]


def consumables(scale=1):
    return [
        item_entry("minecraft:emerald", 10 * scale, (1, 4)),
        item_entry("minecraft:bread", 6 * scale, (1, 3)),
        item_entry("minecraft:golden_apple", 2 * scale),
        item_entry("minecraft:arrow", 6 * scale, (4, 12)),
        item_entry("rpgmod:mana_potion", 4 * scale, (1, 2)),
        item_entry("rpgmod:arrow_fire", 3 * scale, (4, 8)),
        item_entry("rpgmod:arrow_frost", 3 * scale, (4, 8)),
        item_entry("rpgmod:arrow_poison", 2 * scale, (4, 8)),
        item_entry("rpgmod:bolt_basic", 4 * scale, (6, 12)),
        item_entry("rpgmod:bolt_fire", 2 * scale, (4, 8)),
        item_entry("rpgmod:bomb_basic", 3 * scale, (1, 3)),
        item_entry("rpgmod:bomb_fire", 2 * scale, (1, 2)),
        item_entry("rpgmod:bomb_smoke", 2 * scale, (1, 2)),
        item_entry("minecraft:iron_ingot", 4 * scale, (1, 3)),
        item_entry("minecraft:gold_ingot", 3 * scale, (1, 2)),
        item_entry("minecraft:lapis_lazuli", 3 * scale, (2, 5)),
    ]


def pool(rolls, entries, bonus_rolls=0):
    p = {"rolls": rolls if isinstance(rolls, int) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}
    if bonus_rolls:
        p["bonus_rolls"] = bonus_rolls
    return p


def gen_loot_tables():
    chests = os.path.join(DATA, "loot_tables", "chests")
    # default tiers (used by placed chests without a loot table)
    write_json(os.path.join(chests, "default_common.json"), {"type": "minecraft:chest", "pools": [
        pool((1, 2), consumables()),
        pool(1, weapon_entries(["wood", "stone", "iron"], [6, 5, 3]) + [item_entry("minecraft:air", 20)] + armor_entries(1)),
        pool(1, [item_entry("minecraft:air", 30), item_entry("rpgmod:forging_template", 2), item_entry("rpgmod:scroll_fire", 1), item_entry("rpgmod:scroll_frost", 1), item_entry("rpgmod:scroll_lightning", 1), item_entry("rpgmod:scroll_nature", 1)]),
    ]})
    write_json(os.path.join(chests, "default_rare.json"), {"type": "minecraft:chest", "pools": [
        pool((2, 3), consumables()),
        pool(1, weapon_entries(["stone", "iron", "gold", "diamond"], [2, 5, 3, 2], (1, 4)) + armor_entries(2)),
        pool(1, [item_entry("minecraft:air", 8), item_entry("rpgmod:forging_template", 4), item_entry("rpgmod:staff_fire", 2), item_entry("rpgmod:staff_frost", 2),
                 item_entry("rpgmod:staff_lightning", 2), item_entry("rpgmod:staff_nature", 2), item_entry("rpgmod:scroll_fire", 2), item_entry("rpgmod:scroll_frost", 2),
                 item_entry("rpgmod:scroll_lightning", 2), item_entry("rpgmod:scroll_nature", 2)]),
    ]})
    write_json(os.path.join(chests, "default_boss.json"), {"type": "minecraft:chest", "pools": [
        pool((3, 4), consumables(2)),
        pool(2, weapon_entries(["iron", "gold", "diamond", "netherite", "rune"], [2, 2, 4, 2, 1], (3, 6)) + armor_entries(3)),
        pool(1, [item_entry("rpgmod:forging_template", 4), item_entry("minecraft:diamond", 6, (1, 3)), item_entry("minecraft:netherite_scrap", 2), item_entry("minecraft:enchanted_golden_apple", 1)]),
    ]})
    write_json(os.path.join(chests, "jackpot.json"), {"type": "minecraft:chest", "pools": [
        pool(1, weapon_entries(["diamond", "netherite", "rune"], [4, 3, 2], (5, 8))),
        pool(1, [item_entry("minecraft:diamond", 5, (3, 6)), item_entry("minecraft:emerald_block", 3), item_entry("minecraft:enchanted_golden_apple", 2), item_entry("minecraft:netherite_ingot", 1)]),
    ]})
    # dungeon specific
    tier_by_level = {5: ["wood", "stone", "iron"], 6: ["stone", "iron"], 7: ["stone", "iron", "gold"], 8: ["iron", "gold"], 9: ["iron", "gold", "diamond"],
                     10: ["iron", "diamond"], 12: ["diamond", "netherite"], 15: ["diamond", "netherite", "rune"]}
    for dungeon, (level, boss, underground) in DUNGEONS.items():
        tiers = tier_by_level[level]
        weights = [3] * len(tiers)
        write_json(os.path.join(chests, dungeon + ".json"), {"type": "minecraft:chest", "pools": [
            pool((1, 3), consumables()),
            pool(1, weapon_entries(tiers, weights, (0, 3)) + [item_entry("minecraft:air", 12)] + armor_entries(1)),
            pool(1, [item_entry("minecraft:air", 10), item_entry("rpgmod:forging_template", 2)] + [item_entry(f"rpgmod:scroll_{s}", 1) for s in SCHOOLS]),
        ]})
        write_json(os.path.join(chests, dungeon + "_boss.json"), {"type": "minecraft:chest", "pools": [
            pool((2, 4), consumables(2)),
            pool(2, weapon_entries(tiers + (["rune"] if level >= 12 else []), [3] * len(tiers) + ([1] if level >= 12 else []), (2, 5)) + armor_entries(2)),
            pool(1, [item_entry("rpgmod:forging_template", 3), item_entry("minecraft:diamond", 4, (1, 3))] + [item_entry(f"rpgmod:staff_{s}", 1) for s in SCHOOLS]),
        ]})
        # boss drops
        write_json(os.path.join(DATA, "loot_tables", "bosses", boss + ".json"), {"type": "minecraft:entity", "pools": [
            pool(1, weapon_entries(tiers[-2:] + (["rune"] if level >= 12 else []), [3, 3] + ([1] if level >= 12 else []), (3, 6))),
            pool(1, armor_entries(1)),
            pool((1, 2), [item_entry("minecraft:emerald", 5, (4, 9)), item_entry("minecraft:diamond", 3, (1, 2)), item_entry("rpgmod:mana_potion", 3, (1, 3)),
                          item_entry("rpgmod:skill_book", 1), item_entry("rpgmod:forging_template", 2)]),
        ]})
    # block drops: loot chests drop nothing (loot is claimed through the casino), boss altar drops itself
    for block in ["loot_chest", "loot_chest_rare", "loot_chest_boss"]:
        write_json(os.path.join(DATA, "loot_tables", "blocks", block + ".json"), {"type": "minecraft:block", "pools": []})
    write_json(os.path.join(DATA, "loot_tables", "blocks", "boss_spawner.json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "rpgmod:boss_spawner"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # vanilla injections: add mod loot into vanilla chests (small chance)
    for vanilla in ["simple_dungeon", "abandoned_mineshaft", "stronghold_corridor", "desert_pyramid", "jungle_temple", "pillager_outpost", "ancient_city", "woodland_mansion"]:
        write_json(os.path.join(DATA, "loot_tables", "inject", vanilla + ".json"), {"type": "minecraft:chest", "pools": [
            pool(1, [item_entry("minecraft:air", 6), item_entry("rpgmod:forging_template", 1), item_entry("rpgmod:mana_potion", 2, (1, 2)),
                     item_entry("rpgmod:scroll_fire", 1), item_entry("rpgmod:scroll_nature", 1)] + weapon_entries(["stone", "iron"], [1, 1]))]})


# =========================================================================================== recipes
VANILLA_RECIPES = {
    "bow": {"type": "minecraft:crafting_shaped", "pattern": [" #X", "# X", " #X"], "key": {"#": {"item": "minecraft:stick"}, "X": {"item": "minecraft:string"}}, "result": {"item": "minecraft:bow"}},
    "crossbow": {"type": "minecraft:crafting_shaped", "pattern": ["~#~", "%X%", " ~ "], "key": {"~": {"item": "minecraft:stick"}, "#": {"item": "minecraft:iron_ingot"}, "%": {"item": "minecraft:string"}, "X": {"item": "minecraft:tripwire_hook"}}, "result": {"item": "minecraft:crossbow"}},
    "wooden_sword": {"type": "minecraft:crafting_shaped", "pattern": ["X", "X", "#"], "key": {"#": {"item": "minecraft:stick"}, "X": {"tag": "minecraft:planks"}}, "result": {"item": "minecraft:wooden_sword"}},
    "stone_sword": {"type": "minecraft:crafting_shaped", "pattern": ["X", "X", "#"], "key": {"#": {"item": "minecraft:stick"}, "X": {"tag": "minecraft:stone_tool_materials"}}, "result": {"item": "minecraft:stone_sword"}},
    "iron_sword": {"type": "minecraft:crafting_shaped", "pattern": ["X", "X", "#"], "key": {"#": {"item": "minecraft:stick"}, "X": {"item": "minecraft:iron_ingot"}}, "result": {"item": "minecraft:iron_sword"}},
    "golden_sword": {"type": "minecraft:crafting_shaped", "pattern": ["X", "X", "#"], "key": {"#": {"item": "minecraft:stick"}, "X": {"item": "minecraft:gold_ingot"}}, "result": {"item": "minecraft:golden_sword"}},
    "diamond_sword": {"type": "minecraft:crafting_shaped", "pattern": ["X", "X", "#"], "key": {"#": {"item": "minecraft:stick"}, "X": {"item": "minecraft:diamond"}}, "result": {"item": "minecraft:diamond_sword"}},
}


def gen_recipes():
    write_json(os.path.join(DATA, "recipes", "forging.json"), {"type": "rpgmod:forging"})
    write_json(os.path.join(DATA, "recipes", "mana_potion.json"), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "minecraft:lapis_lazuli"}, {"item": "minecraft:glistering_melon_slice"}],
        "result": {"item": "rpgmod:mana_potion", "count": 1}})
    write_json(os.path.join(DATA, "recipes", "relations_map.json"), {
        "type": "minecraft:crafting_shaped", "pattern": ["PPP", "PEP", "PPP"],
        "key": {"P": {"item": "minecraft:paper"}, "E": {"item": "minecraft:emerald"}},
        "result": {"item": "rpgmod:relations_map"}})
    write_json(os.path.join(DATA, "recipes", "skill_book.json"), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:book"}, {"item": "minecraft:lapis_lazuli"}, {"item": "minecraft:experience_bottle"}],
        "result": {"item": "rpgmod:skill_book"}})
    write_json(os.path.join(DATA, "recipes", "bomb_basic.json"), {
        "type": "minecraft:crafting_shaped", "pattern": [" S ", "GPG", " G "],
        "key": {"S": {"item": "minecraft:string"}, "G": {"item": "minecraft:gunpowder"}, "P": {"item": "minecraft:paper"}},
        "result": {"item": "rpgmod:bomb_basic", "count": 2}})
    for kind, extra in {"fire": "minecraft:blaze_powder", "frost": "minecraft:packed_ice", "lightning": "minecraft:copper_ingot", "poison": "minecraft:spider_eye", "smoke": "minecraft:charcoal"}.items():
        write_json(os.path.join(DATA, "recipes", f"bomb_{kind}.json"), {
            "type": "minecraft:crafting_shapeless", "ingredients": [{"item": "rpgmod:bomb_basic"}, {"item": extra}],
            "result": {"item": f"rpgmod:bomb_{kind}", "count": 1}})
    arrow_extra = {"fire": "minecraft:blaze_powder", "frost": "minecraft:snowball", "explosive": "minecraft:gunpowder", "poison": "minecraft:spider_eye",
                   "piercing": "minecraft:iron_nugget", "glowing": "minecraft:glowstone_dust", "lightning": "minecraft:copper_ingot"}
    write_json(os.path.join(DATA, "recipes", "arrow_basic.json"), {
        "type": "minecraft:crafting_shaped", "pattern": ["F", "S", "W"],
        "key": {"F": {"item": "minecraft:flint"}, "S": {"item": "minecraft:stick"}, "W": {"item": "minecraft:feather"}},
        "result": {"item": "rpgmod:arrow_basic", "count": 8}})
    write_json(os.path.join(DATA, "recipes", "bolt_basic.json"), {
        "type": "minecraft:crafting_shaped", "pattern": ["I", "S", "W"],
        "key": {"I": {"item": "minecraft:iron_nugget"}, "S": {"item": "minecraft:stick"}, "W": {"item": "minecraft:feather"}},
        "result": {"item": "rpgmod:bolt_basic", "count": 8}})
    for a in ARROWS[1:]:
        write_json(os.path.join(DATA, "recipes", f"arrow_{a}.json"), {
            "type": "minecraft:crafting_shaped", "pattern": ["AAA", "AXA", "AAA"],
            "key": {"A": {"item": "rpgmod:arrow_basic"}, "X": {"item": arrow_extra[a]}},
            "result": {"item": f"rpgmod:arrow_{a}", "count": 8}})
    for b in BOLTS[1:]:
        write_json(os.path.join(DATA, "recipes", f"bolt_{b}.json"), {
            "type": "minecraft:crafting_shaped", "pattern": ["BBB", "BXB", "BBB"],
            "key": {"B": {"item": "rpgmod:bolt_basic"}, "X": {"item": arrow_extra[b]}},
            "result": {"item": f"rpgmod:bolt_{b}", "count": 8}})
    # vanilla weapon recipes are only available when lootOnlyFromChests=false
    for name, recipe in VANILLA_RECIPES.items():
        r = dict(recipe)
        r["conditions"] = [{"type": "rpgmod:config", "key": "lootOnlyFromChests", "value": False}]
        write_json(os.path.join(MC_DATA, "recipes", name + ".json"), r)


# =========================================================================================== forge data
def gen_forge_data():
    # remove vanilla monster rooms (dungeons) from the overworld
    write_json(os.path.join(DATA, "forge", "biome_modifier", "remove_vanilla_dungeons.json"), {
        "type": "forge:remove_features", "biomes": "#minecraft:is_overworld",
        "features": ["minecraft:monster_room", "minecraft:monster_room_deep"], "steps": ["underground_structures"]})
    # global loot modifiers are not used; vanilla chest injection is done with loot table "inject" pools through GLM-free approach:
    # the mod's LootChestBlockEntity only reads its own tables, injection tables are documented for datapack authors.


def main():
    gen_models()
    gen_sounds()
    gen_skill_tree()
    gen_spells()
    gen_tags()
    gen_loot_tables()
    gen_recipes()
    gen_forge_data()
    en, ru = build_lang()
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), en)
    write_json(os.path.join(ASSETS, "lang", "ru_ru.json"), ru)
    print(f"resources generated: {len(en)} lang keys, {len(SKILL_NODES)} skill nodes")


if __name__ == "__main__":
    main()
