#!/usr/bin/env python3
"""Draws the Tony Stark (no armor) player skin: 64x64, classic 4 px arms.

Own work for viltrumite-mods. Run: python3 tools/assets/make_tony_skin.py OUT.png
"""
import random
import sys

from PIL import Image

random.seed(1970)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

SKIN = (200, 150, 112)
SKIN_D = (176, 126, 92)
HAIR = (52, 36, 27)
HAIR_L = (72, 52, 38)
BEARD = (44, 30, 24)
SHIRT = (36, 36, 40)
SHIRT_L = (50, 50, 56)
RING = (112, 118, 128)
CORE = (170, 236, 255)
JEANS = (46, 61, 92)
JEANS_D = (36, 48, 74)
SHOE = (26, 26, 28)
BELT = (30, 22, 18)
STEEL = (150, 156, 164)


def shade(c, j=6):
    d = random.randint(-j, j)
    return tuple(max(0, min(255, v + d)) for v in c) + (255,)


def fill(x0, y0, x1, y1, c, j=6):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = shade(c, j)


def put(x, y, c):
    px[x, y] = tuple(c) + (255,)


# ---- head (base) ----
fill(8, 0, 16, 8, HAIR, 8)         # top
fill(16, 0, 24, 8, SKIN_D, 4)      # bottom (chin/neck)
for x0 in (0, 16, 24):             # right, left, back
    fill(x0, 8, x0 + 8, 16, SKIN, 5)
fill(24, 8, 32, 14, HAIR, 8)       # back of head
for x0 in (0, 16):                 # sides: hair top, ear
    fill(x0, 8, x0 + 8, 11, HAIR, 8)
    ear = x0 + (3 if x0 == 0 else 4)
    put(ear, 12, SKIN_D)
    put(ear, 13, SKIN_D)
# side burns toward the face edge
for y in (11, 12):
    put(7, y, HAIR)    # right side, front edge
    put(16, y, HAIR)   # left side, front edge
# face
fill(8, 8, 16, 16, SKIN, 4)
for x in range(8, 16):
    put(x, 8, HAIR)
    put(x, 9, HAIR_L if x in (10, 13) else HAIR)
for y in (10, 11):
    put(8, y, HAIR)
    put(15, y, HAIR)
for x in (9, 10, 13, 14):          # brows
    put(x, 11, BEARD)
put(9, 12, (236, 236, 236)); put(10, 12, (86, 56, 36))   # eyes
put(13, 12, (86, 56, 36)); put(14, 12, (236, 236, 236))
put(11, 13, SKIN_D); put(12, 13, SKIN_D)                  # nose
for x in range(10, 14):            # moustache
    put(x, 14, BEARD)
put(9, 15, SKIN_D)
put(10, 15, BEARD); put(13, 15, BEARD)                    # anchor goatee
put(11, 15, BEARD); put(12, 15, BEARD)
# hat layer: a little quiff over the forehead
for x in range(41, 47):
    put(x, 8, HAIR_L if x % 2 else HAIR)
for x in range(41, 47):
    for y in range(2, 6):
        put(x, y, HAIR if (x + y) % 3 else HAIR_L)

# ---- body: dark t-shirt with the arc reactor ----
fill(20, 16, 28, 20, SHIRT, 5)
fill(28, 16, 36, 20, SHIRT, 5)
fill(16, 20, 40, 32, SHIRT, 5)
for x in range(20, 28):            # collar
    put(x, 20, SHIRT_L)
put(23, 20, SKIN); put(24, 20, SKIN)
for x in range(22, 26):            # reactor ring (front x 22-25, y 22-25)
    for y in range(22, 26):
        put(x, y, RING)
for x, y in ((22, 22), (25, 22), (22, 25), (25, 25)):
    put(x, y, SHIRT_L)
for x in (23, 24):
    for y in (23, 24):
        put(x, y, CORE)
for x in range(16, 40):            # belt
    put(x, 31, BELT)
put(23, 31, STEEL); put(24, 31, STEEL)

# ---- arms: short sleeves, skin, watch on the left wrist ----
def arm(top, bottom, faces, watch):
    fill(*top, SHIRT, 5)
    fill(*bottom, SKIN_D, 4)
    for (x0, y0) in faces:
        fill(x0, y0, x0 + 4, y0 + 4, SHIRT, 5)
        fill(x0, y0 + 4, x0 + 4, y0 + 12, SKIN, 4)
        for x in range(x0, x0 + 4):
            put(x, y0 + 3, SHIRT_L)
            if watch:
                put(x, y0 + 9, (24, 24, 26) if x % 2 else STEEL)


arm((44, 16, 48, 20), (48, 16, 52, 20), [(40, 20), (44, 20), (48, 20), (52, 20)], False)
arm((36, 48, 40, 52), (40, 48, 44, 52), [(32, 52), (36, 52), (40, 52), (44, 52)], True)

# ---- legs: jeans and shoes ----
def leg(top, bottom, faces):
    fill(*top, JEANS, 5)
    fill(*bottom, SHOE, 3)
    for i, (x0, y0) in enumerate(faces):
        fill(x0, y0, x0 + 4, y0 + 10, JEANS, 6)
        fill(x0, y0 + 10, x0 + 4, y0 + 12, SHOE, 3)
        if i == 1:
            for y in range(y0, y0 + 10, 3):
                put(x0 + 1, y, JEANS_D)


leg((4, 16, 8, 20), (8, 16, 12, 20), [(0, 20), (4, 20), (8, 20), (12, 20)])
leg((20, 48, 24, 52), (24, 48, 28, 52), [(16, 52), (20, 52), (24, 52), (28, 52)])

img.save(sys.argv[1] if len(sys.argv) > 1 else "ironman.png")
