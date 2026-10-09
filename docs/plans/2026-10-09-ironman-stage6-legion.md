# Iron Man — Stage 6 (Iron Legion) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Status: provisional.** Spec §19–§20 say Legion needs its own design. Task 0 is a hard gate: brainstorm + grill with the user, write `docs/design/<date>-ironman-legion-design.md`, get approval, then update this plan (numbers, scope) before any code. Everything below is the default direction taken from the spec hooks (page 2 slot 6 reserved, empty suits "можно связать с Legion" §12.6).

**Goal (default):** Page 2 slot 6 "Легион" calls up to 3 autonomous empty marks from the Veronica roster (only marks not on cooldown, with their saved durability); they fly in, fight with repulsors and their mark signature, follow simple orders (attack my target / guard me / return), and fly back to Veronica when dismissed, broken or far away. Damage to a Legion suit uses its roster durability; broken → cooldown like §12.7.

**Architecture (default, to be fixed in Task 0):** The existing contracts do not fit a plain entity, so an adaptation must be chosen before this plan is final:
- NPC actions: `entity/action/ViltrumiteAction` takes a `ViltrumiteFakePlayer` (a `ServerPlayer` subclass); `ApproachAction` starts legacy Viltrumite attacks (it checks `core.getPunchTicks()/getChopTicks()`), `MeleeCombatAction` runs Viltrumite strikes. Copying the Viltrumite combat into Legion is not allowed.
- Rendering and abilities: Stage 1b `PlayerGeoLayer` takes an `AbstractClientPlayer`; Stage 4 `MarkSignature` takes a `ServerPlayer` + `IronManState`.
Options for Task 0: **(A)** a shared, hero-neutral NPC action context (`entity/action/NpcContext` interface: position, look, target, `useAbility(id)`, `moveTo`), implemented by `ViltrumiteFakePlayer` (Viltrumite actions unchanged) and by a new `LegionSuitEntity`; plus a shared ability/render actor (`HeroActor`: level, eye position, look, hit routing; `GeoLayerTarget`: model-part poses) that both `ServerPlayer`/`AbstractClientPlayer` and the Legion entity implement, so `MarkSignature` and `PlayerGeoLayer` take the actor, not the player class. **(B)** Legion suits as fake players (`ViltrumiteFakePlayer`-style `ServerPlayer` with the Iron Man hero) that reuse player contracts directly, with new hero-neutral actions instead of the Viltrumite ones. (A) is the default; Task 0 decides with the user and updates the Files below. Ownership uses Stage 4 `MarkRoster`/`MarkLocation` with an appended `LEGION` value; flight steering reuses Stage 1a `FlightMotion`.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3.

**Spec:** `docs/design/2026-10-09-ironman-design.md` §6.2 (slot reserve), §12.6, §19, §20; Legion design doc from Task 0. Previous plans: Stages 1–5.

Paths as in Stage 1; `legion/` = `core/hero/ironman/legion/`.

## Global Constraints

- All Stage 1–5 Global Constraints apply.
- No code before Task 0 is approved by the user.
- A Legion suit is never the same mark the player wears or the current empty suit; a mark can exist in the world only once (worn, empty, or Legion).
- Server performance: max 3 Legion suits per player, AI ticks every 2 t, targets refreshed every 10 t, path = direct flight with obstacle avoidance raycasts (no ground pathfinding).
- Legion suits never hurt the owner, the owner's tamed animals, or each other; friendly fire to other players follows the server PvP rule.
- Lifecycle (§16 style): owner death, logout, dimension change, hero change → all Legion suits fly away, durability back to the roster.

## Review Focus

1. A mark is never duplicated between worn / empty / delivery / Legion: Stage 4 `MarkLocation` (Stage 4 test `markInWorldOnce`) gets `LEGION`; Task 1 test `legionUsesOwnershipTransitions`.
2. Owner leaves (logout/dimension/death) → all suits gone the next tick, durability stored (Task 5 tests).
3. AI never targets the owner or allies, including after the owner hits them by accident (Task 3 test `neverTargetsOwnerOrAllies`).
4. Server cost: 3 owners × 3 suits in combat stay under 1 ms/tick on `runServer` profiling (Task 6).
5. No hero branch: Legion code lives only in `hero/ironman/` and `client/ironman/`.

---

### Task 0: Legion design (gate)

- [ ] Prerequisite: choose the adaptation (A or B above) for the NPC action context and for the render/ability actor; write it into the design doc.
- [ ] Run brainstorming + grill-me with the user: count of suits, which marks, orders UI (radial menu vs key taps), autonomy level, PvP rules, costs (energy or none), cooldown, visuals of the call (from Veronica or from the sky), interaction with empty suits and Hulkbuster.
- [ ] Write `docs/design/<date>-ironman-legion-design.md` (Russian, same structure as the Iron Man spec), user approval.
- [ ] Update this plan: numbers, tasks, Review Focus. Commit `docs(plan): legion plan after design`.

### Task 1: Legion roster and call

**Files:** Create `legion/LegionRoster.java` (pure: pick up to 3 marks with `MarkLocation.STORED` and no cooldown), `legion/LegionCommand.java` (`FOLLOW, ATTACK, GUARD, RETURN`); Modify `mark/MarkLocation.java` (append `LEGION`), `IronManAbilities` (`ironman:legion` page 2 slot 6, cooldown `extraCooldowns[9]`), `HeroAction` append `LEGION`, `IronManHero.java`; Test `test/hero/ironman/LegionRosterTest.java`.

- [ ] Tests: `picksUpToThree`, `skipsCooldownMarks`, `skipsWornEmptyAndDelivery`, `legionUsesOwnershipTransitions`, `noCallWithoutAvailableMarks` → implement → commit `feat(ironman): legion roster and call`.

### Task 2: Legion suit entity and flight

**Files:** Create `core/entity/LegionSuitEntity.java` (mark id, durability, owner UUID, `FlightMotion`-driven movement, obstacle raycasts), `core/client/ironman/legion/LegionSuitRenderer.java` (mark skin + parts + flames); Test `test/hero/ironman/LegionFlightTest.java` (pure steering: `avoidsWallAhead`, `keepsFormationOffset`).

- [ ] Arrival: fly in from the Veronica pod if landed, else from the sky 64 blocks up; formation offsets around the owner. Commit `feat(ironman): legion suit entity`.

### Task 3: Legion AI actions

**Files:** Create `legion/ai/RepulsorVolleyAction.java`, `legion/ai/SignatureAction.java` (uses the Stage 4 `MarkSignature` through the Task 0 actor adaptation), `legion/ai/GuardOwnerAction.java`, `legion/ai/LegionTargeting.java` (pure); no reuse of the Viltrumite `ApproachAction`/`MeleeCombatAction` combat; generic movement/idle parts are reused only through the Task 0 action context; Test `test/hero/ironman/LegionTargetingTest.java`.

- [ ] Tests: `attacksOwnerTarget`, `guardsAgainstOwnerAttacker`, `neverTargetsOwnerOrAllies`, `respectsPvpRule` → implement → commit `feat(ironman): legion ai`.

### Task 4: Orders input and HUD

**Files:** Create `core/client/ironman/legion/LegionOrdersOverlay.java`; Modify `IronManHero.java` (repeat press cycles `FOLLOW → ATTACK → GUARD → RETURN`; attack target = crosshair entity or last scanned target), `IronManHud` (small Legion icons with durability).

- [ ] Commit `feat(ironman): legion orders and hud`.

### Task 5: Damage, break, lifecycle

**Files:** Modify `LegionSuitEntity.java` (damage → durability via `LayerDamage`; break → debris + `MarkRoster.breakMark`), `IronManHero.java` (`cleanup`, `onDimensionChange`); Test `test/hero/ironman/LegionLifecycleTest.java`.

- [ ] Tests: `breakStartsMarkCooldown`, `ownerDeathRecallsAll`, `ownerLogoutRecallsAll`, `durabilityStoredOnRecall` → implement → commit `feat(ironman): legion damage and lifecycle`.

### Task 6: Performance, sounds, lang, ship

- [ ] Profile 9 suits in combat on `runServer` (spark-less: `System.nanoTime` sampler behind a debug flag), keep < 1 ms/tick.
- [ ] Sounds (arrival, order chirps, JARVIS lines for Legion), icon, lang (all locales).
- [ ] `NoHeroBranchTest`, build, checker, `runServer`; in-game checklist from the Legion design doc.
- [ ] Version bump, memory bank, `SESSION.md`. PR «Железный человек — этап 6: Железный легион», body starts with «Для игрока».

---

## Review log

Plan reviewed against spec §6.2, §12.6, §19, §20 and Stage 4 roster interfaces (reviewer pass, 2026-10-09). Fixed:
1. Spec explicitly defers Legion design → Task 0 hard gate, plan marked provisional.
2. Mark duplication across worn / empty / Legion → one-instance rule + test.
3. Server cost of AI suits was unbounded → caps, tick rates, profiling task.

Fixed after PR review (2026-10-09):
4. Fixed after PR review: `ViltrumiteAction` needs a `ViltrumiteFakePlayer`, `ApproachAction`/`MeleeCombatAction` run Viltrumite combat, `PlayerGeoLayer`/`MarkSignature` take player classes → Task 0 prerequisite to choose an adaptation (shared NPC action context + shared render/ability actor, or fake-player Legion); Architecture rewritten; no copied Viltrumite combat.
5. Fixed after PR review: `markInWorldOnce` moved to Stage 4; Legion appends `LEGION` to `MarkLocation`.
