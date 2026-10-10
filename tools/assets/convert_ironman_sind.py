#!/usr/bin/env python3
"""Converts the Iron Man Sind assets (Sind Iron Man pack 2.2.0, FiskHeroes) with tools/tabula2geo.py.

Run: python3 tools/assets/convert_ironman_sind.py SIND_DIR
where SIND_DIR holds the pack's ``models/`` and ``textures/`` (``assets/sind``).
Pairs follow the pack's renderers (``renderers/heroes/mark42.js``, ``mark7.js``,
``external/iron_man_utils.js``, ``mark48.js``, ``external/hulkbuster.js``).
Every output is listed in tools/assets/ironman_sources.md.

Mark 42 (spec §3.1, 14 parts):
- geo/ironman/mark_42/<part>.geo.json: the 14 ``tabula/mk42`` pieces (the
  faceplate rides on the helmet), named after hero/ironman/mark/SuitPart.FOURTEEN;
  one top-level armor bone pivoted at the piece centre (the wrap scales it there);
  <part>_fire.geo.json: the piece thrusters while it flies in;
- textures/entity/ironman/mark_42/pieces{,_glow}.png (``mark42`` / ``mark42_lights``,
  the pieces' 160 px atlas), fire_0..7.png (``anim/repulsor_layer``);
- textures/entity/hero/ironman_mark_42.png: the suit skin, ``mark42_layer2`` with
  ``mark42_layer1`` over it (the renderer's "suit" texture). The pack's generic
  lights texture is not in the archive, so the glow map is baked from the pieces:
  every limb-shaped piece box copies its block of ``mark42_lights`` to the
  vanilla skin position of that limb;
- geo/ironman/marks/fist.geo.json: the rocket fist entity, the right gauntlet
  piece centred on the origin.

Mark 7 micro-laser: ``tabula/mk6/laser`` on the right forearm with ``mark7/mark7_laser``.
"""
import base64
import io
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.dirname(HERE))
import tabula2geo  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(HERE))
RES = os.path.join(ROOT, "viltrumitecore", "src", "main", "resources", "assets", "viltrumitecore")
BIN = os.path.join(ROOT, "viltrumitecore", "src", "main", "binassets", "assets", "viltrumitecore")
# SuitPart.FOURTEEN name -> Sind mk42 piece.
MK42 = {
    "left_boot": "boots_left", "right_boot": "boots_right", "left_thigh": "pants_left", "right_thigh": "pants_right",
    "left_gauntlet": "left_gauntlet", "right_gauntlet": "right_gauntlet", "left_upper_arm": "left_elbow", "right_upper_arm": "right_elbow",
    "left_shoulder": "left_shoulder", "right_shoulder": "right_shoulder", "abdomen": "chest_lower_back", "chest": "chest_front",
    "back": "chest_back", "helmet": "helmet",
}
# Vanilla 64x64 skin box positions: (size w, h, d), base layer UV, outer layer UV.
SKIN_BOXES = {
    "armorHead": ((8, 8, 8), (0, 0), (32, 0)),
    "armorBody": ((8, 12, 4), (16, 16), (16, 32)),
    "armorRightArm": ((4, 12, 4), (40, 16), (40, 32)),
    "armorLeftArm": ((4, 12, 4), (32, 48), (48, 48)),
    "armorRightLeg": ((4, 12, 4), (0, 16), (0, 32)),
    "armorLeftLeg": ((4, 12, 4), (16, 48), (0, 48)),
}
FIRE_FRAMES = (1, 5, 9, 13, 17, 21, 25, 29)


def tbl(src, rel):
    return tabula2geo.load(os.path.join(src, "models/tabula", rel + ".tbl"))


def tex(src, rel):
    return Image.open(os.path.join(src, "textures/heroes", rel + ".png")).convert("RGBA")


def premultiplied(image):
    image = image.convert("RGBA")
    px = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * a // 255, g * a // 255, b * a // 255, 255 if a else 0)
    return image


def write_b64(rel, image):
    out = io.BytesIO()
    image.save(out, "PNG", optimize=True)
    path = os.path.join(BIN, rel + ".b64")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="ascii") as f:
        f.write(base64.encodebytes(out.getvalue()).decode("ascii"))


def write_geo(rel, model, bones):
    tabula2geo.write(os.path.join(RES, rel), tabula2geo.geometry(model, bones, os.path.basename(rel).split(".")[0]))


def centre(bones):
    lo, hi = [1e9] * 3, [-1e9] * 3
    for b in bones:
        for c in b.get("cubes", []):
            for i in range(3):
                lo[i] = min(lo[i], c["origin"][i])
                hi[i] = max(hi[i], c["origin"][i] + c["size"][i])
    return [round((lo[i] + hi[i]) / 2, 4) for i in range(3)]


def recentre_top(bones):
    """The single top-level armor bone pivots at the piece centre (it carries no rotation)."""
    tops = [b for b in bones if "parent" not in b]
    assert len(tops) == 1 and "rotation" not in tops[0], [b["name"] for b in tops]
    tops[0]["pivot"] = centre(bones)
    return bones


def mark42_piece(src, piece):
    model = tbl(src, "mk42/" + piece)
    bones = tabula2geo.convert(model, limbs=True)
    if piece == "helmet":
        face = tabula2geo.convert(tbl(src, "mk42/faceplate"), limbs=True)
        for b in face:
            if b["name"] == "armorHead":
                continue
            b["name"] = "faceplate_" + b["name"]
            b["parent"] = "armorHead" if b["parent"] == "armorHead" else "faceplate_" + b["parent"]
            bones.append(b)
    return model, bones


def bake_glow(src, pieces):
    """Skin-layout glow: each limb-shaped piece box copies its lights block to that limb's skin position."""
    lights = tex(src, "mark42/mark42_lights")
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for bones in pieces:
        top = next(b["name"] for b in bones if "parent" not in b)
        (w, h, d), base, outer = SKIN_BOXES[top]
        for b in bones:
            for c in b.get("cubes", []):
                if [round(v) for v in c["size"]] != [w, h, d]:
                    continue
                u, v = c["uv"]
                block = lights.crop((u, v, u + 2 * (w + d), v + h + d))
                to = outer if c.get("inflate", 0) > 0.3 else base
                out.alpha_composite(block, to)
    return premultiplied(out)


def mark42(src):
    pieces = []
    for part, piece in MK42.items():
        model, bones = mark42_piece(src, piece)
        pieces.append([dict(b) for b in bones])
        write_geo("geo/ironman/mark_42/%s.geo.json" % part, model, recentre_top(bones))
        fire_model = tbl(src, "mk42/" + ("faceplate" if piece == "faceplate" else piece) + "_fire")
        write_geo("geo/ironman/mark_42/%s_fire.geo.json" % part, fire_model, tabula2geo.convert(fire_model, limbs=True))
        if part == "right_gauntlet":
            # Rocket fist entity: the gauntlet around the origin, top bone renamed (it is no player part there).
            c = centre(bones)
            fist = []
            for b in bones:
                nb = json.loads(json.dumps(b))
                nb["pivot"] = [nb["pivot"][i] - c[i] for i in range(3)]
                for cube in nb.get("cubes", []):
                    cube["origin"] = [round(cube["origin"][i] - c[i], 4) for i in range(3)]
                if nb.get("parent") == "armorRightArm":
                    nb["parent"] = "glove"
                if nb["name"] == "armorRightArm":
                    nb["name"] = "glove"
                fist.append(nb)
            write_geo("geo/ironman/marks/fist.geo.json", model, fist)

    write_b64("textures/entity/ironman/mark_42/pieces.png", tex(src, "mark42/mark42"))
    write_b64("textures/entity/ironman/mark_42/pieces_glow.png", premultiplied(tex(src, "mark42/mark42_lights")))
    for i, frame in enumerate(FIRE_FRAMES):
        write_b64("textures/entity/ironman/mark_42/fire_%d.png" % i, premultiplied(tex(src, "anim/repulsor_layer/%d" % frame)))

    skin = tex(src, "mark42/mark42_layer2")
    skin.alpha_composite(tex(src, "mark42/mark42_layer1"))
    write_b64("textures/entity/hero/ironman_mark_42.png", skin)
    write_b64("textures/entity/hero/ironman_mark_42_glow.png", bake_glow(src, pieces))


def laser(src):
    model = tbl(src, "mk6/laser")
    write_geo("geo/ironman/marks/laser_emitters.geo.json", model, tabula2geo.convert(model, attach="armorRightArm"))
    write_b64("textures/entity/ironman/marks/laser.png", tex(src, "mark7/mark7_laser"))


def main(src):
    mark42(src)
    laser(src)


if __name__ == "__main__":
    main(sys.argv[1])
