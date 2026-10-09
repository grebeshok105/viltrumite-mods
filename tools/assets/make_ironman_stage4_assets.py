#!/usr/bin/env python3
"""Iron Man Stage 4 art, own work (style of make_ironman_stage2_icons.py).

Mark skins: the Mark 50 skin (ironman_mark_50.png.b64) is recoloured by palette
mapping, so UV regions and alpha stay exact. Each mark adds its own reactor,
plate details and a glow map (eyes and lights, black elsewhere). Also the suit
interior skin, the Veronica pod icon and seven signature icons.

Run: python3 tools/assets/make_ironman_stage4_assets.py [OUT_ROOT]
Default OUT_ROOT writes base64 files under viltrumitecore/src/main/binassets.
"""
import base64
import io
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


def load(rel):
    with open(os.path.join(TEX, rel + ".b64"), encoding="ascii") as f:
        return Image.open(io.BytesIO(base64.b64decode(f.read()))).convert("RGBA")


def family(r, g, b):
    if r >= 1.6 * g and r >= 1.6 * b:
        return "R"
    if max(r, g, b) - min(r, g, b) < 0.22 * max(r, g, b):
        return "S"
    return "G"


def luma(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def _ranges(src):
    lo, hi = {}, {}
    px = src.load()
    for y in range(64):
        for x in range(64):
            r, g, b, a = px[x, y]
            if a and (r, g, b) != LIGHT:
                f, lum = family(r, g, b), luma(r, g, b)
                lo[f] = min(lo.get(f, 255.0), lum)
                hi[f] = max(hi.get(f, 0.0), lum)
    return {f: (lo[f], hi[f]) for f in lo}


SRC = load(HERO + "ironman_mark_50.png")
SRC_GLOW = load(HERO + "ironman_mark_50_glow.png")
RANGE = _ranges(SRC)
GLOW_SRC = [(x, y) for y in range(64) for x in range(64) if SRC_GLOW.getpixel((x, y))[3]]


def ramp(stops, t):
    t = min(1.0, max(0.0, t))
    if t < 0.5:
        return lerp(stops[0], stops[1], t * 2)
    return lerp(stops[1], stops[2], (t - 0.5) * 2)


def is_limb(x, y):
    return y >= 16 and (x < 16 or x >= 40 or y >= 48)


def recolour(cfg):
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    src, dst = SRC.load(), out.load()
    for y in range(64):
        for x in range(64):
            r, g, b, a = src[x, y]
            if not a:
                continue
            if (r, g, b) == LIGHT:
                dst[x, y] = tuple(cfg["light"]) + (255,)
                continue
            fam = family(r, g, b)
            key = "limb" + fam if is_limb(x, y) and ("limb" + fam) in cfg else fam
            lo, hi = RANGE[fam]
            dst[x, y] = ramp(cfg[key], (luma(r, g, b) - lo) / (hi - lo)) + (255,)
    return out


def reactor_skin(dst, src, rings):
    for y in range(20, 28):
        for x in range(20, 28):
            if not src[x, y][3]:
                continue
            d = math.hypot(x - 23.5, y - 23.5)
            for radius, col in rings:
                if d <= radius:
                    dst[x, y] = tuple(col) + (255,)
                    break


def faceplate(dst, src, cfg):
    # Slim helmet: dark red edges, red forehead, pink visor stripe on the centre.
    for base in (8, 40):
        for x in range(base, base + 8):
            for y in range(8, 16):
                if not src[x, y][3]:
                    continue
                local = x - base
                if local in (0, 7):
                    col = cfg["R"][0]
                elif y in (8, 9):
                    col = cfg["R"][1]
                elif local in (3, 4) and 9 <= y <= 14:
                    col = cfg["S"][2]
                else:
                    continue
                dst[x, y] = tuple(col) + (255,)


def shoulders(dst, src, cfg):
    # Heavy plates on the upper arms: bright top rows, dark seam below.
    light, seam = cfg["S"][2], cfg["S"][0]
    for rows, x0, x1 in (((20, 21, 22), 40, 55), ((52, 53, 54), 32, 47)):
        for y in rows:
            for x in range(x0, x1 + 1):
                if src[x, y][3]:
                    dst[x, y] = tuple(light) + (255,)
        for x in range(x0, x1 + 1):
            y = rows[-1] + 1
            if src[x, y][3]:
                dst[x, y] = tuple(seam) + (255,)


def seams(dst, src, cfg):
    # Panel lines on torso and limbs: the 3rd and 7th row of each 12-row face.
    for y in (23, 27, 39, 43, 55, 59):
        for x in range(64):
            if not src[x, y][3] or dst[x, y][:3] == tuple(cfg["light"]):
                continue
            r, g, b, _ = dst[x, y]
            dst[x, y] = (r // 2, g // 2, b // 2, 255)


def back_plates(dst, src, cfg):
    # Starboost pack: two dark plates on the back with blue vents below.
    plate, edge, vent = cfg["S"][0], cfg["S"][2], BLUE
    for x0 in (33, 37):
        for y in range(21, 28):
            for x in range(x0, x0 + 3):
                if src[x, y][3]:
                    dst[x, y] = tuple(plate) + (255,)
        for x in range(x0, x0 + 3):
            if src[x, 21][3]:
                dst[x, 21] = tuple(edge) + (255,)
        for y in (29, 30):
            for x in range(x0, x0 + 3):
                if src[x, y][3]:
                    dst[x, y] = tuple(vent) + (255,)


def build_skin(cfg):
    out = recolour(cfg)
    src, dst = SRC.load(), out.load()
    if "reactor" in cfg:
        reactor_skin(dst, src, cfg["reactor"])
    if cfg.get("faceplate"):
        faceplate(dst, src, cfg)
    if cfg.get("shoulders"):
        shoulders(dst, src, cfg)
    if cfg.get("seams"):
        seams(dst, src, cfg)
    if cfg.get("plates"):
        back_plates(dst, src, cfg)
    return out


def group(x, y):
    if y == 11 and x in (9, 10, 13, 14):
        return "eyes"
    if 20 <= x <= 27 and 20 <= y <= 27:
        return "reactor"
    return "limb"


def build_glow(cfg):
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dst = out.load()
    glow = cfg["glow"]
    for x, y in GLOW_SRC:
        grp = group(x, y)
        if grp == "reactor" and "disc" in glow:
            continue
        if glow.get(grp):
            dst[x, y] = tuple(glow[grp]) + (255,)
    if "disc" in glow:
        radius, col = glow["disc"]
        for y in range(20, 28):
            for x in range(20, 28):
                if math.hypot(x - 23.5, y - 23.5) <= radius:
                    dst[x, y] = tuple(col) + (255,)
    for x, y, col in glow.get("extra", []):
        dst[x, y] = tuple(col) + (255,)
    return out


# Palette ramps run dark to light. R, G, S are the red, gold and steel families
# of the Mark 50 source; limbR / limbG override them on arms and legs.
MARKS = {
    "mark_7": dict(
        R=((70, 8, 14), (150, 20, 28), (212, 44, 48)),
        G=((118, 72, 18), (204, 148, 50), (250, 216, 126)),
        S=((62, 66, 76), (144, 150, 162), (224, 229, 236)),
        light=LIGHT,
        reactor=[(1.6, LIGHT), (2.6, (224, 229, 236))],
        glow=dict(eyes=LIGHT, limb=LIGHT, disc=(1.6, LIGHT)),
    ),
    "mark_42": dict(
        R=((70, 8, 14), (150, 20, 28), (212, 44, 48)),
        G=((122, 78, 16), (214, 162, 56), (252, 226, 140)),
        S=((60, 60, 64), (136, 136, 142), (214, 214, 220)),
        limbR=((122, 78, 16), (214, 162, 56), (252, 226, 140)),
        light=LIGHT,
        seams=True,
        glow=dict(eyes=LIGHT, limb=LIGHT, reactor=LIGHT),
    ),
    "mark_15": dict(
        R=((14, 16, 22), (34, 38, 48), (58, 64, 76)),
        G=((22, 24, 30), (48, 52, 62), (78, 84, 96)),
        S=((18, 20, 26), (40, 44, 52), (66, 70, 80)),
        light=DIM,
        reactor=[(1.0, DIM), (2.4, (40, 44, 52))],
        glow=dict(eyes=DIM, disc=(1.0, DIM)),
    ),
    "mark_39": dict(
        R=((128, 136, 150), (206, 212, 222), (246, 249, 252)),
        G=((96, 12, 18), (176, 28, 34), (222, 56, 58)),
        S=((70, 76, 88), (140, 148, 160), (206, 212, 220)),
        limbG=((20, 56, 128), (40, 112, 220), (120, 190, 255)),
        light=BLUE_LIGHT,
        reactor=[(1.6, BLUE_LIGHT), (2.6, (206, 212, 220))],
        plates=True,
        glow=dict(eyes=BLUE_LIGHT, limb=BLUE, disc=(1.6, BLUE_LIGHT),
                  extra=[(x, 30, BLUE) for x in (33, 34, 35, 37, 38, 39)]),
    ),
    "mark_17": dict(
        R=((76, 8, 14), (156, 22, 30), (206, 40, 44)),
        G=((104, 108, 118), (186, 190, 200), (240, 244, 248)),
        S=((54, 58, 66), (118, 124, 134), (180, 186, 196)),
        light=LIGHT,
        reactor=[(2.6, UNI), (3.4, (180, 186, 196)), (4.3, (54, 58, 66))],
        glow=dict(eyes=LIGHT, limb=LIGHT, disc=(2.6, UNI)),
    ),
    "war_machine_mk2": dict(
        R=((28, 30, 36), (72, 76, 86), (120, 126, 136)),
        G=((16, 17, 20), (40, 42, 48), (70, 72, 80)),
        S=((90, 96, 108), (168, 174, 186), (232, 236, 242)),
        light=WHITE_RGB,
        shoulders=True,
        glow=dict(eyes=WHITE_RGB),
    ),
    "iron_heart_mk3": dict(
        R=((90, 10, 20), (168, 26, 40), (206, 46, 56)),
        G=((130, 84, 22), (214, 160, 60), (252, 224, 134)),
        S=((74, 28, 86), (150, 64, 156), (232, 128, 206)),
        light=PINK,
        reactor=[(1.6, PINK_CORE), (2.6, (232, 128, 206))],
        faceplate=True,
        glow=dict(eyes=PINK, limb=PINK, disc=(1.6, PINK_CORE)),
    ),
}


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
    for key, cfg in MARKS.items():
        write(build_skin(cfg), HERO + "ironman_%s.png" % key, out_root)
        write(build_glow(cfg), HERO + "ironman_%s_glow.png" % key, out_root)
    write(interior(), HERO + "ironman_interior.png", out_root)
    write(veronica(), ICON + "veronica.png", out_root)
    for name, fn in SIGNATURES:
        write(fn(), ICON + "sig_%s.png" % name, out_root)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else TEX)
