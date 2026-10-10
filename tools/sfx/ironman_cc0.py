#!/usr/bin/env python3
"""Iron Man sounds from CC0 recordings (Kenney "Sci-Fi Sounds" 1.0 and "Impact Sounds" 1.0, kenney.nl, CC0 1.0).

The synthesized stage sounds are gone (user decision 2026-10-10): only the two flight loops from
tools/sfx/ironman_stage1.sh stay. Every event below is built from the Kenney recordings with ffmpeg
(mix, pitch, fade, loudness), mono 44.1 kHz Vorbis. Events in MUTED have no sound (no beeps, no
synthetic chimes); sounds.json lists them with an empty sound list.

Run: python3 tools/sfx/ironman_cc0.py KENNEY_SCIFI_DIR KENNEY_IMPACT_DIR
"""
import base64
import json
import os
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "viltrumitecore", "src", "main", "binassets", "assets", "viltrumitecore", "sounds", "ironman")
SOUNDS_JSON = os.path.join(ROOT, "viltrumitecore", "src", "main", "resources", "assets", "viltrumitecore", "sounds.json")
FLIGHT = {"thruster_loop", "thruster_sonic"}
MUTED = {
    "repulsor_fizzle", "jarvis_warning", "scan_loop", "scan_complete", "jarvis_detect", "jarvis_detect_excited",
    "jarvis_diagnostic", "jarvis_suit_preset", "jarvis_mark85_preset", "overdraft_sputter",
}
# event -> list of layers: (source file stem, pitch factor, volume dB, delay s); optional trim (s) and fades.
RECIPES = {
    "nano_deploy": ([("forceField_001", 0.8, 0, 0), ("impactMetal_light_002", 1.2, -8, 0.55)], 1.1),
    "nano_retract": ([("forceField_002", 0.7, 0, 0)], 0.9),
    "landing_soft": ([("impactSoft_heavy_001", 0.9, 0, 0), ("thrusterFire_001", 1.0, -14, 0)], 0.6),
    "landing_heavy": ([("impactPlate_heavy_002", 0.6, 0, 0), ("lowFrequency_explosion_000", 1.2, -4, 0)], 1.3),
    "air_strike": ([("impactPunch_heavy_003", 0.8, 0, 0), ("explosionCrunch_002", 1.0, -3, 0.02)], 1.0),
    "flyby_hit": ([("impactPunch_heavy_001", 1.0, 0, 0)], 0.4),
    "repulsor_shot": ([("laserLarge_001", 0.75, 0, 0), ("thrusterFire_002", 1.6, -12, 0)], 0.6),
    "repulsor_charge": ([("forceField_004", 0.65, -2, 0)], 1.0),
    "repulsor_volley": ([("laserLarge_003", 0.6, 0, 0), ("explosionCrunch_001", 1.1, -6, 0.05)], 0.9),
    "unibeam_charge": ([("spaceEngineLarge_001", 1.4, -2, 0), ("forceField_000", 0.5, -6, 0)], 1.0),
    "unibeam_loop": ([("spaceEngineLarge_002", 1.6, 0, 0), ("forceField_003", 0.45, -8, 0)], 2.0),
    "unibeam_overheat": ([("thrusterFire_003", 0.6, -2, 0)], 1.5),
    "core_explosion": ([("lowFrequency_explosion_001", 0.9, 0, 0), ("explosionCrunch_004", 0.7, -2, 0.05)], 2.0),
    "nanite_form": ([("forceField_001", 1.3, -2, 0)], 0.5),
    "nanite_dissolve": ([("forceField_002", 1.1, -3, 0)], 0.5),
    "nanite_repair": ([("forceField_000", 1.6, -8, 0)], 0.6),
    "blade_slash": ([("thrusterFire_004", 2.4, -4, 0), ("impactMetal_light_004", 1.3, -6, 0.08)], 0.35),
    "hammer_hit": ([("impactMetal_heavy_003", 0.8, 0, 0), ("impactPunch_heavy_002", 0.9, -3, 0)], 0.5),
    "hammer_slam": ([("impactPlate_heavy_004", 0.6, 0, 0), ("lowFrequency_explosion_001", 1.3, -4, 0)], 1.2),
    "shield_open": ([("doorOpen_001", 1.3, -2, 0), ("forceField_003", 1.2, -6, 0.05)], 0.6),
    "shield_hit": ([("impactMetal_medium_002", 0.9, 0, 0)], 0.4),
    "shield_perfect": ([("impactMetal_heavy_001", 1.1, 0, 0), ("forceField_004", 1.5, -6, 0)], 0.6),
    "missile_flaps": ([("doorOpen_002", 1.4, -1, 0), ("impactMetal_light_000", 1.4, -8, 0.25)], 0.5),
    "missile_launch": ([("thrusterFire_000", 1.5, 0, 0), ("spaceEngineSmall_001", 1.2, -6, 0)], 0.9),
    "missile_explode": ([("explosionCrunch_003", 0.9, 0, 0), ("lowFrequency_explosion_000", 1.5, -6, 0)], 1.2),
    "helmet_close": ([("doorClose_002", 1.15, 0, 0), ("impactMetal_light_001", 1.2, -6, 0.28)], 0.6),
    "helmet_open": ([("doorOpen_000", 1.15, 0, 0)], 0.55),
    "flare_launch": ([("thrusterFire_001", 1.8, -2, 0)], 0.5),
    "flare_burn": ([("thrusterFire_002", 0.8, -6, 0)], 1.5),
    "veronica_fall": ([("thrusterFire_003", 0.55, 0, 0), ("spaceEngineLarge_003", 0.7, -4, 0)], 3.0),
    "veronica_impact": ([("lowFrequency_explosion_001", 0.8, 0, 0), ("explosionCrunch_000", 0.6, -2, 0.03)], 2.0),
    "veronica_open": ([("doorOpen_002", 0.6, 0, 0), ("doorOpen_000", 0.8, -4, 0.3)], 1.2),
    "veronica_leave": ([("spaceEngineLarge_004", 0.8, 0, 0)], 2.5),
    # Parts flying in and locking on the body (spec §12.4): thrust whoosh, then servo + heavy clamp.
    "part_fly": ([("thrusterFire_004", 1.4, -2, 0)], 0.7),
    "part_clamp": ([("doorClose_000", 1.0, 0, 0), ("impactPlate_medium_001", 0.8, -1, 0.1), ("impactMetal_medium_003", 1.2, -6, 0.12)], 0.6),
    "helmet_lock": ([("doorClose_001", 1.25, 0, 0), ("impactPlate_light_002", 1.0, -2, 0.12)], 0.5),
    "mark_enter": ([("doorClose_002", 0.85, 0, 0), ("impactPlate_heavy_001", 0.9, -2, 0.18), ("impactMetal_medium_000", 1.1, -6, 0.22)], 0.9),
    "mark_exit": ([("doorOpen_001", 0.8, 0, 0), ("doorOpen_002", 0.95, -3, 0.12)], 1.0),
    "mark_break": ([("impactMetal_heavy_004", 0.7, 0, 0), ("explosionCrunch_002", 0.8, -2, 0.05), ("impactPlate_heavy_003", 0.8, -4, 0.2)], 1.2),
    "micro_laser": ([("spaceEngineSmall_002", 1.8, -2, 0), ("forceField_000", 1.4, -8, 0)], 1.0),
    "rocket_fist_launch": ([("thrusterFire_000", 1.2, 0, 0), ("doorOpen_001", 1.6, -6, 0)], 0.8),
    "rocket_fist_hit": ([("impactPunch_heavy_004", 0.8, 0, 0), ("impactMetal_heavy_002", 0.9, -3, 0)], 0.5),
    "camo_on": ([("forceField_003", 0.8, -2, 0)], 0.8),
    "camo_off": ([("forceField_004", 0.9, -2, 0)], 0.7),
    "starboost": ([("thrusterFire_000", 0.8, 0, 0), ("lowFrequency_explosion_000", 1.6, -6, 0)], 1.2),
    "pulse_unibeam": ([("laserLarge_004", 0.5, 0, 0), ("lowFrequency_explosion_001", 1.8, -8, 0)], 0.7),
    "shoulder_gun": ([("impactPunch_medium_000", 1.8, -2, 0), ("impactMetal_light_003", 2.0, -6, 0)], 0.15),
    "slam_impact": ([("lowFrequency_explosion_000", 0.9, 0, 0), ("impactPlate_heavy_000", 0.6, -2, 0)], 1.6),
    "hulkbuster_drop": ([("lowFrequency_explosion_001", 0.7, 0, 0), ("impactPlate_heavy_004", 0.5, -2, 0.05)], 2.0),
    "hulkbuster_assemble": ([("doorClose_000", 0.6, 0, 0), ("impactPlate_heavy_002", 0.6, -1, 0.15), ("impactMetal_heavy_000", 0.7, -4, 0.3)], 1.0),
    "hulkbuster_exit": ([("doorOpen_000", 0.55, 0, 0), ("doorOpen_002", 0.6, -3, 0.2)], 1.2),
    "hulkbuster_break": ([("explosionCrunch_004", 0.6, 0, 0), ("impactMetal_heavy_004", 0.5, -2, 0.08)], 1.5),
    "hulkbuster_step": ([("impactPlate_heavy_001", 0.45, -2, 0), ("impactSoft_heavy_002", 0.6, -4, 0)], 0.5),
    "hulkbuster_servo": ([("doorOpen_001", 0.5, -6, 0)], 0.6),
    "hulkbuster_punch": ([("impactPunch_heavy_000", 0.6, 0, 0), ("impactMetal_heavy_001", 0.55, -2, 0)], 0.6),
    "hulkbuster_jackhammer": ([("impactMining_001", 0.8, 0, 0), ("impactMetal_heavy_002", 0.6, -4, 0)], 0.25),
    "hulkbuster_grab": ([("impactMetal_medium_004", 0.6, 0, 0), ("doorClose_001", 0.6, -4, 0.05)], 0.6),
    "hulkbuster_throw": ([("thrusterFire_002", 1.0, -1, 0)], 0.8),
    "hulkbuster_slam": ([("lowFrequency_explosion_000", 0.7, 0, 0), ("explosionCrunch_003", 0.6, -2, 0)], 1.8),
    "hulkbuster_hop": ([("thrusterFire_001", 0.7, 0, 0)], 1.0),
}


def find(dirs, stem):
    for d in dirs:
        for base, _, files in os.walk(d):
            if stem + ".ogg" in files:
                return os.path.join(base, stem + ".ogg")
    raise FileNotFoundError(stem)


def build(dirs, name, layers, length, tmp):
    inputs, filters = [], []
    for i, (stem, pitch, gain, delay) in enumerate(layers):
        inputs += ["-i", find(dirs, stem)]
        # Pitch by resampling (speed and pitch together, like a tape), then gain and delay.
        filters.append("[%d:a]aformat=sample_rates=44100:channel_layouts=mono,asetrate=%d,aresample=44100,volume=%sdB,adelay=%d[l%d]"
                       % (i, int(44100 * pitch), gain, int(delay * 1000), i))
    mix = "".join("[l%d]" % i for i in range(len(layers)))
    fade = max(0.05, min(0.25, length * 0.25))
    filters.append("%samix=inputs=%d:duration=longest:normalize=0,atrim=0:%s,afade=t=out:st=%s:d=%s,loudnorm=I=-16:TP=-1.5:LRA=11[out]"
                   % (mix, len(layers), length, max(0.0, length - fade), fade))
    out = os.path.join(tmp, name + ".ogg")
    subprocess.run(["ffmpeg", "-v", "error", "-y"] + inputs + ["-filter_complex", ";".join(filters), "-map", "[out]", "-ac", "1", "-ar", "44100",
                    "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    return out


def main(scifi, impact):
    dirs = [scifi, impact]
    with tempfile.TemporaryDirectory() as tmp:
        for name, (layers, length) in RECIPES.items():
            path = build(dirs, name, layers, length, tmp)
            with open(path, "rb") as f, open(os.path.join(OUT, name + ".ogg.b64"), "w", encoding="ascii") as out:
                out.write(base64.encodebytes(f.read()).decode("ascii"))
    for name in MUTED:
        path = os.path.join(OUT, name + ".ogg.b64")
        if os.path.exists(path):
            os.remove(path)
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        sounds = json.load(f)
    for event, entry in sounds.items():
        if not event.startswith("ironman_"):
            continue
        name = event[len("ironman_"):]
        if name in MUTED or name not in RECIPES and name not in FLIGHT:
            entry["sounds"] = []
    with open(SOUNDS_JSON, "w", encoding="utf-8") as f:
        json.dump(sounds, f, indent=2, ensure_ascii=False)
        f.write("\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
