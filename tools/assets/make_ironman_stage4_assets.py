#!/usr/bin/env python3
"""Iron Man Stage 4 art, own work (style of make_ironman_stage2_icons.py).

The suit interior skin, the Veronica pod icon and seven signature icons. The mark
skins come from the archive (tools/bake_suit_skin.py, tools/assets/convert_ironman_sind.py).

Run: python3 tools/assets/make_ironman_stage4_assets.py [OUT_ROOT]
Default OUT_ROOT writes base64 files under viltrumitecore/src/main/binassets.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_ironman_stage2_icons import (  # noqa: E402
    BIN, CYAN, CYAN_D, GOLD, RED, RED_D, STEEL, WHITE, background, disc, frame, ring, write)

TEX = os.path.dirname(BIN)
HERO = "entity/hero/"
ICON = "gui/ability/ironman/"

LIGHT = (217, 241, 255)
DIM = (60, 130, 210)
BLUE = (60, 150, 255)
BLUE_LIGHT = (150, 220, 255)
PINK = (255, 120, 220)
PINK_CORE = (255, 170, 240)
UNI = (150, 240, 255)
WHITE_RGB = (240, 250, 255)
FIRE_W = (255, 244, 190)
FIRE_Y = (255, 200, 90)
FIRE_O = (255, 130, 40)
FIRE_R = (190, 40, 30)
SILVER = (214, 220, 228)
SILVER_D = (96, 104, 118)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def put(px, x, y, col):
    if 0 <= x < 64 and 0 <= y < 64:
        px[x, y] = tuple(col) if len(col) == 4 else tuple(col) + (255,)


def interior():
    rng = random.Random(7)
    img = Image.new("RGBA", (64, 64), (24, 26, 32, 255))
    px = img.load()
    for y in range(64):
        for x in range(64):
            col = (38, 42, 52) if (x + y) % 8 == 0 or (x - y) % 8 == 0 else (24, 26, 32)
            if rng.random() < 0.08:
                col = tuple(v + 4 for v in col)
            px[x, y] = col + (255,)
    for k, (c0, amp, per) in enumerate(((14, 4, 18), (44, 5, 22), (30, 3, 14))):
        for x in range(64):
            y = int(round(c0 + amp * math.sin(2 * math.pi * x / per + k)))
            px[x, y] = (10, 11, 15, 255)
            px[x, y - 1] = (66, 70, 80, 255)
    for x0, y0, n in ((4, 6, 6), (40, 22, 8), (12, 52, 5), (48, 58, 6), (20, 36, 6)):
        for i in range(n):
            px[x0 + i, y0] = (46, 168, 210, 255)
    return img


def veronica():
    img = background()
    px = img.load()
    rng = random.Random(11)
    nose, tail = (17.0, 47.0), (41.0, 23.0)
    length = math.hypot(tail[0] - nose[0], tail[1] - nose[1])
    ux, uy = (tail[0] - nose[0]) / length, (tail[1] - nose[1]) / length
    for _ in range(70):
        sx = tail[0] + rng.uniform(-5, 5)
        sy = tail[1] + rng.uniform(-3, 3)
        dx = 0.62 + rng.uniform(-0.15, 0.15)
        dy = -0.78 + rng.uniform(-0.15, 0.15)
        n = rng.randint(8, 24)
        for s in range(n):
            t = s / n
            col = lerp(FIRE_W, FIRE_O, t * 2) if t < 0.5 else lerp(FIRE_O, FIRE_R, (t - 0.5) * 2)
            x, y = int(sx + dx * s), int(sy + dy * s)
            put(px, x, y, col)
            if t < 0.6:
                put(px, x + 1, y, col)
    radius = 6.2
    for y in range(64):
        for x in range(64):
            t = ((x - nose[0]) * ux + (y - nose[1]) * uy) / length
            perp = -(x - nose[0]) * uy + (y - nose[1]) * ux
            if t < -0.05 or t > 1.0 or abs(perp) > radius * min(1.0, 0.35 + t * 2.2):
                continue
            v = perp / radius
            if t < 0.12:
                col = lerp((150, 70, 30), FIRE_Y, 1 - t / 0.12)
            elif t < 0.78:
                col = tuple(int(c * (0.62 + 0.38 * -v)) for c in SILVER)
            elif t < 0.82:
                col = tuple(int(c * (0.7 + 0.3 * -v)) for c in GOLD[:3])
            elif t < 0.86:
                col = tuple(int(c * (0.7 + 0.3 * -v)) for c in RED[:3])
            else:
                col = tuple(int(c * (0.6 + 0.4 * -v)) for c in SILVER_D)
            put(px, x, y, col)
    for _ in range(22):
        a = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(4, 11)
        put(px, int(nose[0] + math.cos(a) * r), int(nose[1] + math.sin(a) * r), rng.choice((FIRE_W, FIRE_Y)))
    frame(img)
    return img


def sig_micro_laser():
    img = background()
    px = img.load()
    d = ImageDraw.Draw(img)
    tx, ty = 38, 20
    for sx, sy in ((12, 58), (52, 58)):
        d.line((sx, sy, tx, ty), fill=(255, 70, 70, 255), width=2)
        d.line((sx, sy, tx, ty), fill=(255, 210, 210, 255), width=1)
        d.rectangle((sx - 4, sy - 3, sx + 4, sy + 3), fill=RED_D)
        d.line((sx - 4, sy - 3, sx + 4, sy - 3), fill=GOLD, width=1)
    ring(px, tx, ty, 6.0, 7.2, RED, gaps=(45, 135, 225, 315))
    disc(px, tx, ty, 1.6, WHITE)
    frame(img)
    return img


def sig_rocket_fist():
    img = background()
    d = ImageDraw.Draw(img)
    for k in range(-2, 3):
        d.line((24, 38 + k, 3, 60 + k * 3), fill=(255, 150, 50, 255), width=3)
    d.line((24, 38, 5, 57), fill=(255, 236, 170, 255), width=1)
    d.rounded_rectangle((24, 16, 44, 38), radius=5, fill=GOLD, outline=RED_D, width=1)
    d.rounded_rectangle((36, 26, 48, 42), radius=4, fill=RED, outline=RED_D, width=1)
    for y in (22, 27, 32):
        d.line((28, y, 40, y), fill=RED_D, width=1)
    d.line((26, 18, 42, 18), fill=(255, 236, 170, 255), width=1)
    ring(img.load(), 53, 10, 5.0, 6.2, CYAN, gaps=(0, 90, 180, 270))
    frame(img)
    return img


def sig_camouflage():
    base = background()
    img = base.copy()
    px, src = img.load(), base.load()
    mask = Image.new("L", (64, 64), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse((27, 6, 37, 16), fill=255)
    md.rounded_rectangle((23, 18, 41, 42), radius=3, fill=255)
    md.rounded_rectangle((16, 19, 22, 40), radius=2, fill=255)
    md.rounded_rectangle((42, 19, 48, 40), radius=2, fill=255)
    md.rounded_rectangle((24, 40, 31, 58), radius=2, fill=255)
    md.rounded_rectangle((33, 40, 40, 58), radius=2, fill=255)
    mpx = mask.load()
    for y in range(64):
        for x in range(64):
            if not mpx[x, y]:
                continue
            sx = min(63, max(0, x + round(2.0 * math.sin(y * 0.9))))
            r, g, b, _ = src[sx, y]
            px[x, y] = (int(r + (150 - r) * 0.22), int(g + (240 - g) * 0.22), int(b + (255 - b) * 0.22), 255)
    for y in range(64):
        for x in range(64):
            if mpx[x, y] and any(not (0 <= x + dx < 64 and 0 <= y + dy < 64 and mpx[x + dx, y + dy])
                                 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = UNI + (255,)
    frame(img)
    return img


def sig_starboost():
    img = background()
    px = img.load()
    d = ImageDraw.Draw(img)
    for k in range(3):
        d.line((46 + k * 4, 14 + k * 6, 60 + k * 2, 6 + k * 6), fill=CYAN_D, width=1)
    for x0, y0 in ((17, 42), (29, 42)):
        for k in range(-1, 2):
            d.line((x0 + 3, y0 + 3, x0 - 6 + k * 3, y0 + 20), fill=(150, 240, 255, 255), width=3)
        d.line((x0 + 3, y0 + 3, x0 - 5, y0 + 19), fill=WHITE, width=1)
    d.rounded_rectangle((14, 14, 38, 40), radius=6, fill=(196, 202, 212, 255), outline=(90, 98, 112, 255), width=2)
    d.rectangle((22, 22, 30, 32), fill=CYAN_D)
    d.line((26, 22, 26, 32), fill=WHITE, width=1)
    d.line((16, 18, 36, 18), fill=RED, width=1)
    for x0 in (17, 29):
        d.rectangle((x0, 39, x0 + 6, 45), fill=(60, 64, 76, 255))
    frame(img)
    return img


def sig_pulse_unibeam():
    img = background()
    px = img.load()
    disc(px, 12, 32, 8.5, STEEL)
    disc(px, 12, 32, 6.5, RED)
    disc(px, 12, 32, 4, CYAN)
    disc(px, 12, 32, 1.8, WHITE)
    for x0, x1, h in ((22, 32, 8), (36, 48, 12), (52, 62, 16)):
        for y in range(32 - h // 2, 32 + h // 2 + 1):
            for x in range(x0, x1 + 1):
                edge = abs(y - 32) > h // 2 - 2
                put(px, x, y, CYAN_D if edge else (WHITE if abs(y - 32) < 1 else CYAN))
    frame(img)
    return img


def sig_shoulder_gun():
    img = background()
    px = img.load()
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((3, 38, 22, 58), radius=5, fill=(96, 104, 118, 255), outline=(160, 168, 180, 255), width=1)
    d.rectangle((18, 29, 46, 35), fill=(70, 76, 90, 255))
    d.rectangle((26, 26, 38, 38), fill=(110, 118, 132, 255))
    d.rectangle((46, 30, 52, 34), fill=(40, 44, 54, 255))
    d.polygon([(52, 32), (58, 26), (56, 32), (63, 30), (56, 34), (60, 40)], fill=(255, 220, 110, 255))
    for k in range(3):
        d.line((58, 32, 52 + k * 4, 8 + k * 6), fill=(255, 220, 130, 255), width=1)
    ring(px, 50, 14, 5.0, 6.0, RED, gaps=(0, 90, 180, 270))
    frame(img)
    return img


def sig_slam():
    img = background()
    px = img.load()
    d = ImageDraw.Draw(img)
    cx, gy = 32, 46
    d.ellipse((14, 42, 50, 52), fill=(40, 30, 28, 255))
    d.ellipse((20, 44, 44, 49), fill=(70, 50, 40, 255))
    for r, col in ((14, CYAN), (22, CYAN_D), (30, CYAN_D)):
        d.arc((cx - r, gy - r // 2 - 2, cx + r, gy + r // 2 + 2), start=200, end=340, fill=col, width=1)
    d.rounded_rectangle((25, 6, 39, 22), radius=4, fill=GOLD, outline=RED_D, width=1)
    for y in (10, 14, 18):
        d.line((28, y, 36, y), fill=RED_D, width=1)
    d.rectangle((30, 0, 34, 5), fill=(60, 64, 76, 255))
    rng = random.Random(5)
    for _ in range(25):
        a = rng.uniform(math.pi * 1.1, math.pi * 1.9)
        r = rng.uniform(10, 26)
        put(px, int(cx + math.cos(a) * r), int(gy - 2 + math.sin(a) * r * 0.5), rng.choice((CYAN, WHITE)))
    for x, y, r in ((12, 46, 5), (52, 46, 5), (20, 40, 4), (44, 40, 4)):
        disc(px, x, y, r, (150, 150, 160, 255))
    frame(img)
    return img


SIGNATURES = (
    ("micro_laser", sig_micro_laser),
    ("rocket_fist", sig_rocket_fist),
    ("camouflage", sig_camouflage),
    ("starboost", sig_starboost),
    ("pulse_unibeam", sig_pulse_unibeam),
    ("shoulder_gun", sig_shoulder_gun),
    ("slam", sig_slam),
)


def main(out_root):
    write(interior(), HERO + "ironman_interior.png", out_root)
    write(veronica(), ICON + "veronica.png", out_root)
    for name, fn in SIGNATURES:
        write(fn(), ICON + "sig_%s.png" % name, out_root)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else TEX)
