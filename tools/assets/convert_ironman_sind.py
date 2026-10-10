#!/usr/bin/env python3
"""Converts the Iron Man Sind assets (Sind Iron Man pack 2.2.0, FiskHeroes) with tools/tabula2geo.py.

Run: python3 tools/assets/convert_ironman_sind.py SIND_DIR EXTRACTED_DIR
where SIND_DIR holds the pack's ``models/`` and ``textures/`` (``assets/sind``) and
EXTRACTED_DIR is the root of ``IronMan_Hulkbuster_models.zip``.
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

Hulkbuster Mark 48 (spec §14; ``tabula/hulkbuster``, native height 68 px; the
renderer draws it at 1.7 x 32/68 so the body is 1.7 players tall):
- geo/ironman/hulkbuster/mark48.geo.json: ``everything`` plus the ``left_arm``;
  jackhammer.geo.json: the jackhammer arm (own 114 px texture: the archive's
  prepared ``jackhammer_arm_mark48`` texture, lights ``mark44/hulkbuster_arm_lights``);
  fire.geo.json (``fire`` + ``fire_leftarm``) and jackhammer_fire.geo.json;
- fp_arm.geo.json: ``fp_arm`` and ``left_arm`` scaled onto the vanilla arms;
- parts.geo.json: the docking groups, the Sind legs, torso, arms and head shrunk
  to the player's limbs (32/68);
- animations/ironman/hulkbuster/mark48.animation.json: clips baked from the
  Sind .fsk scripts with tools/fsk2anim.py (driver per clip below); grab_hold
  has no Sind source and keeps the stage 5 clip, retargeted to the Sind bones;
- textures/entity/ironman/hulkbuster{,_glow,_jackhammer,_jackhammer_glow}.png and
  textures/entity/ironman/repulsor_fire_0..7.png (shared with the Mark 42 thrusters).
"""
import math
import base64
import io
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.dirname(HERE))
import fsk2anim  # noqa: E402
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
        write_b64("textures/entity/ironman/repulsor_fire_%d.png" % i, premultiplied(tex(src, "anim/repulsor_layer/%d" % frame)))

    skin = tex(src, "mark42/mark42_layer2")
    skin.alpha_composite(tex(src, "mark42/mark42_layer1"))
    write_b64("textures/entity/hero/ironman_mark_42.png", skin)
    write_b64("textures/entity/hero/ironman_mark_42_glow.png", bake_glow(src, pieces))


def laser(src):
    model = tbl(src, "mk6/laser")
    write_geo("geo/ironman/marks/laser_emitters.geo.json", model, tabula2geo.convert(model, attach="armorRightArm"))
    write_b64("textures/entity/ironman/marks/laser.png", tex(src, "mark7/mark7_laser"))


HB = "hulkbuster/"
SIND_UNITS = 32.0 / 68.0
FP_SCALE = 1.7 * SIND_UNITS
# Docking groups: Sind subtree root -> (armor bone, vanilla limb box centre).
DOCK = {
    "rightLegBuster": ("armorRightLeg", [-1.9, 6, 0]), "leftLegBuster": ("armorLeftLeg", [1.9, 6, 0]),
    "bodyBuster": ("armorBody", [0, 18, 0]), "pivotRight": ("armorRightArm", [-6, 18, 0]),
    "pivotLeft": ("armorLeftArm", [6, 18, 0]), "headBuster": ("armorHead", [0, 28, 0]),
}
VANILLA_ARM = {"pivotRight": ("armorRightArm", [-5, 22, 0]), "pivotLeft": ("armorLeftArm", [5, 22, 0])}
# grab_hold (no Sind source): the stage 5 clip on the Sind bones; positions in Sind pixels (x 68/32).
GRAB_HOLD = {
    "rightArmBuster": {"rotation": {"0.0": [-55, 0, -12], "0.5": [-58, 0, -12], "1.0": [-55, 0, -12]}},
    "lowerRightArm": {"rotation": {"0.0": [-60, 0, 0], "0.5": [-62, 0, 0], "1.0": [-60, 0, 0]}},
    "leftArmBuster": {"rotation": {"0.0": [-55, 0, 12], "0.5": [-58, 0, 12], "1.0": [-55, 0, 12]}},
    "lowerLeftArm": {"rotation": {"0.0": [-60, 0, 0], "0.5": [-62, 0, 0], "1.0": [-60, 0, 0]}},
    "bodyBuster": {"position": {"0.0": [0, 0, 0], "0.5": [0, 0.85, 0], "1.0": [0, 0, 0]}},
}


def explicit_uv(cube):
    """Box UV as per-face UV exactly as BakedGeoModel reads a box (so the size can change without moving the UV)."""
    if isinstance(cube.get("uv"), dict):
        return cube
    u, v = cube["uv"]
    w, h, d = (round(x) for x in cube["size"])
    out = dict(cube)
    out["uv"] = {
        "north": {"uv": [u + d, v + d], "uv_size": [w, h]}, "south": {"uv": [u + 2 * d + w, v + d], "uv_size": [w, h]},
        "west": {"uv": [u, v + d], "uv_size": [d, h]}, "east": {"uv": [u + d + w, v + d], "uv_size": [d, h]},
        "up": {"uv": [u + d + w, v + d], "uv_size": [w, -d]}, "down": {"uv": [u + d, v], "uv_size": [w, d]},
    }
    return out


def subtree(bones, root, stop=()):
    kids = {}
    for b in bones:
        kids.setdefault(b.get("parent"), []).append(b["name"])
    names, todo = [], [root]
    while todo:
        n = todo.pop()
        names.append(n)
        todo.extend(k for k in kids.get(n, []) if k not in stop)
    return [json.loads(json.dumps(b)) for b in bones if b["name"] in names]


def scaled(bones, anchor, target, k):
    """Uniform scale about anchor, moved to target; UV made explicit first."""
    def at(p):
        return [round((p[i] - anchor[i]) * k + target[i], 4) for i in range(3)]

    for b in bones:
        b["pivot"] = at(b["pivot"])
        cubes = []
        for c in b.get("cubes", []):
            c = explicit_uv(c)
            c["origin"] = at(c["origin"])
            c["size"] = [round(x * k, 4) for x in c["size"]]
            if c.get("inflate"):
                c["inflate"] = round(c["inflate"] * k, 4)
            if c.get("pivot"):
                c["pivot"] = at(c["pivot"])
            cubes.append(c)
        if cubes:
            b["cubes"] = cubes
    return bones


def bbox_centre(bones):
    return centre(bones)


def merge(base, extra, prefix):
    """Adds extra's bones under base's root; names that clash get the prefix."""
    names = {b["name"] for b in base}
    root = next(b["name"] for b in base if "parent" not in b)
    ren = {b["name"]: (prefix + b["name"] if b["name"] in names else b["name"]) for b in extra}
    for b in extra:
        if "parent" not in b:
            continue
        nb = dict(b)
        nb["name"] = ren[b["name"]]
        nb["parent"] = root if b["parent"] not in ren or "parent" not in next(x for x in extra if x["name"] == b["parent"]) else ren[b["parent"]]
        base.append(nb)
    return base


def hb_bones(src, name):
    return tabula2geo.convert(tbl(src, HB + name))


def hb_rest(src):
    rest = {}

    def walk(p):
        rest[p["name"]] = (p["rotAX"], p["rotAY"], p["rotAZ"])
        for c in p.get("children", []):
            walk(c)

    for n in ("everything", "left_arm", "jackhammer"):
        for p in tbl(src, HB + n)["parts"]:
            walk(p)
    return fsk2anim.rest_of(rest)


def fsk(src, *names):
    return "\n".join(open(os.path.join(src, "models/animations/hulkbuster", n + ".fsk"), encoding="utf-8").read() for n in names)


def hulkbuster_clips(src, bone_names):
    rest = hb_rest(src)
    hb = {"data_8": 1.0}
    punches = fsk(src, "hulkbuster_punches")
    actions = fsk(src, "hulkbuster_actions")
    smash = fsk(src, "hulkbuster_smash_stomp", "hulkbuster_punches")
    # clip -> (script, drive(t), length, loop)
    clips = {
        "idle": (punches, lambda t: dict(hb, data_9=t / 5.0, data_10=t / 5.0), 5.0, True),
        "walk": (actions, lambda t: dict(hb, limbSwing=13.0 * t, limbSwingAmount=1.0), 1.0, True),
        "punch_right": (punches, lambda t: dict(hb, data_5=t / 0.6), 0.6, False),
        "punch_left": (punches, lambda t: dict(hb, data_6=t / 0.6), 0.6, False),
        "jackhammer": (fsk(src, "hulkbuster_jackhammer"), lambda t: {"data_0": 1.0, "data_1": 1.0, "data_2": t / 0.25}, 0.25, True),
        "charge": (actions, lambda t: dict(hb, data_4=t / 1.5), 1.5, False),
        "slam_launch": (smash, lambda t: dict(hb, data_0=t / 0.3, data_3=t / 0.3), 0.3, False),
        "slam_air": (smash, lambda t: dict(hb, data_0=1.0, data_3=1.0), 0.4, True),
        "slam_smash": (smash, lambda t: dict(hb, data_0=1.0, data_3=1.0, data_1=t / 0.5), 0.5, False),
        "hop": (actions, lambda t: dict(hb, data_2=math.sin(math.pi * t / 0.5)), 0.5, False),
        # Sind summon run backwards from the end: the faceplate lifts and the head plates open.
        "exit": (fsk(src, "hulkbuster_summon"), lambda t: {"data": 1.0 - 0.2 * t / 1.5}, 1.5, False),
    }
    out = {}
    for name, (script, drive, length, loop) in clips.items():
        bones = fsk2anim.bake(script, rest, drive, length, bones=bone_names)
        out[name] = {"loop": True if loop else "hold_on_last_frame", "animation_length": length, "bones": bones}
    out["grab_hold"] = {"loop": True, "animation_length": 1.0, "bones": GRAB_HOLD}
    return out


def hulkbuster(src, extracted):
    model = tbl(src, HB + "everything")
    body = merge(hb_bones(src, "everything"), hb_bones(src, "left_arm"), "l_")
    write_geo("geo/ironman/hulkbuster/mark48.geo.json", model, body)
    jack_model = tbl(src, HB + "jackhammer")
    jack = hb_bones(src, "jackhammer")
    write_geo("geo/ironman/hulkbuster/jackhammer.geo.json", jack_model, jack)
    fire_model = tbl(src, HB + "fire")
    write_geo("geo/ironman/hulkbuster/fire.geo.json", fire_model, merge(hb_bones(src, "fire"), hb_bones(src, "fire_leftarm"), "l_"))
    jf_model = tbl(src, HB + "jackhammer_fire")
    write_geo("geo/ironman/hulkbuster/jackhammer_fire.geo.json", jf_model, hb_bones(src, "jackhammer_fire"))

    fp = []
    for file, root in (("fp_arm", "pivotRight"), ("left_arm", "pivotLeft")):
        bones = hb_bones(src, file)
        piv = next(b for b in bones if b["name"] == root)
        arm, target = VANILLA_ARM[root]
        part = scaled(subtree(bones, root), piv["pivot"], target, FP_SCALE)
        for b in part:
            if b["name"] == root:
                b["name"] = arm
                b.pop("parent", None)
            elif b.get("parent") == root:
                b["parent"] = arm
        fp += part
    write_geo("geo/ironman/hulkbuster/fp_arm.geo.json", model, fp)

    dock = []
    sources = {"pivotLeft": hb_bones(src, "left_arm")}
    for root, (arm, target) in DOCK.items():
        bones = sources.get(root, body)
        part = subtree(bones, root, stop=set(DOCK) - {root})
        part = scaled(part, bbox_centre(part), target, SIND_UNITS)
        for b in part:
            if b["name"] == root:
                b["name"] = arm
                b.pop("parent", None)
                continue
            # Group prefix: the six subtrees share one file and Sind reuses part names.
            b["name"] = arm + "_" + b["name"]
            b["parent"] = arm if b["parent"] == root else arm + "_" + b["parent"]
        dock += part
    write_geo("geo/ironman/hulkbuster/parts.geo.json", model, dock)

    names = {b["name"] for b in body} | {b["name"] for b in jack}
    path = os.path.join(RES, "animations/ironman/hulkbuster/mark48.animation.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"format_version": "1.8.0", "animations": hulkbuster_clips(src, names)}, f, indent=1)
        f.write("\n")

    write_b64("textures/entity/ironman/hulkbuster.png", tex(src, "mark48/hulkbuster"))
    write_b64("textures/entity/ironman/hulkbuster_glow.png", premultiplied(tex(src, "mark48/hulkbuster_lights")))
    write_b64("textures/entity/ironman/hulkbuster_jackhammer.png",
              Image.open(os.path.join(extracted, "Sind_IronMan_Pack/hulkbuster/jackhammer_arm_mark48/obj/textures/jackhammer.png")).convert("RGBA"))
    write_b64("textures/entity/ironman/hulkbuster_jackhammer_glow.png", premultiplied(tex(src, "mark44/hulkbuster_arm_lights")))


def main(src, extracted):
    mark42(src)
    laser(src)
    hulkbuster(src, extracted)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
