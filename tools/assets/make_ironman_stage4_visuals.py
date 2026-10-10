#!/usr/bin/env python3
"""Iron Man Stage 4 client art (own work, generated, committed):

- geo/ironman/marks/empty_suit_interior.geo.json: the inside of the empty suit (the
  shell is Satsu full_body, tools/assets/convert_ironman_stage4_marks.py);
- geo/ironman/veronica/veronica_pod.geo.json + animations/ironman/*.animation.json;
- textures/entity/ironman/veronica_pod.png (binassets, base64).

Geometry convention (client/anim/geo/BakedGeoModel): JSON x = vanilla player model x,
JSON y = 24 - vanilla model y (feet at 0), pixels. Box UV = vanilla texOffs layout.

Run: python3 tools/assets/make_ironman_stage4_visuals.py
"""
import base64
import io
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
MAIN = os.path.join(HERE, '..', '..', 'viltrumitecore', 'src', 'main')
RES = os.path.join(MAIN, 'resources', 'assets', 'viltrumitecore')
BIN = os.path.join(MAIN, 'binassets', 'assets', 'viltrumitecore')

# Vanilla wide-arm player model, absolute model pixels. key -> (min, size, texOffs, armor bone, pivot).
BOXES = {
    'head': ((-4, -8, -4), (8, 8, 8), (0, 0), 'armorhead', (0, 24, 0)),
    'body': ((-4, 0, -2), (8, 12, 4), (16, 16), 'armorbody', (0, 24, 0)),
    'right_arm': ((-8, 0, -2), (4, 12, 4), (40, 16), 'armorrightarm', (-5, 22, 0)),
    'left_arm': ((4, 0, -2), (4, 12, 4), (32, 48), 'armorleftarm', (5, 22, 0)),
    'right_leg': ((-3.9, 12, -2), (4, 12, 4), (0, 16), 'armorrightleg', (-1.9, 12, 0)),
    'left_leg': ((-0.1, 12, -2), (4, 12, 4), (16, 48), 'armorleftleg', (1.9, 12, 0)),
}
INTERIOR_INFLATE = -0.3
SCALE_PX = 16


def cube(mn, size, uv, inflate):
    x0, y0, z0 = mn
    sx, sy, sz = size
    return {'origin': [x0, 24 - (y0 + sy), z0], 'size': [sx, sy, sz], 'uv': [uv[0], uv[1]], 'inflate': inflate}


def centroid(mn, size):
    return [mn[0] + size[0] / 2, 24 - (mn[1] + size[1] / 2), mn[2] + size[2] / 2]


def geometry(identifier, bones, tex=64):
    return {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {'identifier': identifier, 'texture_width': tex, 'texture_height': tex},
            'bones': bones,
        }],
    }


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def empty_suit_files():
    inner = []
    for key in ['body', 'head', 'right_arm', 'left_arm', 'right_leg', 'left_leg']:
        mn, size, uv, bone_name, pivot = BOXES[key]
        centre = centroid(mn, size)
        joint = list(pivot) if key != 'body' else [0, centre[1], 0]
        inner.append({'name': bone_name, 'pivot': joint, 'cubes': [cube(mn, size, uv, INTERIOR_INFLATE)]})
    write_json(os.path.join(RES, 'geo', 'ironman', 'marks', 'empty_suit_interior.geo.json'),
               geometry('geometry.viltrumitecore.empty_suit_interior', inner))


def pack(boxes, width=128):
    """Shelf-pack box-UV rectangles (2w+2d by d+h) into a width x width texture."""
    placed = {}
    x = y = shelf = 0
    for name, (w, h, d) in sorted(boxes.items(), key=lambda kv: -(kv[1][1] + kv[1][2])):
        rw, rh = 2 * d + 2 * w, d + h
        if x + rw > width:
            x, y, shelf = 0, y + shelf, 0
        placed[name] = (x, y)
        x += rw
        shelf = max(shelf, rh)
    return placed


POD_HULL = [
    ('base', (-14, 0, -14), (28, 4, 28), (46, 48, 52)),
    ('lower', (-15, 4, -15), (30, 8, 30), (196, 202, 210)),
    ('mid', (-16, 12, -16), (32, 18, 32), (236, 240, 246)),
    ('upper', (-15, 30, -15), (30, 14, 30), (222, 228, 236)),
    ('nose', (-11, 44, -11), (22, 6, 22), (170, 180, 192)),
    ('tip', (-6, 50, -6), (12, 4, 12), (80, 216, 255)),
    ('antenna', (-1, 54, -1), (2, 8, 2), (90, 100, 112)),
    ('nozzle', (-6, -4, -6), (12, 4, 12), (34, 40, 48)),
]
POD_DOORS = {'door_left': ((-8, 14, 16), (8, 28, 1), (120, 210, 240)), 'door_right': ((0, 14, 16), (8, 28, 1), (120, 210, 240))}


def pod_geo():
    boxes = {}
    for name, _, size, _ in POD_HULL:
        boxes[name] = (size[0], size[1], size[2])
    for name, (_, size, _) in POD_DOORS.items():
        boxes[name] = (size[0], size[1], size[2])
    uv = pack(boxes, 256)
    hull_bone = {'name': 'pod', 'pivot': [0, 0, 0], 'cubes': []}
    for name, mn, size, _ in POD_HULL:
        hull_bone['cubes'].append({'origin': [mn[0], mn[1], mn[2]], 'size': list(size), 'uv': list(uv[name])})
    bones = [hull_bone]
    for name, (mn, size, _) in POD_DOORS.items():
        bones.append({'name': name, 'parent': 'pod', 'pivot': [0, 28, 16],
                      'cubes': [{'origin': list(mn), 'size': list(size), 'uv': list(uv[name])}]})
    doc = geometry('geometry.viltrumitecore.veronica_pod', bones, tex=256)
    write_json(os.path.join(RES, 'geo', 'ironman', 'veronica', 'veronica_pod.geo.json'), doc)


def pod_animation():
    def door(sign):
        return {'rotation': {'0.0': [0, 0, 0], '0.6': [0, 75 * sign, 0]}}
    close = {'rotation': {'0.0': [0, 75, 0], '0.6': [0, 0, 0]}}
    doc = {
        'format_version': '1.8.0',
        'animations': {
            'open': {'loop': 'hold_on_last_frame', 'animation_length': 0.6,
                     'bones': {'door_left': door(1), 'door_right': door(-1)}},
            'close': {'loop': 'hold_on_last_frame', 'animation_length': 0.6,
                      'bones': {'door_left': close, 'door_right': {'rotation': {'0.0': [0, -75, 0], '0.6': [0, 0, 0]}}}},
        },
    }
    write_json(os.path.join(RES, 'animations', 'ironman', 'veronica_pod.animation.json'), doc)


def empty_suit_animation():
    def keys(values):
        return {'0.0': [0, 0, 0], '0.5': values, '1.0': values, '1.5': [0, 0, 0]}
    doc = {
        'format_version': '1.8.0',
        'animations': {
            'open': {'loop': 'hold_on_last_frame', 'animation_length': 1.5, 'bones': {
                'armorrightarm': {'rotation': keys([0, 0, 40])},
                'armorleftarm': {'rotation': keys([0, 0, -40])},
                'armorhead': {'rotation': keys([-35, 0, 0])},
                'armorbody': {'scale': {'0.0': [1, 1, 1], '0.5': [1.12, 1, 1.12], '1.0': [1.12, 1, 1.12], '1.5': [1, 1, 1]}},
            }},
        },
    }
    write_json(os.path.join(RES, 'animations', 'ironman', 'empty_suit.animation.json'), doc)


def paint_hull(img, uv, size, colour):
    w, h, d = size
    u, v = uv
    faces = [
        (u + d, v, w, d), (u + d + w, v, w, d),
        (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h),
    ]
    r, g, b = colour
    for i, (x, y, fw, fh) in enumerate(faces):
        shade = 1.0 if i == 3 else 0.82
        base = (int(r * shade), int(g * shade), int(b * shade), 255)
        for py in range(y, y + fh):
            for px in range(x, x + fw):
                edge = px in (x, x + fw - 1) or py in (y, y + fh - 1)
                c = tuple(int(ch * 0.7) for ch in base[:3]) + (255,) if edge else base
                img.putpixel((px, py), c)
        if i == 3 and fh > 6:
            mid = y + fh // 2
            for px in range(x + 1, x + fw - 1):
                img.putpixel((px, mid), (80, 216, 255, 255))


def pod_texture():
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    boxes = {name: size for name, _, size, _ in POD_HULL}
    boxes.update({name: size for name, (_, size, _) in POD_DOORS.items()})
    uv = pack({k: (v[0], v[1], v[2]) for k, v in boxes.items()}, 256)
    for name, _, size, colour in POD_HULL:
        paint_hull(img, uv[name], size, colour)
    for name, (_, size, colour) in POD_DOORS.items():
        paint_hull(img, uv[name], size, colour)
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    os.makedirs(os.path.join(BIN, 'textures', 'entity', 'ironman'), exist_ok=True)
    with open(os.path.join(BIN, 'textures', 'entity', 'ironman', 'veronica_pod.png.b64'), 'w', encoding='ascii') as f:
        f.write(base64.b64encode(buf.getvalue()).decode('ascii'))


def main():
    empty_suit_files()
    pod_geo()
    pod_animation()
    empty_suit_animation()
    pod_texture()


if __name__ == '__main__':
    main()
