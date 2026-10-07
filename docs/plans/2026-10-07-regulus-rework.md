# Regulus Rework Implementation Plan

> **For agentic workers:** Use `superpowers:executing-plans` inline. The user forbids subagents, game launches, and in-game verification in this task.

**Goal:** Rework Regulus input, targeting, poses, and VFX over PR #3 while retaining the working server policies.

**Architecture:** Keep HeroRegistry, HeroDamage, ControlManager, lifecycle hooks, and HeroDestruction. Extend the existing public snapshot for action readiness and a server-locked target point. Use the current player-model/first-person mixins, weight manager, and pixel VFX only.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin, existing JUnit suite.

**Spec:** `docs/design/2026-10-05-regulus-design.md`, amended by the user's 2026-10-07 Regulus rework prompt and its no-game/no-subagent instruction.

## Global Constraints

- Do not start `runClient`, `runServer`, UI testing, or child sessions.
- No new animation/VFX engine, dependency, or post chain.
- Client code stays in client packages; all gameplay input is server-authoritative.
- Keep 12 carriers, 20-block assignment reach, carrier death backlash, and unloaded-carrier cleanup.
- Keep Lion timing, damage/control policies, projectile latch, and release behavior.
- Keep Mania channel/freeze and deferred damage; keep world-owned Embrace domes.
- Evangelium uses held book RMB only. Movement and external damage no longer cancel it.
- The approved spec §11.2 says flat +0.4 attack, not +40%. Keep that value and Strength III; correct the inaccurate PR text. Omit ineffective extra +10 Madness armor (base armor already exceeds the vanilla cap).
- Build commands use `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.

## Diagnosis (code evidence, not reproduced in game)

| Symptom | Current code path | Root cause | Planned solution |
|---|---|---|---|
| VFX appear camera-attached | Both Regulus VFX managers → identity model-view + relative vertices | Camera yaw/pitch missing | Apply the same camera rotations used by Block/Punch VFX exactly once; retain relative world vertices |
| Floating sphere panels | `RegulusPixelVfx.domeShell` → `groundPixel` | Horizontal XZ quads used for volumetric points | Camera-facing pixels for volume; horizontal pixels only on actual surfaces |
| Decorative rings everywhere | Lion `BURSTS`; Action `RINGS`; Debris `sendRing` | Shared ring motifs applied without gameplay meaning | Delete these sources; use body/chest flashes, ground fragments, frozen motes; keep only the Embrace boundary as a spatial telegraph |
| Lion fills local view | Lion renderer draws every active aura, including camera owner | No first-person exclusion | No local first-person aura; world projectiles + HUD/sound remain; body aura for third person/observers |
| Hearts appear automatically | Hearts `tick` → `scanForCarriers` | Assignment and lifecycle scan coupled | Tick only prunes; MMB action resolves gaze entirely server-side and toggles one carrier |
| Carrier marker outside body | Lion renderer → gold ground ring + overhead pixel | Wrong anchor, scale, and color | Tiny owner-only red chest-center glow, visible on body, hidden behind terrain |
| Ritual stops on movement/hit | Evangelium position threshold; Hero damage bookkeeping | Old cancellation rules contradict new request | Use server item-use state; remove movement/hit cancellation; completion guard and release feedback |
| Madness described as +40% | PR description vs Passives `ADDITION 0.4` | PR text contradicts approved spec §11.2 | Preserve spec value, remove redundant Madness armor, correct player-facing text |
| Debris is nine rays | Debris `fire` → nine casts, eye-level particles, Block packet | Old hitscan implementation | Eight server-tick ground slices, 1 block/tick, shallow trench, one hit/target, forward fragments via existing particles/destruction helpers |
| Dome floats at sky endpoint | Embrace `lockPoint` → MISS returns 40-block end | No MISS policy or synchronized preview | Aim at exact surface; on MISS use 12-block horizontal reach + downward probe, near fallback; sync locked point at tick 13 |
| Embrace fails silently | `appear` duplicate check → clear action | No player feedback | Check at start and event, actionbar message, no cooldown |
| Mania regression risk | Mania acquisition/channel → ControlManager | Backend already covers interrupt, freeze, cleanup | Retain it; keep tether on actual target; review LOS/control cleanup and improve aiming pose |
| Counter icon claims ready without attacker | Public snapshot lacks attacker readiness | HUD has Madness-only gate | Generic action-availability mask; server evaluates current attacker; same gate drives HUD; keep Lion-blocked attacker capture |
| Poses lose locomotion/aim/reset | Regulus absolute keyframe ends, hard-coded right arm | Recovery ends at zero rather than vanilla; book-side ignored | Smooth existing keyframe lerps, blend recovery to vanilla, additive body/head/legs, reset pivots, follow aim and active book hand |
| Pose timing steps | `actionElapsed + partialTick` in multiple render paths | No shared observation clock; FP and TP weights share state | Per-view weights and bounded game-time snapshot interpolation in existing manager |
| Abstract low-detail icons | 16×16 geometric symbols beside painted Viltrumite icons | Wrong visual language/action silhouette | Draw 16×16 shaded action portraits (chest/ground kick/hand/dome/slam); review contact sheet without game |

## Review Focus

- Invalid/stale/hostile heart target must not assign; removing an owned heart works at cap; no auto-refill.
- Ritual release on final tick must neither double-complete nor grant Madness early.
- Camera yaw/pitch must apply once, volumetric dots must face camera, GL state must restore.
- Ground wave must stop at unbreakable obstruction, honor mobGriefing, and hit anchored/Lion targets only through shared policies.
- Embrace preview and final point must agree after lock; Counter readiness must expire with attacker freshness/range/life.

## Task 1: Manual hearts, ritual, and readiness

**Files:** `client/AbilityInputManager.java`, `client/ViltrumiteCoreClient.java`, `hero/HeroAction.java`, `hero/HeroPublicSnapshot.java`, `hero/regulus/{RegulusHearts,RegulusHero,RegulusState,Evangelium,Counter,RegulusPassives,RegulusRules}.java`, existing HUD policy and tests.

**Interfaces:** Append `ASSIGN_HEART` to HeroAction (do not shift existing wire ordinals). Existing `HeroInputC2SPacket` carries the action only; server chooses the gaze target. Add `availableActions` and nullable `actionTarget` to the snapshot with backward-compatible decoding/default constructor. `Counter.ready(ServerPlayer, RegulusState)` shares the start validation.

- [x] Pin tests for cap-toggle/invalid carrier policies, readiness serialization, and ritual completion eligibility/idempotency.
- [x] Run focused JUnit tests; capture expected failures.
- [x] Register IN_GAME KeyMapping default mouse button 2; send one press, never a client entity id. Resolve block-clipped 20-block gaze on server; validate alive/type/dimension/LOS and report no-target/invalid/full/add/remove/locked. Preserve dimension-bound lifecycle pruning.
- [x] Remove ritual position field/threshold and external-hit interruption. Tick only while the server is using the book; finish only at 60 ticks; release before completion cancels and reports feedback. N does not start the book.
- [x] Sync live Counter readiness; omit ineffective +10 Madness armor, retain flat attack spec and existing buffs/skin/HUD/sound.
- [x] Run focused tests and commit the functional slice.

## Task 2: Ground wave and targeted Embrace

**Files:** `hero/regulus/{DebrisKick,RegulusRules,RegulusState,GreedsEmbrace}.java`, public snapshot production, relevant tests.

**Interfaces:** `DebrisKick.tick` advances a transient wave independent of recovery animation; wave state clears with hero cleanup. `GreedsEmbrace.aimPoint(Player)` is shared by client preview and server lock, with `actionTarget` authoritative after lock.

- [x] Pin pure eight-slice/unique-hit bounds and 12-block MISS fallback; preserve existing cooldown/control tests.
- [x] Run focused tests for the new contracts before implementation.
- [x] Replace nine-ray fire with eight forward ground slices: yaw-based direction, width ~3, depth 1, find walkable surface within ±2 blocks, stop blocked center lane; destroy through HeroDestruction; damage/knockback once per target using ControlManager and hero impulse policy. No Block VFX packet.
- [x] Use existing block particles and directional shards/dust to show progression; no rings or eye-level cone. Preserve generic ray distance helper used by Mania.
- [x] Raycast Embrace to a hit surface, otherwise down-probe a nearby horizontal point; preview until lock 13, dome event 18, recovery 32. Feed exact locked point to VFX. Give duplicate feedback at both start and event.
- [x] Run tests and commit the functional slice.

## Task 3: World-space VFX and body poses

**Files:** `client/render/vfx/{RegulusPixelVfx,RegulusLionVFXManager,RegulusActionVFXManager}.java`, `client/render/animation/RegulusAnimationManager.java`, `client/regulus/RegulusPoseTiming.java`, `client/mixin/{RegulusModelMixin,FirstPersonRegulusMixin,RegulusHudMixin}.java`.

**Interfaces:** `domeShell(..., Camera, ...)` emits billboards; existing manager owns per-view continuous weights and `actionTime(LivingEntity, HeroPublicSnapshot, float)` with bounded client game-time extrapolation.

- [x] Pin source/resource geometry and input contracts plus bounded timing math tests; run focused JUnit tests.
- [x] Fix both camera transforms and ensure restored render state. Remove RINGS/Burst ring emitters and unused generic ring helpers; use dome boundary only as Embrace telegraph.
- [x] Rework Lion windup/activation/active/overheat/off: white-gold frozen mote boundary for observers, chest activation flash, cooling chest fade; never local FP aura. Carrier red point is a separate owner-only LOS pass.
- [x] Render Embrace preview/locked telegraph and fading full 3D dome with billboard motes; frozen targets retain clear thin outlines. Counter uses latched slam point/teleport flash and ground debris, not rings.
- [x] Separate FP/TP pose weights. Preserve model pivots and cloak base, copy every outer layer, retain crouch/vanilla locomotion, shadow pass. Rework full-body kick and Lion chest pose; Mania follows gaze; Embrace casts toward aim; Counter lift/transition/slam recovers; book follows active hand and completion fades to Madness.
- [x] Run focused checks and commit the functional slice. Mark all visual behavior unverified in game.

## Task 4: Icons, docs, build, and self-review

**Files:** five existing Regulus ability PNGs, all 11 language files, version/build docs, design amendments, append-only `SESSION.md`, animation skill if interpolation capability changes.

- [x] Compare painted Viltrumite icons; replace abstract shapes with shaded 16×16 action silhouettes. Check size/alpha via the existing resource tests; view a contact sheet (not game screenshots).
- [x] Update player text for manual hearts, wave, book cancel, feedback and true flat attack value across locales; document new rules and prohibited verification.
- [x] Bump changed core version for new input/content (1.11.0 → 1.12.0); flight is unchanged.
- [x] Run `git diff --check` and `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew build --no-daemon`. Java compilation is the type check; no configured lint task.
- [x] Self-review standards, spec coverage, server/client boundary, tests and assets. No reviewer agents. Re-run relevant checks after any fix.
- [ ] Commit/push to the requested PR #3 branch; read/update existing PR body and CI. Deliver core jar and concise findings; explicitly state game/visual/multiplayer validation was not performed.

## Final verification

- Java 17 `./gradlew :viltrumitecore:test build --no-daemon`: BUILD SUCCESSFUL; 141 JUnit tests, no failures, errors, or skipped tests.
- `git diff --check`: clean. All core JSON resources parse; all 11 locales contain assignment/dome feedback keys; six new textures are 16×16 RGBA.
- Reobfuscated core 1.12.0 jar: ZIP CRC, version and asset parity checked. Flight remains 1.7.0.
- Self-review covered server input validation, item-use completion/release, wave target deduplication and destruction gate, synchronized surface preview, camera-relative geometry, FP/TP and hand separation, pivots/outer layers and left-hand mirroring.
- Plan deviation: runtime validation, recording and independent agent review were omitted by the user's explicit restriction. Visual quality, live input and multiplayer behavior remain unverified. No new dependency or animation/VFX system was introduced.
