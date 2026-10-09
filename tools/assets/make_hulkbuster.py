#!/usr/bin/env python3
"""Iron Man Stage 5 Hulkbuster Mark 48 art (own procedural model, generated, committed):

- geo/ironman/hulkbuster/mark48.geo.json: the big body, drawn by HulkbusterRenderer at 1.7x;
- geo/ironman/hulkbuster/fp_arm.geo.json: both big arms for first person (armorRightArm/armorLeftArm);
- geo/ironman/hulkbuster/parts.geo.json: the four docking groups on the normal-size player (armor bones);
- animations/ironman/hulkbuster/mark48.animation.json: idle, walk, punch_*, jackhammer, charge,
  grab_hold, slam_*, hop, exit;
- textures/entity/ironman/hulkbuster.png and hulkbuster_glow.png (binassets, base64).

Geometry convention (client/anim/geo/BakedGeoModel, same as the Stage 4 plates): JSON x = vanilla
player model x, JSON y up with the feet at 0, pixels (1/16 block). Pivots and cube origins are
absolute in that space. Poses are written as internal turns (degrees, body frame: front = -z,
right = +x, positive X turns the front up, positive Y turns the front left, positive Z lifts +x
outward) and converted to JSON: rotations X and Y negated (AnimationParser), positions x negated
(AnimRenderer). Position values are pixels.

Run: python3 tools/assets/make_hulkbuster.py
"""
import base64
import copy
import io
import json
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
MAIN = os.path.join(HERE, '..', '..', 'viltrumitecore', 'src', 'main')
RES = os.path.join(MAIN, 'resources', 'assets', 'viltrumitecore')
BIN = os.path.join(MAIN, 'binassets', 'assets', 'viltrumitecore')
ATLAS = 512
SEED = 4848

RED = (164, 22, 28, 255)
RED_EDGE = (88, 10, 16, 255)
GOLD = (222, 168, 40, 255)
STEEL = (70, 76, 88, 255)
STEEL_EDGE = (38, 42, 50, 255)
CLEAR = (0, 0, 0, 0)
GLOW_BLACK = (0, 0, 0, 255)


def bone(name, parent, pivot, cubes=()):
    return {'name': name, 'parent': parent, 'pivot': list(pivot), 'cubes': list(cubes)}


def cube(origin, size, material, glow=None):
    return {'origin': list(origin), 'size': list(size), 'material': material, 'glow': glow}


def arm_bones(side):
    """One arm chain (side = 'right' or 'left'), absolute armor coordinates; mirrored by x."""
    upper_x = (-18, -9) if side == 'right' else (9, 18)
    fore_x = (-19, -8) if side == 'right' else (8, 19)
    fist_x = (-20, -7) if side == 'right' else (7, 20)
    pivot_x = -13.5 if side == 'right' else 13.5
    return [
        bone(f'{side}_upper_arm', 'torso', (pivot_x, 22, 0),
             [cube((upper_x[0], 14, -5), (9, 8, 10), 'armor')]),
        bone(f'{side}_forearm', f'{side}_upper_arm', (pivot_x, 14, 0),
             [cube((fore_x[0], 5, -6), (11, 9, 12), 'armor')]),
        bone(f'{side}_fist', f'{side}_forearm', (pivot_x, 5, 0),
             [cube((fist_x[0], 1, -7), (13, 4, 14), 'armor', glow='palm')]),
    ]


def jackhammer_bone():
    return bone('jackhammer', 'right_fist', (-13, 3, -7), [cube((-16, 2, -16), (6, 3, 9), 'steel')])


def body_bones():
    bones = [
        bone('torso', None, (0, 12, 0), [
            cube((-12, 12, -7), (24, 14, 14), 'armor'),
            cube((-10, 8, -6), (20, 4, 12), 'armor'),
            cube((-16, 20, -7), (5, 5, 14), 'armor'),
            cube((11, 20, -7), (5, 5, 14), 'armor'),
            cube((-4, 15, -9), (8, 7, 2), 'steel', glow='ring'),
            cube((-2, 16, -10), (4, 4, 1), 'steel', glow='reactor'),
            cube((-10, 12, 9), (6, 10, 3), 'steel'),
            cube((4, 12, 9), (6, 10, 3), 'steel'),
        ]),
        bone('head', 'torso', (0, 26, 0), [
            cube((-3, 26, -3), (6, 6, 6), 'armor', glow='visor'),
            cube((-2, 24, -2), (4, 2, 4), 'steel'),
        ]),
        bone('back_plate_right', 'torso', (0, 12, 7), [cube((-12, 12, 7), (12, 14, 2), 'armor')]),
        bone('back_plate_left', 'torso', (0, 12, 7), [cube((0, 12, 7), (12, 14, 2), 'armor')]),
        bone('flame_back_right', 'torso', (-7, 13, 12), [cube((-9, 14, 12), (4, 6, 6), 'flame', glow='flame')]),
        bone('flame_back_left', 'torso', (7, 13, 12), [cube((5, 14, 12), (4, 6, 6), 'flame', glow='flame')]),
        bone('right_leg', None, (-6, 12, 0), [cube((-10, 0, -4), (8, 12, 8), 'armor')]),
        bone('right_foot', 'right_leg', (-6, 3, 0), [cube((-11, 0, -8), (10, 3, 12), 'steel')]),
        bone('flame_feet_right', 'right_foot', (-6, 0, 0), [cube((-8, -9, -4), (4, 9, 8), 'flame', glow='flame')]),
        bone('left_leg', None, (6, 12, 0), [cube((2, 0, -4), (8, 12, 8), 'armor')]),
        bone('left_foot', 'left_leg', (6, 3, 0), [cube((1, 0, -8), (10, 3, 12), 'steel')]),
        bone('flame_feet_left', 'left_foot', (6, 0, 0), [cube((4, -9, -4), (4, 9, 8), 'flame', glow='flame')]),
    ]
    bones += arm_bones('right')
    bones += arm_bones('left')
    bones.append(jackhammer_bone())
    return bones


def fp_bones():
    """Arms only, under dummy top bones mapped to the vanilla arms by PlayerBoneMap."""
    right = arm_bones('right')
    left = arm_bones('left')
    for b in right:
        if b['parent'] == 'torso':
            b['parent'] = 'armorRightArm'
    for b in left:
        if b['parent'] == 'torso':
            b['parent'] = 'armorLeftArm'
    return [
        bone('armorRightArm', None, (-5, 22, 0)),
        bone('armorLeftArm', None, (5, 22, 0)),
    ] + right + left + [jackhammer_bone()]


def parts_bones():
    """Docking shells for the normal-size player: legs, torso, arms, helmet (armor bones)."""
    return [
        bone('armorRightLeg', None, (-1.9, 12, 0), [cube((-6, 0, -4), (6, 13, 8), 'armor')]),
        bone('armorLeftLeg', None, (1.9, 12, 0), [cube((0, 0, -4), (6, 13, 8), 'armor')]),
        bone('armorBody', None, (0, 24, 0), [
            cube((-7, 12, -5), (14, 13, 10), 'armor'),
            cube((-2, 18, -6), (4, 4, 1), 'steel', glow='reactor'),
        ]),
        bone('armorRightArm', None, (-5, 22, 0), [cube((-9, 11, -4), (6, 14, 8), 'armor')]),
        bone('armorLeftArm', None, (5, 22, 0), [cube((3, 11, -4), (6, 14, 8), 'armor')]),
        bone('armorHead', None, (0, 24, 0), [cube((-5, 24, -5), (10, 10, 10), 'armor', glow='visor')]),
    ]


class Atlas:
    """Shelf packer: every box gets a region of (2*sz + 2*sx) x (sz + sy) for the Bedrock box UV layout."""

    def __init__(self, size):
        self.size = size
        self.x = 0
        self.y = 0
        self.row = 0

    def place(self, w, h):
        if self.x + w > self.size:
            self.x = 0
            self.y += self.row
            self.row = 0
        if self.y + h > self.size:
            raise ValueError('hulkbuster atlas full')
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v


def face_rects(u, v, sx, sy, sz):
    return {
        'top': (u + sz, v, sx, sz),
        'bottom': (u + sz + sx, v, sx, sz),
        'right': (u, v + sz, sz, sy),
        'front': (u + sz, v + sz, sx, sy),
        'left': (u + sz + sx, v + sz, sz, sy),
        'back': (u + 2 * sz + sx, v + sz, sx, sy),
    }


def assign_uv(bones, atlas, placed):
    """Give every cube a box-UV origin. Identical cube specs share one region (FP arms reuse the body arms)."""
    for b in bones:
        out = []
        for c in b['cubes']:
            key = (tuple(c['origin']), tuple(c['size']))
            if key not in placed:
                sx, sy, sz = c['size']
                w = int(2 * sz + 2 * sx)
                h = int(sz + sy)
                uv = atlas.place(w, h)
                placed[key] = (uv, sx, sy, sz, c['material'], c['glow'])
            uv, sx, sy, sz, material, glow = placed[key]
            out.append({'cube': c, 'uv': uv, 'size': (sx, sy, sz), 'material': material, 'glow': glow})
        b['_cubes'] = out
    return bones


def to_geo(bones, identifier):
    out = []
    for b in bones:
        entry = {'name': b['name'], 'pivot': b['pivot']}
        if b['parent']:
            entry['parent'] = b['parent']
        cubes = []
        for c in b.get('_cubes', []):
            cubes.append({
                'origin': list(c['cube']['origin']),
                'size': list(c['size']),
                'uv': [c['uv'][0], c['uv'][1]],
            })
        if cubes:
            entry['cubes'] = cubes
        out.append(entry)
    return {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {'identifier': identifier, 'texture_width': ATLAS, 'texture_height': ATLAS},
            'bones': out,
        }],
    }


def paint_armor(img, u, v, sx, sy, sz, material):
    base, edge, trim = (RED, RED_EDGE, GOLD) if material == 'armor' else (STEEL, STEEL_EDGE, STEEL_EDGE)
    rects = face_rects(u, v, sx, sy, sz)
    for name, (x, y, w, h) in rects.items():
        if w <= 0 or h <= 0:
            continue
        img.paste(base, (x, y, x + w, y + h))
        if w >= 3 and h >= 3:
            img.paste(edge, (x, y, x + w, y + 1))
            img.paste(edge, (x, y + h - 1, x + w, y + h))
            img.paste(edge, (x, y, x + 1, y + h))
            img.paste(edge, (x + w - 1, y, x + w, y + h))
        if material == 'armor' and w >= 5 and h >= 5 and name in ('front', 'back'):
            img.paste(trim, (x + 1, y + h // 2, x + w - 1, y + h // 2 + 1))
        if material == 'armor' and w >= 3 and h >= 3 and name in ('top', 'bottom'):
            img.paste(trim, (x + 1, y + h // 2, x + w - 1, y + h // 2 + 1))


def paint_glow(img, u, v, sx, sy, sz, tag):
    rects = face_rects(u, v, sx, sy, sz)
    x, y, w, h = rects['front']
    if tag == 'reactor':
        img.paste((190, 246, 255, 255), (x, y, x + w, y + h))
    elif tag == 'ring':
        img.paste((70, 210, 255, 255), (x, y, x + w, y + 1))
        img.paste((70, 210, 255, 255), (x, y + h - 1, x + w, y + h))
        img.paste((70, 210, 255, 255), (x, y, x + 1, y + h))
        img.paste((70, 210, 255, 255), (x + w - 1, y, x + w, y + h))
    elif tag == 'visor':
        row = h // 2
        img.paste((96, 236, 255, 255), (x + 1, y + row - 1, x + w - 1, y + row + 1))
    elif tag == 'palm':
        img.paste((70, 214, 255, 255), (x + 2, y + 1, x + w - 2, y + h - 1))
        img.paste((200, 250, 255, 255), (x + w // 2 - 1, y + h // 2 - 1, x + w // 2 + 1, y + h // 2 + 1))
    elif tag == 'flame':
        for name, (fx, fy, fw, fh) in rects.items():
            img.paste((120, 210, 255, 255), (fx, fy, fx + fw, fy + fh))
            img.paste((230, 248, 255, 255), (fx + fw // 2, fy, fx + fw // 2 + 1, fy + fh))


def paint(placed_items, body_size):
    rng = random.Random(SEED)
    albedo = Image.new('RGBA', (ATLAS, ATLAS), CLEAR)
    glow = Image.new('RGBA', (ATLAS, ATLAS), GLOW_BLACK)
    for (u, v), sx, sy, sz, material, gl in placed_items:
        if material == 'flame':
            albedo.paste(CLEAR, (u, v, u + 2 * sz + 2 * sx, v + sz + sy))
        else:
            paint_armor(albedo, u, v, sx, sy, sz, material)
        if gl:
            paint_glow(glow, u, v, sx, sy, sz, gl)
    px = albedo.load()
    for y in range(ATLAS):
        for x in range(ATLAS):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            n = rng.randint(-5, 5)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), 255)
    return albedo, glow


def png_b64(img):
    buf = io.BytesIO()
    img.save(buf, format='PNG', optimize=True)
    return base64.b64encode(buf.getvalue()).decode('ascii')


# ---- animation ------------------------------------------------------------------------------

def clip(length, kind, bones):
    return {'length': length, 'kind': kind, 'bones': bones}


def ch(*pairs):
    return {t: v for t, v in pairs}


def mirror(bones):
    out = {}
    for name, channels in bones.items():
        new = name.replace('right_', 'TMP_').replace('left_', 'right_').replace('TMP_', 'left_')
        rot = channels.get('rot')
        pos = channels.get('pos')
        out[new] = {
            'rot': {t: (v[0], -v[1], -v[2]) for t, v in rot.items()} if rot else None,
            'pos': {t: (-v[0], v[1], v[2]) for t, v in pos.items()} if pos else None,
        }
    return out


def rot(*pairs):
    return {'rot': ch(*pairs)}


def pos(*pairs):
    return {'pos': ch(*pairs)}


def both(a, b):
    out = dict(a)
    out.update(b)
    return out


def clips():
    idle = clip(2.0, 'loop', {
        'torso': both(pos((0.0, (0, 0, 0)), (1.0, (0, 0.5, 0)), (2.0, (0, 0, 0))), {}),
        'right_upper_arm': rot((0.0, (0, 0, 4)), (1.0, (0, 0, 6)), (2.0, (0, 0, 4))),
        'left_upper_arm': rot((0.0, (0, 0, -4)), (1.0, (0, 0, -6)), (2.0, (0, 0, -4))),
    })
    walk = clip(1.0, 'loop', {
        'right_leg': rot((0.0, (28, 0, 0)), (0.5, (-28, 0, 0)), (1.0, (28, 0, 0))),
        'left_leg': rot((0.0, (-28, 0, 0)), (0.5, (28, 0, 0)), (1.0, (-28, 0, 0))),
        'right_upper_arm': rot((0.0, (-14, 0, 0)), (0.5, (14, 0, 0)), (1.0, (-14, 0, 0))),
        'left_upper_arm': rot((0.0, (14, 0, 0)), (0.5, (-14, 0, 0)), (1.0, (14, 0, 0))),
        'torso': both(rot((0.0, (0, -3, 0)), (0.5, (0, 3, 0)), (1.0, (0, -3, 0))),
                      pos((0.0, (0, 0, 0)), (0.25, (0, 0.8, 0)), (0.5, (0, 0, 0)), (0.75, (0, 0.8, 0)), (1.0, (0, 0, 0)))),
    })
    punch_right = clip(0.6, 'once', {
        'right_upper_arm': rot((0.0, (0, 0, 0)), (0.15, (-40, 0, 0)), (0.25, (72, 0, 12)), (0.35, (62, 0, 8)), (0.6, (0, 0, 0))),
        'right_forearm': rot((0.0, (0, 0, 0)), (0.15, (-25, 0, 0)), (0.25, (15, 0, 0)), (0.35, (10, 0, 0)), (0.6, (0, 0, 0))),
        'torso': both(rot((0.0, (0, 0, 0)), (0.15, (0, -12, 0)), (0.25, (0, 18, 0)), (0.6, (0, 0, 0))),
                      pos((0.0, (0, 0, 0)), (0.25, (0, 0, -2.0)), (0.6, (0, 0, 0)))),
        'left_upper_arm': rot((0.0, (0, 0, 0)), (0.15, (10, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))),
    })
    punch_left = clip(0.6, 'once', mirror(punch_right['bones']))
    jackhammer = clip(0.2, 'loop', {
        'jackhammer': pos((0.0, (0, 0, 0)), (0.1, (0, 0, -2.5)), (0.2, (0, 0, 0))),
        'right_upper_arm': rot((0.0, (6, 0, 0)), (0.05, (3, 0, 0)), (0.1, (9, 0, 0)), (0.15, (3, 0, 0)), (0.2, (6, 0, 0))),
        'right_forearm': rot((0.0, (-15, 0, 0)), (0.1, (-20, 0, 0)), (0.2, (-15, 0, 0))),
        'torso': pos((0.0, (0, 0, 0)), (0.1, (0, -0.3, 0)), (0.2, (0, 0, 0))),
    })
    charge = clip(1.5, 'once', {
        'right_upper_arm': rot((0.0, (0, 0, 0)), (0.5, (50, 0, -8)), (1.5, (55, 0, -8))),
        'right_forearm': rot((0.0, (0, 0, 0)), (0.5, (40, 0, 0)), (1.5, (45, 0, 0))),
        'torso': rot((0.0, (0, 0, 0)), (0.5, (0, 8, 0)), (1.5, (0, 8, 0))),
    })
    grab_hold = clip(1.0, 'loop', {
        'right_upper_arm': rot((0.0, (55, 0, -12)), (0.5, (58, 0, -12)), (1.0, (55, 0, -12))),
        'right_forearm': rot((0.0, (60, 0, 0)), (0.5, (62, 0, 0)), (1.0, (60, 0, 0))),
        'left_upper_arm': rot((0.0, (55, 0, 12)), (0.5, (58, 0, 12)), (1.0, (55, 0, 12))),
        'left_forearm': rot((0.0, (60, 0, 0)), (0.5, (62, 0, 0)), (1.0, (60, 0, 0))),
        'torso': pos((0.0, (0, 0, 0)), (0.5, (0, 0.4, 0)), (1.0, (0, 0, 0))),
    })
    slam_launch = clip(0.3, 'once', {
        'right_leg': rot((0.0, (0, 0, 0)), (0.3, (35, 0, 0))),
        'left_leg': rot((0.0, (0, 0, 0)), (0.3, (35, 0, 0))),
        'right_upper_arm': rot((0.0, (0, 0, 0)), (0.3, (-55, 0, 0))),
        'left_upper_arm': rot((0.0, (0, 0, 0)), (0.3, (-55, 0, 0))),
        'torso': both(rot((0.0, (0, 0, 0)), (0.3, (12, 0, 0))), pos((0.0, (0, 0, 0)), (0.3, (0, -1.5, 0)))),
    })
    slam_air = clip(0.4, 'loop', {
        'right_upper_arm': rot((0.0, (165, 0, -10)), (0.2, (170, 0, -10)), (0.4, (165, 0, -10))),
        'left_upper_arm': rot((0.0, (165, 0, 10)), (0.2, (170, 0, 10)), (0.4, (165, 0, 10))),
        'right_forearm': rot((0.0, (10, 0, 0)), (0.4, (10, 0, 0))),
        'left_forearm': rot((0.0, (10, 0, 0)), (0.4, (10, 0, 0))),
        'right_leg': rot((0.0, (50, 0, 0)), (0.4, (50, 0, 0))),
        'left_leg': rot((0.0, (50, 0, 0)), (0.4, (50, 0, 0))),
    })
    slam_smash = clip(0.5, 'once', {
        'right_upper_arm': rot((0.0, (165, 0, -10)), (0.2, (85, 0, -10)), (0.28, (95, 0, -10)), (0.5, (95, 0, -10))),
        'left_upper_arm': rot((0.0, (165, 0, 10)), (0.2, (85, 0, 10)), (0.28, (95, 0, 10)), (0.5, (95, 0, 10))),
        'right_leg': rot((0.0, (50, 0, 0)), (0.28, (0, 0, 0)), (0.5, (0, 0, 0))),
        'left_leg': rot((0.0, (50, 0, 0)), (0.28, (0, 0, 0)), (0.5, (0, 0, 0))),
        'torso': both(rot((0.0, (0, 0, 0)), (0.25, (28, 0, 0)), (0.5, (28, 0, 0))),
                      pos((0.0, (0, 0, 0)), (0.28, (0, -2, 0)), (0.5, (0, -2, 0)))),
    })
    hop = clip(0.5, 'once', {
        'right_leg': rot((0.0, (0, 0, 0)), (0.1, (30, 0, 0)), (0.25, (-10, 0, 0)), (0.5, (0, 0, 0))),
        'left_leg': rot((0.0, (0, 0, 0)), (0.1, (30, 0, 0)), (0.25, (-10, 0, 0)), (0.5, (0, 0, 0))),
        'right_upper_arm': rot((0.0, (0, 0, 0)), (0.1, (-20, 0, 0)), (0.3, (40, 0, 0)), (0.5, (0, 0, 0))),
        'left_upper_arm': rot((0.0, (0, 0, 0)), (0.1, (-20, 0, 0)), (0.3, (40, 0, 0)), (0.5, (0, 0, 0))),
        'torso': pos((0.0, (0, 0, 0)), (0.1, (0, -1.5, 0)), (0.25, (0, 1, 0)), (0.5, (0, 0, 0))),
    })
    exit_clip = clip(1.5, 'once', {
        'back_plate_right': rot((0.0, (0, 0, 0)), (0.6, (0, 70, 0)), (1.5, (0, 70, 0))),
        'back_plate_left': rot((0.0, (0, 0, 0)), (0.6, (0, -70, 0)), (1.5, (0, -70, 0))),
    })
    return {
        'idle': idle, 'walk': walk, 'punch_right': punch_right, 'punch_left': punch_left,
        'jackhammer': jackhammer, 'charge': charge, 'grab_hold': grab_hold,
        'slam_launch': slam_launch, 'slam_air': slam_air, 'slam_smash': slam_smash,
        'hop': hop, 'exit': exit_clip,
    }


def key(t):
    return f'{t:.2f}'.rstrip('0').rstrip('.') if t != int(t) else f'{t:.1f}'


def anim_json(all_clips):
    animations = {}
    for name, c in all_clips.items():
        bones = {}
        for bone_name, channels in c['bones'].items():
            entry = {}
            if channels.get('rot'):
                entry['rotation'] = {key(t): [-v[0], -v[1], v[2]] for t, v in sorted(channels['rot'].items())}
            if channels.get('pos'):
                entry['position'] = {key(t): [-v[0], v[1], v[2]] for t, v in sorted(channels['pos'].items())}
            if entry:
                bones[bone_name] = entry
        animations[name] = {
            'loop': True if c['kind'] == 'loop' else 'hold_on_last_frame',
            'animation_length': c['length'],
            'bones': bones,
        }
    return {'format_version': '1.8.0', 'animations': animations}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    atlas = Atlas(ATLAS)
    placed = {}
    body = assign_uv(body_bones(), atlas, placed)
    fp = assign_uv(fp_bones(), atlas, placed)
    parts = assign_uv(parts_bones(), atlas, placed)

    geo_dir = os.path.join(RES, 'geo', 'ironman', 'hulkbuster')
    write_json(os.path.join(geo_dir, 'mark48.geo.json'), to_geo(body, 'geometry.viltrumitecore.hulkbuster_mark48'))
    write_json(os.path.join(geo_dir, 'fp_arm.geo.json'), to_geo(fp, 'geometry.viltrumitecore.hulkbuster_fp_arm'))
    write_json(os.path.join(geo_dir, 'parts.geo.json'), to_geo(parts, 'geometry.viltrumitecore.hulkbuster_parts'))
    write_json(os.path.join(RES, 'animations', 'ironman', 'hulkbuster', 'mark48.animation.json'), anim_json(clips()))

    albedo, glow = paint(list(placed.values()), ATLAS)
    tex_dir = os.path.join(BIN, 'textures', 'entity', 'ironman')
    os.makedirs(tex_dir, exist_ok=True)
    with open(os.path.join(tex_dir, 'hulkbuster.png.b64'), 'w', encoding='ascii') as f:
        f.write(png_b64(albedo))
    with open(os.path.join(tex_dir, 'hulkbuster_glow.png.b64'), 'w', encoding='ascii') as f:
        f.write(png_b64(glow))
    print(f'hulkbuster: {len(placed)} boxes, atlas {ATLAS}x{ATLAS}, {len(clips())} clips')


if __name__ == '__main__':
    main()
