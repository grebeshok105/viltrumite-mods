# Iron Man — Stage 2 (Nano Combat Kit) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The nano Mark 50 fights: repulsors on RMB (click / charge / volley / brake / lift / ground shockwave), Unibeam with overheat and the 3-overheat overdraft that ends in a core explosion, micro-missiles with target marks, nano-arsenal (blade ↔ hammer), shield on F with perfect block, F hand-swap blocked in any suit, MMB cycles the RMB tool, crosshair per tool, nano damage/repair visuals.

**Architecture:** Builds on Stage 1a/1b (`IronManState`, `Energy`, `Suit`, `IronManFlags`, damage-layer seam `absorbIncoming`/`clampFinalDamage`/`modifyOutgoingDamage`, mouse + `HeldInputs` seam, `PlayerGeoLayer`, `PanelStyles`). Every weapon is a pure rules/timeline class in `hero/ironman/combat/` with JUnit tests, plus a server driver and a client renderer in `client/ironman/`. New generic seams: `HeroDefinition.guardAction(Player)` (hero owns the swap-hands key) and `HeroDefinition.blocksHandSwap(Player)`. Shared VFX that Homelander already has (scorch buffer + scorch drawing) move to `client/render/vfx/` without behaviour change. Owner-only data gets typed sections (`HeroOwnerSnapshot`), page-2/extra cooldowns get a generic snapshot section.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3.

**Spec:** `docs/design/2026-10-09-ironman-design.md` §4.2, §4.6, §5, §6, §8, §9, §15.2 (crosshair), §16, §17. Previous plans: `docs/plans/2026-10-09-ironman-stage1-foundation.md` (1a), `docs/plans/2026-10-09-ironman-stage1b-visuals.md` (1b).

Paths: `core/`, `res/`, `test/` as in Stage 1; `combat/` = `core/hero/ironman/combat/`.

## Global Constraints

- Everything from Stage 1 Global Constraints applies (server authority, client packages, no hero branches, no renames, append-only enums, animation system, `IronManRules`, all locales, `.b64` binaries, own sounds via script).
- Energy costs exactly spec §5.2 (constants already in `IronManRules` since Stage 1). Energy `weaponsLocked()` → no repulsor, Unibeam, missiles, nano weapon forming, shield (spec §5.3).
- Overheat counter (spec §9.1) resets **only** on core explosion or death; suit change, relog, dimension change keep it. Persist in NBT key `CoreOverheats`.
- Explosions and block breaking respect `mobGriefing` (`HeroDestruction.canDestroy`, explosion interaction `MOB`).
- Control (spec §16): no ability starts while controlled; Unibeam channel, missile aim, charging, blade dash stop; an overdraft that already started is **not** cancelled.
- Each ability: own animation (1st + 3rd person), VFX and sound — no vanilla stand-ins (spec §21).
- Every entity hit goes through `HeroDamage.route` (so marks/other heroes' damage rules work). Incoming damage to Iron Man is changed **only** through the 1a damage-layer hooks; `onHurt` stays notify-only.
- Held actions (RMB charge, Unibeam, missiles, shield, hammer charge) use 1a `HeldInputs`: start checked on press only, release always routed to the started action, forced release on suit off / control / screen open / hero change.

## Review Focus

1. Overdraft: release before the explosion → beam stops but the explosion still happens weaker; death mid-overdraft → no explosion after respawn, counter reset (Task 5 tests `releaseStillExplodesWeaker`, `deathCancelsPendingExplosion`).
2. Player survives the core explosion at ~2 hearts even with low HP and no armor, also through a deferred payout; other damage in the same tick can still kill (Task 5 tests `explosionLeavesTwoHearts`, `deferredPayoutOfOwnExplosionClamped`, `otherDamageStillLethal`).
3. F: in any suit the vanilla swap never happens (client key consumed **and** server `LivingSwapItemsEvent.Hands` cancelled); out of suit F swaps normally (Task 9 tests).
4. Perfect block window ≤ 5 ticks measured on the server from the guard-down packet; holding F forever never counts as perfect (Task 9 test `perfectOnlyFirst5Ticks`).
5. MMB cycle and RMB tool are server state; a forged RMB for the nano weapon without it formed is ignored (Task 2 test `rmbUsesServerTool`).

---

### Task 1: Overheat counter and combat rules

**Files:**
- Create: `combat/CoreOverheat.java`; numbers go to `IronManRules`; flag bits already defined in 1a `IronManFlags`
- Modify: `core/hero/HeroPublicSnapshot.java` (append generic `int[] extraCooldowns`, ≤ 16 entries, encoded only when non-empty; old encodings decode as empty), the panel grey-out reader in `ability/ViltrumiteAbilities` (a slot may name an extra cooldown index), `test/hero/HeroPublicSnapshotTest.java`
- Modify: `IronManState.java` (fields, save/load), `IronManHero.java` (`cleanup(DEATH)` reset)
- Test: `test/hero/ironman/CoreOverheatTest.java`, `test/hero/HeroPublicSnapshotTest.java`

**Interfaces:**
- `CoreOverheat`: `int count()` 0..3, `boolean nextIsOverdraft()` (count == 2 → the beam starting now is the third overheat, spec §9.2 "на этом же луче"), `void add()` (beam ended in overheat), `boolean warn()` (count == 2 → JARVIS warning text now, voice in Stage 3), `void resetByExplosion()`, `void resetByDeath()`, `save/load` key `CoreOverheats`.
- Rules (ticks): `REPULSOR_CHARGE_MAX=20`, `REPULSOR_COOLDOWN=6`, `UNIBEAM_CHARGE=20`, `UNIBEAM_MAX=60`, `UNIBEAM_OVERHEAT_LOCK=40`, `OVERDRAFT_SPUTTER_AT=40`, `OVERDRAFT_EXPLODE_AFTER_SPUTTER=30`, `OVERDRAFT_DAMAGE_MULT=1.5`, `CORE_EXPLOSION_POWER=12`, `CORE_EXPLOSION_POWER_RELEASED=8`, `CORE_SURVIVE_HP=4`, `NANO_LOST_TICKS=600`, `MISSILE_FLAPS=10`, `MISSILE_MARKS=4`, `BLADE_SUNDER_TICKS=100`, `PERFECT_BLOCK_TICKS=5`, damage — every value is **per hit** with an explicit interval; no "/t" mixing:

  | Constant | Value | Rate | Homelander reference (`HomelanderRules`) |
  |---|---|---|---|
  | `REPULSOR_SHOT` | 3.0 every `REPULSOR_COOLDOWN=8` t | 0.375/t | laser 1.5 every 4 t = 0.375/t |
  | `REPULSOR_CHARGED` | 4.0–8.0 by charge, ≥ 20 t cycle | ≤ 0.4/t | — |
  | `REPULSOR_VOLLEY` | 12.0 per 20 t charge, 10 energy | 0.6/t burst, energy-limited | — |
  | `UNIBEAM_HIT` | 1.5 every `UNIBEAM_HIT_INTERVAL=4` t at ≤ 8 blocks → 0.75 at 48 (linear) | 0.375/t → 0.19/t; ≤ 22.5 per 60 t beam | laser 80 t = 30 total |
  | overdraft | ×1.5 → 2.25 every 4 t | 0.56/t, ends in self-explosion | — |
  | `MISSILE_HIT` | 5.0 each, 4 per 15 energy | 20 per volley | roar 6 |
  | `BLADE` / `HAMMER` / `HAMMER_SLAM` | 7 / 9 / 5 | melee cadence | — |
  | `NANO_PUNCH` | vanilla melee × `NANO_MELEE_FACTOR` 1.5 | — | Viltrumite kit punch (stronger) |

  Result: Iron Man's sustained ranged damage equals the Homelander laser at best, Unibeam per beam is below a full laser, and Homelander keeps regen, faster flight and the Viltrumite kit — Iron Man is weaker head-on (spec §1.2 п. 8, §18). Tuned after the in-game check.
- Bits 5–18 of 1a `IronManFlags` are used here; per-ability progress uses `actionId/actionElapsed/actionLength`.
- `extraCooldowns` indexes for Iron Man (fixed in `IronManAbilities`): 0 nano lock, 1 countermeasures, 2 Veronica, 3 signature, 4 mark Starboost/slam cooldown, 5–7 Hulkbuster slots 1–3, 8 Hulkbuster cooldown, 9 Legion; page-1 slots keep `cooldowns[0..5]`.

- [ ] Step 1: Tests: `extraCooldownsRoundTrip`, `oldEncodingDecodesNoExtraCooldowns`, `thirdBeamIsOverdraft`, `secondWarns`, `suitChangeKeepsCount` (save/load + `Suit` transitions), `deathResets`, `explosionResets`, `neverAbove3`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): core overheat counter`.

### Task 2: RMB tool, MMB cycle, mouse claims

**Files:**
- Create: `combat/RightTool.java` (enum `REPULSOR, NANO_BLADE, NANO_HAMMER, SIGNATURE, JACKHAMMER, HULK_REPULSOR` — Stage 4 uses `SIGNATURE`, Stage 5 the last two), `combat/ToolCycle.java`, `core/entity/HeroInteractable.java` (shared interface: `boolean heroInteract(ServerPlayer who)`, `boolean canHeroInteract(Player who)`)
- Modify: `IronManHero.java` (`mouseAction`, `handleInput`), `HeroAction` append `SECONDARY_USE, TOOL_CYCLE, UNIBEAM, MISSILES, NANO_ARSENAL, GUARD, INTERACT`
- Test: `test/hero/ironman/ToolCycleTest.java`

**Interfaces:**
- Suit worn → claims LMB (`PRIMARY_ATTACK`), RMB (`SECONDARY_USE`, hold), MMB (`TOOL_CYCLE`). Without suit → none (vanilla).
- RMB interaction before the tool: if the crosshair entity (reach 4) implements `HeroInteractable` and `canHeroInteract(player)` is true, RMB sends `HeroAction.INTERACT` (no tool start, no energy). The server re-checks: distance ≤ 4.5, line of sight, owner, current suit state (Stage 4 `EmptySuitEntity` rules), else ignores. Without suit RMB is vanilla, so the vanilla entity-use path calls the same `heroInteract`.
- Crosshair per tool (spec §15.2): `REPULSOR` repulsor ring, `NANO_BLADE` blade, `NANO_HAMMER` hammer, `SIGNATURE` → the mark signature's crosshair (Stage 4: Mark 7 laser, War Machine gun), `JACKHAMMER` jackhammer, `HULK_REPULSOR` heavy repulsor.
- `ToolCycle.next(RightTool cur, boolean weaponFormed, RightTool signature)`: nano: `REPULSOR ↔ (formed weapon)`; nothing formed → stays `REPULSOR`. Stage 4 passes the mark signature, Stage 5 jackhammer.
- Server keeps `rightTool`; snapshot `heroFlags` bits 8–10 = tool ordinal (client crosshair + poses).
- LMB in suit on ground: nano punch (`NANO_MELEE_FACTOR`) or weapon strike when formed (Task 7); in flight: fly-by (Stage 1).

- [ ] Step 1: Tests: `cycleRepulsorToFormedWeapon`, `cycleWithoutWeaponStays`, `rmbUsesServerTool`, `noClaimsWithoutSuit`, `rmbOnInteractableDoesNotFire`, `interactRefusedFarAway`, `interactRefusedWithoutLos`, `releaseAfterCycleGoesToStartedTool` (1a `HeldInputs`), `heldRmbReleasedOnSuitOff`, `heldRmbReleasedOnScreenOpen`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): rmb tool and mmb cycle`.

### Task 3: Repulsors

**Files:**
- Create: `combat/Repulsor.java` (pure: charge level, hand alternation, cost, recoil), `core/entity/RepulsorBlastEntity.java` (fast projectile, registered in `ViltrumiteEntities`), `core/client/ironman/RepulsorRenderer.java`, `core/client/ironman/RepulsorVfx.java`
- Modify: `IronManHero.java`, `IronManPoser.java` (arm raise, palm forward, alternate hands)
- Test: `test/hero/ironman/RepulsorTest.java`

**Interfaces:**
- Press RMB (through `HeldInputs`) → start charge; a repeated press while charging is ignored; forced release (suit off, control, screen) cancels the charge without a shot; release before 4 t → **shot** (cost 2, alternate hand, `REPULSOR_COOLDOWN` 8 t); release ≥ 4 t → **charged** (cost 6, damage + knockback by charge 0..1); charge reaches 20 t → **volley** from both hands (cost 10) on release. Energy checked on release (`spend`), not enough → fizzle FX.
- Movement (spec §8.1): in flight, shot direction · velocity < −0.6 → brake: velocity ×(1 − 0.5·power); in HOVER, look pitch ≥ 60° down → lift +0.6·power vy; on ground, full charge with pitch ≥ 70° down → shockwave `HeroShockwave.land`-style ring (radius 4, knockback, no block break).
- Visual: palm glow grows while charging (light part on hand bone via `PlayerGeoLayer`), bright pixel bolt, hit flash + small scorch.

- [ ] Step 1: Tests: `tapIsShotCost2`, `chargeCost6`, `fullChargeVolleyCost10`, `handsAlternate`, `backShotBrakes`, `downShotInHoverLifts`, `groundFullChargeShockwave`, `lockedEnergyFizzles`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): repulsors`.

### Task 4: Unibeam

**Files:**
- Create: `combat/UnibeamTimeline.java` (pure state machine `IDLE, CHARGE, BEAM, OVERHEAT`), `combat/UnibeamServer.java`, `core/client/ironman/UnibeamRenderer.java`, `core/client/render/vfx/ScorchBuffer.java` (moved from `client/homelander/ScorchBuffer.java`) and `core/client/render/vfx/ScorchRenderer.java` (the `Scorch` record, the buffer instance and the scorch drawing extracted from `client/homelander/HomelanderVfx.java:55-83` and its render pass; `HomelanderVfx` calls it; limits stay `HomelanderRules.SCORCH_MAX/SCORCH_LIFETIME` for Homelander, Iron Man passes its own), `core/client/render/vfx/FlashOverlay.java`, `core/network/packet/FlashS2CPacket.java`
- Modify: `IronManHero.java`, `IronManPoser.java` (chest out, arms back), `IronManAbilities` (`ironman:unibeam` page 1 slot 1)
- Test: `test/hero/ironman/UnibeamTimelineTest.java`; move `test/client/homelander/ScorchBufferTest.java` to `test/client/render/vfx/ScorchBufferTest.java` (unchanged asserts); other `test/client/homelander/*` stay green

**Interfaces:**
- Hold slot → `CHARGE` 20 t (chest glow + hum; costs 35 at beam start; not enough → refuse), `BEAM` while held, max 60 t; beam aim follows look with max turn `UNIBEAM_TURN_DEG=3°/t` (slow). Release or 60 t → `OVERHEAT` 40 t (weapons locked, smoke) → `CoreOverheat.add()`.
- Hit: thick ray (radius 0.6) up to 48 blocks, `UNIBEAM_HIT` every `UNIBEAM_HIT_INTERVAL` (4 t) falling linearly with distance (Task 1 table); push target along the beam (velocity add, not hold); glass and leaves broken via `HeroDestruction` (tag `minecraft:leaves`, `forge:glass`, `forge:glass_panes`); other blocks → scorch marks (shared `ScorchRenderer`).
- Blinding (spec §8.2): players in the beam and players within 12 blocks looking into it (angle < 25°) → `FlashS2CPacket(strength, ticks)`; PvP strength capped short (20 t); owner light flash. Mobs in the beam → Blindness 40 t + lose target.
- Control interrupts CHARGE/BEAM (counts as normal end → overheat if beam started).

- [ ] Step 1: Tests: `chargeTakes20`, `beamMax60`, `releaseOverheats40`, `overheatAddsCounter`, `turnRateLimited`, `damageFallsWithDistance`, `onlyGlassAndLeavesBreak` (pure tag predicate), `controlEndsChannel`.
- [ ] Step 2: FAIL → implement → PASS. Move `ScorchBuffer` + extract `ScorchRenderer`; Homelander scorch pixel-identical (screenshot) and tests green.
- [ ] Step 3: Commit `feat(ironman): unibeam with overheat`.

### Task 5: Overdraft and core explosion

**Files:**
- Create: `combat/Overdraft.java` (pure timeline), `core/client/ironman/OverdraftVfx.java`
- Modify: `UnibeamServer.java`, `IronManHero.java` (`clampFinalDamage` survival floor — 1a damage seam, `tick`), `Suit.java` (append `nanoLockTicks`; Stage 4 adds mark breaking)
- Test: `test/hero/ironman/OverdraftTest.java`

**Interfaces:**
- Beam start with `CoreOverheat.nextIsOverdraft()` → the beam enters overdraft (the 60 t beam cap does not apply): thicker/brighter, damage ×1.5, stronger flash. At +40 t → sputter (beam pulses/breaks, sparks, HUD warning blink, sound break); at +30 t after sputter → explosion. Release before → beam off, explosion still at the same moment with `CORE_EXPLOSION_POWER_RELEASED`.
- Explosion: `level.explode(player, …, power, ExplosionInteraction.MOB)` centred on the player (mobGriefing respected), extra knockback to everyone around, player launched up/back. Player damage from the **own core explosion** is floored so HP stays ≥ `CORE_SURVIVE_HP` through `clampFinalDamage` (after armor). The rule is keyed by the source, not only the tick: `isOwnCoreExplosion(src)` = explosion damage type && `src.getEntity() == self` && `state.coreExplosionWindow > 0` (40 t after the blast). So it also holds for a deferred `ControlManager` payout of that explosion (payout runs `clampFinalDamage` via 1a `applyCleanDamage`). Other sources in the same tick are not clamped and can still kill.
- Nano: suit → NONE instantly with nanite scatter VFX, `nanoLockTicks = 600` (Костюм key refused with HUD text). `CoreOverheat.resetByExplosion()`. Stage 4: if a mark is worn → mark breaks, auto nano, no lock.
- Death mid-overdraft → pending explosion cancelled, counter reset (death).

- [ ] Step 1: Tests: `thirdOverheatStartsOverdraft`, `sputterAt40`, `explodesAt70`, `releaseStillExplodesWeaker`, `explosionLeavesTwoHearts` (low HP, no armor → exactly 4 HP), `deferredPayoutOfOwnExplosionClamped`, `otherDamageStillLethal`, `nanoLocked30s`, `deathCancelsPendingExplosion`, `controlDoesNotCancelOverdraft`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): unibeam overdraft and core explosion`.

### Task 6: Micro-missiles

**Files:**
- Create: `combat/MissileLock.java` (pure: marks from aim sweep), `core/hero/OwnerSection.java` (enum `CARRIERS, THREATS, MARKS, SCAN`, append-only), `core/entity/MicroMissileEntity.java` (homing, implements new shared `core/entity/Homing` interface `retarget(Entity)` for Stage 3 countermeasures), `core/client/ironman/MissileRenderer.java`, `res/geo/ironman/missile.geo.json`
- Modify: `core/hero/HeroOwnerSnapshot.java` (typed sections: `Map<OwnerSection, Section>` with `Section(int[] ids, int[] expireTicks)`; `carrierEntityIds()` kept as the `CARRIERS` view for Regulus hearts and Homelander focus), `core/hero/HeroRegistry.java` (`pushOwnerSection(player, section, Section)` replaces only that section; `pushOwnerSnapshot` keeps working = `CARRIERS`), `core/network/packet/HeroOwnerSnapshotS2CPacket.java` (section id + ids + expiry; one packet per changed section), `core/client/hero/ClientHeroData.java` (per-section storage, expiry by client game time), `IronManHero.java`, `IronManPartsProvider.java` (shoulder flaps = `shoulder_rockets` bone of Satsu `full_body` geo, opened by snapshot progress), `IronManAbilities` (`ironman:missiles` page 1 slot 2), `HeroDefinition`-free hook `IronManHero.canMarkTargets(state)` (true now; Stage 3 → helmet closed)
- Test: `test/hero/ironman/MissileLockTest.java`, `test/hero/HeroOwnerSnapshotTest.java`

**Interfaces:**
- Hold slot → flaps open 10 t; while held, entities within 64 blocks and 4° of the crosshair are marked (unique, max `MISSILE_MARKS`, LOS required); marks shown as brackets (client from the `MARKS` owner section). Release → cost 15, one missile per mark (fan launch), or 4 straight along the aim if no marks. Missiles: own model, smoke trail pixel VFX, explosion damage via `HeroDamage.route`, explosion **without** block damage (`ExplosionInteraction.NONE`), own sound.

- [ ] Step 1: Tests: `sectionsIndependent` (threats + 4 marks + 1 scan target active; a `THREATS` update leaves `MARKS` and the 200 t `SCAN` expiry untouched), `carriersViewUnchanged` (Regulus/Homelander), `marksUpToFour`, `noDuplicateMarks`, `needsLineOfSight`, `noMarksFiresStraight`, `flapsTake10`, `cost15`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): micro-missiles with target marks`.

### Task 7: Nano-arsenal (blade ↔ hammer)

**Files:**
- Create: `combat/NanoArsenal.java` (pure: form/switch/dissolve), `combat/BladeDash.java`, `combat/HammerLaunch.java` (tracks launched targets breaking soft blocks), `core/effect/SunderEffect.java` (armor −30%, disables shield blocking 5 s), `core/client/ironman/NanoWeaponParts.java`, `res/geo/ironman/{nano_blade,nano_hammer}.geo.json` (Satsu `nanokatar`, `nano_mallet`)
- Modify: `IronManHero.java`, `IronManPoser.java`, `IronManAbilities` (`ironman:nano_arsenal` page 1 slot 3)
- Test: `test/hero/ironman/NanoArsenalTest.java`

**Interfaces:**
- Slot press: none → form blade (cost 8, wrist nanite wave ~8 t); RMB/LMB holds (dash, hammer charge) go through `HeldInputs`; blade ↔ hammer on further presses (cost 8 each form); MMB → back to repulsor (weapon dissolves). Strikes free.
- Blade: LMB slash (arc 100°, reach 3.5), hit → `SunderEffect` 100 t; RMB → dash to the crosshair target ≤ 8 blocks (`BladeDash` path, stops on block).
- Hammer: LMB heavy hit (big knockback); LMB at a ground block in reach → ground slam, enemies in r=4 launched up; hitting an airborne target → slam it down (vy −2.0); RMB hold to charge (≤ 20 t) → launch target along look, `HammerLaunch` breaks soft blocks (hardness ≤ 1.5, `HeroDestruction.canDestroy`) on its path for 20 t.
- First person mandatory: weapon part on right arm bone in both views.

- [ ] Step 1: Tests: `formCosts8`, `switchBladeHammer`, `mmbDissolves`, `bladeSunders100`, `dashStopsAtWall` (pure grid), `airborneHitSlamsDown`, `launchBreaksOnlySoftBlocks`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): nano blade and hammer`.

### Task 8: Hand-swap block and guard seam

**Files:**
- Create: `core/client/hero/HeroGuardInput.java` (consumes `options.keySwapOffhand` clicks when `guardAction != null`, sends hold edges), `core/network/packet/HeroGuardC2SPacket.java`
- Modify: `core/hero/HeroDefinition.java` (`default boolean blocksHandSwap(Player p) { return false; }`, `default HeroAction guardAction(Player p) { return null; }`), `core/hero/HeroEvents.java` (`LivingSwapItemsEvent.Hands` → cancel if `blocksHandSwap`; the event exists in Forge 47.3.0 — `net/minecraftforge/event/entity/living/LivingSwapItemsEvent$Hands.class` checked in `forge-1.20.1-47.3.0-universal.jar`. Fallback if it is not fired for players at runtime: a common mixin on `ServerGamePacketListenerImpl.handlePlayerAction` for `SWAP_ITEM_WITH_OFFHAND` that returns early when `blocksHandSwap`), `IronManHero.java` (both true/`GUARD` in any suit)
- Test: `test/hero/HeroGuardSeamTest.java`

- [ ] Step 1: Tests: `otherHeroesSwapNormally`, `ironManWithoutSuitSwaps`, `suitBlocksSwap`, `guardActionOnlyInSuit`. Manual: in suit F never swaps (also with a forged swap packet — server event cancels), out of suit swaps.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(hero): guard key seam and hand-swap block`.

### Task 9: Shield and perfect block

**Files:**
- Create: `combat/Shield.java` (pure: raised tick, front cone, perfect window, cost), `core/client/ironman/ShieldParts.java`, `res/geo/ironman/nano_shield.geo.json` (+ first-person variant)
- Modify: `IronManHero.java` (`absorbIncoming` — 1a damage seam, shield is the first layer after control), projectile reflection helper in `combat/Reflect.java`
- Test: `test/hero/ironman/ShieldTest.java`

**Interfaces:**
- Guard down → shield raised instantly (plates from the left forearm, `nano_shield`), up → collapses. Front cone 120°: hit absorbed in `LivingAttackEvent` (event cancelled: no HP loss, no vanilla knockback), cost 4 per hit; not enough energy / locked → no shield. Explosions from the front absorbed too.
- Perfect: hit arrives ≤ 5 t after raise → projectile reflected (velocity reversed toward its owner, owner set to us) / melee attacker knocked back strongly; flash + chime.
- While raised: no RMB fire; flight allowed.

- [ ] Step 1: Tests: `realHurtWithShieldUp` (a real `player.hurt` with the shield raised → HP unchanged, energy −4), `deferredPayoutHitsShieldOnce`, `frontHitAbsorbed`, `backHitPasses`, `costs4PerHit`, `noShieldWhenLocked`, `perfectOnlyFirst5Ticks`, `reflectReversesVelocity`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): nano shield with perfect block`.

### Task 10: Nano damage and repair visuals

**Files:**
- Create: `core/client/ironman/NanoDamageVisuals.java`
- Modify: `IronManHero.java` (`onHurt` notify: hit ≥ 6 → `IronManFlags` bits 12–14 = damaged zone mask/shoulder/chest), `IronManSkinLayer.java`

- [ ] Step 1: Heavy hit → zone shows broken-skin pixels (Tony's skin shows through), then nanite crawl repairs it over 60 t. Visual only (spec §4.2).
- [ ] Step 2: Commit `feat(ironman): nano damage and repair visuals`.

### Task 11: Crosshair, HUD pips, panel icons

**Files:**
- Create: `core/client/ironman/IronManCrosshair.java` (Forge `RenderGuiOverlayEvent.Pre` for `CROSSHAIR` when suit worn; picks the texture by `RightTool`, `SIGNATURE` asks the Stage 4 signature visuals), `res/textures/gui/ironman/crosshair/{repulsor,blade,hammer,laser,gun,jackhammer,hulk_repulsor}.png` (laser/gun/jackhammer/hulk used from Stages 4–5), `res/textures/gui/ability/ironman/{unibeam,missiles,nano_blade,nano_hammer}.png` (+ `.b64`)
- Modify: `IronManHud.java` (3 overheat pips from `IronManFlags` bits 5–6, overheat lock bar, overdraft warning blink), `IronManAbilities` (slot grey-out by `weaponsLocked`, overheat, nano lock from `extraCooldowns[0]`)

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

Fixed after PR review (2026-10-09):
6. Fixed after PR review: shield and core-explosion floor used void `onHurt` → 1a damage-layer seam (`absorbIncoming` for the shield in Task 9, `clampFinalDamage` keyed by source for the explosion in Task 5, covers deferred payouts); tests `realHurtWithShieldUp`, `deferredPayoutOfOwnExplosionClamped`.
7. Fixed after PR review: held RMB/slot actions now follow 1a `HeldInputs` (press-only gating, release to the started action, forced release) — Global Constraints, Tasks 2, 3, 7.
8. Fixed after PR review: `HeroOwnerSnapshot` had one `int[]` replaced wholesale → typed sections with expiry; packet and `ClientHeroData` updated in Task 6 (first user: missile marks); test `sectionsIndependent`.
9. Fixed after PR review: `COOLDOWN_COUNT = 6` could not carry page-2/extra cooldowns → generic `extraCooldowns` with backward-compatible encoding and fixed Iron Man indexes (Task 1).
10. Fixed after PR review: `IronManFlags` moved to 1a; this plan only uses bits 5–18.
11. Fixed after PR review: damage numbers were ambiguous ("every 2 t" vs "/t") and Unibeam was ~4× Homelander's laser → per-hit values with intervals and a Homelander comparison table (Task 1).
12. Fixed after PR review: RMB in suit would fire at the own empty suit → `HeroInteractable` check before the tool, server validation of distance/LOS/owner/suit (Task 2).
13. Fixed after PR review: `client/homelander/ScorchMarks.java` does not exist → move `ScorchBuffer.java` and extract scorch drawing from `HomelanderVfx.java` (Task 4).
14. Fixed after PR review: `LivingSwapItemsEvent.Hands` verified in Forge 47.3.0; mixin fallback on `SWAP_ITEM_WITH_OFFHAND` recorded (Task 8).
15. Fixed after PR review: `RightTool.HULK_REPULSOR` added; crosshairs for Mark 7 laser, War Machine gun and jackhammer bound to tool/signature (Tasks 2, 11).
