#!/usr/bin/env python3
"""Iron Man Stage 4 client art (own work, generated, committed):

- geo/ironman/veronica/veronica_pod.geo.json + animations/ironman/veronica_pod.animation.json;
- textures/entity/ironman/veronica_pod.png and veronica_pod_glow.png (binassets, base64): Veronica after the
  Age of Ultron stills (twin red-orange hulls with pointed tops, silver grilles, dark spine, lit core, thrusters).

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


# Veronica (Avengers: Age of Ultron, reference stills): twin tall hull pods in red-orange armour with
# pointed tops, silver grilles, dark inner sides, a dark central spine with a lit core and thrusters.
# Pixel boxes (Bedrock, feet at 0); the hulls are the door bones (hinged at the back of the spine).
ORANGE = (204, 66, 30)
ORANGE_D = (138, 40, 22)
SILVER = (178, 184, 192)
GUNMETAL = (52, 55, 62)
GUNMETAL_D = (30, 32, 37)
POD_SPINE = [
    # name, origin, size, base colour, pattern
    ('spine', (-3, 4, -3), (6, 30, 6), GUNMETAL, 'ribs'),
    ('spine_cap', (-2, 34, -2), (4, 5, 4), GUNMETAL_D, 'plain'),
    ('core', (-1.5, 16, -3.4), (3, 8, 1), GUNMETAL_D, 'core'),
    ('nozzle', (-2.5, 0, -2.5), (5, 4, 5), GUNMETAL_D, 'nozzle'),
    ('strut_fl', (-11, 0, -7), (2, 4, 2), GUNMETAL, 'plain'),
    ('strut_fr', (9, 0, -7), (2, 4, 2), GUNMETAL, 'plain'),
    ('strut_bl', (-11, 0, 5), (2, 4, 2), GUNMETAL, 'plain'),
    ('strut_br', (9, 0, 5), (2, 4, 2), GUNMETAL, 'plain'),
]
# One hull (the +x one); the -x hull mirrors it.
POD_HULL = [
    ('hull', (3.5, 4, -7), (9, 25, 14), ORANGE, 'armour'),
    ('hull_upper', (4.5, 29, -6), (7, 6, 12), ORANGE, 'armour'),
    ('hull_spike', (6, 35, -4), (4, 6, 8), ORANGE_D, 'armour'),
    ('hull_tip', (7, 41, -2), (2, 4, 4), SILVER, 'plain'),
    ('grille', (12.5, 9, -5), (1.2, 15, 10), SILVER, 'grille'),
    ('front_plate', (4.5, 11, -8.2), (7, 13, 1.2), ORANGE_D, 'armour'),
    ('thruster', (5, 1, -5), (6, 3, 10), GUNMETAL_D, 'nozzle'),
]
POD_HINGE = 7.0
POD_OPEN_DEG = 55.0


def mirror_box(origin, size):
    return (-(origin[0] + size[0]), origin[1], origin[2])


def pod_boxes():
    """name -> (origin, size, colour, pattern, bone)."""
    boxes = {}
    for name, origin, size, colour, pattern in POD_SPINE:
        boxes[name] = (origin, size, colour, pattern, 'pod')
    for name, origin, size, colour, pattern in POD_HULL:
        boxes[name + '_l'] = (origin, size, colour, pattern, 'door_left')
        boxes[name + '_r'] = (mirror_box(origin, size), size, colour, pattern, 'door_right')
    return boxes


def pod_geo():
    boxes = pod_boxes()
    uv = pack({n: tuple(int(-(-v // 1)) for v in b[1]) for n, b in boxes.items()}, 128)
    bones = {'pod': {'name': 'pod', 'pivot': [0, 0, 0], 'cubes': []},
             'door_left': {'name': 'door_left', 'parent': 'pod', 'pivot': [3.5, 16, POD_HINGE], 'cubes': []},
             'door_right': {'name': 'door_right', 'parent': 'pod', 'pivot': [-3.5, 16, POD_HINGE], 'cubes': []}}
    for name, (origin, size, _, _, bone) in boxes.items():
        cube = {'origin': list(origin), 'size': list(size), 'uv': list(uv[name])}
        if name.endswith('_r'):
            cube['mirror'] = True
        bones[bone]['cubes'].append(cube)
    doc = geometry('geometry.viltrumitecore.veronica_pod', list(bones.values()), tex=128)
    write_json(os.path.join(RES, 'geo', 'ironman', 'veronica', 'veronica_pod.geo.json'), doc)
    return uv


def pod_animation():
    def swing(sign, frames):
        return {'rotation': {t: {'vector': [0, POD_OPEN_DEG * sign * k, 0], 'easing': 'easeInOutCubic'} for t, k in frames}}
    opening = [('0.0', 0.0), ('0.8', 1.0)]
    closing = [('0.0', 1.0), ('0.6', 0.0)]
    doc = {
        'format_version': '1.8.0',
        'animations': {
            'open': {'loop': 'hold_on_last_frame', 'animation_length': 0.8,
                     'bones': {'door_left': swing(-1, opening), 'door_right': swing(1, opening)}},
            'close': {'loop': 'hold_on_last_frame', 'animation_length': 0.6,
                      'bones': {'door_left': swing(-1, closing), 'door_right': swing(1, closing)}},
        },
    }
    write_json(os.path.join(RES, 'animations', 'ironman', 'veronica_pod.animation.json'), doc)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def paint_box(img, glow, uv, size, colour, pattern):
    """Box-UV faces: base colour, panel seams, pattern detail; lights go to the glow map."""
    w, h, d = (int(-(-v // 1)) for v in size)
    u, v = uv
    faces = [(u + d, v, w, d, 1.08), (u + d + w, v, w, d, 0.7), (u, v + d, d, h, 0.85), (u + d, v + d, w, h, 1.0),
             (u + d + w, v + d, d, h, 0.85), (u + 2 * d + w, v + d, w, h, 0.9)]
    px, gx = img.load(), glow.load()
    for i, (x0, y0, fw, fh, k) in enumerate(faces):
        for y in range(y0, y0 + fh):
            for x in range(x0, x0 + fw):
                if not (0 <= x < img.width and 0 <= y < img.height):
                    continue
                lx, ly = x - x0, y - y0
                col = shade(colour, k)
                edge = lx in (0, fw - 1) or ly in (0, fh - 1)
                if pattern == 'armour':
                    if edge or (ly % 6 == 5 and fh > 6):
                        col = shade(colour, k * 0.62)
                    elif lx == 1 or ly == 1:
                        col = shade(colour, k * 1.18)
                    if i == 3 and fh > 10 and lx == fw // 2 and 3 < ly < fh - 3:
                        col = shade(SILVER, 0.9)
                elif pattern == 'grille':
                    col = shade(SILVER, k * (0.55 if ly % 2 else 1.0))
                elif pattern == 'ribs':
                    col = shade(colour, k * (0.7 if ly % 4 == 0 else 1.0))
                    if i in (3, 5) and ly % 4 == 2 and 1 <= lx < fw - 1:
                        gx[x, y] = (255, 140, 60, 255)
                        col = (255, 170, 90, 255)
                elif pattern == 'core':
                    if i == 3 and not edge:
                        col = (190, 240, 255, 255)
                        gx[x, y] = (160, 230, 255, 255)
                elif pattern == 'nozzle':
                    if i == 1:
                        r = ((lx - fw / 2 + 0.5) ** 2 + (ly - fh / 2 + 0.5) ** 2) ** 0.5
                        if r < min(fw, fh) / 2 - 0.5:
                            col = (255, 200, 120, 255) if r < min(fw, fh) / 4 else (255, 120, 50, 255)
                            gx[x, y] = col
                    elif edge:
                        col = shade(colour, k * 0.6)
                else:
                    if edge:
                        col = shade(colour, k * 0.7)
                px[x, y] = col


def pod_texture(uv):
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    glow = Image.new('RGBA', (128, 128), (0, 0, 0, 255))
    for name, (_, size, colour, pattern, _) in pod_boxes().items():
        paint_box(img, glow, uv[name], size, colour, pattern)
    for target, image in (('veronica_pod.png.b64', img), ('veronica_pod_glow.png.b64', glow)):
        buf = io.BytesIO()
        image.save(buf, format='PNG')
        os.makedirs(os.path.join(BIN, 'textures', 'entity', 'ironman'), exist_ok=True)
        with open(os.path.join(BIN, 'textures', 'entity', 'ironman', target), 'w', encoding='ascii') as f:
            f.write(base64.b64encode(buf.getvalue()).decode('ascii'))


def main():
    uv = pod_geo()
    pod_animation()
    pod_texture(uv)


if __name__ == '__main__':
    main()
