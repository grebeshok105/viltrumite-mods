#!/usr/bin/env python3
"""Converts the Stage 4 mark models from the Satsu addon (3.6.1).

Run: python3 tools/assets/convert_ironman_stage4_marks.py SATSU_ASSETS_DIR
where SATSU_ASSETS_DIR is ``assets/satsu_iron_man_addon`` of the unpacked jar.
Model/texture pairs follow the addon's palladium render layers of each mark.
Writes (every output is listed in tools/assets/ironman_sources.md):

- geo/ironman/marks/parts/<piece>.geo.json: the flying / partial suit pieces
  (``each_part``), one top-level armor bone pivoted at the piece centre so the
  wrap scales it about its middle. ``each_part`` has no back piece: the chest
  is split into its front (``chest``) and its rear faces (``back``);
- geo/ironman/marks/extras/<mark>.geo.json: the mark's own 3D parts (Mark 7
  flaps, Mark 17 heartbreaker chest, War Machine shoulder pads, Iron Heart
  plates); top-level bones lowercase with the empty-suit pivots, so the empty
  suit clip can drive them;
- geo/ironman/marks/{booster,booster_blast,turret}.geo.json and
  animations/ironman/marks/war_machine_turret.animation.json (signatures);
- geo/ironman/marks/{empty_suit,suit_expulsion}.geo.json (the empty suit shell
  is full_body plus the helmet head box; the expulsion tendrils play on exit);
- textures/entity/ironman/marks/<mark>_suit{,_glow}.png: the raw suit textures
  for the extras (the baked skins carry the helmet in the head rows, which
  the extras' UVs also use).
"""
import copy
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from convert_ironman_stage1b import bones_of, premultiplied, write_b64, write_geo  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "viltrumitecore", "src", "main", "resources", "assets", "viltrumitecore")
ARMOR = "geo/armor_models/"
TOPS = ("armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg")
# Empty-suit pivots (the open clip rotates the body about its middle).
SHELL_PIVOTS = {"armorHead": [0, 24, 0], "armorBody": [0, 18, 0], "armorRightArm": [-5, 22, 0], "armorLeftArm": [5, 22, 0],
                "armorRightLeg": [-1.9, 12, 0], "armorLeftLeg": [1.9, 12, 0]}
EXTRAS = {
    "mark_7": [ARMOR + "iron_man/mark_07/flaps.geo.json"],
    "mark_17": [ARMOR + "iron_man/heartbreakers/chest.geo.json"],
    "war_machine_mk2": [ARMOR + "war_machine/modules/war_machine_2_module.geo.json"],
    "iron_heart_mk3": [ARMOR + "iron_heart/mark_03/" + n + ".geo.json" for n in ("chest", "right_shoulder", "left_shoulder", "right_leg", "left_leg")],
}
SUITS = {
    "mark_7": "iron_man/{}/mark_7_0", "mark_17": "iron_man/{}/mark_17_0", "mark_39": "iron_man/{}/mark_39_0",
    "war_machine_mk2": "war_machine/{}/war_machine_mark_2", "iron_heart_mk3": "iron_heart/{}/mark_3",
}
# Bones of the War Machine module that belong to the shoulder gun (drawn by the turret signature part).
TURRET_BONES = {"Turret2", "turretarm", "turretarmm", "turretweapon"}


def geometry(src, rel):
    with open(os.path.join(src, rel), encoding="utf-8") as f:
        return json.load(f)["minecraft:geometry"][0]["bones"]


def prune(bones, drop=()):
    """Drops bones without cubes in their subtree and the named bones (with their subtrees)."""
    by_name = {b["name"]: b for b in bones}
    children = {}
    for b in bones:
        children.setdefault(b.get("parent"), []).append(b["name"])
    dropped = set()

    def drop_tree(name):
        dropped.add(name)
        for c in children.get(name, []):
            drop_tree(c)

    for name in drop:
        if name in by_name:
            drop_tree(name)

    def has_cubes(name):
        if name in dropped:
            return False
        return bool(by_name[name].get("cubes")) or any(has_cubes(c) for c in children.get(name, []))

    return [copy.deepcopy(b) for b in bones if b["name"] not in dropped and has_cubes(b["name"])]


def cube_bounds(cubes):
    lo, hi = [1e9] * 3, [-1e9] * 3
    for c in cubes:
        for i in range(3):
            a, b = c["origin"][i], c["origin"][i] + c["size"][i]
            lo[i], hi[i] = min(lo[i], a, b), max(hi[i], a, b)
    return [round((lo[i] + hi[i]) / 2, 4) for i in range(3)]


def faces(cube, keep):
    out = copy.deepcopy(cube)
    out["uv"] = {k: v for k, v in cube["uv"].items() if k in keep}
    return out


def piece(src, name, top, cubes):
    write_geo("geo/ironman/marks/parts/" + name + ".geo.json", [{"name": top, "pivot": cube_bounds(cubes), "cubes": cubes}], 64, 64)


def parts(src):
    def cubes_of(file, top):
        bones = geometry(src, ARMOR + "iron_man/each_part/" + file + ".geo.json")
        # The "call" child has pivot 0 and no rotation: its cubes merge into the armor bone unchanged.
        return [c for b in bones if b.get("parent") == top for c in b.get("cubes", [])]

    piece(src, "head", "armorHead", cubes_of("head", "armorHead"))
    chest = cubes_of("chest", "armorBody")
    piece(src, "chest", "armorBody", [faces(c, ("north", "east", "west", "up", "down")) for c in chest])
    piece(src, "back", "armorBody", [faces(c, ("south",)) for c in chest])
    for side in ("left", "right"):
        top = "armor" + side.capitalize() + "Arm"
        arm, shoulder = cubes_of(side + "_arm", top), cubes_of(side + "_shoulder", top)
        piece(src, side + "_arm", top, arm)
        piece(src, side + "_shoulder", top, shoulder)
        piece(src, side + "_arm_full", top, arm + shoulder)
        piece(src, side + "_leg", "armor" + side.capitalize() + "Leg", cubes_of(side + "_leg", "armor" + side.capitalize() + "Leg"))


def merged(src, files, drop=()):
    """Bones of several armor geo files under shared lowercase top bones with the shell pivots."""
    out = {t: {"name": t.lower(), "pivot": SHELL_PIVOTS[t]} for t in TOPS}
    rest = []
    for i, rel in enumerate(files):
        bones = prune(geometry(src, rel), drop)
        names = {b["name"] for b in bones}
        rename = {n: n if n in TOPS else ("%s_%d" % (n, i) if len(files) > 1 else n) for n in names}
        for b in bones:
            if b["name"] in TOPS:
                if b.get("cubes"):
                    out[b["name"]].setdefault("cubes", []).extend(b["cubes"])
                continue
            b["name"] = rename[b["name"]]
            parent = b.get("parent")
            b["parent"] = parent.lower() if parent in TOPS else rename.get(parent, parent)
            rest.append(b)
    used = {b["parent"] for b in rest} | {b["name"] for b in rest}
    tops = [v for t, v in out.items() if v.get("cubes") or v["name"] in used]
    return tops + rest


def extras(src):
    for key, files in EXTRAS.items():
        drop = TURRET_BONES if key == "war_machine_mk2" else ()
        write_geo("geo/ironman/marks/extras/" + key + ".geo.json", merged(src, files, drop), 64, 64)
    for key, path in SUITS.items():
        write_b64("textures/entity/ironman/marks/%s_suit.png" % key, Image.open(os.path.join(src, "textures/models/" + path.format("no_light") + ".png")).convert("RGBA"))
        write_b64("textures/entity/ironman/marks/%s_suit_glow.png" % key,
                  premultiplied(Image.open(os.path.join(src, "textures/models/" + path.format("light") + ".png"))))


def signatures(src):
    # Starboost: the Mark 39 jetpack and its two jet flames (child bone pivoted at the nozzles for scaleY).
    write_geo("geo/ironman/marks/booster.geo.json", merged(src, [ARMOR + "iron_man/mark_39/main.geo.json"]), 64, 64)
    flames = bones_of(src, "geo/flames/marks/mark_39/main.geo.json")["armorBody"]["cubes"]
    top = max(c["origin"][1] + c["size"][1] for c in flames)
    centre = cube_bounds(flames)
    write_geo("geo/ironman/marks/booster_blast.geo.json", [
        {"name": "armorBody", "pivot": [0, 24, 0]},
        {"name": "boosterBlast", "parent": "armorBody", "pivot": [centre[0], top, centre[2]], "cubes": flames},
    ], 4, 4)

    # War Machine shoulder gun: torret_mark_2 under a "turret" bone that turns it toward the aim.
    turret = prune(geometry(src, ARMOR + "war_machine/torret/torret_mark_2.geo.json"))
    for b in turret:
        if b["name"] == "Turret2":
            b["parent"] = "turret"
    write_geo("geo/ironman/marks/turret.geo.json", [
        {"name": "armorBody", "pivot": [0, 24, 0]},
        {"name": "turret", "parent": "armorBody", "pivot": [-2.5, 24, 3]},
    ] + [b for b in turret if b["name"] != "armorBody"], 64, 64)
    with open(os.path.join(src, "animations/war_machine/torret_mark_02/torret_animation.geo.json"), encoding="utf-8") as f:
        clips = json.load(f)["animations"]
    write_json("animations/ironman/marks/war_machine_turret.animation.json",
               {"format_version": "1.8.0", "animations": {k: clips[k] for k in ("start_on", "end")}})


def empty_suit(src):
    shell = merged(src, [ARMOR + "iron_man/full_body/main.geo.json"])
    helmet = [c for b in geometry(src, ARMOR + "all_helmet/main.geo.json") if b["name"] == "red" for c in b["cubes"]]
    for b in shell:
        if b["name"] == "armorhead":
            b["cubes"] = helmet
    if not any(b["name"] == "armorhead" for b in shell):
        shell.insert(0, {"name": "armorhead", "pivot": SHELL_PIVOTS["armorHead"], "cubes": helmet})
    write_geo("geo/ironman/marks/empty_suit.geo.json", shell, 64, 64)
    tendrils = bones_of(src, ARMOR + "iron_man/model_50/abilities/suit_expulsion.geo.json")["armorBody"]["cubes"]
    write_geo("geo/ironman/marks/suit_expulsion.geo.json", [{"name": "armorbody", "pivot": [0, 18, 0], "cubes": tendrils}], 64, 64)


def write_json(rel, doc):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=2)
        f.write("\n")


def main(src):
    parts(src)
    extras(src)
    signatures(src)
    empty_suit(src)


if __name__ == "__main__":
    main(sys.argv[1])
