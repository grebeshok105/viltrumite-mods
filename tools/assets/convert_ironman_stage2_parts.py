#!/usr/bin/env python3
"""Converts the Stage 2 Iron Man combat parts from the Satsu addon (3.6.1).

Run: python3 tools/assets/convert_ironman_stage2_parts.py SATSU_ASSETS_DIR
where SATSU_ASSETS_DIR is ``assets/satsu_iron_man_addon`` of the unpacked jar.
Model and texture pairs follow the addon's palladium render layers
(``render_layers/armor_things/iron_man/marks/mark_50/**``, ``.../model_72/electro_magnetic_shield``).
Every output is listed in tools/assets/ironman_sources.md.
"""
import copy
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


def tinted_field(src):
    """palladium:color transformer: every non-blank texel takes the colour, alpha kept."""
    image = Image.open(os.path.join(src, "textures/models/electro_magnetic_shield/ark_force_field.png")).convert("RGBA")
    px = image.load()
    for y in range(image.height):
        for x in range(image.width):
            a = px[x, y][3]
            px[x, y] = FIELD_RGB + (a,) if a else (0, 0, 0, 0)
    return premultiplied(image)


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

    # Nano shield: left-forearm plate, used as is (the addon has no first-person variant of it).
    shield = bones_of(src, "geo/armor_models/iron_man/additaments/shield/nano_shield.geo.json")["armorLeftArm"]
    write_geo("geo/ironman/nano/nano_shield.geo.json", [
        {"name": "armorLeftArm", "pivot": shield["pivot"], "cubes": shield["cubes"]},
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

    # Mark energy shield: the electro-magnetic force field; third person on the body, first person on the right arm.
    for name, bone in (("electro_magnetic_shield", "armorBody"), ("electro_magnetic_shield_first_person", "armorRightArm")):
        field = bones_of(src, "geo/marks/electro_magnetic_shield/" + name + ".geo.json")[bone]
        out = "energy_shield" if bone == "armorBody" else "energy_shield_first_person"
        write_geo("geo/ironman/marks/" + out + ".geo.json", [{"name": bone, "pivot": field["pivot"], "cubes": copy.deepcopy(field["cubes"])}], 64, 64)
    write_b64("textures/entity/ironman/marks/energy_shield.png", tinted_field(src))


if __name__ == "__main__":
    main(sys.argv[1])
