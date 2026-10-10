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
- geo/ironman/marks/empty_suit{,_interior}.geo.json + animations/ironman/empty_suit.animation.json:
  the opening shell (tools/assets/opening_shell.py): full_body plus the helmet box cut into a
  back half and hinged front plates, with an interior lining in every half;
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
import opening_shell  # noqa: E402

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


# Opening shell (spec §12.5): door bone -> (top bone, hinge pivot, hinge axis, open angle in degrees).
DOORS = {
    "body_door_left": ("armorbody", [4.5, 18, -3.5], 1, -110.0), "body_door_right": ("armorbody", [-4.5, 18, -3.5], 1, 110.0),
    "left_arm_door": ("armorleftarm", [8.5, 18, -2.8], 1, -100.0), "right_arm_door": ("armorrightarm", [-8.5, 18, -2.8], 1, 100.0),
    "left_leg_door": ("armorleftleg", [4.4, 6, -2.5], 1, -95.0), "right_leg_door": ("armorrightleg", [-4.4, 6, -2.5], 1, 95.0),
    "faceplate": ("armorhead", [0, 32.5, 0], 0, 105.0),
}
# Interior atlas regions in 64 px space (the 128 px ironman_interior.png has 2 texels per unit): u, v, w, h.
LINING = {
    "torso": {"south": (0, 0, 8, 13), "north": (0, 0, 8, 13), "east": (32, 0, 3, 13), "west": (32, 0, 3, 13), "up": (36, 0, 8, 3), "down": (36, 0, 8, 3)},
    "limb": {"south": (8, 0, 4, 12), "north": (8, 0, 4, 12), "east": (32, 0, 2, 12), "west": (32, 0, 2, 12), "up": (36, 4, 4, 2), "down": (36, 4, 4, 2)},
    "helmet": {"south": (12, 0, 8, 8), "north": (12, 0, 8, 8), "east": (32, 0, 4, 8), "west": (32, 0, 4, 8), "up": (36, 0, 8, 4), "down": (36, 0, 8, 4)},
    "torso_door": {"north": (20, 0, 4, 13), "south": (20, 0, 4, 13), "east": (32, 0, 3, 13), "west": (32, 0, 3, 13), "up": (36, 0, 4, 3), "down": (36, 0, 4, 3)},
    "limb_door": {"north": (24, 0, 4, 12), "south": (24, 0, 4, 12), "east": (32, 0, 2, 12), "west": (32, 0, 2, 12), "up": (36, 4, 4, 2), "down": (36, 4, 4, 2)},
    "faceplate": {"north": (12, 16, 8, 8), "south": (12, 16, 8, 8), "east": (32, 0, 4, 8), "west": (32, 0, 4, 8), "up": (36, 0, 8, 4), "down": (36, 0, 8, 4)},
}
INF = 1.0e6


def empty_suit(src):
    """Satsu full_body + helmet box cut into a back half and hinged front plates, with an interior lining."""
    fb = {b["name"]: b for b in geometry(src, ARMOR + "iron_man/full_body/main.geo.json")}
    helmet = [c for b in geometry(src, ARMOR + "all_helmet/main.geo.json") if b["name"] == "red" for c in b["cubes"]]
    tops = {"armorhead": [0, 24, 0], "armorbody": [0, 18, 0], "armorrightarm": [-5, 22, 0], "armorleftarm": [5, 22, 0],
            "armorrightleg": [-1.9, 12, 0], "armorleftleg": [1.9, 12, 0]}
    shell = {name: [] for name in list(tops) + list(DOORS)}
    inner = {name: [] for name in list(tops) + list(DOORS)}

    def inner_layer(c):
        return c.get("inflate", 0.0) < 0.4

    def split_front_back(c, back_bone, door_bone, kind, door_kind, open_extra=()):
        back = opening_shell.cut(c, 2, 0.0, INF, drop_low=True)
        front = opening_shell.cut(c, 2, -INF, 0.0, drop_high=True)
        if back:
            shell[back_bone].append(back)
            if inner_layer(c):
                inner[back_bone].append(opening_shell.lining(back, {"north"} | set(open_extra), LINING[kind]))
        if front:
            shell[door_bone].append(front)
            if inner_layer(c):
                inner[door_bone].append(opening_shell.lining(front, {"south"} | set(open_extra), LINING[door_kind]))

    for c in fb["armorBody"]["cubes"]:
        if c.get("rotation"):
            shell["body_door_left" if c["origin"][0] > 0 else "body_door_right"].append(copy.deepcopy(c))
            continue
        back = opening_shell.cut(c, 2, 0.0, INF, drop_low=True)
        front = opening_shell.cut(c, 2, -INF, 0.0, drop_high=True)
        if back:
            shell["armorbody"].append(back)
            if inner_layer(c):
                inner["armorbody"].append(opening_shell.lining(back, {"north"}, LINING["torso"]))
        for door, (a0, a1, low, high, edge) in {"body_door_left": (0.0, INF, True, False, "west"),
                                                "body_door_right": (-INF, 0.0, False, True, "east")}.items():
            half = front and opening_shell.cut(front, 0, a0, a1, drop_low=low, drop_high=high)
            if half:
                shell[door].append(half)
                if inner_layer(c):
                    inner[door].append(opening_shell.lining(half, {"south", edge}, LINING["torso_door"]))
    for side in ("Right", "Left"):
        for c in fb["armor%sArm" % side]["cubes"]:
            split_front_back(c, "armor%sarm" % side.lower(), "%s_arm_door" % side.lower(), "limb", "limb_door")
        for c in fb["armor%sLeg" % side]["cubes"]:
            split_front_back(c, "armor%sleg" % side.lower(), "%s_leg_door" % side.lower(), "limb", "limb_door")
    for c in helmet:
        split_front_back(c, "armorhead", "faceplate", "helmet", "faceplate", open_extra=("down",))

    def bones(cubes):
        out = [{"name": name, "pivot": pivot, "cubes": cubes[name]} for name, pivot in tops.items()]
        out += [{"name": door, "parent": top, "pivot": pivot, "cubes": cubes[door]} for door, (top, pivot, _, _) in DOORS.items()]
        return out

    write_geo("geo/ironman/marks/empty_suit.geo.json", bones(shell), 64, 64)
    write_geo("geo/ironman/marks/empty_suit_interior.geo.json", bones(inner), 64, 64)
    write_json("animations/ironman/empty_suit.animation.json", {"format_version": "1.8.0", "animations": {
        # Exit: plates swing open, stay open while Tony walks out, close again (EmptySuitEntity.OPEN_TICKS = 1.5 s).
        "open": door_clip([(0.0, 0.0), (0.3, 1.0), (1.1, 1.0), (1.5, 0.0)], 1.5),
        # Enter: open fast, Tony steps in, close (ENTER_TICKS = 0.5 s).
        "enter": door_clip([(0.0, 0.0), (0.15, 1.0), (0.3, 1.0), (0.5, 0.0)], 0.5),
    }})


def door_clip(keys, length):
    bones = {}
    for door, (_, _, axis, angle) in DOORS.items():
        frames = {}
        for t, k in keys:
            rot = [0.0, 0.0, 0.0]
            rot[axis] = round(angle * k, 3)
            frames["%g" % t] = {"vector": rot, "easing": "easeOutCubic" if k else "easeInOutSine"}
        bones[door] = {"rotation": frames}
    return {"loop": "hold_on_last_frame", "animation_length": length, "bones": bones}


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
