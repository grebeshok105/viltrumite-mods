#!/usr/bin/env python3
"""Iron Man Stage 2 nano parts, own work: geo for the nano blade, nano hammer,
nano shield and the shoulder missile pods (Bedrock armor coordinates, see
animation-system §7.1) plus one 32x8 palette texture and its glow map.

The Satsu `nanokatar` / `nano_mallet` / `nano_shield` sources were not in
this session, so the parts are drawn here; replace by a conversion later.

Run: python3 tools/assets/make_ironman_stage2_parts.py
"""
import base64
import io
import json
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
GEO = os.path.join(ROOT, "viltrumitecore/src/main/resources/assets/viltrumitecore/geo/ironman/nano")
TEX = os.path.join(ROOT, "viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero")

# Palette cells (4x4 each) in a 32x8 texture: (u, v).
CELLS = {
    "steel": (0, 0), "red": (4, 0), "gold": (8, 0), "cyan": (12, 0),
    "dark": (16, 0), "white": (20, 0), "edge": (24, 0), "plate": (28, 0),
}
COLORS = {
    "steel": (150, 158, 170), "red": (168, 26, 30), "gold": (222, 172, 60), "cyan": (110, 220, 255),
    "dark": (46, 52, 64), "white": (236, 250, 255), "edge": (190, 245, 255), "plate": (132, 22, 26),
}
GLOWS = {"cyan", "white", "edge"}


def cube(origin, size, cell):
    u, v = CELLS[cell]
    face = {"uv": [u, v], "uv_size": [4, 4]}
    return {"origin": origin, "size": size, "uv": {k: dict(face) for k in ("north", "east", "south", "west", "up", "down")}}


def geo(name, bones):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.viltrumitecore.ironman_" + name, "texture_width": 32, "texture_height": 8},
        "bones": bones}]}


def blade():
    # Katar from the right fist (hand at y 12): wrist guard, then a flat blade down the arm axis.
    return geo("nano_blade", [
        {"name": "armorRightArm", "pivot": [-5, 22, 0]},
        {"name": "blade", "parent": "armorRightArm", "pivot": [-6, 12, 0], "cubes": [
            cube([-8.3, 11, -2.3], [4.6, 2, 4.6], "dark"),
            cube([-8.4, 12.2, -2.4], [4.8, 0.6, 4.8], "cyan"),
            cube([-6.35, 1, -1.5], [0.7, 10, 3], "steel"),
            cube([-6.45, 1, 1.2], [0.9, 10, 0.6], "edge"),
            cube([-6.35, -1, -0.8], [0.7, 2, 2], "edge"),
        ]},
    ])


def hammer():
    # Mallet from the right fist: short handle and a big head with cyan seams.
    return geo("nano_hammer", [
        {"name": "armorRightArm", "pivot": [-5, 22, 0]},
        {"name": "hammer", "parent": "armorRightArm", "pivot": [-6, 12, 0], "cubes": [
            cube([-6.75, 5, -0.75], [1.5, 7, 1.5], "dark"),
            cube([-9, 0, -4], [6, 5, 8], "red"),
            cube([-9.2, 2.2, -4.2], [6.4, 0.6, 8.4], "cyan"),
            cube([-8.5, -0.4, -3.5], [5, 0.6, 7], "gold"),
            cube([-6.6, 11, -1.1], [1.2, 1.2, 2.2], "steel"),
        ]},
    ])


def shield():
    # Plates on the outside of the left forearm, a round-ish shield of 3 layers.
    plates = [
        cube([8.1, 8, -5], [0.8, 12, 10], "plate"),
        cube([8.0, 6, -3.5], [0.8, 15, 7], "red"),
        cube([7.9, 10, -6.5], [0.8, 8, 13], "red"),
        cube([8.9, 12.5, -1.5], [0.4, 3, 3], "white"),
        cube([8.9, 7.5, -0.5], [0.4, 1, 1], "cyan"),
        cube([8.9, 19.5, -0.5], [0.4, 1, 1], "cyan"),
    ]
    return geo("nano_shield", [
        {"name": "armorLeftArm", "pivot": [5, 22, 0]},
        {"name": "shield", "parent": "armorLeftArm", "pivot": [8, 14, 0], "cubes": plates},
    ])


def pods():
    # Missile pods on top of each shoulder; the flap child opens around its back edge.
    def side(arm, sign, pivot_x):
        x0 = -8.2 if sign < 0 else 3.7
        return [
            {"name": arm, "pivot": [pivot_x, 22, 0]},
            {"name": arm + "Pod", "parent": arm, "pivot": [pivot_x, 24, 0], "cubes": [
                cube([x0, 24, -1.8], [4.5, 1.2, 3.6], "red"),
                cube([x0 + 0.5, 24.6, -1.4], [3.5, 0.5, 2.8], "dark")]},
            {"name": arm + "Flap", "parent": arm + "Pod", "pivot": [pivot_x, 25.2, 1.8], "cubes": [
                cube([x0 + 0.2, 25.2, -1.6], [4.1, 0.5, 3.4], "plate")]},
            {"name": arm + "Tips", "parent": arm + "Pod", "pivot": [pivot_x, 25.2, 0], "cubes": [
                cube([x0 + 0.8 + i * 1.1, 24.9, -1.0], [0.6, 0.8, 0.6], "white") for i in range(3)]},
        ]
    return geo("missile_pods", side("armorRightArm", -1, -5) + side("armorLeftArm", 1, 5))


def texture(glow):
    img = Image.new("RGBA", (32, 8), (0, 0, 0, 0 if not glow else 255))
    px = img.load()
    for name, (u, v) in CELLS.items():
        r, g, b = COLORS[name]
        for y in range(v, v + 4):
            for x in range(u, u + 4):
                shade = 1.0 - 0.08 * ((x + y) % 2)
                if glow:
                    px[x, y] = (int(r * shade), int(g * shade), int(b * shade), 255) if name in GLOWS else (0, 0, 0, 255)
                else:
                    px[x, y] = (int(r * shade), int(g * shade), int(b * shade), 255)
    return img


def main():
    os.makedirs(GEO, exist_ok=True)
    for name, fn in (("nano_blade", blade), ("nano_hammer", hammer), ("nano_shield", shield), ("missile_pods", pods)):
        with open(os.path.join(GEO, name + ".geo.json"), "w") as f:
            json.dump(fn(), f, indent=1)
            f.write("\n")
    for name, glow in (("ironman_nano_parts", False), ("ironman_nano_parts_glow", True)):
        buf = io.BytesIO()
        texture(glow).save(buf, "PNG")
        with open(os.path.join(TEX, name + ".png.b64"), "w") as f:
            f.write(base64.encodebytes(buf.getvalue()).decode())


if __name__ == "__main__":
    main()
