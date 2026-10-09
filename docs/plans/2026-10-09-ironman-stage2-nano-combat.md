# Iron Man — Stage 2 (Nano Combat Kit) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The nano Mark 50 fights: repulsors on RMB (click / charge / volley / brake / lift / ground shockwave), Unibeam with overheat and the 3-overheat overdraft that ends in a core explosion, micro-missiles with target marks, nano-arsenal (blade ↔ hammer), shield on F with perfect block, F hand-swap blocked in any suit, MMB cycles the RMB tool, crosshair per tool, nano damage/repair visuals.

**Architecture:** Builds on Stage 1 (`IronManState`, `Energy`, `Suit`, mouse seam `HeroDefinition.mouseAction`, `PlayerGeoLayer`, `PanelStyles`). Every weapon is a pure rules/timeline class in `hero/ironman/combat/` with JUnit tests, plus a server driver and a client renderer in `client/ironman/`. New generic seams: `HeroDefinition.guardAction(Player)` (hero owns the swap-hands key) and `HeroDefinition.blocksHandSwap(Player)`. Shared VFX that Homelander already has (scorch marks, beam) move to `client/render/vfx/` without behaviour change.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3.

**Spec:** `docs/design/2026-10-09-ironman-design.md` §4.2, §4.6, §5, §6, §8, §9, §15.2 (crosshair), §16, §17. Previous plan: `docs/plans/2026-10-09-ironman-stage1-foundation.md`.

Paths: `core/`, `res/`, `test/` as in Stage 1; `combat/` = `core/hero/ironman/combat/`.

## Global Constraints

- Everything from Stage 1 Global Constraints applies (server authority, client packages, no hero branches, no renames, append-only enums, animation system, `IronManRules`, all locales, `.b64` binaries, own sounds via script).
- Energy costs exactly spec §5.2 (constants already in `IronManRules` since Stage 1). Energy `weaponsLocked()` → no repulsor, Unibeam, missiles, nano weapon forming, shield (spec §5.3).
- Overheat counter (spec §9.1) resets **only** on core explosion or death; suit change, relog, dimension change keep it. Persist in NBT key `CoreOverheats`.
- Explosions and block breaking respect `mobGriefing` (`HeroDestruction.canDestroy`, explosion interaction `MOB`).
- Control (spec §16): no ability starts while controlled; Unibeam channel, missile aim, charging, blade dash stop; an overdraft that already started is **not** cancelled.
- Each ability: own animation (1st + 3rd person), VFX and sound — no vanilla stand-ins (spec §21).
- Every entity hit goes through `HeroDamage.route` (so marks/other heroes' damage rules work).

## Review Focus

1. Overdraft: release before the explosion → beam stops but the explosion still happens weaker; death mid-overdraft → no explosion after respawn, counter reset (Task 5 tests `releaseStillExplodesWeaker`, `deathCancelsPendingExplosion`).
2. Player survives the core explosion at ~2 hearts even with low HP and no armor; other damage in the same tick can still kill (Task 5 test `explosionLeavesTwoHearts`, `otherDamageStillLethal`).
3. F: in any suit the vanilla swap never happens (client key consumed **and** server `LivingSwapItemsEvent.Hands` cancelled); out of suit F swaps normally (Task 9 tests).
4. Perfect block window ≤ 5 ticks measured on the server from the guard-down packet; holding F forever never counts as perfect (Task 9 test `perfectOnlyFirst5Ticks`).
5. MMB cycle and RMB tool are server state; a forged RMB for the nano weapon without it formed is ignored (Task 2 test `rmbUsesServerTool`).

---

### Task 1: Overheat counter and combat rules

**Files:**
- Create: `combat/CoreOverheat.java`, `core/hero/ironman/IronManFlags.java` (single bit layout of `heroFlags` for all stages); numbers go to `IronManRules`
- Modify: `IronManState.java` (fields, save/load), `IronManHero.java` (`cleanup(DEATH)` reset)
- Test: `test/hero/ironman/CoreOverheatTest.java`

**Interfaces:**
- `CoreOverheat`: `int count()` 0..3, `boolean nextIsOverdraft()` (count == 2 → the beam starting now is the third overheat, spec §9.2 "на этом же луче"), `void add()` (beam ended in overheat), `boolean warn()` (count == 2 → JARVIS warning text now, voice in Stage 3), `void resetByExplosion()`, `void resetByDeath()`, `save/load` key `CoreOverheats`.
- Rules (ticks): `REPULSOR_CHARGE_MAX=20`, `REPULSOR_COOLDOWN=6`, `UNIBEAM_CHARGE=20`, `UNIBEAM_MAX=60`, `UNIBEAM_OVERHEAT_LOCK=40`, `OVERDRAFT_SPUTTER_AT=40`, `OVERDRAFT_EXPLODE_AFTER_SPUTTER=30`, `OVERDRAFT_DAMAGE_MULT=1.5`, `CORE_EXPLOSION_POWER=12`, `CORE_EXPLOSION_POWER_RELEASED=8`, `CORE_SURVIVE_HP=4`, `NANO_LOST_TICKS=600`, `MISSILE_FLAPS=10`, `MISSILE_MARKS=4`, `BLADE_SUNDER_TICKS=100`, `PERFECT_BLOCK_TICKS=5`, damage: `REPULSOR_SHOT=4`, `REPULSOR_CHARGED=4..10` (by charge), `REPULSOR_VOLLEY=14`, `UNIBEAM_DPS_NEAR=1.5/t` (≤ 8 blocks) → `0.5/t` at 48, `MISSILE_HIT=6`, `BLADE=8`, `HAMMER=10`, `HAMMER_SLAM=6`, `NANO_PUNCH=6` — tuned so Iron Man head-on is weaker than Homelander (spec §18), adjusted after the in-game check.
- `IronManFlags`: bits 0 worn, 1 deploying, 2 retracting, 3 glide, 4 heavy-landing pose (Stage 1); 5–6 overheat count, 7 overheat lock, 8–10 RMB tool, 11 shield up, 12–14 damaged zones, 15–16 Unibeam phase (charge/beam/overdraft), 17 missile flaps, 18 overdraft sputter; 19–31 reserved for Stages 3–5 (helmet 19, scan 20, mark camo 21 …). Per-ability progress uses `actionId/actionElapsed/actionLength`.

- [ ] Step 1: Tests: `flagsDoNotOverlap`, `thirdBeamIsOverdraft`, `secondWarns`, `suitChangeKeepsCount` (save/load + `Suit` transitions), `deathResets`, `explosionResets`, `neverAbove3`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): core overheat counter`.

### Task 2: RMB tool, MMB cycle, mouse claims

**Files:**
- Create: `combat/RightTool.java` (enum `REPULSOR, NANO_BLADE, NANO_HAMMER, SIGNATURE, JACKHAMMER` — Stage 4/5 use the last two), `combat/ToolCycle.java`
- Modify: `IronManHero.java` (`mouseAction`, `handleInput`), `HeroAction` append `SECONDARY_USE, TOOL_CYCLE, UNIBEAM, MISSILES, NANO_ARSENAL, GUARD`
- Test: `test/hero/ironman/ToolCycleTest.java`

**Interfaces:**
- Suit worn → claims LMB (`PRIMARY_ATTACK`), RMB (`SECONDARY_USE`, hold), MMB (`TOOL_CYCLE`). Without suit → none (vanilla).
- `ToolCycle.next(RightTool cur, boolean weaponFormed, RightTool signature)`: nano: `REPULSOR ↔ (formed weapon)`; nothing formed → stays `REPULSOR`. Stage 4 passes the mark signature, Stage 5 jackhammer.
- Server keeps `rightTool`; snapshot `heroFlags` bits 8–10 = tool ordinal (client crosshair + poses).
- LMB in suit on ground: nano punch (`NANO_MELEE_FACTOR`) or weapon strike when formed (Task 7); in flight: fly-by (Stage 1).

- [ ] Step 1: Tests: `cycleRepulsorToFormedWeapon`, `cycleWithoutWeaponStays`, `rmbUsesServerTool`, `noClaimsWithoutSuit`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): rmb tool and mmb cycle`.

### Task 3: Repulsors

**Files:**
- Create: `combat/Repulsor.java` (pure: charge level, hand alternation, cost, recoil), `core/entity/RepulsorBlastEntity.java` (fast projectile, registered in `ViltrumiteEntities`), `core/client/ironman/RepulsorRenderer.java`, `core/client/ironman/RepulsorVfx.java`
- Modify: `IronManHero.java`, `IronManPoser.java` (arm raise, palm forward, alternate hands)
- Test: `test/hero/ironman/RepulsorTest.java`

**Interfaces:**
- Press RMB → start charge; release before 4 t → **shot** (cost 2, alternate hand); release ≥ 4 t → **charged** (cost 6, damage + knockback by charge 0..1); charge reaches 20 t → **volley** from both hands (cost 10) on release. Energy checked on release (`spend`), not enough → fizzle FX.
- Movement (spec §8.1): in flight, shot direction · velocity < −0.6 → brake: velocity ×(1 − 0.5·power); in HOVER, look pitch ≥ 60° down → lift +0.6·power vy; on ground, full charge with pitch ≥ 70° down → shockwave `HeroShockwave.land`-style ring (radius 4, knockback, no block break).
- Visual: palm glow grows while charging (light part on hand bone via `PlayerGeoLayer`), bright pixel bolt, hit flash + small scorch.

- [ ] Step 1: Tests: `tapIsShotCost2`, `chargeCost6`, `fullChargeVolleyCost10`, `handsAlternate`, `backShotBrakes`, `downShotInHoverLifts`, `groundFullChargeShockwave`, `lockedEnergyFizzles`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): repulsors`.

### Task 4: Unibeam

**Files:**
- Create: `combat/UnibeamTimeline.java` (pure state machine `IDLE, CHARGE, BEAM, OVERHEAT`), `combat/UnibeamServer.java`, `core/client/ironman/UnibeamRenderer.java`, `core/client/render/vfx/ScorchMarks.java` (moved from `client/homelander/ScorchMarks.java`, Homelander imports updated, behaviour identical), `core/client/render/vfx/FlashOverlay.java`, `core/network/packet/FlashS2CPacket.java`
- Modify: `IronManHero.java`, `IronManPoser.java` (chest out, arms back), `IronManAbilities` (`ironman:unibeam` page 1 slot 1)
- Test: `test/hero/ironman/UnibeamTimelineTest.java`, keep `test/client/homelander/*` green after the move

**Interfaces:**
- Hold slot → `CHARGE` 20 t (chest glow + hum; costs 35 at beam start; not enough → refuse), `BEAM` while held, max 60 t; beam aim follows look with max turn `UNIBEAM_TURN_DEG=3°/t` (slow). Release or 60 t → `OVERHEAT` 40 t (weapons locked, smoke) → `CoreOverheat.add()`.
- Hit: thick ray (radius 0.6) up to 48 blocks, damage every 2 t falling with distance; push target along the beam (velocity add, not hold); glass and leaves broken via `HeroDestruction` (tag `minecraft:leaves`, `forge:glass`, `forge:glass_panes`); other blocks → scorch marks (shared `ScorchMarks`).
- Blinding (spec §8.2): players in the beam and players within 12 blocks looking into it (angle < 25°) → `FlashS2CPacket(strength, ticks)`; PvP strength capped short (20 t); owner light flash. Mobs in the beam → Blindness 40 t + lose target.
- Control interrupts CHARGE/BEAM (counts as normal end → overheat if beam started).

- [ ] Step 1: Tests: `chargeTakes20`, `beamMax60`, `releaseOverheats40`, `overheatAddsCounter`, `turnRateLimited`, `damageFallsWithDistance`, `onlyGlassAndLeavesBreak` (pure tag predicate), `controlEndsChannel`.
- [ ] Step 2: FAIL → implement → PASS. Move ScorchMarks; Homelander tests green.
- [ ] Step 3: Commit `feat(ironman): unibeam with overheat`.

### Task 5: Overdraft and core explosion

**Files:**
- Create: `combat/Overdraft.java` (pure timeline), `core/client/ironman/OverdraftVfx.java`
- Modify: `UnibeamServer.java`, `IronManHero.java` (`onHurt` survival clamp, `tick`), `Suit.java` (append `nanoLockTicks`; Stage 4 adds mark breaking)
- Test: `test/hero/ironman/OverdraftTest.java`

**Interfaces:**
- Beam start with `CoreOverheat.nextIsOverdraft()` → the beam enters overdraft (the 60 t beam cap does not apply): thicker/brighter, damage ×1.5, stronger flash. At +40 t → sputter (beam pulses/breaks, sparks, HUD warning blink, sound break); at +30 t after sputter → explosion. Release before → beam off, explosion still at the same moment with `CORE_EXPLOSION_POWER_RELEASED`.
- Explosion: `level.explode(player, …, power, ExplosionInteraction.MOB)` centred on the player (mobGriefing respected), extra knockback to everyone around, player launched up/back. Player damage from **own core explosion** in that tick clamped so HP ≥ `CORE_SURVIVE_HP` (flag `explodingTick`; other sources unaffected).
- Nano: suit → NONE instantly with nanite scatter VFX, `nanoLockTicks = 600` (Костюм key refused with HUD text). `CoreOverheat.resetByExplosion()`. Stage 4: if a mark is worn → mark breaks, auto nano, no lock.
- Death mid-overdraft → pending explosion cancelled, counter reset (death).

- [ ] Step 1: Tests: `thirdOverheatStartsOverdraft`, `sputterAt40`, `explodesAt70`, `releaseStillExplodesWeaker`, `explosionLeavesTwoHearts`, `otherDamageStillLethal`, `nanoLocked30s`, `deathCancelsPendingExplosion`, `controlDoesNotCancelOverdraft`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): unibeam overdraft and core explosion`.

### Task 6: Micro-missiles

**Files:**
- Create: `combat/MissileLock.java` (pure: marks from aim sweep), `core/entity/MicroMissileEntity.java` (homing, implements new shared `core/entity/Homing` interface `retarget(Entity)` for Stage 3 countermeasures), `core/client/ironman/MissileRenderer.java`, `res/geo/ironman/missile.geo.json`
- Modify: `IronManHero.java`, `IronManPartsProvider.java` (shoulder flaps = `shoulder_rockets` bone of Satsu `full_body` geo, opened by snapshot progress), `IronManAbilities` (`ironman:missiles` page 1 slot 2), `HeroDefinition`-free hook `IronManHero.canMarkTargets(state)` (true now; Stage 3 → helmet closed)
- Test: `test/hero/ironman/MissileLockTest.java`

**Interfaces:**
- Hold slot → flaps open 10 t; while held, entities within 64 blocks and 4° of the crosshair are marked (unique, max `MISSILE_MARKS`, LOS required); marks shown as brackets (client from owner snapshot ids). Release → cost 15, one missile per mark (fan launch), or 4 straight along the aim if no marks. Missiles: own model, smoke trail pixel VFX, explosion damage via `HeroDamage.route`, explosion **without** block damage (`ExplosionInteraction.NONE`), own sound.

- [ ] Step 1: Tests: `marksUpToFour`, `noDuplicateMarks`, `needsLineOfSight`, `noMarksFiresStraight`, `flapsTake10`, `cost15`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): micro-missiles with target marks`.

### Task 7: Nano-arsenal (blade ↔ hammer)

**Files:**
- Create: `combat/NanoArsenal.java` (pure: form/switch/dissolve), `combat/BladeDash.java`, `combat/HammerLaunch.java` (tracks launched targets breaking soft blocks), `core/effect/SunderEffect.java` (armor −30%, disables shield blocking 5 s), `core/client/ironman/NanoWeaponParts.java`, `res/geo/ironman/{nano_blade,nano_hammer}.geo.json` (Satsu `nanokatar`, `nano_mallet`)
- Modify: `IronManHero.java`, `IronManPoser.java`, `IronManAbilities` (`ironman:nano_arsenal` page 1 slot 3)
- Test: `test/hero/ironman/NanoArsenalTest.java`

**Interfaces:**
- Slot press: none → form blade (cost 8, wrist nanite wave ~8 t); blade ↔ hammer on further presses (cost 8 each form); MMB → back to repulsor (weapon dissolves). Strikes free.
- Blade: LMB slash (arc 100°, reach 3.5), hit → `SunderEffect` 100 t; RMB → dash to the crosshair target ≤ 8 blocks (`BladeDash` path, stops on block).
- Hammer: LMB heavy hit (big knockback); LMB at a ground block in reach → ground slam, enemies in r=4 launched up; hitting an airborne target → slam it down (vy −2.0); RMB hold to charge (≤ 20 t) → launch target along look, `HammerLaunch` breaks soft blocks (hardness ≤ 1.5, `HeroDestruction.canDestroy`) on its path for 20 t.
- First person mandatory: weapon part on right arm bone in both views.

- [ ] Step 1: Tests: `formCosts8`, `switchBladeHammer`, `mmbDissolves`, `bladeSunders100`, `dashStopsAtWall` (pure grid), `airborneHitSlamsDown`, `launchBreaksOnlySoftBlocks`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): nano blade and hammer`.

### Task 8: Hand-swap block and guard seam

**Files:**
- Create: `core/client/hero/HeroGuardInput.java` (consumes `options.keySwapOffhand` clicks when `guardAction != null`, sends hold edges), `core/network/packet/HeroGuardC2SPacket.java`
- Modify: `core/hero/HeroDefinition.java` (`default boolean blocksHandSwap(Player p) { return false; }`, `default HeroAction guardAction(Player p) { return null; }`), `core/hero/HeroEvents.java` (`LivingSwapItemsEvent.Hands` → cancel if `blocksHandSwap`), `IronManHero.java` (both true/`GUARD` in any suit)
- Test: `test/hero/HeroGuardSeamTest.java`

- [ ] Step 1: Tests: `otherHeroesSwapNormally`, `ironManWithoutSuitSwaps`, `suitBlocksSwap`, `guardActionOnlyInSuit`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(hero): guard key seam and hand-swap block`.

### Task 9: Shield and perfect block

**Files:**
- Create: `combat/Shield.java` (pure: raised tick, front cone, perfect window, cost), `core/client/ironman/ShieldParts.java`, `res/geo/ironman/nano_shield.geo.json` (+ first-person variant)
- Modify: `IronManHero.java` (`onHurt`), projectile reflection helper in `combat/Reflect.java`
- Test: `test/hero/ironman/ShieldTest.java`

**Interfaces:**
- Guard down → shield raised instantly (plates from the left forearm, `nano_shield`), up → collapses. Front cone 120°: damage fully absorbed, cost 4 per hit; not enough energy / locked → no shield. Explosions from the front absorbed too.
- Perfect: hit arrives ≤ 5 t after raise → projectile reflected (velocity reversed toward its owner, owner set to us) / melee attacker knocked back strongly; flash + chime.
- While raised: no RMB fire; flight allowed.

- [ ] Step 1: Tests: `frontHitAbsorbed`, `backHitPasses`, `costs4PerHit`, `noShieldWhenLocked`, `perfectOnlyFirst5Ticks`, `reflectReversesVelocity`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): nano shield with perfect block`.

### Task 10: Nano damage and repair visuals

**Files:**
- Create: `core/client/ironman/NanoDamageVisuals.java`
- Modify: `IronManHero.java` (`onHurt`: hit ≥ 6 → snapshot bits 12–14 = damaged zone mask/shoulder/chest), `IronManSkinLayer.java`

- [ ] Step 1: Heavy hit → zone shows broken-skin pixels (Tony's skin shows through), then nanite crawl repairs it over 60 t. Visual only (spec §4.2).
- [ ] Step 2: Commit `feat(ironman): nano damage and repair visuals`.

### Task 11: Crosshair, HUD pips, panel icons

**Files:**
- Create: `core/client/ironman/IronManCrosshair.java` (Forge `RenderGuiOverlayEvent.Pre` for `CROSSHAIR` when suit worn), `res/textures/gui/ironman/crosshair/{repulsor,blade,hammer}.png`, `res/textures/gui/ability/ironman/{unibeam,missiles,nano_blade,nano_hammer}.png` (+ `.b64`)
- Modify: `IronManHud.java` (3 overheat pips from snapshot bits 5–6 count, overheat lock bar, overdraft warning blink), `IronManAbilities` (slot grey-out by `weaponsLocked`, overheat, `nanoLockTicks`)

- [ ] Step 1: Crosshair per tool (spec §15.2 center; Stage 3 moves it into the helmet HUD), repulsor charge ring.
- [ ] Step 2: Icons in the panel art style; render 8× preview for review. Commit `feat(ironman): crosshair, overheat pips, combat icons`.

### Task 12: Sounds, lang, ship

- [ ] Sounds via `tools/sfx/ironman_stage2.sh`: repulsor shot/charge/volley, unibeam charge/loop/overheat/overdraft/core explosion, nanite form/dissolve/repair, blade slash, hammer hit/slam, shield open/hit/perfect, missile flaps/launch/explode.
- [ ] Lang (all locales): ability names/descs, HUD texts (`hud.viltrumitecore.ironman.overheat_warning`, `nano_lost`, `weapons_offline`).
- [ ] `NoHeroBranchTest`, `./gradlew build`, checker, `runServer` "Done"; in-game check list = spec §21 rows 6–12; Homelander lasers/scorch unchanged after the move.
- [ ] Version bump, memory bank, `hero-seam.md` (`guardAction`, `blocksHandSwap`), `SESSION.md`. PR «Железный человек — этап 2: боевой набор нано», body starts with «Для игрока».

---

## Review log

Plan reviewed against spec §4.2, §4.6, §5, §6, §8, §9, §15.2, §16, §21 and Stage 1 interfaces (reviewer pass, 2026-10-09). Fixed:
1. §9.2 "third overheat starts overdraft on the same beam": `add()` returning true at 3 would fire after the beam ended → `nextIsOverdraft()` checked at beam start.
2. Overdraft lasts 70 t but the beam cap is 60 t → cap explicitly disabled in overdraft.
3. `heroFlags` bits were assigned ad hoc in several tasks → one `IronManFlags` layout with a non-overlap test, reserved bits for Stages 3–5.
4. Damage numbers were "tuned later" only → concrete starting values in `IronManRules`.
5. §8.2 blinding of mobs under the beam was missing → Blindness + target loss.
