# Credits

Third-party and reused assets in this repository. Every committed file is listed with its source in a per-hero sources file (`tools/assets/<hero>_sources.md`).

## Iron Man

Sources list: `tools/assets/ironman_sources.md`. Permission texts: `docs/licenses/ironman/` (to be added — the owner holds the authors' messages).

### Own work
- Tony Stark skin `textures/entity/hero/ironman.png` (from Stage 1b; the 1a file from Codex-Superheroes was a suit skin and is replaced), the suit ability icon and the eight suit sounds — drawn / synthesized by the scripts in `tools/assets/` and `tools/sfx/`.

- Stage 3: helmet, scan and flare sounds (`tools/sfx/ironman_stage3.sh`), scan / countermeasures / helmet icons and the helmet HUD frame (`tools/assets/make_ironman_stage3_assets.py`).

- Stage 4: suit interior skin, Veronica pod model and texture, Veronica and signature icons, Mark 15 camouflage shimmer (`tools/assets/make_ironman_stage4_assets.py`, `make_ironman_stage4_visuals.py`, `make_ironman_stage4_signatures.py`), and 19 suit sounds (`tools/sfx/ironman_stage4.sh`).

### Codex-Superheroes — JARVIS voice lines
- Source: Codex-Superheroes repository, `assets/superheroes/sounds/ironman/`. License: CC0 1.0 (public domain, see its `LICENSE`).
- Used (Stage 3): `jarvis_detect`, `jarvis_detect_excited`, `jarvis_diagnostic`, `jarvis_mark85_preset` — re-encoded to mono 44.1 kHz by `tools/sfx/ironman_stage3.sh --jarvis`.

### Satsu — Iron Man Addon 3.6.1
- Author: Satsu.
- Used (Stage 1b): Mark 50 suit skin and glow map, Mark 50 head mask geometry, feet / palm / stabilizer flame geometry, `new_thruster_white` flame textures.
- Used (Stage 2 models): nano blade (`nanokatar`), nano hammer (`nano_mallet`), nano shield, `full_body` shoulder rockets, forearm rocket launcher (+ first person), nano stabilizer, electro-magnetic force field (+ first person) — `tools/assets/convert_ironman_stage2_parts.py`.
- Used (Stage 4 models): Mark 7, 15, 17, 39, War Machine Mk2, Iron Heart Mk3 skins (suit + `all_helmet` mask baked by `tools/bake_suit_skin.py`), `each_part` suit pieces, Mark 7 flaps, Mark 17 heartbreaker chest, War Machine shoulder pads and `torret_mark_2` with its clip, Iron Heart Mk3 plates, Mark 39 jetpack and flames, empty suit shell (`full_body` + helmet), `model_50` `suit_expulsion` — `tools/assets/convert_ironman_stage4_marks.py`.
- Permission: received by the repository owner, 2026-10-09 (spec §3.1). Text: pending in `docs/licenses/ironman/`.

### Sind — Iron Man pack 2.2.0
- Author: Sind.
- Used: Mark 42 suit skin, the 14 `mk42` pieces with their thrusters, glow map baked from the pieces' lights; Mark 7 micro-laser (`mk6/laser`, `mark7_laser`); Hulkbuster Mark 48 body, left arm, jackhammer arm (texture from the archive's prepared `jackhammer_arm_mark48`), fire models, first-person arms, `repulsor_layer` flame frames, and animations baked from the `hulkbuster/*.fsk` scripts — `tools/tabula2geo.py`, `tools/fsk2anim.py`, `tools/assets/convert_ironman_sind.py`. HUD reference only (redrawn).
- Permission: received by the repository owner, 2026-10-09 (spec §3.1). Text: pending in `docs/licenses/ironman/`.

The raw archive `IronMan_Hulkbuster_models.zip` is not committed. Only converted files that the mod uses are committed.
