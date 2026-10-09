#!/usr/bin/env python3
"""Draws the Iron Man ability icon (64x64, panel art style): Tony's face
turning into the Mark 50 helmet behind a cyan nano front.

Own work. Run: python3 tools/assets/make_ironman_icons.py TONY.png MARK50.png OUT.png
(the skins are the decoded ironman.png and ironman_mark_50.png).
"""
import math
import random
import sys

from PIL import Image

random.seed(50)


def face(skin):
    """Front of the head: base 8x8 at (8,8) with the hat layer (40,8) on top."""
    base = skin.crop((8, 8, 16, 16)).convert("RGBA")
    hat = skin.crop((40, 8, 48, 16)).convert("RGBA")
    base.alpha_composite(hat)
    return base


def main(tony_path, suit_path, out_path):
    tony = face(Image.open(tony_path))
    suit = face(Image.open(suit_path))
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    # Background: dark navy with a cyan radial glow behind the head.
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 32, y - 30) / 40.0
            glow = max(0.0, 1.0 - d) ** 2
            band = 1.0 if (x + y) % 16 < 2 else 0.0
            r = int(16 + 30 * glow + 6 * band)
            g = int(22 + 90 * glow + 8 * band)
            b = int(40 + 120 * glow + 10 * band)
            px[x, y] = (r, g, b, 255)
    # Head: 8x8 face scaled x5 (40 px), left half Tony, right half helmet.
    scale, ox, oy = 5, 12, 8
    for fy in range(8):
        for fx in range(8):
            src = suit if fx >= 4 else tony
            c = src.getpixel((fx, fy))
            if c[3] == 0:
                c = tony.getpixel((fx, fy))
            for dy in range(scale):
                for dx in range(scale):
                    px[ox + fx * scale + dx, oy + fy * scale + dy] = c[:3] + (255,)
    # Nano front: bright cyan seam with scale pixels spilling onto Tony's side.
    seam = ox + 4 * scale
    for y in range(oy - 2, oy + 8 * scale + 2):
        for w in (0, 1):
            px[seam - 1 + w, y] = (150, 240, 255, 255)
        if random.random() < 0.45:
            x = seam - 2 - random.randint(0, 6)
            px[x, y] = (90, 210, 255, 255)
    # Floating nanites around the head.
    for _ in range(26):
        a = random.uniform(0, 2 * math.pi)
        r = random.uniform(22, 30)
        x = int(32 + math.cos(a) * r)
        y = int(28 + math.sin(a) * r * 0.9)
        if 0 <= x < 63 and 0 <= y < 63:
            px[x, y] = (170, 245, 255, 255)
            if random.random() < 0.3:
                px[x + 1, y] = (90, 200, 255, 255)
    # Arc reactor below the chin.
    for y in range(52, 60):
        for x in range(28, 36):
            d = math.hypot(x - 31.5, y - 55.5)
            if d < 4.2:
                core = d < 2.0
                px[x, y] = (200, 250, 255, 255) if core else (90, 110, 130, 255)
    # Frame: 1 px dark border.
    for i in range(64):
        for x, y in ((i, 0), (i, 63), (0, i), (63, i)):
            px[x, y] = (8, 12, 22, 255)
    img.save(out_path)


if __name__ == "__main__":
    main(*sys.argv[1:4])
