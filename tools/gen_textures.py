#!/usr/bin/env python3
"""Generates every texture of RPG Mod programmatically (plan section 21).

Run from the repository root:  python3 tools/gen_textures.py
Outputs go to src/main/resources/assets/rpgmod/textures/**. Also writes a contact sheet to build/texture_sheet.png
(for eyeballing) when --sheet is passed.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pixelkit import Canvas, rgb, shade, mix  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "rpgmod", "textures")
OUTLINE = (20, 14, 10, 255)

# ---------------------------------------------------------------------------------------- palettes
TIERS = {
    "wood": (0x9C6B3C, 0x5E3C1C, 0xC8955A),
    "stone": (0x8F8F8F, 0x575757, 0xB8B8B8),
    "iron": (0xD9D9D9, 0x8A8A8A, 0xFFFFFF),
    "gold": (0xF2C132, 0xA47A10, 0xFFF1A6),
    "diamond": (0x4FE3E3, 0x1C9FA8, 0xBDFFFF),
    "netherite": (0x4D4247, 0x2A2326, 0x7A6B71),
    "rune": (0x8F5CEA, 0x5429A6, 0xD2B7FF),
}
HANDLE = (rgb(0x6B4423), rgb(0x3F2812), rgb(0x8E5F33))
GRIP = (rgb(0x3A2A1A), rgb(0x1E1410), rgb(0x55402A))
STRING = rgb(0xE8E0C8)
ARROW_WOOD = rgb(0x8C6A3F)
FEATHER = rgb(0xEDEDED)

EFFECTS = {
    "basic": 0xBFBFBF,
    "fire": 0xFF6A1A,
    "frost": 0x8FDFFF,
    "explosive": 0xE03A2E,
    "poison": 0x5DBE3C,
    "piercing": 0xE5E5E5,
    "glowing": 0xFFF27A,
    "lightning": 0xFFF04C,
}
BOMBS = {
    "basic": 0x5A5A5A,
    "fire": 0xE25822,
    "frost": 0x7FD7FF,
    "lightning": 0xF8F32B,
    "poison": 0x4E9A2A,
    "smoke": 0xB0B0B0,
}
SCHOOLS = {
    "fire": 0xFF6A2A,
    "frost": 0x7FD7FF,
    "lightning": 0xFFF26B,
    "nature": 0x62C85A,
}
CLASSES = {
    "warrior": (0xB8B8C0, 0xA8362E),
    "mage": (0x6E3FB5, 0xE7C85A),
    "archer": (0x4F8A3C, 0x8C6A3F),
    "crossbowman": (0x2F5E3A, 0x5B5B5B),
    "rogue": (0x2E2436, 0x7A3BA8),
    "druid": (0x6B4A2A, 0x3FA68A),
    "spearman": (0xC9A227, 0x7A1F1F),
    "bomber": (0x5A2222, 0x8A8A8A),
}


def tier_colors(tier):
    base, dark, light = TIERS[tier]
    return rgb(base), rgb(dark), rgb(light)


def save(canvas, rel):
    canvas.save(os.path.join(TEX, rel))


# ---------------------------------------------------------------------------------------- weapons
def draw_sword(tier):
    base, dark, light = tier_colors(tier)
    c = Canvas()
    # blade: from (5,10) to (13,2)
    c.line(5, 10, 13, 2, base, 2)
    c.line(6, 10, 13, 3, dark, 1)
    c.line(5, 9, 12, 2, light, 1)
    c.set(14, 1, light)
    c.set(13, 1, base)
    c.set(14, 2, dark)
    # guard
    c.line(3, 9, 7, 13, HANDLE[1], 1)
    c.line(3, 8, 6, 11, HANDLE[2], 1)
    # grip + pommel
    c.line(2, 12, 4, 10, GRIP[0], 1)
    c.set(1, 14, HANDLE[1])
    c.set(2, 13, GRIP[1])
    if tier == "rune":
        for x, y in ((8, 7), (10, 5)):
            c.set(x, y, rgb(0x5FF9FF))
    if tier == "gold":
        c.set(9, 6, light)
    c.outline(OUTLINE)
    return c


def draw_mace(tier):
    base, dark, light = tier_colors(tier)
    c = Canvas()
    c.line(2, 13, 8, 7, HANDLE[0], 1)
    c.line(2, 14, 4, 12, GRIP[0], 1)
    c.set(1, 15, HANDLE[1])
    c.disc(10.5, 4.5, 3.2, base)
    c.disc(9.6, 3.6, 1.3, light)
    for x, y in ((10, 0), (14, 4), (10, 8), (6, 4), (7, 1), (13, 1), (13, 7)):
        c.set(x, y, dark)
    c.set(13, 6, dark)
    c.set(12, 7, dark)
    if tier == "rune":
        c.set(11, 5, rgb(0x5FF9FF))
        c.set(9, 6, rgb(0x5FF9FF))
    c.outline(OUTLINE)
    return c


def draw_spear(tier):
    base, dark, light = tier_colors(tier)
    c = Canvas()
    c.line(1, 14, 10, 5, HANDLE[0], 1)
    c.line(2, 14, 4, 12, GRIP[0], 1)
    # binding
    c.set(9, 6, GRIP[1])
    c.set(10, 6, GRIP[1])
    # head
    c.triangle((10, 5), (15, 0), (12, 3), base)
    c.line(10, 5, 14, 1, base, 2)
    c.line(11, 5, 14, 2, dark, 1)
    c.line(10, 4, 13, 1, light, 1)
    c.set(15, 0, light)
    if tier == "rune":
        c.set(12, 3, rgb(0x5FF9FF))
    c.outline(OUTLINE)
    return c


def draw_bow(tier, pull=None):
    """pull: None, 0, 1, 2."""
    base, dark, light = tier_colors(tier)
    c = Canvas()
    import math
    # stave: quarter circle centre (2,13) radius 11 bulging to the top-right
    pts = []
    for i in range(0, 91, 3):
        a = math.radians(-90 + i)
        pts.append((round(2 + 11 * math.cos(a)), round(13 + 11 * math.sin(a))))
    for x, y in pts:
        c.set(x, y, base)
    for x, y in pts[6:26]:
        c.set(x - 1, y, dark) if c.get(x - 1, y)[3] == 0 else None
    # limb tips & grip
    c.set(2, 2, dark)
    c.set(13, 13, dark)
    c.rect(9, 3, 10, 4, GRIP[0]) if False else None
    grip = pts[len(pts) // 2]
    c.set(grip[0], grip[1], GRIP[0])
    c.set(grip[0] - 1, grip[1] + 1, GRIP[0])
    c.set(grip[0] + 1, grip[1] - 1, GRIP[0])
    # string
    if pull is None:
        c.line(2, 3, 12, 13, STRING, 1)
    else:
        mid = {0: (7, 9), 1: (6, 10), 2: (4, 11)}[pull]
        c.line(2, 3, mid[0], mid[1], STRING, 1)
        c.line(mid[0], mid[1], 12, 13, STRING, 1)
        # nocked arrow pointing to the top-right
        c.line(mid[0] + 1, mid[1] - 1, 11, 4, ARROW_WOOD, 1)
        c.set(12, 3, rgb(0xBFBFBF))
        c.set(mid[0], mid[1] - 1, FEATHER)
        c.set(mid[0] + 1, mid[1], FEATHER)
    if tier == "rune":
        c.set(pts[10][0], pts[10][1], rgb(0x5FF9FF))
        c.set(pts[20][0], pts[20][1], rgb(0x5FF9FF))
    c.outline(OUTLINE)
    return c


def draw_crossbow(tier, state="idle"):
    """state: idle, pulling_0, pulling_1, pulling_2, arrow, firework."""
    base, dark, light = tier_colors(tier)
    c = Canvas()
    # stock from bottom-left to top-right
    c.line(2, 13, 10, 5, HANDLE[0], 2)
    c.line(3, 14, 10, 7, HANDLE[1], 1)
    c.set(1, 14, HANDLE[1])
    c.set(2, 15, HANDLE[1])
    # trigger
    c.set(5, 13, GRIP[1])
    c.set(6, 13, GRIP[1])
    # limbs perpendicular to the stock through (10,5): along (1,1)
    c.line(6, 1, 14, 9, base, 1)
    c.line(6, 2, 13, 9, dark, 1)
    c.set(6, 1, light)
    c.set(14, 9, light)
    # string
    if state == "idle":
        c.line(5, 2, 13, 10, STRING, 1)
    else:
        back = {"pulling_0": (8, 7), "pulling_1": (7, 8), "pulling_2": (6, 9)}.get(state, (6, 9))
        c.line(5, 2, back[0], back[1], STRING, 1)
        c.line(back[0], back[1], 13, 10, STRING, 1)
    if state in ("arrow", "firework"):
        # loaded projectile along the stock
        c.line(6, 9, 12, 3, ARROW_WOOD if state == "arrow" else rgb(0xC94A3B), 1)
        tip = rgb(0xBFBFBF) if state == "arrow" else rgb(0xE8D44D)
        c.set(13, 2, tip)
        c.set(12, 2, tip)
        c.set(13, 3, tip)
    if tier == "rune":
        c.set(9, 4, rgb(0x5FF9FF))
        c.set(11, 6, rgb(0x5FF9FF))
    c.outline(OUTLINE)
    return c


def draw_bomb(kind):
    color = rgb(BOMBS[kind])
    c = Canvas()
    c.disc(8, 9.5, 4.6, color)
    c.disc(6.5, 8, 1.4, mix(color, (255, 255, 255, 255), 0.45))
    c.disc(9.5, 11.5, 2.0, shade(color, 0.75))
    # neck + fuse
    c.rect(7, 4, 9, 5, GRIP[1])
    c.line(9, 3, 11, 1, rgb(0xD9C27A), 1)
    c.set(12, 0, rgb(0xFFE14D))
    c.set(12, 1, rgb(0xFF8A1A))
    if kind == "smoke":
        c.disc(8, 9.5, 4.6, rgb(0xA8A8A8))
        c.disc(6.5, 8, 1.4, rgb(0xDADADA))
        c.rect(7, 4, 9, 5, GRIP[1])
    if kind == "lightning":
        c.line(7, 7, 9, 11, rgb(0xFFFFFF), 1)
    if kind == "frost":
        c.set(8, 9, rgb(0xFFFFFF))
        c.set(7, 10, rgb(0xFFFFFF))
    if kind == "poison":
        c.set(8, 10, rgb(0x1F4D14))
        c.set(9, 9, rgb(0x1F4D14))
    c.outline(OUTLINE)
    return c


def draw_arrow(effect, bolt=False):
    color = rgb(EFFECTS[effect])
    c = Canvas()
    if bolt:
        c.line(4, 12, 11, 5, rgb(0x5E5E5E), 1)
        c.line(5, 12, 11, 6, rgb(0x3C3C3C), 1)
        head = ((11, 5), (14, 2), (13, 3))
        c.triangle((10, 5), (14, 1), (12, 3), rgb(0xD0D0D0))
        c.line(11, 4, 13, 2, color, 1)
        # fletching
        c.set(4, 11, color)
        c.set(5, 12, color)
        c.set(3, 12, color)
        c.set(4, 13, color)
    else:
        c.line(2, 13, 12, 3, ARROW_WOOD, 1)
        c.triangle((11, 4), (15, 0), (13, 2), rgb(0xD0D0D0))
        c.line(12, 3, 14, 1, color, 1)
        c.set(15, 0, color)
        for x, y in ((2, 12), (3, 13), (1, 13), (2, 14), (3, 11), (4, 12)):
            c.set(x, y, FEATHER if (x + y) % 2 == 0 else color)
    c.outline(OUTLINE)
    return c


def draw_staff(school):
    color = rgb(SCHOOLS[school])
    c = Canvas()
    c.line(1, 14, 10, 5, HANDLE[0], 1)
    c.line(2, 14, 4, 12, GRIP[0], 1)
    c.set(1, 15, HANDLE[1])
    # prongs
    c.set(9, 4, HANDLE[1])
    c.set(11, 6, HANDLE[1])
    c.set(9, 3, HANDLE[0])
    c.set(12, 6, HANDLE[0])
    # orb
    c.disc(12, 3, 2.1, color)
    c.set(11, 2, mix(color, (255, 255, 255, 255), 0.6))
    c.set(13, 4, shade(color, 0.7))
    # glow dots
    glow = (color[0], color[1], color[2], 140)
    for x, y in ((9, 1), (15, 3), (12, 0), (14, 6)):
        c.set(x, y, glow)
    c.outline(OUTLINE)
    return c


def draw_scroll(school):
    color = rgb(SCHOOLS[school])
    paper = rgb(0xEADFB8)
    c = Canvas()
    c.rect(4, 3, 12, 13, paper)
    c.rect(3, 2, 13, 3, shade(paper, 0.8))
    c.rect(3, 13, 13, 14, shade(paper, 0.8))
    c.rect(3, 2, 3, 14, shade(paper, 0.7))
    c.rect(13, 2, 13, 14, shade(paper, 0.7))
    # writing
    for y in (6, 8, 10):
        c.line(6, y, 10, y, rgb(0x6B5A3A), 1)
    # ribbon / seal
    c.rect(7, 11, 9, 12, color)
    c.set(8, 12, shade(color, 0.7))
    c.outline(OUTLINE)
    return c


# ---------------------------------------------------------------------------------------- armor icons
def draw_armor(cls, piece):
    main, accent = rgb(CLASSES[cls][0]), rgb(CLASSES[cls][1])
    dark = shade(main, 0.65)
    light = mix(main, (255, 255, 255, 255), 0.35)
    c = Canvas()
    if piece == "helmet":
        c.rect(3, 5, 12, 10, main)
        c.rect(4, 3, 11, 4, main)
        c.rect(5, 2, 10, 2, main)
        c.rect(3, 11, 4, 12, main)
        c.rect(11, 11, 12, 12, main)
        c.rect(6, 9, 9, 10, (0, 0, 0, 0))  # face opening
        c.rect(5, 3, 7, 3, light)
        c.line(8, 2, 8, 6, accent, 1)
    elif piece == "chestplate":
        c.rect(3, 3, 12, 12, main)
        c.rect(2, 3, 2, 7, main)
        c.rect(13, 3, 13, 7, main)
        c.rect(6, 3, 9, 4, (0, 0, 0, 0))  # neck
        c.rect(4, 13, 11, 13, dark)
        c.line(5, 4, 5, 11, light, 1)
        c.rect(7, 7, 8, 9, accent)
    elif piece == "leggings":
        c.rect(3, 3, 12, 5, main)
        c.rect(3, 6, 6, 13, main)
        c.rect(9, 6, 12, 13, main)
        c.rect(3, 3, 12, 3, accent)
        c.line(4, 6, 4, 12, light, 1)
        c.line(11, 6, 11, 12, dark, 1)
    else:  # boots
        c.rect(2, 7, 6, 10, main)
        c.rect(2, 11, 7, 13, dark)
        c.rect(9, 7, 13, 10, main)
        c.rect(8, 11, 13, 13, dark)
        c.rect(2, 7, 6, 7, accent)
        c.rect(9, 7, 13, 7, accent)
    c.outline(OUTLINE)
    return c


# ---------------------------------------------------------------------------------------- misc items
def draw_forging_template():
    c = Canvas()
    slate = rgb(0x3F5B66)
    c.rect(3, 2, 12, 13, slate)
    c.frame(3, 2, 12, 13, shade(slate, 0.7))
    pattern = rgb(0x8FE3D7)
    for x, y in ((5, 4), (6, 5), (7, 6), (8, 7), (9, 8), (10, 9), (10, 4), (5, 9), (7, 11), (8, 11)):
        c.set(x, y, pattern)
    c.outline(OUTLINE)
    return c


def draw_mana_potion():
    c = Canvas()
    glass = (200, 220, 255, 120)
    liquid = rgb(0x2E6BFF)
    c.rect(6, 1, 9, 2, rgb(0x8A6A3A))  # cork
    c.rect(6, 3, 9, 5, glass)
    c.rect(4, 6, 11, 13, glass)
    c.rect(5, 14, 10, 14, glass)
    c.rect(5, 8, 10, 13, liquid)
    c.rect(6, 14, 9, 14, shade(liquid, 0.8))
    c.set(5, 9, mix(liquid, (255, 255, 255, 255), 0.5))
    c.set(6, 10, mix(liquid, (255, 255, 255, 255), 0.4))
    c.outline(OUTLINE)
    return c


def draw_relations_map():
    c = Canvas()
    paper = rgb(0xE4D3A3)
    c.rect(1, 2, 14, 13, paper)
    c.frame(1, 2, 14, 13, shade(paper, 0.75))
    c.line(4, 5, 11, 10, rgb(0xC84A3F), 1)
    c.line(4, 5, 10, 4, rgb(0x5BBE55), 1)
    c.line(10, 4, 11, 10, rgb(0xD9A441), 1)
    for (x, y), col in (((4, 5), 0x5C8FD6), ((10, 4), 0x8C9BB0), ((11, 10), 0x9A3030)):
        c.rect(x - 1, y - 1, x + 1, y + 1, rgb(col))
    c.outline(OUTLINE)
    return c


def draw_skill_book():
    c = Canvas()
    cover = rgb(0x5A2E8C)
    c.rect(3, 2, 12, 13, cover)
    c.rect(4, 3, 12, 12, shade(cover, 1.15))
    c.rect(3, 2, 3, 13, shade(cover, 0.6))
    c.rect(12, 3, 12, 12, rgb(0xEADFB8))
    c.rect(11, 3, 11, 12, rgb(0xD4C79F))
    star = rgb(0xFFD966)
    for x, y in ((7, 5), (6, 7), (7, 7), (8, 7), (7, 9), (5, 8), (9, 8), (7, 6), (7, 8)):
        c.set(x, y, star)
    c.outline(OUTLINE)
    return c


# ---------------------------------------------------------------------------------------- blocks
CHEST_TRIM = {"common": 0x9C9C9C, "rare": 0xE6B422, "boss": 0xA04FE0}


def draw_chest(tier, face):
    wood = rgb(0x8A5A2B)
    dark = rgb(0x5A3A1A)
    trim = rgb(CHEST_TRIM[tier])
    c = Canvas()
    c.rect(0, 0, 15, 15, wood)
    c.noise(0, 0, 15, 15, [shade(wood, 0.9), shade(wood, 1.08)], seed=hash(tier + face) & 0xFFFF, density=0.3)
    c.frame(0, 0, 15, 15, dark)
    if face == "top":
        c.frame(1, 1, 14, 14, shade(wood, 1.1))
        c.rect(7, 2, 8, 13, trim)
        c.rect(2, 7, 13, 8, trim)
    elif face == "front":
        c.rect(0, 6, 15, 6, dark)  # lid seam
        c.rect(6, 4, 9, 8, trim)
        c.rect(7, 5, 8, 7, shade(trim, 0.6))
        if tier == "boss":
            c.rect(7, 5, 8, 6, rgb(0x5FF9FF))
        c.rect(1, 1, 1, 14, trim)
        c.rect(14, 1, 14, 14, trim)
    else:
        c.rect(0, 6, 15, 6, dark)
        c.rect(1, 1, 1, 14, trim)
        c.rect(14, 1, 14, 14, trim)
    return c


def draw_boss_spawner(top=False):
    c = Canvas()
    base = rgb(0x1C1620)
    c.rect(0, 0, 15, 15, base)
    c.noise(0, 0, 15, 15, [rgb(0x26202C), rgb(0x130F16)], seed=7 if top else 3, density=0.4)
    c.frame(0, 0, 15, 15, rgb(0x3A2F44))
    rune = rgb(0xD63A3A)
    glow = rgb(0xFF7A5A)
    if top:
        c.disc(7.5, 7.5, 4.2, rgb(0x2E1A1A))
        c.disc(7.5, 7.5, 2.2, rune)
        c.set(7, 7, glow)
        c.set(8, 8, glow)
    else:
        for x, y in ((4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (11, 4), (10, 5), (5, 10), (4, 11), (7, 3), (8, 12)):
            c.set(x, y, rune)
        c.set(7, 7, glow)
        c.set(8, 8, glow)
    return c


# ---------------------------------------------------------------------------------------- gui
def draw_panel(c, x0, y0, x1, y1):
    face = rgb(0xC6C6C6)
    c.rect(x0, y0, x1, y1, face)
    c.rect(x0, y0, x1, y0 + 1, rgb(0xFFFFFF))
    c.rect(x0, y0, x0 + 1, y1, rgb(0xFFFFFF))
    c.rect(x0, y1 - 1, x1, y1, rgb(0x555555))
    c.rect(x1 - 1, y0, x1, y1, rgb(0x555555))
    c.frame(x0, y0, x1, y1, rgb(0x000000))
    c.set(x0, y0, (0, 0, 0, 0))
    c.set(x1, y0, (0, 0, 0, 0))
    c.set(x0, y1, (0, 0, 0, 0))
    c.set(x1, y1, (0, 0, 0, 0))


def draw_slot(c, x, y, size=18):
    c.rect(x, y, x + size - 1, y + size - 1, rgb(0x8B8B8B))
    c.rect(x, y, x + size - 1, y, rgb(0x373737))
    c.rect(x, y, x, y + size - 1, rgb(0x373737))
    c.rect(x, y + size - 1, x + size - 1, y + size - 1, rgb(0xFFFFFF))
    c.rect(x + size - 1, y, x + size - 1, y + size - 1, rgb(0xFFFFFF))


def draw_casino_gui():
    c = Canvas(256, 256)
    draw_panel(c, 0, 0, 175, 189)
    # machine body
    body = rgb(0x8E2B2B)
    c.rect(8, 26, 167, 94, body)
    c.frame(8, 26, 167, 94, rgb(0x4A1212))
    c.rect(9, 27, 166, 27, rgb(0xC94848))
    # reel windows (3) at x = 38 + i*36, y = 30, 32x54
    for i in range(3):
        x = 38 + i * 36
        c.rect(x - 2, 28, x + 33, 85, rgb(0xE0C060))
        c.rect(x - 1, 29, x + 32, 84, rgb(0x1B1B1B))
    # lever
    c.rect(170, 30, 173, 70, rgb(0x6B6B6B))
    c.disc(171.5, 28, 3.5, rgb(0xD63A3A))
    # lights row
    for i in range(0, 160, 10):
        c.set(10 + i, 90, rgb(0xFFE14D) if (i // 10) % 2 == 0 else rgb(0x4DFF88))
    # reward slots at (16,118) step 18, 8 slots
    for i in range(8):
        draw_slot(c, 15 + i * 18, 117)
    # status strip
    c.rect(8, 96, 167, 112, rgb(0x2A2A2A))
    c.frame(8, 96, 167, 112, rgb(0x111111))
    return c


def draw_character_gui():
    c = Canvas(256, 256)
    draw_panel(c, 0, 0, 229, 199)
    c.rect(6, 42, 130, 43, rgb(0x8B8B8B))
    c.rect(136, 42, 223, 43, rgb(0x8B8B8B))
    c.rect(6, 145, 223, 146, rgb(0x8B8B8B))
    return c


def draw_skill_tree_gui():
    c = Canvas(256, 256)
    bg = rgb(0x151A24)
    c.rect(0, 0, 255, 255, bg)
    c.noise(0, 0, 255, 255, [rgb(0x1A2130), rgb(0x10141C)], seed=99, density=0.5)
    return c


def draw_hud():
    c = Canvas(256, 256)
    # mana background (0,0) 81x5, fill (0,5), xp background (0,10), fill (0,15)
    c.rect(0, 0, 80, 4, rgb(0x1A2238))
    c.frame(0, 0, 80, 4, rgb(0x0A0E18))
    c.rect(0, 5, 80, 9, rgb(0x3D7BFF))
    c.rect(0, 6, 80, 6, rgb(0x8FB7FF))
    c.rect(0, 9, 80, 9, rgb(0x1F47B0))
    c.rect(0, 10, 80, 14, rgb(0x2A2412))
    c.frame(0, 10, 80, 14, rgb(0x130F08))
    c.rect(0, 15, 80, 19, rgb(0xE0B23A))
    c.rect(0, 16, 80, 16, rgb(0xFFE59A))
    c.rect(0, 19, 80, 19, rgb(0x9A7418))
    return c


# ---------------------------------------------------------------------------------------- entities
def draw_arrow_entity():
    c = Canvas(32, 32)
    shaft = rgb(0x8C6A3F)
    for y in (0, 5):
        c.rect(0, y, 31, y + 4, (0, 0, 0, 0))
        c.rect(4, y + 2, 28, y + 2, shaft)
        c.rect(4, y + 1, 28, y + 1, shade(shaft, 1.15))
        c.rect(4, y + 3, 28, y + 3, shade(shaft, 0.8))
        c.rect(0, y + 1, 4, y + 3, rgb(0xD0D0D0))
        c.set(0, y + 2, rgb(0xFFFFFF))
        c.rect(27, y, 31, y + 4, rgb(0xEDEDED))
        c.rect(29, y, 31, y + 4, rgb(0xC94A3B))
    c.rect(0, 10, 4, 14, rgb(0xEDEDED))
    c.rect(1, 11, 3, 13, rgb(0xC94A3B))
    return c


def draw_spell_orb(school):
    color = rgb(SCHOOLS[school])
    c = Canvas(16, 16)
    for r, t in ((7.5, 0.25), (6.0, 0.55), (4.5, 0.85), (2.5, 1.0)):
        col = (color[0], color[1], color[2], int(255 * t))
        c.disc(7.5, 7.5, r, col)
    c.disc(6.5, 6.5, 1.4, (255, 255, 255, 230))
    return c


def draw_armor_layer(cls, layer):
    """64x32 humanoid armor texture (vanilla layout)."""
    main, accent = rgb(CLASSES[cls][0]), rgb(CLASSES[cls][1])
    dark = shade(main, 0.7)
    light = mix(main, (255, 255, 255, 255), 0.25)
    c = Canvas(64, 32)

    def box(u, v, w, h, d, color):
        # top/bottom
        c.rect(u + d, v, u + d + w - 1, v + d - 1, light)
        c.rect(u + d + w, v, u + d + 2 * w - 1, v + d - 1, dark)
        # sides: right, front, left, back
        c.rect(u, v + d, u + d - 1, v + d + h - 1, dark)
        c.rect(u + d, v + d, u + d + w - 1, v + d + h - 1, color)
        c.rect(u + d + w, v + d, u + 2 * d + w - 1, v + d + h - 1, dark)
        c.rect(u + 2 * d + w, v + d, u + 2 * d + 2 * w - 1, v + d + h - 1, shade(color, 0.85))

    if layer == 1:
        box(0, 0, 8, 8, 8, main)        # head
        box(16, 16, 8, 12, 4, main)     # body
        box(40, 16, 4, 12, 4, main)     # arm
        box(0, 16, 4, 12, 4, main)      # leg (boots use the lower part)
        # accents: visor line, chest emblem, cuffs
        c.rect(8, 11, 15, 11, accent)
        c.rect(22, 22, 25, 25, accent)
        c.rect(44, 26, 47, 27, accent)
        c.rect(4, 26, 7, 27, accent)
    else:
        box(16, 16, 8, 12, 4, dark)     # belt region
        box(0, 16, 4, 12, 4, main)      # legs
        c.rect(20, 20, 27, 21, accent)
        c.rect(4, 22, 7, 22, accent)
    # knock out the pixels that vanilla leaves empty (keep simple: nothing else)
    return c


def draw_particle(kind):
    c = Canvas(8, 8)
    if kind == "mana_spark":
        col = rgb(0x6FA8FF)
        c.diamond(3, 3, 3, (col[0], col[1], col[2], 180))
        c.diamond(3, 3, 1, (255, 255, 255, 255))
    elif kind == "casino_sparkle":
        col = rgb(0xFFE14D)
        c.line(3, 0, 3, 7, col, 1)
        c.line(0, 3, 7, 3, col, 1)
        c.diamond(3, 3, 1, (255, 255, 255, 255))
    else:
        col = rgb(0x62C85A)
        c.disc(3.5, 3.5, 2.6, col)
        c.line(1, 6, 6, 1, shade(col, 0.6), 1)
    return c


def draw_logo():
    c = Canvas(128, 128)
    shield = rgb(0x2B3A55)
    c.disc(64, 60, 50, shield)
    c.rect(14, 20, 113, 60, shield)
    c.disc(64, 64, 42, rgb(0x3B4F73))
    # sword
    c.line(34, 94, 94, 34, rgb(0xE8E8E8), 6)
    c.line(36, 94, 94, 36, rgb(0xB8B8B8), 2)
    c.line(28, 66, 62, 100, rgb(0xC9A227), 6)
    c.line(26, 102, 40, 88, rgb(0x5A3A1A), 6)
    # rune glow
    c.disc(64, 64, 6, rgb(0x5FF9FF))
    return c


# ---------------------------------------------------------------------------------------- driver
def main():
    sheet = []
    tiers = list(TIERS.keys())
    for tier in tiers:
        save(draw_sword(tier), f"item/sword_{tier}_rpg.png")
        save(draw_mace(tier), f"item/mace_{tier}_rpg.png")
        save(draw_spear(tier), f"item/spear_{tier}_rpg.png")
        save(draw_bow(tier), f"item/bow_{tier}_rpg.png")
        for p in range(3):
            save(draw_bow(tier, p), f"item/bow_{tier}_rpg_pulling_{p}.png")
        save(draw_crossbow(tier), f"item/crossbow_{tier}_rpg.png")
        for p in range(3):
            save(draw_crossbow(tier, f"pulling_{p}"), f"item/crossbow_{tier}_rpg_pulling_{p}.png")
        save(draw_crossbow(tier, "arrow"), f"item/crossbow_{tier}_rpg_arrow.png")
        save(draw_crossbow(tier, "firework"), f"item/crossbow_{tier}_rpg_firework.png")
        sheet += [draw_sword(tier), draw_mace(tier), draw_spear(tier), draw_bow(tier), draw_bow(tier, 2), draw_crossbow(tier), draw_crossbow(tier, "arrow")]
    for kind in BOMBS:
        save(draw_bomb(kind), f"item/bomb_{kind}.png")
        sheet.append(draw_bomb(kind))
    for effect in ("basic", "fire", "frost", "explosive", "poison", "piercing", "glowing"):
        save(draw_arrow(effect), f"item/arrow_{effect}.png")
        sheet.append(draw_arrow(effect))
    for effect in ("basic", "fire", "frost", "poison", "piercing", "lightning"):
        save(draw_arrow(effect, bolt=True), f"item/bolt_{effect}.png")
        sheet.append(draw_arrow(effect, bolt=True))
    for school in SCHOOLS:
        save(draw_staff(school), f"item/staff_{school}.png")
        save(draw_scroll(school), f"item/scroll_{school}.png")
        save(draw_spell_orb(school), f"entity/spell/{school}.png")
        sheet += [draw_staff(school), draw_scroll(school)]
    for cls in CLASSES:
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            save(draw_armor(cls, piece), f"item/{cls}_{piece}.png")
            sheet.append(draw_armor(cls, piece))
        save(draw_armor_layer(cls, 1), f"models/armor/{cls}_layer_1.png")
        save(draw_armor_layer(cls, 2), f"models/armor/{cls}_layer_2.png")
    save(draw_forging_template(), "item/forging_template.png")
    save(draw_mana_potion(), "item/mana_potion.png")
    save(draw_relations_map(), "item/relations_map.png")
    save(draw_skill_book(), "item/skill_book.png")
    sheet += [draw_forging_template(), draw_mana_potion(), draw_relations_map(), draw_skill_book()]
    for tier in CHEST_TRIM:
        for face in ("top", "side", "front"):
            save(draw_chest(tier, face), f"block/loot_chest_{tier}_{face}.png")
        sheet.append(draw_chest(tier, "front"))
    save(draw_boss_spawner(False), "block/boss_spawner_side.png")
    save(draw_boss_spawner(True), "block/boss_spawner_top.png")
    sheet += [draw_boss_spawner(False), draw_boss_spawner(True)]
    save(draw_casino_gui(), "gui/casino.png")
    save(draw_character_gui(), "gui/character.png")
    save(draw_skill_tree_gui(), "gui/skill_tree.png")
    save(draw_hud(), "gui/hud.png")
    save(draw_arrow_entity(), "entity/projectiles/rpg_arrow.png")
    for kind in ("mana_spark", "casino_sparkle", "nature_leaf"):
        save(draw_particle(kind), f"particle/{kind}.png")
    draw_logo().save(os.path.join(ROOT, "src", "main", "resources", "rpgmod_logo.png"))

    if "--sheet" in sys.argv:
        cols = 16
        scale = 4
        rows = (len(sheet) + cols - 1) // cols
        out = Canvas(cols * 18 * scale, rows * 18 * scale, (40, 40, 40, 255))
        for i, tile in enumerate(sheet):
            big = tile.img.resize((16 * scale, 16 * scale), 0)
            out.img.alpha_composite(big, ((i % cols) * 18 * scale, (i // cols) * 18 * scale))
        os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
        out.img.save(os.path.join(ROOT, "build", "texture_sheet.png"))
    print("textures generated")


if __name__ == "__main__":
    main()
