#!/usr/bin/env python3
"""Iron Man Stage 2 GUI art, own work (panel art style of make_ironman_icons.py):
ability icons 64x64 (unibeam, missiles, nano_arsenal) and crosshairs 32x32
per RMB tool (repulsor, blade, hammer, laser, gun, jackhammer, hulk_repulsor).

Run: python3 tools/assets/make_ironman_stage2_icons.py [OUT_ROOT]
Default OUT_ROOT writes base64 files under viltrumitecore/src/main/binassets.
"""
import base64
import io
import math
import os
import random
import sys

from PIL import Image

random.seed(2)
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BIN = os.path.join(ROOT, "viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/gui")

CYAN = (150, 240, 255, 255)
CYAN_D = (90, 200, 255, 255)
WHITE = (235, 252, 255, 255)
GOLD = (240, 190, 70, 255)
RED = (190, 40, 40, 255)
RED_D = (120, 22, 28, 255)
STEEL = (120, 130, 145, 255)


def background():
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 32, y - 32) / 40.0
            glow = max(0.0, 1.0 - d) ** 2
            band = 1.0 if (x + y) % 16 < 2 else 0.0
            px[x, y] = (int(16 + 30 * glow + 6 * band), int(22 + 90 * glow + 8 * band), int(40 + 120 * glow + 10 * band), 255)
    return img


def frame(img):
    px = img.load()
    for i in range(64):
        for x, y in ((i, 0), (i, 63), (0, i), (63, i)):
            px[x, y] = (8, 12, 22, 255)


def disc(px, cx, cy, r, color, w=64, h=64):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            if 0 <= x < w and 0 <= y < h and math.hypot(x - cx, y - cy) <= r:
                px[x, y] = color


def ring(px, cx, cy, r0, r1, color, w=64, h=64, gaps=None):
    for y in range(h):
        for x in range(w):
            d = math.hypot(x - cx, y - cy)
            if r0 <= d <= r1:
                a = math.degrees(math.atan2(y - cy, x - cx)) % 360
                if gaps and any(abs(((a - g + 180) % 360) - 180) < 12 for g in gaps):
                    continue
                px[x, y] = color


def rect(px, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = color


def sparks(px, n, cx, cy, r0, r1):
    for _ in range(n):
        a = random.uniform(0, 2 * math.pi)
        r = random.uniform(r0, r1)
        x, y = int(cx + math.cos(a) * r), int(cy + math.sin(a) * r)
        if 1 <= x < 63 and 1 <= y < 63:
            px[x, y] = CYAN if random.random() < 0.6 else WHITE


def unibeam():
    img = background()
    px = img.load()
    # Chest plate with the triangle-ish reactor and a thick beam to the right.
    rect(px, 6, 18, 26, 46, RED_D)
    rect(px, 8, 20, 24, 44, RED)
    for y in range(64):
        for x in range(20, 64):
            half = 6 + (x - 20) * 0.12
            d = abs(y - 32)
            if d <= half:
                t = d / half
                c = WHITE if t < 0.35 else CYAN if t < 0.7 else CYAN_D
                px[x, y] = c
    disc(px, 16, 32, 7.5, (40, 60, 80, 255))
    disc(px, 16, 32, 6, CYAN)
    disc(px, 16, 32, 3.5, WHITE)
    sparks(px, 20, 40, 32, 12, 22)
    frame(img)
    return img


def missiles():
    img = background()
    px = img.load()
    # Shoulder pod and three missiles fanning out with smoke trails.
    rect(px, 4, 40, 20, 56, RED_D)
    rect(px, 6, 42, 18, 54, RED)
    rect(px, 6, 40, 18, 41, GOLD)
    for i, ang in enumerate((-35, -15, 5)):
        a = math.radians(ang)
        dx, dy = math.cos(a), math.sin(a)
        sx, sy = 18, 46 - i * 2
        for s in range(0, 34):
            x, y = int(sx + dx * s), int(sy + dy * s - s * 0.55)
            if 1 <= x < 63 and 1 <= y < 63:
                if s < 22:
                    if s % 3 != 0:
                        px[x, y] = (150, 160, 175, 255)
                else:
                    for w in (-1, 0, 1):
                        yy = y + w
                        if 1 <= yy < 63:
                            px[x, yy] = WHITE if s > 31 else STEEL
                    if s == 22:
                        px[x, y] = (255, 170, 60, 255)
    # Target brackets top right.
    for (x0, y0) in ((44, 6), (56, 6), (44, 18), (56, 18)):
        rect(px, x0, y0, x0 + 3, y0, CYAN)
        rect(px, x0 if x0 == 44 else x0 + 3, y0 - 0 if y0 == 6 else y0 - 3, (x0 if x0 == 44 else x0 + 3), y0 + 3 if y0 == 6 else y0, CYAN)
    frame(img)
    return img


def nano_arsenal():
    img = background()
    px = img.load()
    # Left: blade, right: hammer head, both growing from nanite scales.
    for s in range(40):
        x = 10 + s // 3
        y = 54 - s
        for w in range(-2, 3):
            xx = x + w
            if 1 <= xx < 32:
                px[xx, y] = WHITE if w == 0 else (CYAN if abs(w) == 1 else CYAN_D)
    rect(px, 6, 52, 18, 55, RED)
    rect(px, 38, 14, 58, 30, (60, 70, 90, 255))
    rect(px, 40, 16, 56, 28, STEEL)
    rect(px, 40, 16, 56, 17, CYAN)
    rect(px, 46, 30, 50, 56, RED)
    rect(px, 46, 30, 50, 31, GOLD)
    sparks(px, 30, 32, 34, 4, 26)
    frame(img)
    return img


def crosshair(kind):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    c = (cx, cy) = (15.5, 15.5)
    W = (255, 255, 255, 230)
    C = (150, 240, 255, 230)
    if kind == "repulsor":
        ring(px, cx, cy, 6.0, 7.2, C, 32, 32, gaps=(45, 135, 225, 315))
        px[15, 15] = px[16, 16] = px[15, 16] = px[16, 15] = W
    elif kind == "hulk_repulsor":
        ring(px, cx, cy, 8.0, 9.6, C, 32, 32, gaps=(0, 90, 180, 270))
        ring(px, cx, cy, 3.0, 4.0, W, 32, 32)
    elif kind == "blade":
        for i in range(-7, 8):
            x, y = int(cx + i), int(cy - i)
            if abs(i) > 1:
                px[x, y] = W
        for i in (-9, -8, 8, 9):
            px[int(cx + i), int(cy + i * 0.0)] = C
    elif kind == "hammer":
        rect(px, 9, 9, 22, 12, C)
        rect(px, 9, 19, 22, 22, C)
        px[15, 15] = px[16, 16] = px[15, 16] = px[16, 15] = W
    elif kind == "laser":
        rect(px, 15, 3, 16, 11, C)
        rect(px, 15, 20, 16, 28, C)
        rect(px, 3, 15, 11, 16, C)
        rect(px, 20, 15, 28, 16, C)
        px[15, 15] = px[16, 16] = W
    elif kind == "gun":
        ring(px, cx, cy, 4.0, 5.0, W, 32, 32)
        rect(px, 15, 1, 16, 8, W)
        rect(px, 15, 23, 16, 30, W)
        rect(px, 1, 15, 8, 16, W)
        rect(px, 23, 15, 30, 16, W)
    elif kind == "jackhammer":
        rect(px, 12, 6, 19, 9, C)
        rect(px, 14, 10, 17, 22, W)
        rect(px, 15, 23, 16, 27, C)
    return img


def write(img, rel, out_root):
    path = os.path.join(out_root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    buf = io.BytesIO()
    img.save(buf, "PNG")
    if "binassets" in out_root:
        with open(path + ".b64", "w") as f:
            f.write(base64.encodebytes(buf.getvalue()).decode())
    else:
        with open(path, "wb") as f:
            f.write(buf.getvalue())


def main(out_root):
    for name, fn in (("unibeam", unibeam), ("missiles", missiles), ("nano_arsenal", nano_arsenal)):
        write(fn(), "ability/ironman/%s.png" % name, out_root)
    for kind in ("repulsor", "blade", "hammer", "laser", "gun", "jackhammer", "hulk_repulsor"):
        write(crosshair(kind), "ironman/crosshair/%s.png" % kind, out_root)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else BIN)
