#!/usr/bin/env python3
"""Stage 4 mark-signature parts: geo JSON and pixel textures (own art).

Writes geo into viltrumitecore/src/main/resources/assets/viltrumitecore/geo/ironman/marks/
and base64 PNGs into viltrumitecore/src/main/binassets/.../textures/entity/ironman/marks/
(Gradle decodeBinaryAssets turns the .b64 files into resources).

Run from the repository root: python3 tools/assets/make_ironman_stage4_signatures.py
Geo units are Bedrock armor pixels (feet at y 0, body pivot y 24, arm pivots x +-5 y 22,
leg pivots x +-1.9 y 12). Right side is negative x.
"""
import base64
import io
import json
import pathlib
import random

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
CORE = ROOT / "viltrumitecore"
GEO_OUT = CORE / "src/main/resources/assets/viltrumitecore/geo/ironman/marks"
TEX_OUT = CORE / "src/main/binassets/assets/viltrumitecore/textures/entity/ironman/marks"


def faces(u, v, w, h, d):
    return {
        "up": {"uv": [u + d, v], "uv_size": [w, d]},
        "down": {"uv": [u + d + w, v], "uv_size": [w, d]},
        "east": {"uv": [u, v + d], "uv_size": [d, h]},
        "north": {"uv": [u + d, v + d], "uv_size": [w, h]},
        "west": {"uv": [u + d + w, v + d], "uv_size": [d, h]},
        "south": {"uv": [u + 2 * d + w, v + d], "uv_size": [w, h]},
    }


def cube(origin, size, uv):
    w, h, d = size
    return {"origin": origin, "size": size, "uv": faces(uv[0], uv[1], w, h, d)}


def bone(name, pivot, cubes=(), parent=None):
    entry = {"name": name, "pivot": pivot}
    if parent:
        entry["parent"] = parent
    if cubes:
        entry["cubes"] = list(cubes)
    return entry


def geo(identifier, width, height, bones):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": identifier, "texture_width": width, "texture_height": height},
            "bones": bones,
        }],
    }


GEOS = {
    # Mark 7: emitters with a lens on each forearm (glow pass uses the lens region).
    "laser_emitters": geo("geometry.viltrumitecore.ironman_mark_laser", 16, 16, [
        bone("armorRightArm", [-5, 22, 0]),
        bone("laserRight", [-5, 22, 0], [
            cube([-7.6, 13.0, -3.05], [2.6, 2.4, 1.0], (0, 0)),
            cube([-6.9, 13.9, -3.25], [1.2, 0.8, 0.3], (8, 0)),
        ], parent="armorRightArm"),
        bone("armorLeftArm", [5, 22, 0]),
        bone("laserLeft", [5, 22, 0], [
            cube([5.0, 13.0, -3.05], [2.6, 2.4, 1.0], (0, 0)),
            cube([5.7, 13.9, -3.25], [1.2, 0.8, 0.3], (8, 0)),
        ], parent="armorLeftArm"),
    ]),
    # Mark 42: right gauntlet over the hand, with a gold cuff.
    "glove": geo("geometry.viltrumitecore.ironman_mark_glove", 16, 16, [
        bone("armorRightArm", [-5, 22, 0]),
        bone("gauntlet", [-5, 22, 0], [
            cube([-9.0, 11.4, -2.6], [4.6, 5.0, 5.2], (0, 0)),
            cube([-9.2, 15.6, -2.8], [4.9, 1.0, 5.6], (0, 8)),
        ], parent="armorRightArm"),
    ]),
    # Mark 42 rocket fist in flight (entity model, root at the origin, pixel units).
    "fist": geo("geometry.viltrumitecore.ironman_mark_fist", 16, 16, [
        bone("glove", [0, 0, 0], [
            cube([-2.75, -2.5, -2.75], [5.5, 5.0, 5.5], (0, 0)),
            cube([-1.2, -2.8, 2.5], [2.4, 2.4, 1.4], (0, 8)),
        ]),
    ]),
    # Mark 15: a shell a little bigger than the body, drawn with a shimmer texture.
    "camo_shell": geo("geometry.viltrumitecore.ironman_mark_camo", 16, 16, [
        bone("armorHead", [0, 24, 0], [cube([-4.7, 23.3, -4.7], [9.4, 9.4, 9.4], (0, 0))]),
        bone("armorBody", [0, 24, 0], [cube([-4.6, 11.4, -2.6], [9.2, 13.2, 5.2], (0, 0))]),
        bone("armorRightArm", [-5, 22, 0], [cube([-8.6, 11.4, -2.6], [5.2, 13.2, 5.2], (0, 0))]),
        bone("armorLeftArm", [5, 22, 0], [cube([3.4, 11.4, -2.6], [5.2, 13.2, 5.2], (0, 0))]),
        bone("armorRightLeg", [-1.9, 12, 0], [cube([-4.6, 0.0, -2.6], [5.2, 12.6, 5.2], (0, 0))]),
        bone("armorLeftLeg", [1.9, 12, 0], [cube([-0.6, 0.0, -2.6], [5.2, 12.6, 5.2], (0, 0))]),
    ]),
}


def canvas(size, fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (size, size), fill)


def paint_rect(img, x0, y0, x1, y1, color):
    for x in range(x0, x1):
        for y in range(y0, y1):
            img.putpixel((x, y), color)


def metal(size, base, light, seed):
    rnd = random.Random(seed)
    img = canvas(size, base + (255,))
    for x in range(size):
        for y in range(size):
            if rnd.random() < 0.08:
                img.putpixel((x, y), light + (255,))
    return img


def textures():
    out = {}
    laser = metal(16, (58, 63, 72), (120, 128, 138), 1)
    paint_rect(laser, 0, 0, 5, 3, (150, 40, 44, 255))
    out["laser"] = laser

    laser_glow = canvas(16)
    paint_rect(laser_glow, 8, 0, 10, 2, (255, 60, 60, 255))
    paint_rect(laser_glow, 10, 0, 11, 1, (120, 240, 255, 255))
    out["laser_glow"] = laser_glow

    glove = canvas(16, (184, 50, 44, 255))
    paint_rect(glove, 0, 8, 16, 9, (224, 176, 64, 255))
    paint_rect(glove, 0, 0, 16, 2, (118, 28, 30, 255))
    out["glove"] = glove

    camo = canvas(16)
    rnd = random.Random(15)
    for x in range(16):
        for y in range(16):
            roll = rnd.random()
            if roll < 0.10:
                camo.putpixel((x, y), (127, 208, 255, 255))
            elif roll < 0.14:
                camo.putpixel((x, y), (207, 239, 255, 255))
    out["camo_ripple"] = camo
    return out


def write_geo():
    GEO_OUT.mkdir(parents=True, exist_ok=True)
    for name, body in GEOS.items():
        (GEO_OUT / f"{name}.geo.json").write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")


def write_textures():
    TEX_OUT.mkdir(parents=True, exist_ok=True)
    for name, img in textures().items():
        buffer = io.BytesIO()
        img.save(buffer, format="PNG")
        encoded = base64.b64encode(buffer.getvalue()).decode("ascii") + "\n"
        (TEX_OUT / f"{name}.png.b64").write_text(encoded, encoding="ascii")


if __name__ == "__main__":
    write_geo()
    write_textures()
    print(f"wrote {len(GEOS)} geo files and {len(textures())} textures")
