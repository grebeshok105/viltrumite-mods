#!/usr/bin/env python3
"""Opening empty-suit shell (Iron Man exit, spec §12.5): the Satsu full_body + helmet boxes cut into a
back half (stays) and front plates on hinges (open), plus an interior lining drawn inside every half.

Used by tools/assets/convert_ironman_stage4_marks.py. Face UV directions follow BakedGeoModel
(checked with tools/preview_geo.py): north u along -x, south u along +x, JSON east (max-x face) u along -z,
JSON west (min-x face) u along +z, up u along -x / v along -z, down u along -x / v along +z; v of the side
faces runs down y.
"""
import copy

# face -> (u axis, u sign, v axis, v sign); sign +1 = the uv start sits at the low end of the axis
FACE_AXES = {
    "north": (0, -1, 1, -1), "south": (0, 1, 1, -1), "east": (2, -1, 1, -1), "west": (2, 1, 1, -1),
    "up": (0, -1, 2, -1), "down": (0, -1, 2, 1),
}
# the face lying on the low / high side of each axis
LOW_FACE = {0: "west", 1: "down", 2: "north"}
HIGH_FACE = {0: "east", 1: "up", 2: "south"}


def box_uv(cube):
    """Per-face UV of a box-UV cube (same layout BakedGeoModel builds)."""
    if isinstance(cube.get("uv"), dict):
        return copy.deepcopy(cube["uv"])
    u, v = cube["uv"]
    w, h, d = (round(x) for x in cube["size"])
    return {
        "north": {"uv": [u + d, v + d], "uv_size": [w, h]}, "south": {"uv": [u + 2 * d + w, v + d], "uv_size": [w, h]},
        "west": {"uv": [u, v + d], "uv_size": [d, h]}, "east": {"uv": [u + d + w, v + d], "uv_size": [d, h]},
        "up": {"uv": [u + d + w, v + d], "uv_size": [w, -d]}, "down": {"uv": [u + d, v], "uv_size": [w, d]},
    }


def cut(cube, axis, a0, a1, drop_low=False, drop_high=False):
    """The part of a cube between a0 and a1 on one axis (model px, inflate kept); face UVs cut to match.

    drop_low / drop_high remove the new faces on the cut planes (left open)."""
    lo, size = cube["origin"][axis], cube["size"][axis]
    hi = lo + size
    a0, a1 = max(lo, a0), min(hi, a1)
    if a1 <= a0:
        return None
    out = copy.deepcopy(cube)
    out["origin"][axis] = a0
    out["size"][axis] = a1 - a0
    uv = box_uv(cube)
    for face, rect in list(uv.items()):
        if face not in FACE_AXES:
            continue
        ua, us, va, vs = FACE_AXES[face]
        for (ax, sign, idx) in ((ua, us, 0), (va, vs, 1)):
            if ax != axis or size == 0:
                continue
            start, length = rect["uv"][idx], rect["uv_size"][idx]
            f0 = (a0 - lo) / size if sign > 0 else (hi - a1) / size
            f1 = (a1 - lo) / size if sign > 0 else (hi - a0) / size
            rect["uv"][idx] = round(start + length * f0, 4)
            rect["uv_size"][idx] = round(length * (f1 - f0), 4)
    if drop_low and a0 > lo:
        uv.pop(LOW_FACE[axis], None)
    if drop_high and a1 < hi:
        uv.pop(HIGH_FACE[axis], None)
    out["uv"] = uv
    return out


def lining(cube, open_faces, regions, inset=0.12):
    """Interior box just inside a half: every face except the open ones, UVs from the interior atlas."""
    inner = {k: v for k, v in cube.items() if k in ("origin", "size", "pivot", "rotation")}
    inner = copy.deepcopy(inner)
    infl = cube.get("inflate", 0.0) - inset
    inner["inflate"] = round(infl, 4)
    faces = {}
    for face in FACE_AXES:
        if face in open_faces:
            continue
        u, v, w, h = regions[face]
        faces[face] = {"uv": [u, v], "uv_size": [w, h]}
    inner["uv"] = faces
    return inner
