#!/usr/bin/env python3
"""Iron Man Stage 4 client art (own work, generated, committed):

- geo/ironman/veronica/veronica_pod.geo.json + animations/ironman/veronica_pod.animation.json;
- textures/entity/ironman/veronica_pod.png and veronica_pod_glow.png (binassets, base64): Veronica after the
  Age of Ultron still (one joined module: glossy bright-red hulls with pointed tops, grey armour panels and
  silver grilles, dark central body with yokes, two lit thrusters), 256 px.

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


# Veronica (Avengers: Age of Ultron, reference still from the user, PR #19 iteration 3): one joined
# module — two tall glossy bright-red hulls with pointed tops, grey armour panels and silver grilles
# on the outer sides, joined to a dark central body by yokes; two lit thrusters low on the body.
# Pixel boxes (Bedrock, feet at 0); the hulls are the door bones (hinged at the back of the body).
RED = (214, 28, 22)
RED_D = (150, 16, 14)
SILVER = (196, 200, 208)
GREY = (112, 116, 124)
GUNMETAL = (58, 60, 67)
GUNMETAL_D = (32, 34, 39)
TEX = 256
POD_SPINE = [
    # name, origin, size, base colour, pattern
    ('body', (-3, 4, -4), (6, 32, 8), GUNMETAL, 'ribs'),
    ('body_front', (-2.5, 14, -4.6), (5, 18, 0.6), GUNMETAL_D, 'panel'),
    ('body_cap', (-2, 36, -2.5), (4, 4, 5), GUNMETAL_D, 'plain'),
    ('light_l', (-2.6, 8, -4.8), (2, 3, 0.8), GUNMETAL_D, 'light'),
    ('light_r', (0.6, 8, -4.8), (2, 3, 0.8), GUNMETAL_D, 'light'),
    ('nozzle', (-2.5, 0, -2.5), (5, 4, 5), GUNMETAL_D, 'nozzle'),
    ('yoke_low', (-4, 11, -2.5), (8, 3, 5), GREY, 'panel'),
    ('yoke_high', (-4, 27, -2.5), (8, 3, 5), GREY, 'panel'),
    ('yoke_top', (-4, 33, -1.5), (8, 2, 3), GREY, 'panel'),
]
# One hull (the +x one); the -x hull mirrors it. Stacked boxes give the tapering, pointed silhouette.
POD_HULL = [
    ('hull_base', (4, 2, -5), (8, 3, 10), RED_D, 'gloss'),
    ('hull_low', (3.5, 5, -6.5), (9, 4, 13), RED, 'gloss'),
    ('hull', (3, 9, -7), (10, 19, 14), RED, 'gloss'),
    ('hull_shoulder', (3.5, 28, -6), (8.5, 5, 12), RED, 'gloss'),
    ('hull_crest', (4.5, 33, -4.5), (6.5, 5, 9), RED, 'gloss'),
    ('hull_spike', (6, 38, -3), (4.5, 4, 6), RED, 'gloss'),
    ('hull_tip', (7.5, 42, -1.5), (2.5, 3, 3), RED_D, 'gloss'),
    ('front_plate', (4, 11, -7.8), (8, 16, 0.8), RED, 'plate'),
    ('back_plate', (4, 11, 7), (8, 16, 0.8), RED_D, 'plate'),
    ('armour_outer', (13, 10, -6), (1, 20, 12), GREY, 'panel'),
    ('grille', (14, 14, -4), (0.7, 12, 8), SILVER, 'grille'),
    ('armour_inner', (2.4, 15, -5), (0.6, 10, 10), GREY, 'panel'),
    ('fin', (12, 3, -3), (2, 6, 6), GREY, 'panel'),
    ('thruster', (5, 0, -4), (6, 2, 8), GUNMETAL_D, 'nozzle'),
    ('trim_top', (3.6, 32.4, -6.2), (8.2, 0.8, 12.4), SILVER, 'plain'),
]
POD_HINGE = 7.0
POD_OPEN_DEG = 40.0


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
    uv = pack({n: tuple(int(-(-v // 1)) for v in b[1]) for n, b in boxes.items()}, TEX)
    bones = {'pod': {'name': 'pod', 'pivot': [0, 0, 0], 'cubes': []},
             'door_left': {'name': 'door_left', 'parent': 'pod', 'pivot': [3.0, 16, POD_HINGE], 'cubes': []},
             'door_right': {'name': 'door_right', 'parent': 'pod', 'pivot': [-3.0, 16, POD_HINGE], 'cubes': []}}
    for name, (origin, size, _, _, bone) in boxes.items():
        cube = {'origin': list(origin), 'size': list(size), 'uv': list(uv[name])}
        if name.endswith('_r'):
            cube['mirror'] = True
        bones[bone]['cubes'].append(cube)
    doc = geometry('geometry.viltrumitecore.veronica_pod', list(bones.values()), tex=TEX)
    write_json(os.path.join(RES, 'geo', 'ironman', 'veronica', 'veronica_pod.geo.json'), doc)
    return uv


def pod_animation():
    def swing(sign, frames):
        return {'rotation': {t: {'vector': [0, POD_OPEN_DEG * sign * k, 0], 'easing': 'easeInOutCubic'} for t, k in frames}}
    opening = [('0.0', 0.0), ('0.9', 1.0)]
    closing = [('0.0', 1.0), ('0.6', 0.0)]
    doc = {
        'format_version': '1.8.0',
        'animations': {
            'open': {'loop': 'hold_on_last_frame', 'animation_length': 0.9,
                     'bones': {'door_left': swing(-1, opening), 'door_right': swing(1, opening)}},
            'close': {'loop': 'hold_on_last_frame', 'animation_length': 0.6,
                      'bones': {'door_left': swing(-1, closing), 'door_right': swing(1, closing)}},
        },
    }
    write_json(os.path.join(RES, 'animations', 'ironman', 'veronica_pod.animation.json'), doc)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def paint_box(img, glow, uv, size, colour, pattern):
    """Box-UV faces: base colour with a top-lit gradient, seams, gloss streaks; lights go to the glow map."""
    w, h, d = (int(-(-v // 1)) for v in size)
    u, v = uv
    # (x, y, w, h, light): up, down, east(-x side in box UV), north (front), west, south (back)
    faces = [(u + d, v, w, d, 1.12), (u + d + w, v, w, d, 0.62), (u, v + d, d, h, 0.86), (u + d, v + d, w, h, 1.0),
             (u + d + w, v + d, d, h, 0.86), (u + 2 * d + w, v + d, w, h, 0.8)]
    px, gx = img.load(), glow.load()
    for i, (x0, y0, fw, fh, k) in enumerate(faces):
        for y in range(y0, y0 + fh):
            for x in range(x0, x0 + fw):
                if not (0 <= x < img.width and 0 <= y < img.height):
                    continue
                lx, ly = x - x0, y - y0
                edge = lx in (0, fw - 1) or ly in (0, fh - 1)
                # Top-lit: brighter at the top of side faces.
                grad = 1.0 if i in (0, 1) or fh <= 1 else 1.12 - 0.24 * ly / (fh - 1)
                col = shade(colour, k * grad)
                if pattern in ('gloss', 'plate'):
                    if edge:
                        col = shade(colour, k * 0.66)
                    elif pattern == 'gloss' and fh > 8 and ly % 7 == 6:
                        col = shade(colour, k * 0.74)
                    # Gloss: a bright vertical streak a quarter in, a soft halo next to it, white glints.
                    if i in (2, 3, 4, 5) and fw >= 3 and not edge:
                        streak = int(fw * 0.28)
                        if lx == streak:
                            col = mix(col, (255, 196, 182), 0.62)
                        elif lx in (streak - 1, streak + 1):
                            col = mix(col, (255, 150, 130), 0.3)
                        if lx == streak and ly in (2, 3):
                            col = (255, 240, 235, 255)
                    if i == 0 and not edge and (lx + ly) % 5 == 0:
                        col = mix(col, (255, 190, 170), 0.35)
                    if pattern == 'plate' and i == 3 and lx == fw // 2 and 2 < ly < fh - 2:
                        col = shade(SILVER, 0.92)
                elif pattern == 'grille':
                    col = shade(SILVER, k * (0.5 if ly % 2 else 1.05))
                elif pattern == 'panel':
                    if edge:
                        col = shade(colour, k * 0.6)
                    elif (ly % 5 == 2 and fw > 3) or (lx % 6 == 3 and fh > 6):
                        col = shade(colour, k * 0.78)
                    elif lx == 1 and ly == 1:
                        col = shade(SILVER, 1.0)
                elif pattern == 'ribs':
                    col = shade(colour, k * (0.72 if ly % 4 == 0 else 1.0))
                    if i in (3, 5) and ly % 8 == 4 and 1 <= lx < fw - 1:
                        gx[x, y] = (255, 120, 50, 255)
                        col = (255, 150, 80, 255)
                elif pattern == 'light':
                    if i == 3:
                        r = ((lx - fw / 2 + 0.5) ** 2 + (ly - fh / 2 + 0.5) ** 2) ** 0.5
                        col = (255, 246, 220, 255) if r < 1.0 else (255, 196, 120, 255)
                        gx[x, y] = col
                elif pattern == 'nozzle':
                    if i == 1:
                        r = ((lx - fw / 2 + 0.5) ** 2 + (ly - fh / 2 + 0.5) ** 2) ** 0.5
                        if r < min(fw, fh) / 2 - 0.5:
                            col = (255, 214, 140, 255) if r < min(fw, fh) / 4 else (255, 120, 50, 255)
                            gx[x, y] = col
                    elif edge:
                        col = shade(colour, k * 0.6)
                else:
                    if edge:
                        col = shade(colour, k * 0.7)
                px[x, y] = col


def pod_texture(uv):
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 255))
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
