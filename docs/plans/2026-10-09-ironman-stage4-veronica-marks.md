# Iron Man — Stage 4 (Veronica, Suit Parts, Exit, Empty Suit, Seven Marks) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Veronica drops a capsule like a meteor (random point ≤ 30 blocks, not water, ~1 TNT), it stands ~60 s; a repeated key press within ~64 blocks opens a suit menu with 3D previews; the chosen mark flies to Tony in parts that smoothly wrap the limbs (legs → arms → chest → back → helmet, ~2–3 s, vulnerable); marks take all damage on their own durability and break apart (auto nano, ~5 min cooldown); the "Костюм" key exits a mark (plates open, interior, Tony steps out), the empty suit stays (max one) and can be re-entered with RMB; seven marks with their signatures and passives.

**Architecture:** `SuitState` gets appended values `MARK, EQUIPPING, EXITING` (Stage 5 appends Hulkbuster separately as a layer). Data: `MarkId` enum + `MarkSpec` record (all numbers) + `MarkRoster` (per-mark durability and cooldowns, NBT). Systems read the active `SuitSpec` (nano or mark) instead of nano constants — speed, sonic drain, weapon factor, armor, shield strength, missile marks, Unibeam charge. Signatures implement one interface `MarkSignature` (server) + `MarkSignatureVisuals` (client), selected by `MarkSpec`, so marks add no branches in `IronManHero`. Entities: `VeronicaPodEntity` (extends meteor flight behaviour by composition, not by editing `MeteorEntity`), `EmptySuitEntity`, `SuitDebrisEntity` (client-light). Part flight is a server timeline + client visuals (parts are not entities; spec §19 "сбивание частей" out of scope).

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3. Asset tools in Python (`tools/`).

**Spec:** `docs/design/2026-10-09-ironman-design.md` §3, §4.3, §4.5, §12, §13, §15.2, §16, §17, §18. Previous plans: Stages 1–3.

Paths as in Stage 1; `mark/` = `core/hero/ironman/mark/`, `vero/` = `core/hero/ironman/veronica/`.

## Global Constraints

- All Stage 1–3 Global Constraints apply.
- Mark numbers (durability, armor, factors) in `MarkSpec` table only; Iron Man head-on weaker than Homelander (spec §18). Starting table in Task 1, tuned after the in-game check.
- All damage to a worn mark goes to its durability; Tony takes nothing until it hits 0 (spec §4.3). Hulkbuster (Stage 5) sits above this.
- Veronica, empty suit and debris are visible to everyone; capsule and empty suit fly away on owner death, logout, dimension change, hero change (spec §16). Durability of a mark that flew away is kept in the roster.
- Mark models: Satsu `suits/IronMan/mark_07|mark_15|mark_17|mark_39`, `suits/WarMachine/*` (Mk2), `suits/IronHeart/*` (Mk3) — suit **skin** + 3D parts like Mark 50 (Stage 1 Task 8 pipeline); Mark 42 = Sind `tabula/mk42/*` (14 parts) + `textures/heroes/mark42/*` (skin-format layers). Flight parts of other marks = Satsu `each_part/*.geo.json` (head, chest, shoulders, arms, legs) with the mark texture.
- Converters are committed tools: `tools/tabula2geo.py` (Tabula `.tbl` zip → Bedrock geo.json for `BakedGeoModel`), `tools/fsk2anim.py` (FiskHeroes `.fsk` → our `AnimationParser` format) — Stage 5 reuses both.
- Veronica capsule model and the suit interior texture are drawn new (pixel style).

## Review Focus

1. Only one empty suit per player in the world; entering, logout, dimension change never duplicate or lose a mark's durability (Task 8 tests `secondEmptySuitSendsFirstAway`, `flyAwayKeepsDurability`).
2. Mark breaks while Tony is mid-air → auto nano without a fall (nano deploy is instant-armored), no Stage 2 nano lock (spec §9.4 mark case) (Task 3 tests).
3. Equip window: Tony slowed, no shield/abilities, damage during the window goes to Tony (the new mark is not on yet) but the previous mark/nano already left → test `equipWindowDamageHitsTony`.
4. Capsule drop point never in water/lava, never inside blocks, falls back to the player position if 16 tries fail (Task 4 test `fallbackWhenNoDryGround`).
5. Mark 15 camo hides from Homelander focus and Iron Man scan through the generic hooks only (`hiddenFromScan`, new `hiddenFromFocus`) — no `HeroId` checks (Task 13 test).

---

### Task 1: Mark data model and roster

**Files:**
- Create: `mark/MarkId.java` (append-only: `MARK_7, MARK_42, MARK_15, MARK_39, MARK_17, WAR_MACHINE_MK2, IRON_HEART_MK3`), `mark/MarkSpec.java`, `mark/SuitSpec.java` (nano + mark view used by all systems), `mark/MarkRoster.java`
- Modify: `SuitState.java` (append `MARK, EQUIPPING, EXITING`), `IronManState.java`, Stage 1–3 systems that read nano constants → `SuitSpec.of(state)`
- Test: `test/hero/ironman/MarkRosterTest.java`, `test/hero/ironman/SuitSpecTest.java`

**Interfaces:**
- `MarkSpec(MarkId id, float durability, float armor, float toughness, float knockbackRes, float flightSpeedMul, float sonicDrainMul, float weaponMul, float shieldMul, int missileMarks, float missileMul, int unibeamCharge, boolean silentFlight, Signature primary, Signature secondary)`.
- Starting table: Mark 7 (dur 120, armor 16, all ×1.0); Mark 42 (dur 100, armor 15, parts 14); Mark 15 (dur 80, armor 11, silent); Mark 39 (dur 100, armor 14, speed ×1.35, sonic drain ×0.6, weapon ×0.85); Mark 17 (dur 110, armor 15, Unibeam charge 10 t); War Machine Mk2 (dur 150, armor 18, speed ×0.8, sonic drain ×1.5, 8 marks, missile ×1.4); Iron Heart Mk3 (dur 180, armor 20, kb res 0.9, shield ×1.5, speed ×0.8).
- `MarkRoster`: `float durability(MarkId)`, `int cooldown(MarkId)`, `boolean available(MarkId)`, `void breakMark(MarkId)` (cooldown 6000 t, durability restored to full when cooldown ends), `void store(MarkId, float)`, `tick()`, `save/load` key `MarkRoster`. Death keeps cooldowns and durabilities (spec §16).

- [ ] Step 1: Tests: `allSevenMarksHaveSpecs`, `breakStarts5MinCooldown`, `unavailableOnCooldown`, `durabilityRestoredAfterCooldown`, `saveLoadRoundTrip`, `suitSpecNanoDefaults`, `suitSpecUsesMarkFactors`, `markIdAppendOnly` (ordinal snapshot).
- [ ] Step 2: FAIL → implement → PASS; all Stage 1–3 tests still green. Commit `feat(ironman): mark specs and roster`.

### Task 2: Damage on the mark

**Files:**
- Modify: `IronManHero.java` (`onHurt` → mark durability first), `core/hero/HeroPublicSnapshot.java` (append generic `int variant` = active suit variant for every client's renderer, `int resource2` = durability ×10; old encodings decode with 0), extend `test/hero/HeroPublicSnapshotTest.java` (`oldEncodingDecodesVariantZero`)
- Create: `mark/MarkDamage.java` (pure)
- Test: `test/hero/ironman/MarkDamageTest.java`

**Interfaces:**
- `MarkDamage.apply(float durability, float amount, float armorFactor)` → `(float newDurability, float toTony)`: while durability > 0 Tony takes 0; the hit that empties it does not spill over (spec "пока прочность не кончилась"). Shield (Stage 2) is checked before.
- HUD (Stage 3 left column) shows mark durability.

- [ ] Step 1: Tests: `allDamageToMark`, `breakingHitDoesNotSpill`, `shieldBeforeMark`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): marks absorb damage`.

### Task 3: Mark break, debris, auto nano

**Files:**
- Create: `core/entity/SuitDebrisEntity.java` (plate chunk, physics, 200 t life, client renders a `each_part` piece with the mark texture), `core/client/ironman/MarkBreakVfx.java`
- Modify: `IronManHero.java`, `Suit.java` (instant-armored nano deploy on break), `combat/Overdraft.java` (core explosion with a mark → break, auto nano, no `nanoLockTicks`)
- Test: `test/hero/ironman/MarkBreakTest.java`

- [ ] Step 1: Tests: `zeroDurabilityBreaks`, `breakAutoNano`, `breakCooldown`, `overdraftWithMarkBreaksWithoutNanoLock`, `noFallDamageOnAirBreak`.
- [ ] Step 2: Visual: sparks, plates fall off in chunks (6–9 debris), lie and fade; nano wave on Tony at once. Commit `feat(ironman): mark breaking`.

### Task 4: Veronica capsule

**Files:**
- Create: `vero/DropPoint.java` (pure), `vero/VeronicaPod.java` (state `FALLING, LANDED, LEAVING`), `core/entity/VeronicaPodEntity.java`, `core/client/ironman/veronica/VeronicaPodRenderer.java`, `res/geo/ironman/veronica_pod.geo.json` + texture (+ `.b64`), `res/animations/ironman/veronica_pod.animation.json` (open/close/leave)
- Modify: `IronManHero.java` (`VERONICA` input), `HeroAction` append `VERONICA`, `IronManAbilities` (`ironman:veronica` page 2 slot 3), `ViltrumiteEntities`
- Test: `test/hero/ironman/DropPointTest.java`, `test/hero/ironman/VeronicaPodTest.java`

**Interfaces:**
- `DropPoint.pick(RandomSource, origin, Surface surface)` → random point r ≤ 30 with solid dry top (`Surface.dryTop(x,z)`), 16 tries, fallback = player position.
- Falls from y+120 with a fire trail (reuse `MeteorImpactVFXManager` style), impact: dust ring + explosion power 4 (~1 TNT), `ExplosionInteraction.MOB` (mobGriefing).
- LANDED 1200 t or owner > 96 blocks / death / logout / dimension / hero change → LEAVING (flies up, despawns). Veronica cooldown 3600 t starts at LEAVING (spec §12.2). One pod per player.

- [ ] Step 1: Tests: `pointWithin30`, `neverWater`, `fallbackWhenNoDryGround`, `landedLasts1200`, `leavesWhenOwnerFar`, `cooldownStartsOnLeave`, `onePodPerPlayer`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): veronica capsule`.

### Task 5: Suit menu

**Files:**
- Create: `core/client/ironman/veronica/VeronicaScreen.java` (grid: 7 marks + Hulkbuster slot (Stage 5 enables), 3D preview of a dummy player model in the mark skin, rotating; cooldown overlay; durability bar), `core/network/packet/VeronicaChooseC2SPacket.java`, `core/network/packet/VeronicaMenuS2CPacket.java` (roster view)
- Modify: `IronManHero.java` (repeat press while pod LANDED and distance ≤ 64 → open menu; works on ground and in flight)
- Test: `test/hero/ironman/VeronicaMenuTest.java` (server validation)

- [ ] Step 1: Tests: `menuOnlyWithin64`, `chooseUnavailableRefused`, `chooseWhileEquippingRefused`, `unlimitedSwapsWhilePodStands`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): veronica suit menu`.

### Task 6: Asset converters and mark skins

**Files:**
- Create: `tools/tabula2geo.py`, `tools/fsk2anim.py`, `tools/bake_suit_skin.py` (generalised Stage 1 baker), `res/textures/entity/hero/ironman_{mark_7,mark_15,mark_17,mark_39,war_machine_mk2,iron_heart_mk3,mark_42}{,_glow}.png` (+ `.b64`), `res/geo/ironman/mark_42/*.geo.json` (14 parts), `res/geo/ironman/parts/*.geo.json` (Satsu `each_part`), `res/textures/entity/hero/ironman_interior.png`
- Test: `tools/test_tabula2geo.py` (unit test on a tiny `.tbl` fixture; run with `python3 -m unittest`)

- [ ] Step 1: Converter tests (cube origin/size/rotation/pivot mapping, texture size, parenting).
- [ ] Step 2: Convert Mark 42 parts; bake skins for the six Satsu marks; draw the shared interior texture (padding, cables, light strips).
- [ ] Step 3: `tools/preview_geo.py` (Pillow, orthographic front/side render of geo cubes with textures, no network libs) renders every model to PNG for review. Commit `feat(ironman): mark assets and converters`.

### Task 7: Equip by parts

**Files:**
- Create: `vero/EquipTimeline.java` (pure), `core/client/ironman/veronica/PartFlightVisuals.java`, `core/client/ironman/veronica/PartWrapAnimator.java`
- Modify: `IronManHero.java` (`EQUIPPING` state: slowness attribute, abilities/shield off), `IronManPoser.java` (arms slightly out, Mark 42 pose), `IronManSkinLayer.java` (per-part reveal: each limb becomes the mark skin when its part locks)
- Test: `test/hero/ironman/EquipTimelineTest.java`

**Interfaces:**
- On choose: previous mark exits (Task 8, fast 15 t variant) and becomes the empty suit under the one-per-owner rule, or nano retracts; then `EquipTimeline(parts=7..9 or 14)`, total 50 t: legs → arms → chest → back (shoulder parts + rear plate) → helmet; each part: launch from pod, homing to its bone (follows a moving player), wrap 8 t (plates open, close around the limb, click), lock. Helmet last: click + eye flash. Camera not switched; 1st person shows arm wrapping.
- Damage during the window → Tony (spec §12.4 "окно уязвимости"); control interrupts → parts already locked stay, remaining parts snap on instantly at the end of control (no lost suit).

- [ ] Step 1: Tests: `orderLegsArmsChestBackHelmet`, `total50Ticks`, `mark42Has14Parts`, `equipWindowDamageHitsTony`, `noAbilitiesWhileEquipping`, `controlSnapsRemaining`.
- [ ] Step 2: FAIL → implement → PASS. Visual check moving/flying while equipping. Commit `feat(ironman): suit parts fly and wrap`.

### Task 8: Exit and the empty suit

**Files:**
- Create: `core/entity/EmptySuitEntity.java` (stands, has the mark id + durability, not pushable, invulnerable — spec defines no damage to empty suits, so it cannot be farmed or lost), `core/client/ironman/EmptySuitRenderer.java` (player-shaped shell: mark skin outside, interior texture inside, open/close animation), `vero/EmptySuitRegistry.java` (pure: one per owner)
- Modify: `IronManHero.java` ("Костюм" in `MARK` → `EXITING` 30 t: plates open, helmet tilts, Tony pushed forward 0.4 + step; suit closes and stands), RMB on own empty suit → enter (open, walk-in, close) keeping durability
- Test: `test/hero/ironman/EmptySuitTest.java`

**Interfaces:**
- `EmptySuitRegistry.place(owner, entityId)` → previous empty suit gets `flyAway()` (to the pod if LANDED, else to the sky); owner > 96 blocks, logout, death, dimension, hero change → fly away; durability goes back to the roster.
- Others cannot enter (only owner). Enter allowed from no armor or nano (nano retracts instantly, spec §4.1); in another mark → refused with a HUD hint. Entering = `EQUIPPING` short variant 20 t without part flight.

- [ ] Step 1: Tests: `exitLeavesEmptySuit`, `secondEmptySuitSendsFirstAway`, `enterKeepsDurability`, `flyAwayKeepsDurability`, `onlyOwnerEnters`, `ownerFarFliesAway`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): suit exit and empty suit`.

### Task 9: Signature seam and panel/RMB integration

**Files:**
- Create: `mark/MarkSignature.java` (`start/tick/stop(ServerPlayer, IronManState)`, `boolean onRmb()`, `HeroAction slotAction()`), `core/client/ironman/mark/MarkSignatureVisuals.java` registry
- Modify: `IronManAbilities` (page 1 slot 3 = primary signature in a mark, empty slots = secondary signature), `combat/ToolCycle.java` (MMB: repulsor ↔ signature on RMB if `onRmb()`), `HeroAction` append `SIGNATURE_1, SIGNATURE_2`
- Test: `test/hero/ironman/SignatureSlotsTest.java`

- [ ] Step 1: Tests: `nanoSlot3IsArsenal`, `markSlot3IsSignature`, `rmbSignatureCycles`, `noNanoArsenalInMark`, `noNanoShieldVisualInMark` (energy hex shield instead).
- [ ] Step 2: FAIL → implement → PASS. Mark shield visual = `electro_magnetic_shield` hexes from the palms (same mechanics). Commit `feat(ironman): mark signature slots`.

### Task 10: Mark 7 — micro-lasers

- Files: `mark/sig/MicroLaser.java`, `client/ironman/mark/MicroLaserVisuals.java`, Sind `mk6 laser` part → `res/geo/ironman/mark_7/laser.geo.json`; Test `test/hero/ironman/MicroLaserTest.java`.
- RMB hold: thin precise beam from the forearm, 24 blocks, single target, 1.0/t, `SunderEffect`-like **shield break**: disables a vanilla shield (cooldown 100 t) and drops an active Iron Man shield of another player.
- [ ] Tests `singleTarget`, `breaksShield`, `costsEnergyPerTick` (0.15/t) → implement → commit `feat(ironman): mark 7 micro-lasers`.

### Task 11: Mark 42 — rocket fist, part loss

- Files: `mark/sig/RocketFist.java`, `core/entity/RocketFistEntity.java`, `mark/Mark42Parts.java` (pure); Test `test/hero/ironman/Mark42Test.java`.
- Signature: glove detaches (arm shows Tony's hand), flies to the crosshair target (homing 32 blocks), hits (8 + knockback), returns and re-attaches; RMB/repulsor on that hand unavailable while away.
- Passive: durability thresholds (each 1/14) drop one part as debris; lost arm → that hand's weapon ×0.5, lost leg → flight speed −10% each, lost helmet → helmet open forced, lost chest → Unibeam off.
- [ ] Tests `fistReturns`, `handUnavailableWhileAway`, `partsLostByThreshold`, `lostLegSlowsFlight`, `lostChestDisablesUnibeam` → implement → commit `feat(ironman): mark 42 rocket fist and part loss`.

### Task 12: Mark 15 — Sneaky camo

- Files: `mark/sig/Camo.java`, `client/ironman/mark/CamoRenderer.java` (refraction: render the player into a ripple shader using the screen copy — `res/shaders/post/` or core shader `ironman_camo`), Test `test/hero/ironman/CamoTest.java`.
- Modify: `core/hero/HeroDefinition.java` (`default boolean hiddenFromFocus(Player self) { return false; }`), Homelander `FocusTargets` candidate filter uses it (generic), scan uses `hiddenFromScan` (Stage 3).
- Signature 160 t, cooldown 400 t: semi-transparent with ripple; moving/attacking → ripple stronger, hover → almost invisible. Mobs farther than 8 blocks lose the player (re-cleared each 10 t), closer notice. First hit from camo ×2 then camo ends. Passive: hidden from focus/scan always in this mark, silent flight without flames; armor lower (spec table).
- [ ] Tests `lasts160`, `mobsBeyond8Lose`, `firstHitDoubleEndsCamo`, `hiddenFromFocusAndScan`, `silentFlightNoFlames` → implement → commit `feat(ironman): mark 15 camo`.

### Task 13: Mark 39 — Starboost

- Files: `mark/sig/Starboost.java`, Satsu `mark_39` booster part, Test `test/hero/ironman/StarboostTest.java`.
- Signature: booster on the back fires → instant throttle 1.0 + velocity = look × max speed in any direction incl. vertical take-off from the ground (starts flight), cost 15, cooldown 100 t. Passives via `MarkSpec` (speed ×1.35, sonic drain ×0.6, weapon ×0.85).
- [ ] Tests `boostSetsFullThrottle`, `verticalTakeoffFromGround`, `cost15` → implement → commit `feat(ironman): mark 39 starboost`.

### Task 14: Mark 17 — pulse Unibeam

- Files: `mark/sig/PulseUnibeam.java`, Test `test/hero/ironman/PulseUnibeamTest.java`.
- Signature: three short strong pulses (each 6 t beam, 12 damage-equivalent, 10 t apart), cost 35, **no overheat**, cooldown 200 t. Normal Unibeam in this mark: charge 10 t, overheat as usual and counts to the overdraft counter.
- [ ] Tests `threePulses`, `pulseNoOverheat`, `normalUnibeamCharge10`, `normalOverheatCounts` → implement → commit `feat(ironman): mark 17 pulse unibeam`.

### Task 15: War Machine Mk2 — shoulder gun

- Files: `mark/sig/ShoulderGun.java`, Satsu `war_machine torret` part (rotates toward the aim), Test `test/hero/ironman/ShoulderGunTest.java`.
- Signature on RMB (via MMB cycle) or slot: hold → burst at the crosshair target, 1 bullet / 2 t, 2 damage, spread small, 0.25 energy/t, no ammo; shell casings pixel VFX. Missiles up to 8 marks, ×1.4. Passives: flight ×0.8, sonic drain ×1.5.
- [ ] Tests `firesEvery2Ticks`, `drainsEnergy`, `eightMarks` → implement → commit `feat(ironman): war machine shoulder gun`.

### Task 16: Iron Heart Mk3 — slam

- Files: `mark/sig/Slam.java`, Test `test/hero/ironman/SlamTest.java`.
- Signature: on ground → jump 6 blocks then dive; in flight → dive; impact → strong ring shockwave (`HeroShockwave` big spec, r=7, `HeroDebris.erupt`), cooldown 240 t. Passives: armor highest, repulsor recoil and enemy knockback almost none (kb res 0.9, recoil ×0.2), shield ×1.5 (energy per hit ×0.67), flight ×0.8.
- [ ] Tests `groundSlamJumpsThenDives`, `impactOncePerUse`, `recoilReduced` → implement → commit `feat(ironman): iron heart slam`.

### Task 17: Lifecycle, sounds, lang, ship

- [ ] Lifecycle (spec §16): death → mark off (durability to roster), pod and empty suit fly away; relog → mark worn + durability + cooldowns saved, pod/empty suit gone; dimension → pod/empty suit fly away, mark kept; hero change → everything off; control → equip/exit/signature channels stop (Task 7 snap rule).
- [ ] Sounds (`tools/sfx/ironman_stage4.sh`): Veronica fall/impact/open/leave, part flight, plate clicks, exit, camo, booster, gun, slam, rocket fist, micro-laser; JARVIS lines for Veronica/mark break.
- [ ] Icons for Veronica + every signature; lang (all locales) for marks, menu, signatures.
- [ ] `NoHeroBranchTest`, build, checker, `runServer`; in-game list = spec §21 rows "Вероника…", "Выход из костюма…", "Весь урон…", "Фирменные…".
- [ ] Version bump, memory bank (mark pipeline, converters), `animation-system` skill (part flight/wrap), `CREDITS.md` (Sind parts), `SESSION.md`. PR «Железный человек — этап 4: Вероника и марки», body starts with «Для игрока».

---

## Review log

Plan reviewed against spec §3, §4.1, §4.3, §12, §13, §16, §19 and Stage 1–3 interfaces (reviewer pass, 2026-10-09). Fixed:
1. Snapshot had no room for the active mark and durability (other clients must render the right skin) → generic `variant` + `resource2` fields with a backward-decode test.
2. Swapping via Veronica: the old mark must become the empty suit (§12.6 "последний, из которого вышел") — made explicit.
3. "Back" part does not exist in Satsu `each_part` → back = shoulder parts + rear plate.
4. Empty suit damage was invented → invulnerable (spec does not define it).
5. Entering an empty suit from nano (§4.1) was undefined → nano retracts instantly; from another mark refused.
6. Model preview used three.js (no network in the sandbox) → Pillow orthographic preview tool.
