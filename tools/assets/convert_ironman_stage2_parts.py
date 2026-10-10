#!/usr/bin/env python3
"""Converts the Stage 2 Iron Man combat parts from the Satsu addon (3.6.1).

Run: python3 tools/assets/convert_ironman_stage2_parts.py SATSU_ASSETS_DIR
where SATSU_ASSETS_DIR is ``assets/satsu_iron_man_addon`` of the unpacked jar.
Model and texture pairs follow the addon's palladium render layers
(``render_layers/armor_things/iron_man/marks/mark_50/**``, ``.../model_72/electro_magnetic_shield``).
Every output is listed in tools/assets/ironman_sources.md.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from convert_ironman_stage1b import bones_of, premultiplied, write_b64, write_geo  # noqa: E402

NANO = "geo/armor_models/iron_man/nano_armors/"
NANO_TEX = "textures/models/iron_man/nano_stuff/"
OUT_TEX = "textures/entity/ironman/nano/"
# Repulsor cyan of the Iron Man VFX: the addon tints the force field with the suit AI colour.
FIELD_RGB = (140, 216, 255)


def arm(side):
    return {"name": "armor" + side + "Arm", "pivot": [-5 if side == "Right" else 5, 22, 0]}


def texture(src, rel, out, glow=False):
    image = Image.open(os.path.join(src, rel)).convert("RGBA")
    write_b64(OUT_TEX + out, premultiplied(image) if glow else image)


def main(src):
    # Nano blade: right-arm katar of nanokatar.geo.json; the child bone pivots at the fist so the wave grows it from there.
    katar = bones_of(src, NANO + "nanokatar.geo.json")["armorRightArm"]
    write_geo("geo/ironman/nano/nano_blade.geo.json", [
        arm("Right"),
        {"name": "blade", "parent": "armorRightArm", "pivot": [-8, 12, 0], "cubes": katar["cubes"]},
    ], 64, 64)
    texture(src, NANO_TEX + "nano_katars/no_light/mark_50_0.png", "nano_blade.png")
    texture(src, NANO_TEX + "nano_katars/light/mark_50_0.png", "nano_blade_glow.png", glow=True)

    # Nano hammer: right-arm mallet of nano_mallet.geo.json, grown from the wrist.
    mallet = bones_of(src, NANO + "nano_mallet.geo.json")["armorRightArm"]
    write_geo("geo/ironman/nano/nano_hammer.geo.json", [
        arm("Right"),
        {"name": "hammer", "parent": "armorRightArm", "pivot": [-6, 16, -2], "cubes": mallet["cubes"]},
    ], 64, 64)
    texture(src, NANO_TEX + "nano_mallet/no_light/mark_50_0.png", "nano_hammer.png")
    texture(src, NANO_TEX + "nano_mallet/light/mark_50_0.png", "nano_hammer_glow.png", glow=True)

    # Nano shield: the addon's two shield faces (19 x 14 texels each) on one plate, 1:1 texels,
    # on the outer side of the left forearm. The addon's own cube is a 35 x 19 slab behind the arm.
    # The bone turns the plate so the texture's top (the shield point) faces the forearm's front:
    # in the guard pose (forearm across the chest) the point is up.
    write_geo("geo/ironman/nano/nano_shield.geo.json", [
        {"name": "armorLeftArm", "pivot": [5, 22, 0]},
        {"name": "shield", "parent": "armorLeftArm", "pivot": [8.75, 16, 0], "rotation": [90, 0, 0], "cubes": [{
            "origin": [8.45, 9, -9.5], "size": [0.6, 14, 19],
            "uv": {
                "east": {"uv": [58, 28], "uv_size": [-19, 14]}, "west": {"uv": [38, 28], "uv_size": [-19, 14]},
                "north": {"uv": [39, 28], "uv_size": [-1, 14]}, "south": {"uv": [59, 28], "uv_size": [-1, 14]},
                "up": {"uv": [39, 9], "uv_size": [-1, 19]}, "down": {"uv": [40, 28], "uv_size": [-1, -19]},
            },
        }]},
    ], 64, 64)
    texture(src, "textures/models/shields/nano_shield.png", "nano_shield.png")

    # Missile flaps: shoulder_rockets bone of full_body; drawn with the worn suit skin.
    rockets = bones_of(src, "geo/armor_models/iron_man/full_body/main.geo.json")["shoulder_rockets"]
    write_geo("geo/ironman/nano/shoulder_rockets.geo.json", [
        {"name": "armorBody", "pivot": [0, 24, 0]},
        {"name": "shoulder_rockets", "parent": "armorBody", "pivot": rockets["pivot"], "cubes": rockets["cubes"]},
    ], 64, 64)

    # Forearm rocket launcher: third-person and first-person geo (the first-person file's bb_main has no player part).
    for name in ("rocket_launcher", "rocket_launcher_first_person"):
        launcher = bones_of(src, NANO + "each_arms/right/" + name + ".geo.json")["rocket_launcher"]
        write_geo("geo/ironman/nano/" + name + ".geo.json", [
            arm("Right"),
            {"name": "rocket_launcher", "parent": "armorRightArm", "pivot": launcher["pivot"], "cubes": launcher["cubes"]},
        ], 64, 64)
    texture(src, NANO_TEX + "nano_rocket_launcher/no_light/mark_50_0.png", "rocket_launcher.png")

    # Nano stabilizer rods on the back (hover).
    stab = bones_of(src, NANO + "nano_stabilizer.geo.json")
    write_geo("geo/ironman/nano/stabilizer.geo.json", [{"name": "armorBody", "pivot": [0, 24, 0]}] + [
        {"name": b, "parent": "armorBody", "pivot": stab[b]["pivot"], "cubes": stab[b]["cubes"]} for b in ("bone", "bone2", "bone3", "bone4")
    ], 64, 64)
    texture(src, NANO_TEX + "nano_stabilizer/no_light/mark_50_0.png", "stabilizer.png")
    texture(src, NANO_TEX + "nano_stabilizer/light/mark_50_0.png", "stabilizer_glow.png", glow=True)

    # Mark energy shield: the same plate filled with the addon's hex force field (tinted, additive).
    write_b64("textures/entity/ironman/marks/energy_shield.png", energy_plate(src))


def energy_plate(src):
    """nano_shield.png alpha, filled with the tiled ark_force_field hexes in the repulsor colour (premultiplied)."""
    shape = Image.open(os.path.join(src, "textures/models/shields/nano_shield.png")).convert("RGBA")
    field = Image.open(os.path.join(src, "textures/models/electro_magnetic_shield/ark_force_field.png")).convert("RGBA")
    out = Image.new("RGBA", shape.size, (0, 0, 0, 0))
    sp, fp, op = shape.load(), field.load(), out.load()
    for y in range(shape.height):
        for x in range(shape.width):
            if sp[x, y][3] == 0:
                continue
            hex_alpha = fp[x % field.width, y % field.height][3]
            k = 0.35 + 0.65 * hex_alpha / 255.0
            op[x, y] = tuple(int(c * k) for c in FIELD_RGB) + (255,)
    return out


if __name__ == "__main__":
    main(sys.argv[1])
