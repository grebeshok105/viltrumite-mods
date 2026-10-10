#!/usr/bin/env python3
"""First-person arm preview without the game (Pillow + numpy).

Replays the vanilla 1.20.1 first-person arm chain (ItemInHandRenderer.renderPlayerArm,
PlayerRenderer.renderHand) after an extra hero transform applied right after pushPose,
the hook every FirstPerson*Mixin uses (PoseRig.firstPersonRaw: translate x*side, y, z,
then rotate pitch, yaw*side, roll*side, degrees). Draws both arms as seen from the camera
(FOV 70, 16:9) with the palm end in cyan and the arm axis extended in yellow, so a pose can
be checked against the crosshair.

Run: python3 tools/preview_fp_arm.py out.png "label:rp,ry,rr,rx,ry,rz|lp,ly,lr,lx,ly,lz" ...
     (left part optional; "-" hides that arm)
     python3 tools/preview_fp_arm.py --solve hx,hy,hz dx,dy,dz
     prints the right-arm key (pitch, yaw, roll, x, y, z) that puts the palm centre at view point H
     and turns the arm axis (shoulder to palm) along D with the smallest turn from the vanilla arm.
"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

W, H = 640, 360
FOV = 70.0


def rot(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    if axis == "x":
        m[1, 1], m[1, 2], m[2, 1], m[2, 2] = c, -s, s, c
    elif axis == "y":
        m[0, 0], m[0, 2], m[2, 0], m[2, 2] = c, s, -s, c
    else:
        m[0, 0], m[0, 1], m[1, 0], m[1, 1] = c, -s, s, c
    return m


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


# Vanilla right shoulder in view space (palm-side face centre at the arm pivot, swing 0, equip 0):
# hero keys rotate about it, so a blended key moves the arm as a rigid limb (IronManFirstPerson).
SHOULDER = (0.4787, -0.8435, -0.5281)


def arm_matrix(side, pose):
    pitch, yaw, roll, x, y, z = pose
    px, py, pz = SHOULDER
    m = tr((px + x) * side, py + y, pz + z) @ rot("x", pitch) @ rot("y", yaw * side) @ rot("z", roll * side) @ tr(-px * side, -py, -pz)
    # renderPlayerArm with swing 0, equip 0
    m = m @ tr(side * 0.64000005, -0.6, -0.71999997) @ rot("y", side * 45.0)
    m = m @ tr(side * -1.0, 3.6, 3.5) @ rot("z", side * 120.0) @ rot("x", 200.0) @ rot("y", side * -135.0) @ tr(side * 5.6, 0.0, 0.0)
    # renderHand: arm pivot (+-5, 2, 0) px, zRot = 0.1 * side (bobModelPart at age 0), xRot reset to 0
    m = m @ tr(-5.0 * side / 16.0, 2.0 / 16.0, 0.0) @ rot("z", math.degrees(0.1 * side))
    return m


def cube_faces(side):
    x0 = -3.0 if side > 0 else -1.0
    lo = np.array([x0, -2.0, -2.0]) / 16.0
    hi = lo + np.array([4.0, 12.0, 4.0]) / 16.0
    c = lambda i, j, k: np.array([(lo, hi)[i][0], (lo, hi)[j][1], (lo, hi)[k][2], 1.0])
    return {
        "down": [c(0, 1, 0), c(1, 1, 0), c(1, 1, 1), c(0, 1, 1)],  # max y = the hand end
        "up": [c(0, 0, 0), c(1, 0, 0), c(1, 0, 1), c(0, 0, 1)],
        "north": [c(0, 0, 0), c(1, 0, 0), c(1, 1, 0), c(0, 1, 0)],
        "south": [c(0, 0, 1), c(1, 0, 1), c(1, 1, 1), c(0, 1, 1)],
        "west": [c(0, 0, 0), c(0, 0, 1), c(0, 1, 1), c(0, 1, 0)],
        "east": [c(1, 0, 0), c(1, 0, 1), c(1, 1, 1), c(1, 1, 0)],
    }


def project(p):
    f = 1.0 / math.tan(math.radians(FOV) / 2.0)
    aspect = W / H
    if p[2] > -0.01:
        return None
    x = (p[0] / -p[2]) * f / aspect
    y = (p[1] / -p[2]) * f
    return ((x + 1.0) * 0.5 * W, (1.0 - y) * 0.5 * H)


def render(path, poses):
    img = Image.new("RGB", (W, H * len(poses)), (40, 44, 52))
    for row, (label, right, left) in enumerate(poses):
        tile = Image.new("RGB", (W, H), (40, 44, 52))
        draw = ImageDraw.Draw(tile)
        oy = 0
        quads = []
        axes = []
        for side, pose in ((1.0, right), (-1.0, left)):
            if pose is None:
                continue
            m = arm_matrix(side, pose)
            for name, corners in cube_faces(side).items():
                pts = [m @ c for c in corners]
                depth = sum(p[2] for p in pts) / 4.0
                quads.append((depth, name, pts))
            x0 = -3.0 if side > 0 else -1.0
            cx = (x0 + 2.0) / 16.0
            a = m @ np.array([cx, -2.0 / 16.0, 0.0, 1.0])
            b = m @ np.array([cx, 30.0 / 16.0, 0.0, 1.0])
            axes.append((a, b))
        quads.sort(key=lambda q: q[0])
        shade = {"down": (90, 230, 255), "up": (150, 60, 50), "north": (190, 60, 50), "south": (120, 40, 35), "west": (165, 50, 45), "east": (140, 45, 40)}
        for _, name, pts in quads:
            proj = [project(p) for p in pts]
            if any(p is None for p in proj):
                continue
            draw.polygon([(x, y + oy) for x, y in proj], fill=shade[name], outline=(20, 20, 20))
        for a, b in axes:
            pa, pb = project(a), project(b)
            if pa and pb:
                draw.line([(pa[0], pa[1] + oy), (pb[0], pb[1] + oy)], fill=(255, 220, 60), width=1)
        draw.line([(W / 2 - 8, oy + H / 2), (W / 2 + 8, oy + H / 2)], fill=(255, 255, 255))
        draw.line([(W / 2, oy + H / 2 - 8), (W / 2, oy + H / 2 + 8)], fill=(255, 255, 255))
        draw.rectangle([0, oy, W - 1, oy + H - 1], outline=(90, 90, 90))
        draw.text((6, oy + 4), label, fill=(255, 255, 255))
        img.paste(tile, (0, row * H))
    img.save(path)


def euler_xyz(r):
    """Angles (deg) with r = Rx(a) Ry(b) Rz(c), the order of PoseRig.firstPersonRaw."""
    b = math.asin(max(-1.0, min(1.0, r[0, 2])))
    a = math.atan2(-r[1, 2], r[2, 2])
    c = math.atan2(-r[0, 1], r[0, 0])
    return math.degrees(a), math.degrees(b), math.degrees(c)


def rotation_to(u, v):
    u = u / np.linalg.norm(u)
    v = v / np.linalg.norm(v)
    axis = np.cross(u, v)
    s = np.linalg.norm(axis)
    c = float(np.dot(u, v))
    if s < 1e-9:
        return np.eye(3)
    k = axis / s
    kx = np.array([[0, -k[2], k[1]], [k[2], 0, -k[0]], [-k[1], k[0], 0]])
    ang = math.atan2(s, c)
    return np.eye(3) + math.sin(ang) * kx + (1 - math.cos(ang)) * (kx @ kx)


def vanilla_points():
    m = arm_matrix(1.0, [0.0] * 6)
    palm = (m @ np.array([-1.0 / 16.0, 10.0 / 16.0, 0.0, 1.0]))[:3]
    shoulder = (m @ np.array([-1.0 / 16.0, -2.0 / 16.0, 0.0, 1.0]))[:3]
    return palm, shoulder


def solve(hand, direction):
    palm, shoulder = vanilla_points()
    r = rotation_to(palm - shoulder, np.array(direction, dtype=float))
    p = np.array(SHOULDER)
    # translate(p + d) R translate(-p) maps the vanilla palm to the wanted point
    d = np.array(hand, dtype=float) - p - r @ (palm - p)
    pitch, yaw, roll = euler_xyz(r)
    return [round(pitch, 2), round(yaw, 2), round(roll, 2), round(d[0], 3), round(d[1], 3), round(d[2], 3)]


def parse(arg):
    label, _, rest = arg.partition(":")
    parts = rest.split("|")

    def one(s):
        s = s.strip()
        if s == "-":
            return None
        return [float(v) for v in s.split(",")] if s else [0.0] * 6

    right = one(parts[0]) if parts else [0.0] * 6
    left = one(parts[1]) if len(parts) > 1 else None
    return label, right, left


if __name__ == "__main__":
    if sys.argv[1] == "--solve":
        print(solve([float(v) for v in sys.argv[2].split(",")], [float(v) for v in sys.argv[3].split(",")]))
    else:
        render(sys.argv[1], [parse(a) for a in sys.argv[2:]])
