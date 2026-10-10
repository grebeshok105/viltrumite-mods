#!/usr/bin/env python3
"""Converts the Stage 1b Iron Man assets from the Satsu addon (3.6.1).

Run: python3 tools/assets/convert_ironman_stage1b.py SATSU_ASSETS_DIR
where SATSU_ASSETS_DIR is ``assets/satsu_iron_man_addon`` of the unpacked jar.
Writes into viltrumitecore (textures as base64 under binassets, geo as JSON).
Every output is listed in tools/assets/ironman_sources.md.
"""
import base64
import io
import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MAIN = os.path.join(ROOT, "viltrumitecore", "src", "main")
BIN = os.path.join(MAIN, "binassets", "assets", "viltrumitecore")
RES = os.path.join(MAIN, "resources", "assets", "viltrumitecore")


def write_b64(rel, image):
    out = io.BytesIO()
    image.save(out, "PNG", optimize=True)
    path = os.path.join(BIN, rel + ".b64")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="ascii") as f:
        f.write(base64.encodebytes(out.getvalue()).decode("ascii"))


def write_geo(rel, bones, width, height):
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.viltrumitecore." + os.path.basename(rel).split(".")[0],
                "texture_width": width,
                "texture_height": height,
            },
            "bones": bones,
        }],
    }
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(geo, f, indent=2)
        f.write("\n")


def bones_of(src, rel):
    with open(os.path.join(src, rel), encoding="utf-8") as f:
        return {b["name"]: b for b in json.load(f)["minecraft:geometry"][0]["bones"]}


def premultiplied(image):
    """Additive (eyes) render type ignores alpha: bake it into the colour."""
    image = image.convert("RGBA")
    px = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * a // 255, g * a // 255, b * a // 255, 255 if a else 0)
    return image


def flame_bone(parent, name, cube, top_y, length_axis="y"):
    """Child bone pivoted at the flame root so scaleY sets the flame length."""
    origin, size = cube["origin"], cube["size"]
    pivot = [origin[0] + size[0] / 2, top_y, origin[2] + size[2] / 2]
    return {"name": name, "parent": parent, "pivot": pivot, "cubes": [cube]}


def main(src):
    # Mark 50 suit skin: vanilla 64x64 player layout (classic arms), used as is.
    write_b64("textures/entity/hero/ironman_mark_50.png",
              Image.open(os.path.join(src, "textures/models/iron_man/no_light/mark_50_0.png")).convert("RGBA"))
    write_b64("textures/entity/hero/ironman_mark_50_glow.png",
              Image.open(os.path.join(src, "textures/models/iron_man/light/mark_50_0.png")).convert("RGBA"))

    # Helmet: armorHead of the nano head mask (two inflated cubes on head + hat UV).
    head = bones_of(src, "geo/armor_models/iron_man/nano_armors/marks/head_mask/main.geo.json")["armorHead"]
    write_geo("geo/ironman/mark_50/head_mask.geo.json",
              [{"name": "armorHead", "pivot": head["pivot"], "cubes": head["cubes"]}], 64, 64)

    # Feet flames: cube hangs below the boot (y -5..0); root at the sole.
    feet = bones_of(src, "geo/flames/below_flames_both_feet/flames_down.geo.json")
    write_geo("geo/ironman/flames/feet.geo.json", [
        {"name": "armorRightLeg", "pivot": feet["armorRightLeg"]["pivot"]},
        flame_bone("armorRightLeg", "flameRight", feet["armorRightLeg"]["cubes"][0], 0),
        {"name": "armorLeftLeg", "pivot": feet["armorLeftLeg"]["pivot"]},
        flame_bone("armorLeftLeg", "flameLeft", feet["armorLeftLeg"]["cubes"][0], 0),
    ], 4, 4)

    # Palm flames: cubes hang below the hand (y 9..12, the hand ends at y 12).
    arms = bones_of(src, "geo/flames/upper_flames_both_arms/flames.geo.json")
    palm = []
    for side in ("Right", "Left"):
        bone = arms["armor" + side + "Arm"]
        palm.append({"name": "armor" + side + "Arm", "pivot": [-5.0 if side == "Right" else 5.0, 22, 0]})
        palm.append(flame_bone("armor" + side + "Arm", "flame" + side, bone["cubes"][0], 12))
    write_geo("geo/ironman/flames/palms.geo.json", palm, 4, 4)

    # Back stabilizers: the four flames at the tips of the nano stabilizer rods
    # (Stage 2 converter). Each cube's own rotation moves to a child bone so
    # scaleY stretches the flame along its rod.
    stab = bones_of(src, "geo/flames/stabilizer_flames/main.geo.json")["armorBody"]["cubes"]
    body = [{"name": "armorBody", "pivot": [0, 24, 0]}]
    for cube in stab:
        side = "Right" if cube["origin"][0] < 0 else "Left"
        name = "stab" + side + ("" if cube["origin"][1] > 20 else "Low")
        flat = {k: v for k, v in cube.items() if k not in ("pivot", "rotation")}
        body.append({"name": name, "parent": "armorBody", "pivot": cube["pivot"], "rotation": cube["rotation"], "cubes": [flat]})
    write_geo("geo/ironman/flames/stabilizer.geo.json", body, 4, 4)

    for i in range(8):
        flame = Image.open(os.path.join(src, "textures/models/flames/new_thruster_white/%d.png" % i))
        write_b64("textures/entity/hero/ironman_flame_%d.png" % i, premultiplied(flame))


if __name__ == "__main__":
    main(sys.argv[1])
