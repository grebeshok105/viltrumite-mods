# Architecture

C4-level structure. The code is the truth for details (`AGENTS.md` §6).

## Modules
- Two Forge mods in one Gradle multi-project: `viltrumitecore` and `viltrumiteflight`.
- Dependency direction: `viltrumitecore` → `viltrumiteflight`. Core visual effects and abilities read flight state (`ViltrumiteFlightPlayer`, `FlightState`). Flight never reads core.
- `./gradlew runClient` in core loads both mods together.

## Client/common split
- Gameplay runs on the server. Client code lives in `client` packages and in `*.client.mixins.json`.
- Client model mixins read synced player data to pose the model. They do not own state.

## Per-player state
- Per-player state lives on `Player` through mixin interfaces: `ViltrumiteCorePlayer` (core), `ViltrumiteAbilityUser` (core ability bar), `ViltrumiteFlightPlayer` (flight).
- `PlayerEntityCoreMixin` (~1500 lines) holds most core player logic: synced data (`IS_VILTRUMITE`, `HAS_CHOSEN_RACE`, `IS_DASHING`, `DASH_TICKS`, `PUNCH_TICKS`, `IS_LEFT_ARM_PUNCH`, ...), dash, punch, chop, grab. It is a refactor target for the hero seam.
- Impact effects are driven by `PunchImpactManager`, `ChopImpactManager`, `ThunderClapManager`.

## Networking
- Core packets register in `CoreMessages` (Forge `SimpleChannel`). Packet classes live in `network/packet/`. Flight has its own `network/`.
- Input is C2S (`PunchC2SPacket`, `ChopC2SPacket`, toggles). Hit and VFX feedback is S2C (`*HitS2CPacket`, `BlockVFXS2CPacket`, ...).

## Abilities
- Abilities are `ViltrumiteAbility` entries in `ViltrumiteAbilities.REGISTRY`, keyed by string id (`viltrumite:block`, ...).
- Ability bar: 6 keys (`AbilityInputManager.abilityKey1..6`) × 3 pages = 18 slots. Slot ids sync as entity data in `PlayerAbilityMixin` and save to NBT as `ViltrumiteAbilitySlot_<i>` and `ViltrumiteActivePage`.
- `SwapAbilityBarC2SPacket` cycles the active page on the server: `(page + 1) % 3`.
- Cooldowns are per ability (`getPunchCooldown`, `getBlockCooldown`, `getBarrageCooldown` on `ViltrumiteCorePlayer`). No shared energy, mana or stamina pool exists.

## Animation and VFX
- Core has its own geo/animation loader in `client/anim/` for JSON in `assets/viltrumitecore/geo/` and `assets/viltrumitecore/animations/`. Details: `.agents/skills/animation-system/SKILL.md`.

## Hero model
- No hero abstraction exists yet. The code assumes one race (Viltrumite or human). The first second hero must add a shared hero contract (`AGENTS.md` §7).
