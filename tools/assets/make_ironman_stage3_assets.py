#!/usr/bin/env python3
"""Iron Man Stage 3 GUI art, own work (style of make_ironman_stage2_icons.py):
ability icons 64x64 (scan, countermeasures, helmet) and the helmet HUD frame
480x270 (edge overlay with a transparent centre, stretched to the screen).

Run: python3 tools/assets/make_ironman_stage3_assets.py [OUT_ROOT]
Default OUT_ROOT writes base64 files under viltrumitecore/src/main/binassets.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_ironman_stage2_icons import (  # noqa: E402
    BIN, CYAN, CYAN_D, GOLD, RED, RED_D, STEEL, WHITE, background, disc, frame, rect, ring, sparks, write)

random.seed(3)


def scan():
    img = background()
    px = img.load()
    # Target in the middle, rotating reticle rings and a sweep line.
    ring(px, 32, 32, 22, 23.5, CYAN, gaps=(45, 135, 225, 315))
    ring(px, 32, 32, 14, 15, CYAN_D, gaps=(0, 90, 180, 270))
    disc(px, 32, 32, 6, (60, 70, 90, 255))
    disc(px, 32, 32, 4, RED)
    disc(px, 32, 32, 1.5, WHITE)
    for s in range(0, 23):
        a = math.radians(-40)
        x, y = int(32 + math.cos(a) * s), int(32 + math.sin(a) * s)
        px[x, y] = WHITE
    for x0, y0, dx, dy in ((6, 6, 1, 1), (57, 6, -1, 1), (6, 57, 1, -1), (57, 57, -1, -1)):
        for i in range(6):
            px[x0 + dx * i, y0] = CYAN
            px[x0, y0 + dy * i] = CYAN
    rect(px, 44, 48, 58, 49, CYAN_D)
    rect(px, 44, 52, 54, 53, CYAN_D)
    frame(img)
    return img


def countermeasures():
    img = background()
    px = img.load()
    # Suit back at the bottom, flares fanning upward with bright heads.
    rect(px, 24, 48, 40, 58, RED_D)
    rect(px, 26, 50, 38, 56, RED)
    rect(px, 30, 48, 34, 49, GOLD)
    for ang in (-150, -125, -100, -80, -55, -30):
        a = math.radians(ang)
        for s in range(4, 30):
            x = int(32 + math.cos(a) * s)
            y = int(50 + math.sin(a) * s + (s * s) * 0.012)
            if 1 <= x < 63 and 1 <= y < 63 and s % 2 == 0:
                px[x, y] = (200, 150, 90, 255) if s < 24 else (255, 200, 90, 255)
        hx = int(32 + math.cos(a) * 30)
        hy = int(50 + math.sin(a) * 30 + 900 * 0.012)
        disc(px, hx, hy, 2.5, (255, 170, 60, 255))
        disc(px, hx, hy, 1.2, WHITE)
    frame(img)
    return img


def helmet():
    img = background()
    px = img.load()
    # Mark 50 faceplate: red helmet, gold face, glowing eye slits.
    for y in range(8, 58):
        for x in range(10, 54):
            nx, ny = (x - 32) / 21.0, (y - 30) / 26.0
            if nx * nx + ny * ny * (1.0 if y < 30 else 0.75) <= 1.0:
                px[x, y] = RED_D if abs(nx) > 0.82 or ny < -0.85 else RED
    for y in range(20, 54):
        half = 12 - max(0, y - 40) * 0.5
        for x in range(int(32 - half), int(32 + half) + 1):
            px[x, y] = GOLD
    rect(px, 31, 20, 32, 52, (200, 150, 50, 255))
    for side in (-1, 1):
        for i in range(8):
            x = 32 + side * (3 + i)
            y0 = 28 + (i // 3 if side == 1 else i // 3)
            px[x, y0] = WHITE
            px[x, y0 + 1] = CYAN
    rect(px, 27, 46, 37, 46, (150, 110, 40, 255))
    sparks(px, 10, 32, 30, 24, 30)
    frame(img)
    return img


def helmet_frame():
    W, H = 480, 270
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Dark vignette at the edges, inner oval stays clear.
    vign = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vign)
    vd.rectangle((0, 0, W, H), fill=210)
    vd.ellipse((-40, -30, W + 40, H + 30), fill=0)
    vign = vign.filter(ImageFilter.GaussianBlur(28))
    dark = Image.new("RGBA", (W, H), (6, 14, 24, 255))
    dark.putalpha(vign)
    img = Image.alpha_composite(img, dark)
    d = ImageDraw.Draw(img)
    c = (120, 225, 255, 200)
    cd = (80, 180, 230, 120)
    # Corner brackets.
    for x0, y0, sx, sy in ((14, 12, 1, 1), (W - 15, 12, -1, 1), (14, H - 13, 1, -1), (W - 15, H - 13, -1, -1)):
        d.line((x0, y0, x0 + sx * 46, y0), fill=c, width=2)
        d.line((x0, y0, x0, y0 + sy * 30), fill=c, width=2)
        d.line((x0 + sx * 6, y0 + sy * 6, x0 + sx * 26, y0 + sy * 6), fill=cd, width=1)
    # Side tick ladders.
    for side in (-1, 1):
        for i in range(-5, 6):
            y = H / 2 + i * 12
            x = W / 2 + side * (222 - abs(i) * 1.5)
            d.line((x, y, x - side * (8 if i % 5 == 0 else 4), y), fill=c if i % 5 == 0 else cd, width=1)
    # Top and bottom thin bars with notches.
    d.line((W / 2 - 120, 8, W / 2 + 120, 8), fill=cd, width=1)
    d.line((W / 2 - 8, 8, W / 2 + 8, 8), fill=c, width=2)
    d.line((W / 2 - 150, H - 9, W / 2 + 150, H - 9), fill=cd, width=1)
    for i in range(-6, 7):
        x = W / 2 + i * 25
        d.line((x, H - 9, x, H - 13), fill=cd, width=1)
    # Faint hexagon grid only near the edges.
    grid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grid)
    r = 9
    for row in range(0, H // 14 + 2):
        for col in range(0, W // 16 + 2):
            x = col * 16 + (8 if row % 2 else 0)
            y = row * 14
            nx, ny = (x - W / 2) / (W / 2), (y - H / 2) / (H / 2)
            if nx * nx + ny * ny < 0.9:
                continue
            pts = [(x + r * math.cos(math.radians(60 * k + 30)), y + r * math.sin(math.radians(60 * k + 30))) for k in range(7)]
            gd.line(pts, fill=(110, 210, 255, 40), width=1)
    img = Image.alpha_composite(img, grid)
    return img


def main(out_root):
    for name, fn in (("scan", scan), ("countermeasures", countermeasures), ("helmet", helmet)):
        write(fn(), "ability/ironman/%s.png" % name, out_root)
    write(helmet_frame(), "ironman/helmet_frame.png", out_root)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else BIN)
