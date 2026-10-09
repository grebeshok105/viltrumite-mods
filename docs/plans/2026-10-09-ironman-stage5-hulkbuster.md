# Iron Man — Stage 5 (Hulkbuster Mark 48) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hulkbuster Mark 48 is chosen in the Veronica menu, its parts drop from the capsule and assemble **over** the current suit (nano or mark); the player becomes big (Mark 48 model, bigger hitbox); own durability absorbs all damage, breaking leaves the player in their suit; kit: huge LMB punches, RMB jackhammer (hold + series), MMB jackhammer ↔ slow repulsors, slot 1 grab & throw, slot 2 jump slam, slot 3 short thruster hop; no real flight, no Unibeam/missiles; "Костюм" climbs out; cooldown 10 min after break or exit.

**Architecture:** Hulkbuster is a **layer** in `IronManState` (`HulkbusterLayer`: active, durability, cooldown, assemble/exit timelines) above `Suit` — the inner suit state is untouched and resumes after. Size goes through a new generic hook `HeroDefinition.bodyScale(Player)` (default 1.0) read by a Forge `EntityEvent.Size` handler (scales the pose dimensions and eye height; the event exists in Forge 47.3.0), with `refreshDimensions()` on every change. No dimension mixin. The Mark 48 renders from Sind Tabula models converted with Stage 4 `tools/tabula2geo.py`, animations from Sind `.fsk` converted with `tools/fsk2anim.py`, played with the existing `client/anim` controller; the player model is hidden while inside. Grab reuses the shared grab mechanics behind a helper, not the Viltrumite kit gate.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3.

**Spec:** `docs/design/2026-10-09-ironman-design.md` §3.1, §4.4, §4.5, §14, §15.2, §16, §17. Previous plans: Stages 1–4.

Paths as in Stage 1; `hb/` = `core/hero/ironman/hulkbuster/`.

## Global Constraints

- All Stage 1–4 Global Constraints apply.
- `allowsFlight` false while the Hulkbuster is on (the hop is not mod flight); Unibeam, missiles, nano-arsenal, mark signatures, Veronica part-equip refused; shield F = Hulkbuster forearm block with the same `Shield` rules (×1.5 strength); helmet/JARVIS/scan work (Hulkbuster helmet always closed).
- Bigger hitbox must not clip into blocks: free space is checked with the **real final box** = current pose dimensions × 1.7 (standing ≈ 1.02 × 3.06, crouching and swimming differ), at three moments: before the drop, again at the `ASSEMBLING → ACTIVE` transition, and when an ACTIVE layer is restored on load (or DROPPING/ASSEMBLING resolved to ACTIVE). No room → search for a free spot within 2 blocks (`BodyScale.findFree`); none → safe refusal: the layer becomes `NONE`, parts fly back, **no cooldown**, HUD hint. A pose change while ACTIVE that does not fit (e.g. standing up under a low ceiling) keeps the smaller pose (vanilla `canEnterPose` with the scaled box).
- Damage goes through the 1a damage-layer seam (`absorbIncoming`): control → shield → **Hulkbuster** → mark → nano armor → Tony.
- Cooldowns reach the client through Stage 2 `extraCooldowns` (5–7 slots 1–3, 8 Hulkbuster cooldown).
- Durability, cooldown: `HULKBUSTER_DURABILITY=400`, `HULKBUSTER_COOLDOWN=12000` t (starts on break or exit, spec §14.1). Iron Man in Hulkbuster still weaker than Homelander head-on (spec §18) — tuned after the in-game check.
- Dedicated server: the `EntityEvent.Size` handler is common code; the renderer is client-only.

## Review Focus

1. Hulkbuster over a mark: damage → Hulkbuster durability → (after break) mark durability → Tony; never two layers damaged by one hit (Task 2 test `layersInOrder`).
2. Hitbox: grow only with free space; shrink on exit/break/death/logout never leaves the player inside blocks or suffocating (Task 3 tests + manual in a 2-block tunnel).
3. Relog inside the Hulkbuster → still inside, size restored, no double cooldown (Task 1 test `saveLoadInside`).
4. Grab & throw: grabbed entity is released on any end (death, control, break, logout of either side) (Task 6 test `releaseOnEveryEnd`).
5. No hero branch in the size handler — only `bodyScale` (NoHeroBranchTest + grep).

---

### Task 1: Hulkbuster layer state

**Files:**
- Create: `hb/HulkbusterLayer.java` (`NONE, DROPPING, ASSEMBLING, PARTIAL, ACTIVE, EXITING, BREAKING`), `hb/HulkbusterRules.java` (or section of `IronManRules`)
- Modify: `IronManState.java`, `IronManHero.java` ("Костюм" while ACTIVE → EXITING, spec §4.5 row 4), `VeronicaScreen` (enable the Hulkbuster card, cooldown shown), `IronManFlags` (bits for layer phase), snapshot `variant` high bits = Hulkbuster on
- Test: `test/hero/ironman/HulkbusterLayerTest.java`

**Interfaces:**
- `HulkbusterLayer`: `boolean start()` (only from nano or mark, not on cooldown), `tick()`, `exit()`, `breakNow()`, `float durability()`, `int cooldown()`, `save/load` key `Hulkbuster` (ACTIVE persisted; DROPPING/ASSEMBLING resolve to ACTIVE **only if the final box fits at load**, else safe refusal; PARTIAL persisted as PARTIAL; EXITING/BREAKING → NONE + cooldown).
- Death → BREAKING (falls apart, cooldown), hero change → NONE, dimension → kept.

- [ ] Step 1: Tests: `startOnlyOverSuit`, `refusedOnCooldown`, `exitStartsCooldown`, `breakStartsCooldown`, `saveLoadInside`, `loadInTightSpaceRefusesSafely`, `deathBreaks`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): hulkbuster layer state`.

### Task 2: Damage layers

**Files:** Modify `IronManHero.java` (`absorbIncoming` order: shield → Hulkbuster → mark; then vanilla armor → Tony), `mark/MarkDamage.java` (generic `LayerDamage.apply`), Test `test/hero/ironman/LayerDamageTest.java`

- [ ] Step 1: Tests: `layersInOrder`, `realHurtOnHulkbusterKeepsHpAndMark`, `breakingHitDoesNotSpill`, `deferredPayoutChargedOnce`, `hulkbusterBreakLeavesSuit`.
- [ ] Step 2: FAIL → implement → PASS. Break: chunks fall as `SuitDebrisEntity` with Mark 48 pieces, player drops to the suit, short stun-free stumble. Commit `feat(ironman): hulkbuster takes damage first`.

### Task 3: Body scale seam

**Files:**
- Create: `core/hero/HeroSizeEvents.java` (Forge `EntityEvent.Size` subscriber, common: `event.setNewSize(event.getNewSize().scale(s))`, `event.setNewEyeHeight(eye × s)` when `bodyScale != 1`), `core/hero/BodyScale.java` (pure: `fits(Box)`, `findFree(origin, box, radius 2)`, `pushOut`)
- Modify: `core/hero/HeroDefinition.java` (`default float bodyScale(Player p) { return 1.0f; }`), `IronManHero.java` (`bodyScale` = 1.7 while ACTIVE/EXITING; `refreshDimensions()` on every change, also on load and pose change)
- Test: `test/hero/BodyScaleTest.java`

- [ ] Step 1: Tests: `defaultScaleOne`, `boxIsPoseTimesScale` (standing/crouching/swimming), `fitsChecksVolume`, `findsFreeSpotWithin2`, `refuseWithoutRoom`, `shrinkPushesOutOfBlocks`.
- [ ] Manual: block placed in the space during assembly → refusal at ACTIVE; player moves during assembly → check at the new position; load in a 2-block tunnel → safe refusal; crouch/stand under a low ceiling; exit/break in a tunnel never leaves the player inside blocks.
- [ ] Step 2: FAIL → implement → PASS; every existing hero keeps vanilla dimensions (build + manual). Commit `feat(hero): body scale seam`.

### Task 4: Mark 48 assets and renderer

**Files:**
- Create: `res/geo/ironman/hulkbuster/{body,jackhammer,fp_arm,repulsor_hand_left,repulsor_hand_right,repulsor_feet,fire*}.geo.json` (from Sind `tabula/hulkbuster/*.tbl`), `res/textures/entity/ironman/hulkbuster*.png` (+ `.b64`), `res/animations/ironman/hulkbuster/*.animation.json` (from Sind `.fsk`: idle, walk, punch L/R, jackhammer, ground smash, hop/boost, summon/landing, exit), `core/client/ironman/hulkbuster/HulkbusterRenderer.java` (replaces player rendering while ACTIVE: hide player model + layers, draw Mark 48 with `AnimRenderer` at the player transform, scale 1.7, glow layer), `core/client/ironman/hulkbuster/HulkbusterFirstPerson.java` (Sind `fp_arm`)
- Modify: `.agents/skills/animation-system/SKILL.md` (whole-body replacement model: allowed only through this renderer pattern)

- [ ] Step 1: Convert with Stage 4 tools; `tools/preview_geo.py` previews for review.
- [ ] Step 2: Renderer: walk/idle blend by speed, punches/jackhammer by `actionId`, thruster flames on hop, heavy step dust; 1st person big arm. Other players see the same.
- [ ] Step 3: Commit `feat(ironman): hulkbuster model and renderer`.

### Task 5: Drop, assemble over the suit, exit

**Files:**
- Create: `hb/AssembleTimeline.java` (pure), `core/client/ironman/hulkbuster/HulkbusterAssembleVisuals.java`
- Modify: `vero/VeronicaPod.java` (Hulkbuster parts drop from the pod), `IronManHero.java`
- Test: `test/hero/ironman/AssembleTimelineTest.java`

- [ ] Step 1: Assemble 60 t: legs dock → torso closes around the player → arms → helmet; player rooted (no move), damage goes to the current suit; free-space checks before the drop and at ACTIVE (Global Constraints). Control interrupts assembly (user decision, spec §16): locked parts stay → `PARTIAL` (parts drawn, no scale, no kit, layer durability not active, player slowed), undelivered parts fly back to the pod; "Костюм" in PARTIAL drops the parts back (no cooldown — the Hulkbuster was never active); choosing the Hulkbuster again in Veronica sends only the missing parts. Exit 30 t: back opens, player climbs out backwards, the empty Hulkbuster flies back to the pod / sky (no empty Hulkbuster entity stays).
- [ ] Step 2: Tests: `assembleOrder`, `rootedWhileAssembling`, `exitFliesAway`, `controlInterruptsAssemble` (locked stay → PARTIAL, rest return), `partialHasNoScaleOrKit`, `partialExitNoCooldown`, `blockPlacedDuringAssembleRefusesAtActive` → implement → PASS. Commit `feat(ironman): hulkbuster assemble and exit`.

### Task 6: Kit — punches, jackhammer, repulsors, grab & throw

**Files:**
- Create: `hb/HulkPunch.java`, `hb/Jackhammer.java`, `hb/SlowRepulsor.java`, `hb/GrabThrow.java`, `core/hero/HeroGrab.java` (shared grab helper extracted from the Viltrumite grab: attach/carry/release/throw; Viltrumite behaviour identical)
- Modify: `combat/ToolCycle.java` (Hulkbuster: `JACKHAMMER ↔ HULK_REPULSOR`; crosshairs `jackhammer`/`hulk_repulsor` from Stage 2 Task 11), `IronManAbilities` (page 1 slots 1–3 in Hulkbuster = grab/throw, jump slam, hop)
- Test: `test/hero/ironman/HulkbusterKitTest.java`

**Interfaces:**
- LMB: alternating huge punches (14 dmg, big knockback, 12 t cadence, small camera shake).
- Holds (jackhammer, slow repulsor charge, grab carry) use 1a `HeldInputs`.
- RMB jackhammer: hold → pin the crosshair target in front (≤ 3 blocks), hits every 4 t (4 dmg), max 60 t, then release with knockback; target is held, not grabbed (no carry).
- MMB → `HULK_REPULSOR` (slow repulsors) on RMB (charged only, 30 t charge, heavy blast, cost 10).
- Slot 1: grab (≤ 3 blocks) → carry → second press throws along look (velocity 2.2, impact damage via `HeroDebris`-style hit).
- Releases on every end: death, control, break, exit, logout of either side, target dimension change.

- [ ] Step 1: Tests: `punchCadence12`, `jackhammerHitsEvery4Max60`, `toolCycleJackhammerRepulsor`, `grabThenThrow`, `releaseOnEveryEnd`, `viltrumiteGrabUnchanged` (shared helper contract).
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): hulkbuster punches, jackhammer, grab`.

### Task 7: Kit — jump slam and thruster hop

**Files:** Create `hb/JumpSlam.java`, `hb/ThrusterHop.java`; Test `test/hero/ironman/HulkbusterMoveTest.java`

- Slot 2: jump 5 blocks forward-up, slam on landing: `HeroShockwave` big ring r=6 + `HeroDebris.erupt`, cooldown 200 t.
- Slot 3: short hop on thrusters (velocity look×1.4 + up 0.8 for 10 t, feet flames), cooldown 80 t, energy 8; no sustained flight (spec §14.3).
- Fall damage cancelled in the Hulkbuster; heavy landings always (Stage 1 heavy landing FX scaled ×1.7).

- [ ] Step 1: Tests: `slamOncePerJump`, `hopIsNotFlight` (`FlightState` stays NONE), `cooldowns` → implement → PASS. Commit `feat(ironman): hulkbuster slam and hop`.

### Task 8: HUD, sounds, lang, ship

- [ ] HUD: Hulkbuster durability bar in the left column (Stage 3), crosshair `jackhammer`/`repulsor_slow`, Sind `hud/mark48` style accents.
- [ ] Sounds (`tools/sfx/ironman_stage5.sh`): heavy steps, servo whine, punches, jackhammer loop, hop thrusters, assemble clanks, break, exit.
- [ ] Icons for grab/slam/hop + Hulkbuster card; lang (all locales).
- [ ] Lifecycle §16: death breaks, relog keeps, dimension keeps, hero change removes, control stops jackhammer/grab.
- [ ] `NoHeroBranchTest`, build, checker, `runServer`; in-game list = spec §21 row "Халкбастер…"; Viltrumite grab regression.
- [ ] Version bump, memory bank (`bodyScale`, `HeroGrab`), `hero-seam.md`, `animation-system` skill, `CREDITS.md`, `SESSION.md`. PR «Железный человек — этап 5: Халкбастер», body starts with «Для игрока».

---

## Review log

Plan reviewed against spec §4.4, §4.5, §14, §15.2, §16 and Stage 1–4 interfaces (reviewer pass, 2026-10-09). Fixed:
1. Hitbox growth could put the player into blocks → free-space check before the drop and push-out on shrink.
2. Damage order with Hulkbuster over a mark made explicit (shield → Hulkbuster → mark → Tony, no spill).
3. Spec gives no empty Hulkbuster → exit sends it back to the pod/sky (no world entity).
4. Grab would have depended on the Viltrumite kit gate → shared `HeroGrab` helper with a regression test.
5. The hop must not toggle mod flight (spec "полноценного полёта нет") → test `hopIsNotFlight`.

Fixed after PR review (2026-10-09):
6. Fixed after PR review: dimension mixin replaced by a Forge `EntityEvent.Size` handler + `refreshDimensions()` (Task 3).
7. Fixed after PR review: free space checked with the real final box (pose × 1.7) before the drop, at ACTIVE and on load; free-spot search or safe refusal without cooldown; manual cases listed (Global Constraints, Tasks 1, 3, 5).
8. Fixed after PR review: control interrupts assembly (user decision) → `PARTIAL`; `controlDuringAssembleFinishesInstantly` renamed to `controlInterruptsAssemble` (Task 5).
9. Fixed after PR review: Hulkbuster layer uses the 1a damage-layer seam; tests with a real `hurt` and a deferred payout (Task 2).
10. Fixed after PR review: slow repulsors are a separate `RightTool.HULK_REPULSOR`; holds use `HeldInputs`; cooldowns via `extraCooldowns` (Task 6, Global Constraints).
