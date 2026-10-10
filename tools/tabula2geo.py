#!/usr/bin/env python3
"""Tabula (.tbl, projVersion 5) to Bedrock geo.json for BakedGeoModel (Iron Man stages 4-5).

A .tbl is a zip with model.json: nested parts (rotation point, rotation in
degrees, boxes with offset, size, per-axis expand and box UV). Conversion:

- Tabula is the Java model space (y down, the player's neck at y 0, feet at 24).
  Bedrock y = 24 - y; x and z are kept (BakedGeoModel mirrors x itself, the
  same way it reads Blockbench player geo). Rotation angles carry over as they
  are: BakedGeoModel's (-x, -y, z) turn convention equals a Java ModelPart turn.
- Every part becomes a bone; its pivot is the sum of the rotation points above
  it (Bedrock pivots and cube origins are absolute, in the rest pose).
- Repeated part names get a numeric suffix (Bedrock bone names are unique).
- ``limbs=True``: biped limb parts (head, body, rightArm, leftArm, rightLeg,
  leftLeg) become the top-level armor bones (armorHead, ...) that
  PlayerGeoLayer attaches to the player model; empty, unrotated parts above
  them are dropped.
- Expand is Bedrock inflate; a non-uniform expand grows the box by the extra
  per axis (UV keeps the box layout of the integer size).

- ``attach=BONE``: the whole model (built in body coordinates) goes under one
  armor bone, like a FiskHeroes model effect anchored to that limb.

Run: python3 tools/tabula2geo.py IN.tbl OUT.geo.json [--limbs | --attach BONE] [--identifier NAME]
"""
import argparse
import json
import os
import re
import sys
import zipfile

LIMBS = {"head": "armorHead", "body": "armorBody", "rightarm": "armorRightArm", "leftarm": "armorLeftArm",
         "rightleg": "armorRightLeg", "leftleg": "armorLeftLeg"}
GROUND = 24.0


def load(path):
    with zipfile.ZipFile(path) as z:
        return json.loads(z.read("model.json"))


def r4(v):
    v = round(float(v), 4)
    return 0.0 if v == 0 else v


def cube(part_rp, box, part):
    ex, ey, ez = (float(box.get("expand" + a, 0.0)) for a in "XYZ")
    inflate = min(ex, ey, ez)
    dx, dy, dz = (float(box["dim" + a]) for a in "XYZ")
    x0 = part_rp[0] + float(box["posX"]) - (ex - inflate)
    y0 = part_rp[1] + float(box["posY"]) - (ey - inflate)
    z0 = part_rp[2] + float(box["posZ"]) - (ez - inflate)
    sx, sy, sz = dx + 2 * (ex - inflate), dy + 2 * (ey - inflate), dz + 2 * (ez - inflate)
    out = {
        "origin": [r4(x0), r4(GROUND - (y0 + sy)), r4(z0)],
        "size": [r4(sx), r4(sy), r4(sz)],
        "uv": [int(part.get("texOffX", 0)) + int(box.get("texOffX", 0)), int(part.get("texOffY", 0)) + int(box.get("texOffY", 0))],
    }
    if inflate:
        out["inflate"] = r4(inflate)
    if part.get("mirror"):
        out["mirror"] = True
    return out


ARMOR_PIVOTS = {"armorHead": [0, 24, 0], "armorBody": [0, 24, 0], "armorRightArm": [-5, 22, 0], "armorLeftArm": [5, 22, 0],
                "armorRightLeg": [-1.9, 12, 0], "armorLeftLeg": [1.9, 12, 0]}


def convert(model, limbs=False, attach=None):
    """Bedrock bones of a Tabula model (list of dicts, parents before children).

    ``attach``: an armor bone name; every top-level part goes under that bone
    (a model built in body coordinates and anchored to one limb).
    """
    bones = []
    used = {}

    def unique(name):
        base = re.sub(r"[^A-Za-z0-9_]", "_", name) or "part"
        n = used.get(base, 0)
        used[base] = n + 1
        return base if n == 0 else "%s_%d" % (base, n)

    def walk(part, parent, parent_rp, top):
        rp = [parent_rp[0] + float(part["rotPX"]), parent_rp[1] + float(part["rotPY"]), parent_rp[2] + float(part["rotPZ"])]
        rot = [float(part.get("rotA" + a, 0.0)) for a in "XYZ"]
        limb = LIMBS.get(part["name"].lower()) if limbs and top else None
        boxes = part.get("boxes", [])
        children = part.get("children", [])
        # Above the limbs, an empty unrotated part (the "player" root) only moves the frame.
        if limbs and top and limb is None and not boxes and not any(rot):
            for child in children:
                walk(child, None, rp, True)
            return
        name = limb if limb else unique(part["name"])
        bone = {"name": name, "pivot": [r4(rp[0]), r4(GROUND - rp[1]), r4(rp[2])]}
        if parent:
            bone["parent"] = parent
        if any(rot):
            bone["rotation"] = [r4(v) for v in rot]
        if boxes:
            bone["cubes"] = [cube(rp, b, part) for b in boxes]
        bones.append(bone)
        for child in children:
            walk(child, name, rp, False)

    for part in model.get("parts", []):
        walk(part, None, [0.0, 0.0, 0.0], True)
    if attach:
        for b in bones:
            if "parent" not in b:
                b["parent"] = attach
        bones.insert(0, {"name": attach, "pivot": ARMOR_PIVOTS[attach]})
    return bones


def geometry(model, bones, identifier):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.viltrumitecore." + identifier,
                "texture_width": int(model.get("texWidth", 64)),
                "texture_height": int(model.get("texHeight", 64)),
            },
            "bones": bones,
        }],
    }


def write(path, doc):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=2)
        f.write("\n")


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("tbl")
    ap.add_argument("out")
    ap.add_argument("--limbs", action="store_true")
    ap.add_argument("--attach", choices=sorted(ARMOR_PIVOTS))
    ap.add_argument("--identifier")
    args = ap.parse_args(argv)
    model = load(args.tbl)
    ident = args.identifier or os.path.basename(args.out).split(".")[0]
    write(args.out, geometry(model, convert(model, args.limbs, args.attach), ident))


if __name__ == "__main__":
    main(sys.argv[1:])
