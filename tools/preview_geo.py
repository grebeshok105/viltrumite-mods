#!/usr/bin/env python3
"""Orthographic preview of Bedrock geo models (Pillow only, no network libs).

Renders front, right side and back views of one or more geo.json files with
their textures, the way BakedGeoModel reads them (pivots, bone and cube
rotations, inflate, box and per-face UV, mirror). Used to review converted
models; previews are not committed.

Run: python3 tools/preview_geo.py OUT.png GEO[:TEXTURE[:GLOW]] [GEO[:TEXTURE[:GLOW]] ...]
     [--scale N] [--views front,side,back,top]
"""
import argparse
import json
import math
import sys

from PIL import Image


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


IDENTITY = translate(0, 0, 0)


def apply(m, p):
    x, y, z = p
    return (m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
            m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
            m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3])


def rotation(pivot, rot):
    """Bedrock pivot rotation, ZYX order like GeckoLib (rx, ry in Bedrock signs)."""
    if not rot or not any(rot):
        return IDENTITY
    px, py, pz = pivot
    rx, ry, rz = (math.radians(v) for v in rot)
    # BakedGeoModel mirrors x and turns by (-rx, -ry, rz); expressed back in Bedrock space that is (-rx, ry, -rz).
    m = translate(px, py, pz)
    m = mat_mul(m, rot_z(-rz))
    m = mat_mul(m, rot_y(ry))
    m = mat_mul(m, rot_x(-rx))
    return mat_mul(m, translate(-px, -py, -pz))


def face_uvs(cube):
    size = cube.get("size", [0, 0, 0])
    uv = cube.get("uv")
    if isinstance(uv, dict):
        out = {}
        for face, f in uv.items():
            if isinstance(f, dict) and "uv" in f:
                u, v = f["uv"]
                su, sv = f.get("uv_size", [0, 0])
                out[face] = (u, v, u + su, v + sv)
        return out
    u, v = uv if isinstance(uv, list) else (0, 0)
    sx, sy, sz = size
    out = {
        "north": (u + sz, v + sz, u + sz + sx, v + sz + sy),
        "east": (u, v + sz, u + sz, v + sz + sy),
        "south": (u + sz + sx + sz, v + sz, u + sz + sx + sz + sx, v + sz + sy),
        "west": (u + sz + sx, v + sz, u + sz + sx + sz, v + sz + sy),
        "up": (u + sz + sx, v + sz, u + sz, v),
        "down": (u + sz + sx + sx, v, u + sz + sx, v + sz),
    }
    if cube.get("mirror"):
        out = {k: (r[2], r[1], r[0], r[3]) for k, r in out.items()}
        out["east"], out["west"] = out["west"], out["east"]
    return out


def cube_quads(cube):
    """Six quads in Bedrock space: (corners, uv rect). Corners go TL, TR, BR, BL as seen from outside."""
    ox, oy, oz = cube["origin"]
    sx, sy, sz = cube["size"]
    i = cube.get("inflate", 0)
    x0, y0, z0 = ox - i, oy - i, oz - i
    x1, y1, z1 = ox + sx + i, oy + sy + i, oz + sz + i
    uvs = face_uvs(cube)
    # Bedrock x grows to the model's left: seen from the front (north, -z) +x is on the right of the image.
    faces = {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "west": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "up": [(x1, y1, z1), (x0, y1, z1), (x0, y1, z0), (x1, y1, z0)],
        "down": [(x1, y0, z0), (x0, y0, z0), (x0, y0, z1), (x1, y0, z1)],
    }
    return [(faces[k], uvs[k]) for k in faces if k in uvs]


def collect(geo):
    g = geo["minecraft:geometry"][0]
    desc = g.get("description", {})
    tw, th = desc.get("texture_width", 16), desc.get("texture_height", 16)
    bones = {b["name"]: b for b in g.get("bones", [])}
    cache = {}

    def bone_matrix(name):
        if name in cache:
            return cache[name]
        b = bones[name]
        parent = b.get("parent")
        m = bone_matrix(parent) if parent in bones else IDENTITY
        m = mat_mul(m, rotation(b.get("pivot", [0, 0, 0]), b.get("rotation")))
        cache[name] = m
        return m

    quads = []
    for name, b in bones.items():
        m = bone_matrix(name)
        for cube in b.get("cubes", []):
            cm = mat_mul(m, rotation(cube.get("pivot", [0, 0, 0]), cube.get("rotation")))
            for corners, uv in cube_quads(cube):
                quads.append(([apply(cm, c) for c in corners], uv))
    return quads, tw, th


VIEWS = {
    # (screen x, screen y up, depth toward the viewer) from a Bedrock point.
    "front": lambda p: (p[0], p[1], -p[2]),
    "back": lambda p: (-p[0], p[1], p[2]),
    "side": lambda p: (p[2], p[1], p[0]),
    "top": lambda p: (p[0], -p[2], p[1]),
}


def raster(img, zbuf, quads, tex, tw, th, view, scale, ox, oy, additive=False):
    w, h = img.size
    px = img.load()
    tpx = tex.load()
    tex_w, tex_h = tex.size
    su, sv = tex_w / tw, tex_h / th
    for corners, (u0, v0, u1, v1) in quads:
        pts = [view(c) for c in corners]
        uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = (pts[k] for k in tri)
            ta, tb, tc = (uvs[k] for k in tri)
            ax, ay = ox + a[0] * scale, oy - a[1] * scale
            bx, by = ox + b[0] * scale, oy - b[1] * scale
            cx, cy = ox + c[0] * scale, oy - c[1] * scale
            den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
            if abs(den) < 1e-9:
                continue
            minx, maxx = max(0, int(min(ax, bx, cx))), min(w - 1, int(max(ax, bx, cx)) + 1)
            miny, maxy = max(0, int(min(ay, by, cy))), min(h - 1, int(max(ay, by, cy)) + 1)
            for yy in range(miny, maxy + 1):
                fy = yy + 0.5
                for xx in range(minx, maxx + 1):
                    fx = xx + 0.5
                    l1 = ((by - cy) * (fx - cx) + (cx - bx) * (fy - cy)) / den
                    l2 = ((cy - ay) * (fx - cx) + (ax - cx) * (fy - cy)) / den
                    l3 = 1 - l1 - l2
                    if l1 < -1e-6 or l2 < -1e-6 or l3 < -1e-6:
                        continue
                    depth = l1 * a[2] + l2 * b[2] + l3 * c[2]
                    u = (l1 * ta[0] + l2 * tb[0] + l3 * tc[0]) * su
                    v = (l1 * ta[1] + l2 * tb[1] + l3 * tc[1]) * sv
                    iu = min(tex_w - 1, max(0, int(u)))
                    iv = min(tex_h - 1, max(0, int(v)))
                    r, g, bl, al = tpx[iu, iv]
                    if additive:
                        if depth + 0.05 >= zbuf[yy][xx] and (r or g or bl):
                            pr, pg, pb, _ = px[xx, yy]
                            k = al / 255
                            px[xx, yy] = (min(255, pr + int(r * k)), min(255, pg + int(g * k)), min(255, pb + int(bl * k)), 255)
                        continue
                    if al < 16 or depth <= zbuf[yy][xx]:
                        continue
                    zbuf[yy][xx] = depth
                    px[xx, yy] = (r, g, bl, 255)


def render(models, views, scale):
    loaded = []
    for spec in models:
        parts = spec.split(":")
        with open(parts[0], encoding="utf-8") as f:
            quads, tw, th = collect(json.load(f))
        tex = Image.open(parts[1]).convert("RGBA") if len(parts) > 1 and parts[1] else None
        glow = Image.open(parts[2]).convert("RGBA") if len(parts) > 2 and parts[2] else None
        if tex is None:
            tex = Image.new("RGBA", (int(tw), int(th)), (180, 180, 190, 255))
        loaded.append((quads, tw, th, tex, glow))

    panels = []
    for name in views:
        view = VIEWS[name]
        pts = [view(c) for quads, *_ in loaded for corners, _ in quads for c in corners] or [(0, 0, 0)]
        minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
        miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
        w = int((maxx - minx) * scale) + 2 * scale
        h = int((maxy - miny) * scale) + 2 * scale
        img = Image.new("RGBA", (max(w, 8), max(h, 8)), (40, 44, 52, 255))
        zbuf = [[-1e9] * img.size[0] for _ in range(img.size[1])]
        ox, oy = scale - minx * scale, scale + maxy * scale
        for quads, tw, th, tex, glow in loaded:
            raster(img, zbuf, quads, tex, tw, th, view, scale, ox, oy)
        for quads, tw, th, tex, glow in loaded:
            if glow is not None:
                raster(img, zbuf, quads, glow, tw, th, view, scale, ox, oy, additive=True)
        panels.append(img)

    out = Image.new("RGBA", (sum(p.size[0] for p in panels) + 4 * (len(panels) - 1), max(p.size[1] for p in panels)), (20, 20, 24, 255))
    x = 0
    for p in panels:
        out.paste(p, (x, 0))
        x += p.size[0] + 4
    return out


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("models", nargs="+")
    ap.add_argument("--scale", type=int, default=8)
    ap.add_argument("--views", default="front,side,back")
    args = ap.parse_args(argv)
    render(args.models, args.views.split(","), args.scale).save(args.out)


if __name__ == "__main__":
    main(sys.argv[1:])
