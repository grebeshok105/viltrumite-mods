#!/usr/bin/env python3
"""FiskHeroes .fsk animation scripts to our AnimationParser JSON (Iron Man stage 5).

An .fsk script sets model part channels ({bone_rotX}, {bone_posY}, ...) from
inputs ({data}, {data_N}, {limbSwing}, ...) every frame. This tool evaluates the
script for a series of input values and bakes the channel offsets from the rest
pose into keyframes (no Molang).

Supported subset (what the Sind Hulkbuster scripts use): ``;`` comments,
``{var} = | += | -= | *= | /= expr``, ``{var} @ weight -> target`` (blend the
channel toward the target by the weight), the ``'`` degree suffix, implicit
multiplication (``40'{x}``, ``-1{x}``), + - * / ^ ( ), comparisons, the prefix
``curve``, and the functions animate2, animate, min, max, clamp, sin, cos, abs,
sqrt, signum, atan2, log1p, if. Fisk semantics assumed (not documented in the
archive): ``animate2(x, duration, start, fadeIn, fadeOut)`` is 0 outside
[start, start + duration], ramps up over fadeIn and down over fadeOut;
``curve x`` eases with (1 - cos(pi x)) / 2.

Units: Fisk rotations are radians in the Java model space and positions are
Java pixels (y down). Our JSON keeps the Java turn in degrees (AnimationParser
negates X and Y like BakedGeoModel does for the geo) and flips the y position.

Run as a module: fsk2anim.bake(script_text, rest, drive, length, fps) -> bones dict.
"""
import math
import re

TOKEN = re.compile(r"\s*(?:(\d+\.\d*|\.\d+|\d+)(')?|(\{[^}]*\})|([A-Za-z_][A-Za-z0-9_]*)|(\+=|-=|\*=|/=|->|<=|>=|==|[-+*/%(),=<>@^']))")
CHANNELS = ("rotX", "rotY", "rotZ", "posX", "posY", "posZ")


def tokenize(text):
    out, i = [], 0
    while i < len(text):
        m = TOKEN.match(text, i)
        if not m or m.end() == i:
            if text[i:].strip() == "":
                break
            raise ValueError("bad token at %r" % text[i:])
        num, deg, var, name, op = m.groups()
        if num is not None:
            out.append(("num", float(num) * (math.pi / 180.0 if deg else 1.0)))
        elif var is not None:
            out.append(("var", var[1:-1]))
        elif name is not None:
            out.append(("name", name))
        else:
            out.append(("op", op))
        i = m.end()
    return out


def animate2(x, duration, start, fade_in, fade_out):
    if x < start or x > start + duration:
        return 0.0
    if fade_in > 0 and x < start + fade_in:
        return (x - start) / fade_in
    end = start + duration
    if fade_out > 0 and x > end - fade_out:
        return (end - x) / fade_out
    return 1.0


FUNCS = {
    "animate2": animate2,
    "animate": lambda x, duration, start: max(0.0, min(1.0, (x - start) / duration)) if duration else 0.0,
    "min": min, "max": max, "clamp": lambda x, a, b: max(a, min(b, x)),
    "sin": math.sin, "cos": math.cos, "abs": abs, "sqrt": lambda x: math.sqrt(max(0.0, x)),
    "signum": lambda x: (x > 0) - (x < 0), "atan2": math.atan2, "log1p": math.log1p,
    "if": lambda c, a, b: a if c else b, "curve": lambda x: (1.0 - math.cos(math.pi * max(0.0, min(1.0, x)))) / 2.0,
}
CONSTS = {"pi": math.pi, "PI": math.pi, "true": 1.0, "false": 0.0}


class Parser:
    def __init__(self, tokens, env):
        self.t, self.i, self.env = tokens, 0, env

    def peek(self):
        return self.t[self.i] if self.i < len(self.t) else (None, None)

    def take(self):
        tok = self.peek()
        self.i += 1
        return tok

    def expr(self):
        v = self.sum()
        kind, val = self.peek()
        if kind == "op" and val in ("<", ">", "<=", ">=", "=="):
            self.take()
            r = self.sum()
            return float({"<": v < r, ">": v > r, "<=": v <= r, ">=": v >= r, "==": v == r}[val])
        return v

    def sum(self):
        v = self.term()
        while self.peek() in (("op", "+"), ("op", "-")):
            op = self.take()[1]
            r = self.term()
            v = v + r if op == "+" else v - r
        return v

    def term(self):
        v = self.unary()
        while True:
            kind, val = self.peek()
            if (kind, val) in (("op", "*"), ("op", "/"), ("op", "%")):
                self.take()
                r = self.unary()
                v = v * r if val == "*" else (v % r if val == "%" else v / r) if r else 0.0
            elif kind in ("num", "var") or (kind == "name" and val != "curve") or (kind, val) == ("op", "("):
                v = v * self.unary()
            else:
                return v

    def unary(self):
        kind, val = self.peek()
        if (kind, val) == ("op", "-"):
            self.take()
            return -self.unary()
        if (kind, val) == ("op", "+"):
            self.take()
            return self.unary()
        if (kind, val) == ("name", "curve"):
            self.take()
            return FUNCS["curve"](self.unary())
        v = self.primary()
        while True:
            if self.peek() == ("op", "'"):
                self.take()
                v *= math.pi / 180.0
            elif self.peek() == ("op", "^"):
                self.take()
                v = v ** self.primary()
            else:
                return v

    def primary(self):
        kind, val = self.take()
        if kind == "num":
            return val
        if kind == "var":
            return float(self.env.get(val, 0.0))
        if kind == "op" and val == "(":
            v = self.expr()
            self.take()
            return v
        if kind == "name":
            if self.peek() == ("op", "("):
                self.take()
                args = []
                if self.peek() != ("op", ")"):
                    args.append(self.expr())
                    while self.peek() == ("op", ","):
                        self.take()
                        args.append(self.expr())
                self.take()
                return float(FUNCS[val](*args))
            return CONSTS.get(val, 0.0)
        raise ValueError("unexpected %r" % ((kind, val),))


SKIPPED = []


def statements(script):
    """Parsed statements; lines outside the subset (Fisk model calls, flight inputs) are skipped and listed in SKIPPED."""
    out = []
    for raw in script.splitlines():
        line = raw.split(";", 1)[0].strip()
        if not line:
            continue
        try:
            tokens = tokenize(line)
        except ValueError:
            SKIPPED.append(line)
            continue
        if ("op", "@") in tokens and ("op", "->") not in tokens:
            SKIPPED.append(line)
            continue
        if len(tokens) < 3 or tokens[0][0] != "var" or tokens[1][0] != "op":
            continue
        if tokens[1][1] == "@":
            split = tokens.index(("op", "->"))
            out.append((tokens[0][1], "@", (tokens[2:split], tokens[split + 1:])))
            continue
        out.append((tokens[0][1], tokens[1][1], tokens[2:]))
    return out


def evaluate(stmts, rest, inputs):
    """Channel values after one run; rest = {'bone_rotX': value, ...}."""
    env = dict(rest)
    env.update(inputs)
    for name, op, expr in stmts:
        cur = float(env.get(name, 0.0))
        if op == "@":
            weight = Parser(expr[0], env).expr()
            env[name] = cur + (Parser(expr[1], env).expr() - cur) * weight
            continue
        v = Parser(expr, env).expr()
        env[name] = {"=": v, "+=": cur + v, "-=": cur - v, "*=": cur * v, "/=": cur / v if v else cur}[op]
    return env


def bake(script, rest, drive, length, fps=20, bones=None):
    """Keyframes {bone: {'rotation': {t: [x,y,z]}, 'position': {...}}} of the channel offsets from rest.

    drive(t) returns the input dict at clip time t (seconds). ``bones`` limits the output.
    """
    stmts = statements(script)
    steps = max(1, int(round(length * fps)))
    frames = []
    for k in range(steps + 1):
        t = length * k / steps
        frames.append((round(t, 4), evaluate(stmts, rest, drive(t))))
    names = sorted({key.rsplit("_", 1)[0] for _, env in frames for key in env if key.rsplit("_", 1)[-1] in CHANNELS})
    out = {}
    for bone in names:
        if bones is not None and bone not in bones:
            continue
        rot, pos = {}, {}
        for t, env in frames:
            r = [math.degrees(env.get(bone + "_rot" + a, rest.get(bone + "_rot" + a, 0.0)) - rest.get(bone + "_rot" + a, 0.0)) for a in "XYZ"]
            p = [env.get(bone + "_pos" + a, rest.get(bone + "_pos" + a, 0.0)) - rest.get(bone + "_pos" + a, 0.0) for a in "XYZ"]
            rot["%g" % t] = [round(v, 3) for v in r]
            pos["%g" % t] = [round(p[0], 3), round(-p[1], 3), round(p[2], 3)]
        entry = {}
        if any(any(abs(v) > 1e-3 for v in r) for r in rot.values()):
            entry["rotation"] = simplify(rot)
        if any(any(abs(v) > 1e-3 for v in p) for p in pos.values()):
            entry["position"] = simplify(pos)
        if entry:
            out[bone] = entry
    return out


def simplify(keys):
    """Drops keyframes on a straight line between their neighbours."""
    times = list(keys)
    if len(times) <= 2:
        return keys
    kept = [times[0]]
    for i in range(1, len(times) - 1):
        a, b, c = keys[kept[-1]], keys[times[i]], keys[times[i + 1]]
        ta, tb, tc = float(kept[-1]), float(times[i]), float(times[i + 1])
        f = (tb - ta) / (tc - ta) if tc != ta else 0.0
        if any(abs(a[j] + (c[j] - a[j]) * f - b[j]) > 0.05 for j in range(3)):
            kept.append(times[i])
    kept.append(times[-1])
    return {t: keys[t] for t in kept}


def rest_of(bones_with_rotation):
    """Rest channel values of Tabula parts: {name: (rx, ry, rz) degrees} -> {'name_rotX': radians}."""
    rest = {}
    for name, rot in bones_with_rotation.items():
        for a, v in zip("XYZ", rot):
            rest[name + "_rot" + a] = math.radians(v)
    return rest
