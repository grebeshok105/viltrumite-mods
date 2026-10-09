# Credits

Third-party and reused assets in this repository. Every committed file is listed with its source in a per-hero sources file (`tools/assets/<hero>_sources.md`).

## Iron Man

Sources list: `tools/assets/ironman_sources.md`. Permission texts: `docs/licenses/ironman/` (to be added — the owner holds the authors' messages).

### Own work
- Tony Stark skin `textures/entity/hero/ironman.png` (from Stage 1b; the 1a file from Codex-Superheroes was a suit skin and is replaced), the suit ability icon and the eight suit sounds — drawn / synthesized by the scripts in `tools/assets/` and `tools/sfx/`.

- Stage 3: helmet, scan and flare sounds (`tools/sfx/ironman_stage3.sh`), scan / countermeasures / helmet icons and the helmet HUD frame (`tools/assets/make_ironman_stage3_assets.py`).

### Codex-Superheroes — JARVIS voice lines
- Source: Codex-Superheroes repository, `assets/superheroes/sounds/ironman/`. License: CC0 1.0 (public domain, see its `LICENSE`).
- Used (Stage 3): `jarvis_detect`, `jarvis_detect_excited`, `jarvis_diagnostic`, `jarvis_mark85_preset` — re-encoded to mono 44.1 kHz by `tools/sfx/ironman_stage3.sh --jarvis`.

### Satsu — Iron Man Addon 3.6.1
- Author: Satsu.
- Used (Stage 1b): Mark 50 suit skin and glow map, Mark 50 head mask geometry, feet / palm / stabilizer flame geometry, `new_thruster_white` flame textures.
- Planned: Mark 50 animations, marks, nano weapons, shields, `each_part` cut, `suit_expulsion`.
- Permission: received by the repository owner, 2026-10-09 (spec §3.1). Text: pending in `docs/licenses/ironman/`.

### Sind — Iron Man pack 2.2.0
- Author: Sind.
- Used (planned from Stage 1b): Mark 42 modular parts, Hulkbuster Mark 48, flames, HUD reference.
- Permission: received by the repository owner, 2026-10-09 (spec §3.1). Text: pending in `docs/licenses/ironman/`.

The raw archive `IronMan_Hulkbuster_models.zip` is not committed. Only converted files that the mod uses are committed.
