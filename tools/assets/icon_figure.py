#!/usr/bin/env python3
"""Tiny software renderer of a posed Minecraft player model from a 64x64 skin
(numpy + Pillow). Used by make_ironman_icons_v3.py for character ability art in
the Viltrumite / Homelander panel style. Not used at runtime.

Model space: pixels, y up, x = player's left, camera looks at the face from -z.
"""
import math

import numpy as np
from PIL import Image

# name: (box uv, size w h d, origin of the box min corner, pivot, overlay uv, inflate)
PARTS = {
    "head": ((0, 0), (8, 8, 8), (-4, 24, -4), (0, 24, 0), (32, 0), 0.5),
    "body": ((16, 16), (8, 12, 4), (-4, 12, -2), (0, 24, 0), (16, 32), 0.25),
    "right_arm": ((40, 16), (4, 12, 4), (-8, 12, -2), (-5, 22, 0), (40, 32), 0.25),
    "left_arm": ((32, 48), (4, 12, 4), (4, 12, -2), (5, 22, 0), (48, 48), 0.25),
    "right_leg": ((0, 16), (4, 12, 4), (-4, 0, -2), (-2, 12, 0), (0, 32), 0.25),
    "left_leg": ((16, 48), (4, 12, 4), (0, 0, -2), (2, 12, 0), (0, 48), 0.25),
}


def rot(rx, ry, rz):
    """Rotation matrix, degrees, applied Z then Y then X (like ModelPart)."""
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    mx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    my = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    mz = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return mx @ my @ mz


def box_faces(uv, size, lo, inflate):
    """Faces of a skin box: (origin, edge_u, edge_v, (u0, v0, du, dv), normal). edge_v points down the texture."""
    u, v = uv
    w, h, d = size
    x0, y0, z0 = lo[0] - inflate, lo[1] - inflate, lo[2] - inflate
    x1, y1, z1 = lo[0] + w + inflate, lo[1] + h + inflate, lo[2] + d + inflate
    P = np.array
    return [
        # front (-z): texture left = player's right (-x)
        (P([x0, y1, z0]), P([x1 - x0, 0, 0]), P([0, y0 - y1, 0]), (u + d, v + d, w, h), P([0, 0, -1])),
        # back (+z)
        (P([x1, y1, z1]), P([x0 - x1, 0, 0]), P([0, y0 - y1, 0]), (u + 2 * d + w, v + d, w, h), P([0, 0, 1])),
        # player's right side (-x): u from back to front
        (P([x0, y1, z1]), P([0, 0, z0 - z1]), P([0, y0 - y1, 0]), (u, v + d, d, h), P([-1, 0, 0])),
        # player's left side (+x): u from front to back
        (P([x1, y1, z0]), P([0, 0, z1 - z0]), P([0, y0 - y1, 0]), (u + d + w, v + d, d, h), P([1, 0, 0])),
        # top: v from back to front
        (P([x0, y1, z1]), P([x1 - x0, 0, 0]), P([0, 0, z0 - z1]), (u + d, v, w, d), P([0, 1, 0])),
        # bottom
        (P([x0, y0, z0]), P([x1 - x0, 0, 0]), P([0, 0, z1 - z0]), (u + d + w, v, w, d), P([0, -1, 0])),
    ]


class Figure:
    def __init__(self, skin, glow=None, bulk=1.0):
        self.skin = np.asarray(skin.convert("RGBA"), dtype=np.float32)
        self.glow = None if glow is None else np.asarray(glow.convert("RGBA"), dtype=np.float32)
        self.pose = {name: (0.0, 0.0, 0.0) for name in PARTS}
        self.bulk = bulk
        self.offset = {name: (0.0, 0.0, 0.0) for name in PARTS}
        self.hidden = set()
        self.root = (0.0, 0.0, 0.0)

    def set(self, name, rx=0.0, ry=0.0, rz=0.0):
        self.pose[name] = (rx, ry, rz)
        return self

    def faces(self):
        out = []
        rr = rot(*self.root)
        rc = np.array([0.0, 16.0, 0.0])
        for name, (uv, size, lo, pivot, over, infl) in PARTS.items():
            if name in self.hidden:
                continue
            r = rot(*self.pose[name])
            piv = np.array(pivot, dtype=float)
            off = np.array(self.offset[name], dtype=float)
            s = self.bulk if name != "head" else 1.0
            for layer_uv, inflate, overlay in ((uv, 0.0, False), (over, infl, True)):
                for o, eu, ev, rect, n in box_faces(layer_uv, size, lo, inflate):
                    # bulk: scale the part around its pivot (thicker limbs / body)
                    o2 = piv + (o - piv) * s
                    po = r @ (o2 - piv) + piv + off
                    out.append((rr @ (po - rc) + rc, rr @ (r @ (eu * s)), rr @ (r @ (ev * s)), rect, rr @ (r @ n), overlay))
        return out


def render(figure, size=64, yaw=25.0, pitch=8.0, scale=2.0, center=(0.0, 20.0), shift=(32.0, 32.0), light=(-0.5, 0.8, -0.6), supersample=1):
    """Orthographic render. Returns RGBA image (size x size) and a depth-tested coverage mask."""
    ss = supersample
    W = size * ss
    cam = rot(pitch, yaw, 0.0)
    L = np.array(light, dtype=float)
    L = L / np.linalg.norm(L)
    color = np.zeros((W, W, 4), dtype=np.float32)
    zbuf = np.full((W, W), np.inf, dtype=np.float32)
    glow = np.zeros((W, W, 4), dtype=np.float32)
    ys, xs = np.mgrid[0:W, 0:W].astype(np.float32)
    px = (xs + 0.5) / ss
    py = (ys + 0.5) / ss
    for o, eu, ev, (u0, v0, du, dv), n, overlay in figure.faces():
        o, eu, ev, n = cam @ o, cam @ eu, cam @ ev, cam @ n
        sx0 = shift[0] + (o[0] - center[0]) * scale
        sy0 = shift[1] - (o[1] - center[1]) * scale
        ax, ay = eu[0] * scale, -eu[1] * scale
        bx, by = ev[0] * scale, -ev[1] * scale
        det = ax * by - ay * bx
        if abs(det) < 1e-6:
            continue
        corners_x = [sx0, sx0 + ax, sx0 + bx, sx0 + ax + bx]
        corners_y = [sy0, sy0 + ay, sy0 + by, sy0 + ay + by]
        x_lo = max(0, int(math.floor(min(corners_x) * ss)))
        x_hi = min(W, int(math.ceil(max(corners_x) * ss)) + 1)
        y_lo = max(0, int(math.floor(min(corners_y) * ss)))
        y_hi = min(W, int(math.ceil(max(corners_y) * ss)) + 1)
        if x_lo >= x_hi or y_lo >= y_hi:
            continue
        qx = px[y_lo:y_hi, x_lo:x_hi] - sx0
        qy = py[y_lo:y_hi, x_lo:x_hi] - sy0
        a = (qx * by - qy * bx) / det
        b = (ax * qy - ay * qx) / det
        inside = (a >= 0) & (a < 1) & (b >= 0) & (b < 1)
        if not inside.any():
            continue
        tu = np.clip((u0 + a * du).astype(int), 0, 63)
        tv = np.clip((v0 + b * dv).astype(int), 0, 63)
        tex = figure.skin[tv, tu]
        depth = o[2] + a * eu[2] + b * ev[2] - (0.01 if overlay else 0.0)
        ok = inside & (tex[..., 3] > 127) & (depth < zbuf[y_lo:y_hi, x_lo:x_hi])
        if not ok.any():
            continue
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(n, L)))
        if n[1] > 0.5:
            shade = max(shade, 0.95)
        rgb = tex[..., :3] * shade
        reg = color[y_lo:y_hi, x_lo:x_hi]
        reg[ok, :3] = rgb[ok]
        reg[ok, 3] = 255
        zbuf[y_lo:y_hi, x_lo:x_hi][ok] = depth[ok]
        g = glow[y_lo:y_hi, x_lo:x_hi]
        if figure.glow is not None:
            gt = figure.glow[tv, tu]
            g[ok] = gt[ok]
        else:
            g[ok] = 0
    # glow on top (emissive)
    if figure.glow is not None:
        ga = glow[..., 3:4] / 255.0
        color[..., :3] = color[..., :3] * (1 - ga) + np.maximum(color[..., :3], glow[..., :3]) * ga
    img = Image.fromarray(color.clip(0, 255).astype(np.uint8), "RGBA")
    if ss > 1:
        img = img.resize((size, size), Image.NEAREST)
    return img


def project(figure, part, point, yaw=25.0, pitch=8.0, scale=2.0, center=(0.0, 20.0), shift=(32.0, 32.0)):
    """Screen position (x, y) of a model-space point attached to a posed part (pose about its pivot)."""
    pivot = np.array(PARTS[part][3], dtype=float)
    r = rot(*figure.pose[part])
    p = r @ (np.array(point, dtype=float) - pivot) + pivot + np.array(figure.offset[part], dtype=float)
    rc = np.array([0.0, 16.0, 0.0])
    p = rot(*figure.root) @ (p - rc) + rc
    p = rot(pitch, yaw, 0.0) @ p
    return (shift[0] + (p[0] - center[0]) * scale, shift[1] - (p[1] - center[1]) * scale)
