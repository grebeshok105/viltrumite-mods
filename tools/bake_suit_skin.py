#!/usr/bin/env python3
"""Bakes the Satsu mark skins into the 64x64 player skin layout (Iron Man stage 4).

Satsu draws a mark with two geo models: ``full_body`` (vanilla skin UVs, no head
cubes) with the suit texture, and ``all_helmet`` (three 8x8x8 head boxes, box UV
at (0,0)/(32,0), (0,19)/(32,19) and (0,43)/(32,43)) with the mask texture. The
helmet boxes have the vanilla head shape, so the baker stacks the three mask
layers into the head (0,0) and hat (32,0) rows of the suit texture. The result is
an ordinary player skin; the glow map is baked the same way from the light
textures (premultiplied: black where nothing glows).

Mark 50 (nano) is converted by tools/assets/convert_ironman_stage1b.py.

Run: python3 tools/bake_suit_skin.py SATSU_ASSETS_DIR
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "assets"))
from convert_ironman_stage1b import premultiplied, write_b64  # noqa: E402

T = "textures/models/"
# mark key -> (suit, suit light, mask, mask light); pairs from the addon's palladium render layers.
MARKS = {
    "mark_7": ("iron_man/no_light/mark_7_0", "iron_man/light/mark_7_0", "iron_man/mask/no_light/mark_7_0", "iron_man/mask/light/mark_light_7_0"),
    "mark_15": ("iron_man/no_light/mark_15_0", "iron_man/light/mark_15_0", "iron_man/mask/no_light/mark_15_0", "iron_man/mask/light/mark_light_15_0"),
    "mark_17": ("iron_man/no_light/mark_17_0", "iron_man/light/mark_17_0", "iron_man/mask/no_light/mark_17_0", "iron_man/mask/light/mark_light_17_0"),
    "mark_39": ("iron_man/no_light/mark_39_0", "iron_man/light/mark_39_0", "iron_man/mask/no_light/mark_39_0", "iron_man/mask/light/mark_light_39_0"),
    "war_machine_mk2": ("war_machine/no_light/war_machine_mark_2", "war_machine/light/war_machine_mark_2",
                        "war_machine/mask/no_light/war_machine_mask_mark_2", "war_machine/mask/light/war_machine_mask_mark_2"),
    "iron_heart_mk3": ("iron_heart/no_light/mark_3", "iron_heart/light/mark_3", "iron_heart/no_light/mask/mark_3", "iron_heart/light/mask/mark_3"),
}
# all_helmet layers (red shell, face plate, back), each a head box plus an outer box.
HELMET_ROWS = (0, 19, 43)


def load(src, rel):
    return Image.open(os.path.join(src, T + rel + ".png")).convert("RGBA")


def bake(suit, mask):
    """Suit texture with the helmet stacked into the head (0..32, 0..16) and hat (32..64, 0..16) rows."""
    out = suit.copy()
    for x0 in (0, 32):
        head = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
        for row in HELMET_ROWS:
            head.alpha_composite(mask.crop((x0, row, x0 + 32, row + 16)))
        out.paste(head, (x0, 0))
    return out


def main(src):
    for key, (suit, suit_light, mask, mask_light) in MARKS.items():
        write_b64("textures/entity/hero/ironman_" + key + ".png", bake(load(src, suit), load(src, mask)))
        write_b64("textures/entity/hero/ironman_" + key + "_glow.png", premultiplied(bake(load(src, suit_light), load(src, mask_light))))


if __name__ == "__main__":
    main(sys.argv[1])
