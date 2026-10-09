#!/usr/bin/env python3
"""Generates the emissive arc-reactor mask for Tony's skin (64x64, player layout).

Bright core on the reactor pixels of ironman.png (body front 23..24 x 23..24),
dim halo around it. Everything else is transparent black (RenderType.eyes is
additive). Usage: python3 tools/assets/gen_reactor_glow.py OUT.png
"""
import sys
from PIL import Image

im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
for x in range(22, 26):
    for y in range(22, 26):
        core = 23 <= x <= 24 and 23 <= y <= 24
        im.putpixel((x, y), (200, 250, 255, 255) if core else (40, 110, 140, 255))
im.save(sys.argv[1])
