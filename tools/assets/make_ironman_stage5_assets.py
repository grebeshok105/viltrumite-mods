#!/usr/bin/env python3
"""Iron Man Stage 5 art, own work (style of make_ironman_stage4_assets.py):
panel icons of the Hulkbuster kit (grab, jump slam, thruster hop).
Run: python3 tools/assets/make_ironman_stage5_assets.py [OUT_ROOT]
Default OUT_ROOT writes base64 files under viltrumitecore/src/main/binassets.
"""
import math
import os
import random
import sys

from PIL import ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_ironman_stage2_icons import (  # noqa: E402
    BIN, CYAN, CYAN_D, GOLD, RED, RED_D, STEEL, WHITE, background, disc, frame, write)

TEX = os.path.dirname(BIN)
ICON = "gui/ability/ironman/"
FIRE_W = (255, 245, 200, 255)
FIRE_O = (255, 150, 40, 255)
FIRE_R = (220, 50, 30, 255)


def gauntlet(d, x0, y0, scale=1.0):
    """Bulky Hulkbuster fist: red block with gold knuckle plates."""
    s = scale
    d.rounded_rectangle((x0, y0, x0 + 20 * s, y0 + 18 * s), radius=int(3 * s), fill=RED, outline=RED_D, width=1)
    for i in range(4):
        kx = x0 + (2 + i * 4.5) * s
        d.rectangle((kx, y0 + 1 * s, kx + 3.5 * s, y0 + 5 * s), fill=GOLD)
    d.rectangle((x0 + 3 * s, y0 + 10 * s, x0 + 17 * s, y0 + 12 * s), fill=RED_D)


def hulk_grab():
    img = background()
    d = ImageDraw.Draw(img)
    # A smaller enemy silhouette caught in a huge gauntlet.
    d.rectangle((36, 10, 46, 20), fill=(70, 80, 95, 255))
    d.rectangle((34, 20, 48, 40), fill=(70, 80, 95, 255))
    d.rectangle((34, 40, 39, 52), fill=(60, 68, 82, 255))
    d.rectangle((43, 40, 48, 52), fill=(60, 68, 82, 255))
    d.rounded_rectangle((8, 30, 30, 56), radius=4, fill=RED, outline=RED_D, width=1)
    d.rectangle((10, 32, 28, 36), fill=GOLD)
    for i in range(3):
        y = 22 + i * 7
        d.rounded_rectangle((24, y, 44, y + 5), radius=2, fill=RED, outline=RED_D, width=1)
        d.rectangle((40, y + 1, 43, y + 4), fill=GOLD)
    d.rounded_rectangle((20, 42, 34, 48), radius=2, fill=RED, outline=RED_D, width=1)
    for x, y in ((50, 14), (52, 26), (31, 8)):
        d.line((x - 2, y, x + 2, y), fill=CYAN)
        d.line((x, y - 2, x, y + 2), fill=CYAN)
    frame(img)
    return img


def hulk_slam():
    img = background()
    px = img.load()
    d = ImageDraw.Draw(img)
    cx, gy = 32, 48
    d.ellipse((10, 44, 54, 56), fill=(40, 30, 28, 255))
    for r, col in ((16, CYAN), (24, CYAN_D), (31, CYAN_D)):
        d.arc((cx - r, gy - r // 2 - 3, cx + r, gy + r // 2 + 3), start=195, end=345, fill=col, width=2 if r == 16 else 1)
    gauntlet(d, 21, 24, 1.1)
    d.rectangle((26, 4, 38, 24), fill=RED_D)
    d.rectangle((28, 6, 36, 22), fill=RED)
    d.rectangle((29, 12, 35, 14), fill=GOLD)
    rng = random.Random(7)
    for _ in range(30):
        a = rng.uniform(math.pi * 1.05, math.pi * 1.95)
        r = rng.uniform(12, 29)
        x, y = int(cx + math.cos(a) * r), int(gy - 3 + math.sin(a) * r * 0.5)
        if 0 <= x < 64 and 0 <= y < 64:
            px[x, y] = rng.choice((CYAN, WHITE))
    for x, y, r in ((8, 46, 4), (56, 46, 4), (14, 38, 3), (50, 38, 3)):
        disc(px, x, y, r, (150, 150, 160, 255))
    frame(img)
    return img


def hulk_hop():
    img = background()
    px = img.load()
    d = ImageDraw.Draw(img)
    # Bulky torso with small head, big arms, short legs.
    d.rounded_rectangle((20, 14, 44, 36), radius=4, fill=RED, outline=RED_D, width=1)
    disc(px, 32, 24, 4, CYAN)
    disc(px, 32, 24, 2, WHITE)
    d.rectangle((28, 6, 36, 14), fill=GOLD, outline=RED_D)
    d.rectangle((30, 9, 34, 10), fill=WHITE)
    d.rounded_rectangle((10, 16, 20, 34), radius=3, fill=RED, outline=RED_D, width=1)
    d.rounded_rectangle((44, 16, 54, 34), radius=3, fill=RED, outline=RED_D, width=1)
    d.rectangle((11, 30, 19, 36), fill=GOLD)
    d.rectangle((45, 30, 53, 36), fill=GOLD)
    d.rectangle((22, 36, 30, 44), fill=RED_D)
    d.rectangle((34, 36, 42, 44), fill=RED_D)
    rng = random.Random(9)
    for fx in (26, 38):
        for _ in range(28):
            x = fx + rng.uniform(-3, 3)
            n = rng.randint(6, 14)
            for s in range(n):
                t = s / n
                col = FIRE_W if t < 0.25 else FIRE_O if t < 0.65 else FIRE_R
                yy = 45 + s
                xx = int(x + rng.uniform(-0.6, 0.6))
                if 0 <= xx < 64 and 0 <= yy < 62:
                    px[xx, yy] = col
    for y in (48, 52, 56):
        d.line((4, y, 12, y - 3), fill=CYAN_D)
        d.line((52, y - 3, 60, y), fill=CYAN_D)
    frame(img)
    return img


def main(out_root):
    write(hulk_grab(), ICON + "hulk_grab.png", out_root)
    write(hulk_slam(), ICON + "hulk_slam.png", out_root)
    write(hulk_hop(), ICON + "hulk_hop.png", out_root)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else TEX)
