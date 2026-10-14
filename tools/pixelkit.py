"""Tiny pixel-art helpers shared by the texture generators (Pillow based)."""
from PIL import Image, ImageDraw
import math
import random


def rgb(hex_value, alpha=255):
    return ((hex_value >> 16) & 255, (hex_value >> 8) & 255, hex_value & 255, alpha)


def shade(color, factor):
    """Multiplies RGB by factor (keeps alpha)."""
    r, g, b, a = color
    return (max(0, min(255, int(r * factor))), max(0, min(255, int(g * factor))), max(0, min(255, int(b * factor))), a)


def mix(c1, c2, t):
    return tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(4))


class Canvas:
    def __init__(self, w=16, h=16, bg=(0, 0, 0, 0)):
        self.w = w
        self.h = h
        self.img = Image.new("RGBA", (w, h), bg)
        self.px = self.img.load()

    def inside(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def set(self, x, y, color):
        if self.inside(int(x), int(y)):
            self.px[int(x), int(y)] = color

    def get(self, x, y):
        if self.inside(x, y):
            return self.px[x, y]
        return (0, 0, 0, 0)

    def line(self, x0, y0, x1, y1, color, thick=1):
        steps = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(steps + 1):
            t = i / steps
            x = round(x0 + (x1 - x0) * t)
            y = round(y0 + (y1 - y0) * t)
            for dx in range(thick):
                for dy in range(thick):
                    self.set(x + dx - thick // 2, y + dy - thick // 2, color)

    def rect(self, x0, y0, x1, y1, color):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                self.set(x, y, color)

    def frame(self, x0, y0, x1, y1, color):
        self.line(x0, y0, x1, y0, color)
        self.line(x0, y1, x1, y1, color)
        self.line(x0, y0, x0, y1, color)
        self.line(x1, y0, x1, y1, color)

    def disc(self, cx, cy, r, color):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for y in range(int(cy - r - 1), int(cy + r + 2)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + 0.3:
                    self.set(x, y, color)

    def diamond(self, cx, cy, r, color):
        for x in range(cx - r, cx + r + 1):
            for y in range(cy - r, cy + r + 1):
                if abs(x - cx) + abs(y - cy) <= r:
                    self.set(x, y, color)

    def triangle(self, p0, p1, p2, color):
        """Filled triangle with integer vertices."""
        xs = [p0[0], p1[0], p2[0]]
        ys = [p0[1], p1[1], p2[1]]
        for x in range(min(xs), max(xs) + 1):
            for y in range(min(ys), max(ys) + 1):
                if _in_triangle((x + 0.5, y + 0.5), p0, p1, p2):
                    self.set(x, y, color)

    def outline(self, color, diagonal=False):
        """Draws an outline around every opaque pixel on transparent neighbours."""
        src = self.img.copy().load()
        offsets = [(1, 0), (-1, 0), (0, 1), (0, -1)]
        if diagonal:
            offsets += [(1, 1), (-1, -1), (1, -1), (-1, 1)]
        for x in range(self.w):
            for y in range(self.h):
                if src[x, y][3] == 0:
                    for dx, dy in offsets:
                        nx, ny = x + dx, y + dy
                        if self.inside(nx, ny) and src[nx, ny][3] != 0:
                            self.set(x, y, color)
                            break

    def noise(self, x0, y0, x1, y1, colors, seed=0, density=0.35):
        rnd = random.Random(seed)
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                if rnd.random() < density:
                    self.set(x, y, rnd.choice(colors))

    def darken_edges(self, x0, y0, x1, y1, factor=0.75):
        for x in range(x0, x1 + 1):
            for y in (y0, y1):
                c = self.get(x, y)
                if c[3]:
                    self.set(x, y, shade(c, factor))
        for y in range(y0, y1 + 1):
            for x in (x0, x1):
                c = self.get(x, y)
                if c[3]:
                    self.set(x, y, shade(c, factor))

    def paste(self, other, x, y):
        self.img.alpha_composite(other.img, (x, y))

    def save(self, path):
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


def _in_triangle(p, a, b, c):
    def sign(p1, p2, p3):
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1])
    d1 = sign(p, a, b)
    d2 = sign(p, b, c)
    d3 = sign(p, c, a)
    has_neg = d1 < 0 or d2 < 0 or d3 < 0
    has_pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (has_neg and has_pos)
