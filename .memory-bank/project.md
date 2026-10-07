# Project memory

Read this file before every task. Keep it short. See `AGENTS.md` §6 for the rules.

## Origin
- The original author (package prefix `dev.baranhan`) gave the mods to the repository owner. Only the release JARs existed. The sources in this repo were decompiled from them.
- Original JARs: `original-jars/viltrumitecore-forge-1.10.3.jar`, `original-jars/viltrumiteflight-forge-1.6.7.jar`.

## Direction
- Goal: revival on a new architecture, then many heroes and characters. Viltrumites are the first hero.
- `grebeshok105/Codex-Superheroes` (Fabric) is dropped. It is an idea source only.

## Architecture relations
- `viltrumitecore` depends on `viltrumiteflight`. Core visual effects read flight state.
- `./gradlew runClient` loads both mods together.
- Per-player state lives on `Player` through mixin interfaces: `ViltrumiteCorePlayer` (core) and `ViltrumiteFlightPlayer` (flight, FlightState).
- `PlayerEntityCoreMixin` (~1500 lines) holds most core player logic: synced data (`IS_VILTRUMITE`, `HAS_CHOSEN_RACE`, `IS_DASHING`, `DASH_TICKS`, `PUNCH_TICKS`, `IS_LEFT_ARM_PUNCH`, ...), dash, punch, chop, grab. Client model mixins read this synced data to pose the model. It is a refactor target for the hero seam.
- Impact effects are driven by `PunchImpactManager`, `ChopImpactManager`, `ThunderClapManager`.
- Core has its own geo/animation loader in `client/anim/` for JSON in `assets/viltrumitecore/geo/` and `assets/viltrumitecore/animations/`. Details: `.agents/skills/animation-system/SKILL.md`.

## Topic files
- `decompile.md`: decompile artifacts and their fixes.