#!/usr/bin/env python3
"""Forearm micro-missile launchers (PR 19 iteration 2) and the tintable suit lining.

Writes, from scratch (no external source):
  geo/ironman/missiles/forearm_launchers.geo.json   housing + muzzle petals on both outer forearms
  geo/ironman/missiles/forearm_rockets.geo.json     2x2 rocket tips per pod (same pod bones)
  binassets textures/entity/ironman/missiles/forearm_launcher.png.b64  light grayscale, tinted by the suit colour
  binassets textures/entity/ironman/missiles/forearm_rockets.png.b64   untinted rocket tips
  binassets textures/entity/hero/ironman_interior.png.b64              the lining converted to grayscale (tinted in EmptySuitRenderer)

Bedrock coordinates: right arm x -8..-4, mark plating out to x -8.6; a pod sits on the outer side,
hinged at its elbow end (y 18.2), muzzle towards the hand (y 12.9). The left pod is mirrored.
Run: python3 tools/assets/make_ironman_forearm_launchers.py
"""
import base64
import io
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "viltrumitecore", "src", "main")
RES = os.path.join(ROOT, "resources", "assets", "viltrumitecore")
BIN = os.path.join(ROOT, "binassets", "assets", "viltrumitecore")


def cube(origin, size, uv, inflate=0.0):
    c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def mirror_cube(c):
    o, s = c["origin"], c["size"]
    m = dict(c)
    m["origin"] = [round(-(o[0] + s[0]), 3), o[1], o[2]]
    m["mirror"] = True
    return m


def side_bones(right, rockets):
    arm = "armorRightArm" if right else "armorLeftArm"
    sx = 1.0 if right else -1.0
    tag = "Right" if right else "Left"

    def px(x):
        return round(x * sx, 3)

    def fix(cubes):
        return cubes if right else [mirror_cube(c) for c in cubes]

    bones = [{"name": arm, "pivot": [px(-5.0), 22.0, 0.0]}]
    pod = {"name": "pod" + tag, "parent": arm, "pivot": [px(-8.4), 18.2, 0.0]}
    bones.append(pod)
    if rockets:
        tips = []
        for x in (-9.75, -8.85):
            for z in (-0.7, 0.7):
                tips.append(cube((x - 0.3, 12.3, z - 0.3), (0.6, 1.3, 0.6), [0, 0]))
        bones.append({"name": "rockets" + tag, "parent": "pod" + tag, "pivot": [px(-9.3), 12.9, 0.0], "cubes": fix(tips)})
        return bones
    pod["cubes"] = fix([
        cube((-10.2, 13.6, -1.5), (1.8, 4.6, 3.0), [0, 0]),
        cube((-8.9, 14.2, -1.0), (0.6, 3.4, 2.0), [0, 16]),
        cube((-10.4, 12.9, -1.7), (2.2, 0.7, 3.4), [12, 16]),
        cube((-10.45, 16.6, -1.2), (0.3, 1.2, 2.4), [0, 24]),
    ])
    bones.append({"name": "petalA" + tag, "parent": "pod" + tag, "pivot": [px(-9.3), 12.9, -1.5],
                  "cubes": fix([cube((-10.1, 12.6, -1.5), (1.6, 0.3, 1.5), [20, 0])])})
    bones.append({"name": "petalB" + tag, "parent": "pod" + tag, "pivot": [px(-9.3), 12.9, 1.5],
                  "cubes": fix([cube((-10.1, 12.6, 0.0), (1.6, 0.3, 1.5), [20, 4])])})
    return bones


def geo(name, tex, rockets):
    bones = side_bones(True, rockets) + side_bones(False, rockets)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": "geometry.viltrumitecore." + name, "texture_width": tex, "texture_height": tex},
            "bones": bones,
        }],
    }


def b64png(img, path):
    buf = io.BytesIO()
    img.save(buf, "PNG")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(base64.b64encode(buf.getvalue()).decode())


def launcher_texture():
    rnd = random.Random(19)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            v = 222 + rnd.randint(-8, 8)
            if x % 8 == 0 or y % 9 == 0:
                v = 150  # panel seams
            if 12 <= x < 26 and 16 <= y < 23:
                v = 70 if (x + y) % 3 == 0 else 95  # muzzle ring: dark bores
            if 0 <= x < 12 and 16 <= y < 24:
                v = 120  # mount rail
            if 0 <= x < 8 and 24 <= y < 32:
                v = 245  # light strip
            img.putpixel((x, y), (v, v, v, 255))
    return img


def rockets_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = (205, 208, 214) if y >= 2 else (190, 40, 36)
            img.putpixel((x, y), c + (255,))
    return img


def lining_grayscale():
    path = os.path.join(BIN, "textures", "entity", "hero", "ironman_interior.png.b64")
    raw = base64.b64decode(open(path).read())
    img = Image.open(io.BytesIO(raw)).convert("RGBA")
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if r > g + 30 and r > b + 30:
                # red lining -> light gray, so the suit colour shows through the tint
                v = min(255, int(r * 1.55) + 20)
            else:
                v = int(0.3 * r + 0.59 * g + 0.11 * b)
            out.putpixel((x, y), (v, v, v, a))
    return out


if __name__ == "__main__":
    missiles = os.path.join(RES, "geo", "ironman", "missiles")
    with open(os.path.join(missiles, "forearm_launchers.geo.json"), "w") as f:
        json.dump(geo("forearm_launchers", 32, False), f, indent=1)
    with open(os.path.join(missiles, "forearm_rockets.geo.json"), "w") as f:
        json.dump(geo("forearm_rockets", 16, True), f, indent=1)
    tex = os.path.join(BIN, "textures", "entity", "ironman", "missiles")
    b64png(launcher_texture(), os.path.join(tex, "forearm_launcher.png.b64"))
    b64png(rockets_texture(), os.path.join(tex, "forearm_rockets.png.b64"))
    import sys
    if "--lining" in sys.argv:
        b64png(lining_grayscale(), os.path.join(BIN, "textures", "entity", "hero", "ironman_interior.png.b64"))
    print("ok")
