# Homelander Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the playable hero Homelander that replaces the Viltrumite race, reuses the Viltrumite kit (flight, punch, thunderclap, dash in flight) and adds eye lasers, focus, roar, eye heat and regeneration.

**Architecture:** `HeroId.HOMELANDER` with `hero/homelander/` (server) and `client/homelander/` (client). The legacy Viltrumite kit becomes a shared kit: `isViltrumite()` means "uses the legacy kit" and every legacy ability start asks the hero (`allowsLegacyAbility`). Saved Viltrumites load as Homelander through `HeroId` aliases. New input, snapshot and owner-snapshot data go through generic seams, not Homelander-named fields in shared code.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5 (already set up in `viltrumitecore`).

**Spec:** `docs/design/2026-10-08-homelander-design.md`

Paths: `core/` = `viltrumitecore/src/main/java/dev/baranhan/viltrumitecore/`, `res/` = `viltrumitecore/src/main/resources/assets/viltrumitecore/`, `test/` = `viltrumitecore/src/test/java/dev/baranhan/viltrumitecore/`.

## Global Constraints

- `AGENTS.md` §3: animations and VFX only with `client/anim/`, model mixins, `client/render/vfx/`, particles, post shaders. Read `.agents/skills/animation-system/SKILL.md` before Tasks 7, 8, 10, 11.
- `AGENTS.md` §7: no `HeroId.HOMELANDER` branch in shared code. Shared code asks `HeroDefinition`.
- `AGENTS.md` §8: never rename registry ids, NBT keys (`IsViltrumite`, `HeroData`), synced data, key-mapping ids. Append to enums (`HeroId`, `HeroAction`); never reorder.
- Client classes only in `client` packages and `*.client.mixins.json`. Dedicated server must start.
- Kit behaviour, VFX and sounds of flight, punch, thunderclap, dash, landing do not change.
- All numbers come from spec §10 and live in `HomelanderRules`.
- Lang: every key in every file of `res/lang/`.
- Assets from Codex (`assets/superheroes/...`): skin `textures/entity/hero/homelander.png`, sounds `sounds/homelander/{laser_charge,laser_loop,laser_release,roar,roar_deep}.ogg`. Binaries pushed through GitHub MCP go to `viltrumitecore/src/main/binassets/<path>.b64`.

## Review Focus

1. Relog or death while the laser key is held → no stuck beam, pose or loop sound on any client (Task 7 test: `cleanup` resets `LaserState`).
2. Lasers and focus both active when heat reaches 100 → both stop in the same tick, lock 60 t (Task 5 test `overheatStopsBothSources`).
3. Focus target dies, unloads or changes dimension → removed on the next refresh, its fear and outline end (Task 9 test `dropsDeadAndFarTargets`).
4. Old world: player saved with `IsViltrumite=true` or hero key `viltrumite` → loads as Homelander, no race screen (Task 1 tests).
5. Forged C2S packet for grab/chop/barrage/speed/block/lock from a Homelander → refused on the server (Task 2 test `homelanderRefusesExcludedKit`).

---

### Task 1: Hero id and migration

**Files:**
- Modify: `core/hero/HeroId.java`, `core/mixin/PlayerHeroMixin.java:54`
- Test: `test/hero/HeroIdMigrationTest.java`

**Interfaces:**
- Produces: `HeroId.HOMELANDER("homelander")` (last value). `HeroId.fromKey("viltrumite") == HOMELANDER`. `HeroId.fromLegacyBoolean(true) == HOMELANDER`. `HeroId.legacySet(cur, b)` toggles `HUMAN <-> HOMELANDER` when `cur` is `HUMAN`, `VILTRUMITE` or `HOMELANDER`; other heroes unchanged.

- [ ] Step 1: Tests: `fromKeyAliasesViltrumite`, `legacyTrueIsHomelander`, `legacySetKeepsRegulus` (`legacySet(REGULUS, true) == REGULUS`), `legacySetFalseFromHomelanderIsHuman`, `homelanderIsLastOrdinal`.
- [ ] Step 2: Run `./gradlew :viltrumitecore:test --tests '*HeroIdMigrationTest'` → FAIL.
- [ ] Step 3: Implement. `VILTRUMITE` stays in the enum (ordinal stability) but no path produces it. Default hero from `isViltrumiteByDefault` → `HOMELANDER`.
- [ ] Step 4: Tests PASS.
- [ ] Step 5: Commit `feat(hero): add homelander id and migrate viltrumites`.

### Task 2: Shared legacy kit gate + Homelander skeleton

**Files:**
- Create: `core/hero/homelander/HomelanderHero.java`, `core/hero/homelander/HomelanderAbilities.java`, `core/hero/LegacyKit.java`
- Modify: `core/hero/HeroDefinition.java`, `core/hero/HeroRegistry.java`, `core/mixin/PlayerEntityCoreMixin.java:216` (`isViltrumite`), the C2S packets `Punch/Dash/Grab/Block/Chop/Speed/Barrage/Thunderclap*C2SPacket`, `core/client/ViltrumiteCoreClient.java` (kit key block), `core/ability/ViltrumiteAbilities.java` (dash grey-out)
- Test: `test/hero/homelander/HomelanderHeroTest.java`

**Interfaces:**
- Produces: `HeroDefinition.allowsLegacyAbility(Player p, String abilityId)` default `allowsLegacyAbilities(p) && ownsAbility(abilityId)`. `LegacyKit.allows(Player p, String abilityId)` = `HeroRegistry.get(p).allowsLegacyAbility(p, abilityId)`; used by every kit packet and client key branch instead of `isViltrumite()`.
- `isViltrumite()` returns `HeroRegistry.get(this).allowsLegacyAbilities(this)` (stats, immunities, flight stay for kit users).
- `HomelanderAbilities`: ids `homelander:lasers`, `homelander:focus`, `homelander:roar`; kit ids `viltrumite:punch`, `viltrumite:dash`, `viltrumite:thunderclap`. `defaultLoadout()` page 1 = `punch, dash, lasers, focus, roar, thunderclap` (punch and dash keep their Viltrumite keys R, Y).
- `HomelanderHero`: `allowsFlight` true, `allowsLegacyAbilities` true, `allowsLegacyAbility(p, "viltrumite:dash")` true only when `FlightState != NONE`.

- [ ] Step 1: Tests: `ownsKitSubset` (owns punch/dash/thunderclap; not grab/chop/barrage/speed/block/lock), `homelanderRefusesExcludedKit`, `dashOnlyInFlight` (pure predicate `HomelanderAbilities.dashAllowed(FlightState)`), `loadoutOrder`.
- [ ] Step 2: FAIL. Step 3: implement, register in `HeroRegistry.registerDefaults()`. Step 4: PASS.
- [ ] Step 5: Commit `feat(hero): homelander uses shared viltrumite kit`.

### Task 3: Choosing the hero, skin, lang

**Files:**
- Create: `core/client/homelander/HomelanderClient.java`, `res/textures/entity/hero/homelander.png` (+ `.b64`)
- Modify: `core/client/gui/RaceSelectionScreen.java`, `core/client/ViltrumiteCoreClient.java` (init), `res/lang/*.json`

- [ ] Step 1: Viltrumite button → `become_homelander` sending `HeroChoiceC2SPacket("homelander")`. Head preview helper draws Regulus and Homelander heads over their buttons (one method, skin as parameter).
- [ ] Step 2: `HomelanderClient.init()` → `HeroSkins.register(HeroId.HOMELANDER, ...)`; model type from the Codex skin (check arm width pixels).
- [ ] Step 3: Lang keys `gui.viltrumitecore.race_selection.become_homelander`, hero name, ability names/descs (Task 12 adds the rest) in all locales.
- [ ] Step 4: `./gradlew build` green; `check_hero_resources.py --hero homelander` 0 new problems.
- [ ] Step 5: Commit `feat(homelander): race button, skin`.

### Task 4: Generic hero input routing and snapshot fields

**Files:**
- Modify: `core/hero/HeroDefinition.java`, `core/hero/HeroAction.java`, `core/client/ViltrumiteCoreClient.java` (Regulus key block), `core/hero/regulus/RegulusHero.java`, `core/hero/HeroPublicSnapshot.java`
- Test: `test/hero/HeroPublicSnapshotTest.java` (extend if present)

**Interfaces:**
- Produces: `HeroDefinition.heroInputSlots()` → `String[]` (default empty) and `heroActionFor(String id)` → `HeroAction` (default null). The client loop sends `HeroInputC2SPacket(action, down)` on key edge for any hero. Regulus implements both with `RegulusAbilities`; the Regulus branch is removed.
- `HeroAction` append `LASERS, FOCUS, ROAR`.
- `HeroPublicSnapshot` append `int resource` (0–1000, heat ×10), `boolean resourceLocked`, `int heroFlags` (bit 0 = laser on, bit 1 = focus on). Old strings decode with defaults 0/false/0.

- [ ] Step 1: Tests `encodeDecodeKeepsResourceFields`, `oldEncodingDecodesWithDefaults`.
- [ ] Step 2: FAIL → implement → PASS. Regulus input still works (build + existing Regulus tests green).
- [ ] Step 3: Commit `refactor(hero): generic hero input slots and resource snapshot`.

### Task 5: Rules and eye heat

**Files:**
- Create: `core/hero/homelander/HomelanderRules.java`, `core/hero/homelander/EyeHeat.java`, `core/hero/homelander/HomelanderState.java`
- Test: `test/hero/homelander/EyeHeatTest.java`

**Interfaces:**
- `HomelanderRules`: all constants of spec §10 with the names in the table.
- `EyeHeat`: `void tick(boolean laser, boolean focus)`, `float heat()`, `boolean locked()`, `boolean justOverheated()`. Heat +1/t laser, +0.25/t focus, additive; cooling starts after 20 t idle at 100/120 per t; at 100 → locked 60 t.
- `HomelanderState`: `EyeHeat heat`, `LaserState laser`, `FocusState focus`, `int roarCooldown`, `int regenTimer`, `int regenPause`.

- [ ] Step 1: Tests: `laserFillsIn100Ticks`, `focusFillsIn400Ticks`, `sourcesAdd`, `coolingWaits20Ticks`, `coolsFrom100In120Ticks`, `overheatStopsBothSources`, `lockLasts60Ticks`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(homelander): eye heat rules`.

### Task 6: Regeneration and hero tick

**Files:** Modify `HomelanderHero.java`; Test `test/hero/homelander/RegenTest.java`

- [ ] Step 1: Pure `HomelanderRules.regenStep(int timer, int pause)` tests: `healsEvery40Ticks`, `damagePauses100Ticks`.
- [ ] Step 2: Implement; damage hook through `LivingHurtEvent` in `HeroEvents` → new default hook `HeroDefinition.onHurt(ServerPlayer, DamageSource, float)` (no hero branch). `tick` drives heat, regen, cooldowns, `syncSnapshot`.
- [ ] Step 3: PASS. Commit `feat(homelander): regeneration`.

### Task 7: Eye lasers

**Files:**
- Create: `core/hero/homelander/EyeLasers.java`, `core/client/homelander/LaserBeamRenderer.java`, `core/client/homelander/ScorchMarks.java`, `core/client/homelander/HomelanderPoser.java`, sounds in `res/sounds.json` + core sound registry
- Test: `test/hero/homelander/EyeLasersTest.java`

**Interfaces:**
- `EyeLasers.tryStart(ServerPlayer)`, `tick(ServerPlayer)`, `cancel(ServerPlayer)`. Hold input: `LASERS` pressed → start (4 t charge), released → cancel. Server ray-cast 64 blocks, first entity or block; 3 damage every 4 t through `HeroDamage.route`, `setSecondsOnFire(4)`. No block changes.
- Client: beam from eyes for every player whose snapshot flag bit 0 is set; client-side ray-cast for the hit point; `ScorchMarks.add(pos, face)` cap 256, life 600 t, oldest removed first.

- [ ] Step 1: Tests: `damageEvery4Ticks` (pure cadence `HomelanderRules.laserDamageTick(int age)`), `chargeDelays4Ticks`, `scorchCapDropsOldest` (pure ring buffer in `ScorchMarks.Buffer`).
- [ ] Step 2: FAIL → implement server → PASS.
- [ ] Step 3: Client per `animation-system`: head pose forward, red eye glow, beam core + glow, hit spark, loop sound; first and third person.
- [ ] Step 4: `cleanup` stops lasers for every reason. Commit `feat(homelander): eye lasers`.

### Task 8: Roar

**Files:** Create `core/hero/homelander/Roar.java`, client pose/FX in `client/homelander/`; Test `test/hero/homelander/RoarTest.java`

- [ ] Step 1: Pure `HomelanderRules.inCone(Vec3 look, Vec3 toTarget, double dist)` tests: `insideCone`, `outside30Degrees`, `beyond12Blocks`.
- [ ] Step 2: Implement: knockback, Slowness II 60 t, camera shake for target players (`CameraShake` via `HeroFx`), cooldown 300 t in snapshot `cooldowns[0]`. Sounds `roar`, `roar_deep`. Pose arms back, chest forward; air wave via `HeroFx.shockwave` cone.
- [ ] Step 3: PASS. Commit `feat(homelander): roar`.

### Task 9: Focus (server) and fear

**Files:**
- Create: `core/hero/homelander/Focus.java`, `core/hero/homelander/FocusTargets.java`, `core/effect/FearEffect.java` (registered in the core effect registry)
- Modify: `core/hero/HeroOwnerSnapshot.java` (rename javadoc meaning to "owner-only entity ids"; field name unchanged)
- Test: `test/hero/homelander/FocusTargetsTest.java`

**Interfaces:**
- `FocusTargets.select(List<Candidate> all, int max)` → nearest `max` within 24; `Candidate(int id, double dist, boolean excluded)`. `FocusTargets.keep(dist, alive)` → false beyond 48 or dead.
- Toggle input `FOCUS`. Refresh every 10 t. Owner ids pushed with `HeroRegistry.pushOwnerSnapshot`.
- Fear: `FearEffect` amplifier 0, re-applied each refresh with duration 30 t (linger 20 t after drop). Players: Slowness I + client visuals (Task 10). `PathfinderMob`: Slowness I + `navigation.moveTo(DefaultRandomPos.getPosAway(...))` every refresh. Bosses (`WitherBoss`, `EnderDragon`) get no fear.
- Exclusions: self, own tamed animals, armor stands.

- [ ] Step 1: Tests: `picksNearest8`, `skipsExcluded`, `dropsDeadAndFarTargets`, `keepsTargetBetween24And48`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(homelander): focus targets and fear`.

### Task 10: Focus (client)

**Files:** Create `core/client/homelander/FocusClient.java`, `FocusPalette.java`, `FocusFootsteps.java`, `FearClient.java`; Test `test/client/homelander/FocusPaletteTest.java`

- [ ] Step 1: `FocusPalette.colorFor(int slotIndex)` — 8 fixed colours, no red (hue outside 345°–15°); colour sticks to entity id while it stays a target. Tests `eightDistinctColors`, `noRed`, `colorSticks`.
- [ ] Step 2: Outline through walls via the vanilla outline buffer (memory bank "Rendering gotchas"), per-target colour; HP label `❤ cur/max` above the target; vignette; muffle non-target sounds ×0.6 via `PlaySoundEvent`; `FocusFootsteps` plays louder step sounds at target position from its client walk distance.
- [ ] Step 3: `FearClient`: local player with `FearEffect` → dark vignette, heartbeat loop, one-time title «Ты чувствуешь на себе чей-то взгляд» (lang key `hud.viltrumitecore.homelander.fear`).
- [ ] Step 4: Eye glow while focus on. Commit `feat(homelander): focus visuals`.

### Task 11: Focus route

**Files:** Create `core/client/homelander/GroundRoute.java` (pure), `FocusRouteRenderer.java`; Test `test/client/homelander/GroundRouteTest.java`

**Interfaces:**
- `GroundRoute.find(BlockPos from, BlockPos to, Walkable walkable, int maxNodes)` → `List<BlockPos>` or empty. `Walkable { boolean standable(int x,int y,int z); }`. A* over 4 neighbours with step up/down 1 block, max 4000 nodes. Start = ground under the player (scan down ≤ 64).
- Recompute every 10 t per target. Empty → dashed straight arc at 50% alpha.

- [ ] Step 1: Tests: `straightCorridor`, `goesAroundWall`, `climbsStep`, `noRouteInSealedRoom`, `nodeLimitReturnsEmpty`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Renderer: thin line in target colour, moving soft sparks, fades toward target. Commit `feat(homelander): focus route`.

### Task 12: HUD, panel, icons, lang

**Files:** Modify `core/ability/ViltrumiteAbilities.java`; create `res/textures/gui/ability/homelander/{lasers,focus,roar,thunderclap,dash,punch}.png` (+ `.b64`), heat bar in a client HUD overlay; lang all locales.

- [ ] Step 1: Register slots `homelander:lasers|focus|roar` with grey-out from snapshot (`resourceLocked`, `cooldowns[0]`). Homelander icons for kit slots through a hero icon override hook `HeroDefinition.abilityIcon(String id)` (default null → registry icon).
- [ ] Step 2: Draw six 16×16 icons in the Viltrumite style (pixel art, hero in action, flat bright background with highlight). Homelander: blue suit, red cape, gold eagle and epaulettes, blond hair. Render a 8× preview for review.
- [ ] Step 3: Heat bar above the hotbar: visible when `resource > 0`, blinks when locked.
- [ ] Step 4: Checker 0 new problems. Commit `feat(homelander): panel, icons, heat bar`.

### Task 13: Architecture guard

**Files:** Create `test/architecture/NoHeroBranchTest.java`

- [ ] Step 1: Test scans `core/**/*.java` outside `hero/<id>/` and `client/<id>/` for `HeroId.(VILTRUMITE|REGULUS|HOMELANDER)`; fails on files not in the legacy allowlist (current `grep -rln "HeroId.REGULUS"` list + `HeroId.java`).
- [ ] Step 2: PASS. Commit `test(arch): guard against hero branches in shared code`.

### Task 14: Ship

- [ ] `./gradlew build` green with JDK 17; `git diff --check` clean.
- [ ] Dedicated server start: `./gradlew :viltrumitecore:runServer` reaches "Done" (accept EULA in run dir).
- [ ] In-game check if a client can run; otherwise state it in the PR.
- [ ] Update `.memory-bank/project.md` (kit gate, aliases, generic input/snapshot), `add_hero` skill (`hero-seam.md` new hooks, remove the fixed Regulus input debt), `animation-system` skill if extended, `SESSION.md` entry in Russian.
- [ ] PR: title «Homelander вместо Вильтрумита», body starts with «Для игрока».
