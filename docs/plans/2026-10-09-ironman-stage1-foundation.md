# Iron Man — Stage 1 (Foundation) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Playable Tony Stark: pick Iron Man on the race screen, Tony has a glowing reactor, puts on / takes off the nano Mark 50 with one key (wave from the reactor ~1 s), flies with an Iron Man flight profile (inertia, braking, hover stabilization, sonic on Ctrl, sonic ram, fly-by punch), spends and regenerates energy, lands softly / heavily (kneel + fist) / with an air strike (dive + LMB near the ground), sees an energy HUD and a restyled holographic panel. No weapons yet (Stage 2).

**Architecture:** `HeroId.IRON_MAN("ironman")` with `hero/ironman/` (server) and `client/ironman/` (client). Flight gets a generic per-player **flight profile** seam in `viltrumiteflight` (`FlightProfile` + `FlightProfiles.setResolver`, same pattern as `FlightPermissions.setPolicy`); no profile → the current flight code path is byte-for-byte the same. Core installs the resolver and asks `HeroDefinition.flightProfile(Player)` (default `null`). Suit, energy and landing logic are pure classes with JUnit tests; the hero only wires them. Per spec §3.2 the suit body is a **suit skin on the player model** (same for everyone, first-person arm uses it), revealed pixel by pixel by the nano wave; only 3D parts (helmet mask, later weapons/flaps/flames) are `BakedGeoModel`s attached to player model parts by a new generic `client/anim` player layer (approved extension of the animation core — update the skill in the same task). Mouse buttons reach heroes through a new generic seam `HeroDefinition.mouseAction(MouseButton, Player)` (default none → vanilla), reused by Stages 2–5.

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3 (`viltrumitecore`; added to `viltrumiteflight` in Task 4).

**Spec:** `docs/design/2026-10-09-ironman-design.md` (§ numbers below refer to it). Later stages (nano weapons + F block; helmet/JARVIS/scan/countermeasures; Veronica + marks; Hulkbuster; Legion) get their own plans.

Paths: `core/` = `viltrumitecore/src/main/java/dev/baranhan/viltrumitecore/`, `res/` = `viltrumitecore/src/main/resources/assets/viltrumitecore/`, `test/` = `viltrumitecore/src/test/java/dev/baranhan/viltrumitecore/`, `fl/` = `viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/`, `fltest/` = `viltrumiteflight/src/test/java/dev/baranhan/viltrumiteflight/`.

## Global Constraints

- `AGENTS.md`: server is authoritative (suit state, energy, flight profile values decided on the server, client only displays/predicts). Client classes only in `client` packages and `*.client.mixins.json`; dedicated server must start.
- `AGENTS.md` §7: no `HeroId.IRON_MAN` branch in shared code; add `HeroDefinition` default hooks. `NoHeroBranchTest` allowlist must not grow.
- `viltrumiteflight` must not depend on `viltrumitecore`. Only generic seams (`FlightProfile`, resolver) live in flight.
- Other heroes (Human, Homelander, Regulus): flight, landing, HUD and panel unchanged. Test for it in Task 4 (`nullProfileKeepsLegacyMotion`).
- `AGENTS.md` §8: no renames of registry ids, NBT keys, synced data, key-mapping ids. Append to enums (`HeroId`, `HeroAction`), never reorder.
- Animations/VFX only through `client/anim/`, model mixins at `setupAnim` TAIL with priorities, weight managers, `client/render/vfx/` managers on `RenderLevelStageEvent`, pixel style. Read `.agents/skills/animation-system/SKILL.md` before Tasks 8–10.
- All numbers live in `IronManRules` (values from spec: energy §, flight §, landings §). Tuning later only changes constants.
- Lang: every new key in every file of `res/lang/`.
- Binaries (png/ogg) go to `viltrumitecore/src/main/binassets/<path>.b64` when pushed through GitHub MCP. Geo/animation JSON are text and go straight to `res/geo/ironman/…`, `res/animations/ironman/…`.
- Assets: Satsu Mark 50 (`geo/armor_models/iron_man/full_body/main.geo.json`, `nano_armors/marks/head_mask/main.geo.json`, `nano_armors/marks/full_body_without_head_mask/main.geo.json`, textures `textures/models/iron_man/{no_light,light}/mark_50_0.png`), flames `geo/flames/below_flames_both_feet/flames_down.geo.json`, `geo/flames/upper_flames_both_arms/flames.geo.json`, `geo/flames/stabilizer_flames/main.geo.json`; Tony skin = Codex `textures/entity/hero/ironman.png`. Authors' permission obtained (Satsu, Sind) — credit in `CREDITS.md`.
- Commits in English conventional style; PR title in Russian, body starts with «Для игрока».

## Review Focus

1. Human/Homelander/Regulus flight is unchanged: `FlightProfiles` resolver returns `null` for them and the mixin takes the old branch (Task 4 test `nullProfileKeepsLegacyMotion`, Task 5 manual check).
2. Relog / death / dimension change during the nano wave → suit ends in a stable state (NANO or NONE), no half-drawn armor or stuck sound on any client (Task 3 tests `saveLoadMidDeployResolvesToTarget`, `cleanupDeathRemovesSuit`).
3. Energy hits 0 in sonic flight → profile switches to glide in the same tick, no sonic, player descends, no fall damage spike (Task 7 tests `emptyEnergyForcesGlide`, `glideBlocksSonic`).
4. Forged `HeroInputC2SPacket(SUIT)` while deploying, while dead or from a non-Iron-Man → ignored (Task 3 test `suitToggleIgnoredWhileTransitioning`, hero `canAct`).
5. Landing classification by impact speed, not only `fallDistance` (flight has no fall distance): heavy landing once per touchdown; air strike only from a dive with LMB near the ground and once per dive (Task 7 tests `airStrikeNeedsLmbInDive`, `airStrikeFiresOnce`).
6. Mouse seam: a non-Iron-Man player's LMB/RMB/MMB stay 100% vanilla; Iron Man without the suit — vanilla too (Task 6 test `noMouseClaimWithoutSuit`).

---

### Task 1: Hero id, skeleton, race button, Tony skin, reactor glow

**Files:**
- Modify: `core/hero/HeroId.java`, `core/hero/HeroAction.java`, `core/hero/HeroRegistry.java` (`registerDefaults`), `core/client/gui/RaceSelectionScreen.java`, `core/client/ViltrumiteCoreClient.java` (init call), `res/lang/*.json`, `CREDITS.md`
- Create: `core/hero/ironman/IronManHero.java`, `core/hero/ironman/IronManAbilities.java`, `core/client/ironman/IronManClient.java`, `core/client/ironman/ReactorGlowLayer.java`, `res/textures/entity/hero/ironman.png`, `res/textures/entity/hero/ironman_reactor_glow.png` (+ `.b64`)
- Test: `test/hero/ironman/IronManHeroTest.java`, extend `test/hero/HeroIdMigrationTest.java`

**Interfaces:**
- `HeroId.IRON_MAN("ironman")` appended last. `HeroAction` append `SUIT`.
- `IronManAbilities`: id `ironman:suit` (page 2, slot 5 = key B; page 2 order per spec §6.2: Scan, Countermeasures, Veronica, Helmet, **Suit**, Legion reserve). Later-stage ids are not registered yet. `defaultLoadout()` 18 slots: page 1 empty, page 2 index 4 = `ironman:suit`, flight slots unchanged.
- `IronManHero`: `allowsLegacyAbilities` false (no Viltrumite kit), `ownsAbility` only own ids, `allowsFlight(p)` = suit worn (Task 3; Stage 1 stub returns false), `heroInputSlots()` = `{ironman:suit}`, `heroActionFor("ironman:suit") = SUIT`, `hasAbilityPanel` true, `allowsAbilityPages` true, `meleeDamageFactor` 1.0 without suit.
- Client: `HeroSkins.register(IRON_MAN, …)` with Tony skin (check arm-width pixels → classic/slim), race button `become_ironman` sending `HeroChoiceC2SPacket("ironman")`, head preview with the shared head helper. `ReactorGlowLayer` = emissive (`RenderType.eyes`) chest dot for Tony, hidden while the suit skin covers it.

- [ ] Step 1: Tests: `ironManIsLastOrdinal`, `fromKeyIronman`, `ownsOnlyOwnIds`, `noLegacyKit`, `loadoutPutsSuitOnPage2SlotB`, `noFlightWithoutSuit`.
- [ ] Step 2: `./gradlew :viltrumitecore:test --tests '*IronMan*' --no-daemon` → FAIL.
- [ ] Step 3: Implement + register; lang keys `gui.viltrumitecore.race_selection.become_ironman`, `hero.viltrumitecore.ironman`, `ability.viltrumitecore.ironman_suit.name/.desc` in all locales.
- [ ] Step 4: Tests PASS; `./gradlew build --no-daemon` green; `python3 .agents/skills/add_hero/scripts/check_hero_resources.py --hero ironman` → 0 new problems.
- [ ] Step 5: Commit `feat(ironman): hero id, race button, tony skin`.

### Task 2: Rules and energy

**Files:**
- Create: `core/hero/ironman/IronManRules.java`, `core/hero/ironman/Energy.java`
- Test: `test/hero/ironman/EnergyTest.java`

**Interfaces:**
- `IronManRules` constants (ticks, per tick): `ENERGY_MAX=100`, `ENERGY_REGEN_PER_TICK=0.5f` (10/s), `ENERGY_REGEN_DELAY=20`, `DRAIN_HOVER=0.05f`, `DRAIN_CRUISE=0.05f` (1/s), `DRAIN_SONIC=0.3f` (6/s), `WEAPONS_UNLOCK=20`, `SUIT_DEPLOY_TICKS=20`, `SUIT_RETRACT_TICKS=20`, nano armor values (`NANO_ARMOR=14`, `NANO_TOUGHNESS=4`, `NANO_KNOCKBACK_RES=0.3` — tuned so Iron Man head-on is weaker than Homelander, spec §18), flight profile values (Task 5), landing / ram / fly-by values (Tasks 6–7). Stage 2+ costs from spec §5.2 (repulsor 2, charged 6, volley 10, Unibeam 35, missiles 15, nano weapon 8, shield 4/hit, War Machine gun 0.25/t) are added now so later plans only use them.
- `Energy`: `float value()`, `boolean spend(float)` (false if not enough, no partial), `void drain(float)` (continuous, clamps at 0), `void tick()` (regen after `ENERGY_REGEN_DELAY` ticks without spend/drain), `boolean empty()`, `boolean weaponsLocked()` (true from hitting 0 until `value ≥ WEAPONS_UNLOCK`), `save/load(CompoundTag)` key `Energy`.

- [ ] Step 1: Tests: `startsFull`, `spendRefusesWhenShort`, `drainClampsAtZero`, `regenWaits20Ticks`, `regensTenPerSecond`, `zeroLocksWeaponsUntil20`, `saveLoadRoundTrip`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): energy rules`.

### Task 3: Suit state machine, "Костюм" key, persistence, lifecycle

**Files:**
- Create: `core/hero/ironman/SuitState.java` (enum `NONE, DEPLOYING, NANO, RETRACTING`; later stages append `MARK, …` — never reorder), `core/hero/ironman/Suit.java`, `core/hero/ironman/IronManState.java`
- Modify: `IronManHero.java` (`handleInput`, `tick`, `save/load/cloneHeroState`, `cleanup`, `onDimensionChange`, `snapshot`, `canAct`, `allowsFlight`, `cancelsFallDamage`)
- Test: `test/hero/ironman/SuitTest.java`

**Interfaces:**
- `Suit`: `boolean toggle()` (NONE→DEPLOYING, NANO→RETRACTING; ignored while DEPLOYING/RETRACTING), `void tick()` (DEPLOYING→NANO after `SUIT_DEPLOY_TICKS`, RETRACTING→NONE after `SUIT_RETRACT_TICKS`), `void interrupt()` (control effect: DEPLOYING→NONE, RETRACTING→NONE), `float progress()` 0..1 (for the wave), `SuitState state()`, `boolean worn()` (NANO only), `boolean armored()` (NANO or RETRACTING — no fall damage while the wave goes back), `save/load` key `Suit` (stores target state: mid-deploy saves as NANO, mid-retract as NONE).
- `IronManState` = `Suit suit`, `Energy energy`, `LandingTracker landing` (Task 7); `of/ensure` through `HeroPlayer.viltrumitecore$getHeroState` like Homelander; `cloneHeroState` keeps suit + energy on respawn only per spec §16 (death → suit off, cooldowns kept).
- Armor while worn: attribute modifiers with fixed UUIDs (`NANO_ARMOR`, `NANO_TOUGHNESS`, `NANO_KNOCKBACK_RES`), added at NANO, removed at NONE and in every `cleanup`. Damage goes to Tony (no suit durability for nano, spec §4.2).
- Snapshot: `heroFlags` bit 0 = suit worn, bit 1 = deploying, bit 2 = retracting, bit 3 = glide; `resource` = energy ×10; `actionElapsed/actionLength` = wave progress; `resourceLocked` = `weaponsLocked`.
- Lifecycle (spec §16): `cleanup(DEATH)` → NONE; `cleanup(DISCONNECT)` → resolve to target state (saved by `save`); `cleanup(HERO_CHANGE)` → NONE + remove modifiers + stop flight; `onDimensionChange` → suit kept; control (`ControlManager` freeze/grab/anchor) → `canAct` false, `Suit.interrupt()` on an active wave.
- Undress stops flight (`FlightPermissions.resetModFlight`); the player falls, but `armored()` keeps fall damage off until the wave ends.

- [ ] Step 1: Tests: `toggleStartsDeploy`, `deployTakes20Ticks`, `suitToggleIgnoredWhileTransitioning`, `retractTakes20Ticks`, `interruptCancelsWave`, `saveLoadMidDeployResolvesToTarget`, `cleanupDeathRemovesSuit`, `heroChangeClearsAll`, `snapshotFlags`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): nano suit state and key`.

### Task 4: Flight profile seam (viltrumiteflight)

**Files:**
- Create: `fl/util/FlightProfile.java`, `fl/util/FlightProfiles.java`, `fl/util/FlightMotion.java`
- Modify: `viltrumiteflight/build.gradle` (JUnit block copied from `viltrumitecore/build.gradle:59–70`, without core deps)
- Test: `fltest/util/FlightMotionTest.java`

**Interfaces:**
- `record FlightProfile(float maxSpeed, float throttleUpPerTick, float throttleDownPerTick, float inertia, float turnRateSlowDeg, float turnRateFastDeg, float hoverDamping, boolean glide, float glideSink)`. `inertia` 0..1 = share of old velocity kept per tick; turn rate in degrees per tick, lerped from slow (throttle 0) to fast (throttle 1) — bigger radius at high speed (spec §7.2); `glide` = no thrust, throttle forced down, sink speed `glideSink`.
- `FlightProfiles.setResolver(Function<Player, FlightProfile>)`, `FlightProfiles.of(Player)` → profile or `null` (default resolver returns `null`).
- `FlightMotion` (pure; only `Vec3`): `static float throttle(float cur, boolean accelerating, FlightProfile p)`, `static Vec3 velocity(Vec3 oldVel, Vec3 look, float throttle, FlightProfile p)` — target = look × throttle × maxSpeed; direction rotated from old toward target by at most the turn rate; magnitude blended with `inertia`; glide → sink + slow forward drift. `static Vec3 hover(Vec3 oldVel, Vec3 input, FlightProfile p)` → damped, slow and precise strafe.
- Legacy path: `FlightMotion.legacyVelocity(look, throttle, maxSpeed)` = exact old formula; the mixin keeps its old lines for `null` (no refactor of the legacy branch).

- [ ] Step 1: Tests: `nullProfileKeepsLegacyMotion`, `accelReachesFullIn30Ticks` (throttle 0→1 with `1/30`), `brakeStopsIn20Ticks`, `turnRateShrinksWithSpeed`, `cannotTurnInstantlyAtFullSpeed`, `glideSinksAndHasNoThrust`, `hoverDampsDrift`.
- [ ] Step 2: `./gradlew :viltrumiteflight:test --no-daemon` → FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(flight): per-player flight profile seam`.

### Task 5: Wire the profile into flight

**Files:**
- Modify: `fl/mixin/PlayerEntityMixin.java` (tick TAIL throttle + velocity block, lines ~240–295), `fl/client/FlightInputHandler.java` (local throttle prediction uses `FlightMotion.throttle` when a profile exists), `core/hero/HeroDefinition.java` (`default FlightProfile flightProfile(Player p) { return null; }`), `core/hero/HeroRegistry.java` (`installFlightPolicy` also calls `FlightProfiles.setResolver(p -> get(p).flightProfile(p))`), `IronManHero.java`
- Test: `test/hero/ironman/IronManFlightTest.java`

**Interfaces:**
- Mixin: `FlightProfile p = FlightProfiles.of(player)`; `p == null` → old code untouched; else throttle and velocity through `FlightMotion`, HOVER through `FlightMotion.hover`. State thresholds (≥0.8 SONIC, >0 CRUISE), sonic boom sound and collision handling stay (spec §7.1, §7.3).
- Sonic only with Ctrl held: Iron Man `throttleDownPerTick` brings throttle below 0.8 within a few ticks of releasing Ctrl (existing accelerate key = Ctrl), so SONIC = "Ctrl held at full throttle".
- Client resolves the profile from the synced hero snapshot (verify `HeroRegistry.get(player)` on a remote client returns the snapshot hero; if not, read hero id + flags from `HeroPublicSnapshot`), so prediction matches the server.
- `IronManRules.profile(boolean glide)` → normal `(maxSpeed 6.0, up 1/30, down 1/20, inertia 0.85, turn 9°→2.5°, hoverDamping 0.8, glide false)`; glide `glide=true, glideSink 0.12`. Max speed below Homelander (9.0) on purpose (spec §18).

- [ ] Step 1: Tests: `ironManProfileValues`, `profileNullForOtherHeroes` (Human/Homelander/Regulus return null), `profileNullWithoutSuit`, `glideProfileWhenEmpty`.
- [ ] Step 2: FAIL → implement → PASS. `./gradlew build` green.
- [ ] Step 3: Manual: Homelander flies exactly like before (accel, sonic, boom, collision); Iron Man has inertia and wide high-speed turns.
- [ ] Step 4: Commit `feat(ironman): flight profile with inertia and turn radius`.

### Task 6: Hero mouse seam, fly-by punch, sonic ram

**Files:**
- Create: `core/hero/MouseButton.java` (enum `PRIMARY, SECONDARY, MIDDLE`), `core/network/packet/HeroMouseC2SPacket.java`, `core/client/hero/HeroMouseInput.java` (Forge `InputEvent.InteractionKeyMappingTriggered` HIGHEST + MMB key mapping `key.viltrumitecore.hero_tool` default MMB, same pattern as `client/regulus/RegulusInputPriority`), `core/hero/ironman/FlyBy.java`, `core/hero/ironman/SonicRam.java`
- Modify: `core/hero/HeroDefinition.java` (`default HeroAction mouseAction(MouseButton b, Player p) { return null; }` — non-null = hero claims the button, vanilla interaction is cancelled), `core/network/…` registration, `IronManHero.java`
- Test: `test/hero/ironman/FlyByTest.java`, `test/hero/HeroMouseSeamTest.java`

**Interfaces:**
- `HeroAction` append `PRIMARY_ATTACK`. Packet `(MouseButton, boolean down)`; server re-checks `mouseAction(...) != null` and `canAct`, else ignores.
- Iron Man Stage 1 claims only LMB while flying in the suit (`FlightState != NONE`). On ground in the suit LMB stays vanilla (melee with `meleeDamageFactor` from `IronManRules.NANO_MELEE_FACTOR`). Stage 2 widens the claims.
- `FlyBy.hit(ServerPlayer)`: ray/cone 3.5 blocks along velocity + look, damage scales with speed (`FLYBY_BASE + speed × FLYBY_PER_SPEED`), via `HeroDamage.route`, knockback along velocity; **player velocity untouched** (spec §7.4); swing animation + impact FX via `HeroImpactFx`.
- `SonicRam.tick(ServerPlayer)` in SONIC: entities in the swept box of this tick get damage + knockback once per 10 t each (`BoundedMap` of hit cooldowns); no block destruction beyond the existing flight collision.

- [ ] Step 1: Tests: `noMouseClaimWithoutSuit`, `otherHeroesNeverClaim`, `lmbClaimedOnlyInFlight`, `flyByDamageGrowsWithSpeed`, `flyByKeepsVelocity` (pure `FlyBy.resultVelocity`), `ramHitsEachTargetOncePer10Ticks`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): mouse seam, fly-by punch, sonic ram`.

### Task 7: Energy in flight, glide, landings, air strike

**Files:**
- Create: `core/hero/ironman/LandingTracker.java`, `core/hero/ironman/Landing.java` (enum `NONE, SOFT, HEAVY, AIR_STRIKE`), `core/hero/ironman/AirStrike.java`
- Modify: `IronManHero.java` (`tick`, `cancelsFallDamage`, `onLanded`, `handleInput(PRIMARY_ATTACK)`)
- Test: `test/hero/ironman/LandingTest.java`, `test/hero/ironman/FlightEnergyTest.java`

**Interfaces:**
- Drain per tick by `FlightState`: HOVER/CRUISE `DRAIN_CRUISE`, SONIC `DRAIN_SONIC`; NONE → regen. `Energy.empty()` → snapshot bit 3 glide → profile glide (spec §5.3).
- `LandingTracker.tick(boolean onGround, double speed, double vy, FlightState state, boolean strikeArmed)` → `Landing` once per touchdown. `vy ≤ -HEAVY_VY` (or speed ≥ `HEAVY_SPEED` while flying into the ground) → `HEAVY`; else `SOFT`. Re-arms only after ≥ 5 airborne ticks.
- Air strike (spec §8.6): in CRUISE/SONIC with pitch ≥ 35° down and ground ≤ `AIR_STRIKE_ARM_DIST` (raycast), LMB arms the strike (consumes this LMB instead of fly-by) → next touchdown is `AIR_STRIKE` instead of HEAVY; disarms after 30 t without touchdown.
- Effects: SOFT — repulsor dust puff, quiet thud, no pose; HEAVY — superhero kneel pose (one knee, fist in the ground, ~0.8 s, `HeroAction`-free client pose from snapshot flag bit 4) + `HeroShockwave.land` small `Landing` spec + `CameraShake` light + dust; AIR_STRIKE — fist grows nanites (client VFX), `HeroShockwave.land` big + `HeroDebris.erupt` crater + damage/knockback via `HeroDamage.route`, blocks only through `HeroDestruction.canDestroy` (mobGriefing). Fall damage always cancelled while `armored()`.

- [ ] Step 1: Tests: `hoverDrainsOnePerSecond`, `sonicDrainsSixPerSecond`, `emptyEnergyForcesGlide`, `glideBlocksSonic`, `softLandingSlow`, `heavyLandingFastFall`, `airStrikeNeedsLmbInDive`, `airStrikeFiresOnce`, `armDisarmsAfter30Ticks`, `rearmsAfterAirborne`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): flight energy, glide and landings`.

### Task 8: Suit skin with pixel reveal + geo parts layer (animation core extension)

**Files:**
- Create: `core/client/anim/render/PlayerGeoLayer.java`, `core/client/anim/render/PlayerBoneMap.java`, `core/client/anim/render/RevealMask.java`, `core/client/ironman/IronManSkinLayer.java`, `core/client/ironman/IronManPartsProvider.java`, `res/textures/entity/hero/ironman_mark_50{,_glow}.png` (+ `.b64`), `res/geo/ironman/mark_50/head_mask.geo.json`
- Modify: `core/client/ViltrumiteCoreClient.java` (layer registration on `EntityRenderersEvent.AddLayers` for both player skins), `core/client/ironman/IronManClient.java` (`HeroSkins` provider swaps hand texture to the suit when worn), `.agents/skills/animation-system/SKILL.md` (new capability: "geo parts on player parts")
- Test: `test/client/anim/PlayerBoneMapTest.java`, `test/client/anim/RevealMaskTest.java`

**Interfaces:**
- **Suit skin:** a 64×64 player-format skin of Mark 50. Source: Satsu `textures/models/iron_man/{no_light,light}/mark_50_0.png` are UV-mapped for `full_body/main.geo.json` (bones `armorHead/Body/RightArm/LeftArm/RightLeg/LeftLeg`, inflated). Step 1 checks whether that geo uses vanilla skin UV layout; if yes use as is, if not bake a player-layout skin with a small Python script (sample per face from the geo UVs) and commit the script under `tools/`. Glow pixels → `_glow` texture (`RenderType.eyes`).
- `IronManSkinLayer` draws the suit skin over Tony with `RevealMask` per pixel: pixel visible when its body-space distance from the reactor (chest centre) ≤ `progress × maxDistance` (deploy) or ≥ (retract); front edge = bright cyan hex rim. Implemented as a fragment `discard` in a small core shader (`res/shaders/core/ironman_reveal.{json,vsh,fsh}`) with uniforms `Progress`, `Origin`, or — if shaders are too heavy — by pre-baked 16 reveal frames. Decision recorded in the skill.
- `PlayerBoneMap`: `armorHead→head`, `armorBody→body`, `armorRightArm→rightArm`, `armorLeftArm→leftArm`, `armorRightLeg→rightLeg`, `armorLeftLeg→leftLeg` (child bones follow parents). Unknown top bone → skipped with one log warning.
- `PlayerGeoLayer` generic: `Provider { List<Part> parts(AbstractClientPlayer, float pt); }`, `Part(BakedGeoModel model, ResourceLocation tex, ResourceLocation glow, float reveal)`. Copies each `ModelPart` pose (after all `setupAnim` TAIL mixins) into the geo bones, renders base + glow. Mark 50 Stage 1 part: helmet `head_mask` — revealed last (spec §4.2 "helmet closes last"), cubes assemble along the bone by `RevealMask.cubeVisible`.
- First person: hand texture = suit skin when worn (via `HeroSkins` hand texture), reveal applied on the arm too.

- [ ] Step 1: UV check script on `full_body/main.geo.json` → decision (as is / bake).
- [ ] Step 2: Tests: `mapsAllSixArmorBones`, `unknownBoneSkipped`, `revealGrowsFromChest`, `fullProgressShowsAll`, `zeroShowsNone`, `helmetRevealsLast`.
- [ ] Step 3: FAIL → implement → PASS.
- [ ] Step 4: Visual check (third + first person, sneaking, swimming, Viltrumite flight poses): suit follows limbs, no z-fighting, glow on reactor/eyes, other players see the same.
- [ ] Step 5: Update `animation-system` skill. Commit `feat(anim): suit skin reveal and geo parts on player`.

### Task 9: Nano wave VFX and sounds

**Files:**
- Create: `core/client/ironman/NanoWaveVfx.java` (VFX manager on `RenderLevelStageEvent`), `core/client/ironman/ThrusterSound.java`, `res/sounds/ironman/{nano_deploy,nano_retract,thruster_loop,thruster_sonic,landing_soft,landing_heavy,air_strike,flyby_hit}.ogg` (+ `.b64`), sound registration in core sound registry + `res/sounds.json`, `tools/sfx/ironman_stage1.sh`
- Modify: `IronManSkinLayer.java` (reveal from snapshot `actionElapsed/actionLength`)

**Behaviour:**
- Deploy: reactor pulse → pixel nanite scales/hexes flowing outward on the wave front (spec §4.2) → skin revealed in ~1 s, helmet last; retract: reverse, pixels flow back into the reactor.
- Sounds: own sounds only, no vanilla (spec §17). Packs have none; JARVIS voice is Stage 3. Make them with `ffmpeg` synthesis + processing (filtered noise, sweeps, layered) by the committed script so they can be regenerated; list them in the PR for possible replacement.
- Thruster loop: client `TickableSoundInstance` while flying in the suit, pitch/volume by throttle, sonic layer ≥ 0.8; stops on any state change (no stuck loop).

- [ ] Step 1: Implement; verify: relog mid-wave on a second client → final state shown, no loop left.
- [ ] Step 2: Commit `feat(ironman): nano wave vfx and suit sounds`.

### Task 10: Thruster flames, hover and landing poses

**Files:**
- Create: `core/client/ironman/IronManPoser.java` (model mixin hook at `setupAnim` TAIL, priority after flight's `SlowFlyingModelMixin`), `core/client/ironman/ThrusterFlames.java`, `res/geo/ironman/flames/{feet,palms,stabilizer}.geo.json` (from Satsu `geo/flames/below_flames_both_feet/flames_down`, `upper_flames_both_arms/flames`, `stabilizer_flames/main`)
- Modify: `core/client/ironman/IronManClient.java`, `IronManPartsProvider.java`

**Behaviour:**
- HOVER (spec §7.5): palms down, arms slightly out and moving, feet stabilizing; small idle sway; feet + palm flames. CRUISE/SONIC: keep the existing flight pose (no flips), long feet flames, palm flames back along the body. Glide: no flames, arms out.
- HEAVY landing (snapshot bit 4): kneel + fist into the ground ~0.8 s, blended out by a weight manager.
- Flames are `PlayerGeoLayer` parts on arm/leg bones, emissive, flicker by time, length by throttle; ground light (dust + glow) under hover when ground ≤ 4 blocks. First person: palm flames on the arms in hover.

- [ ] Step 1: Implement through `animation-system` (weights, no direct `ModelPart` overrides outside the poser).
- [ ] Step 2: Visual check of all states + sprint/sneak transitions. Commit `feat(ironman): thruster flames, hover and landing poses`.

### Task 11: HUD and holographic panel

**Files:**
- Create: `core/client/ironman/IronManHud.java`, `core/client/hero/PanelStyle.java`, `core/client/hero/PanelStyles.java`, `res/textures/gui/ironman/{panel_frame,energy_bar}.png` (+ `.b64`), `res/textures/gui/ability/ironman/suit.png` (+ `.b64`)
- Modify: `core/client/mixin/ViltrumiteInGameHudMixin.java` (frame/background/cooldown fill through `PanelStyles.of(player)`; default style = current drawing code moved as is), `IronManClient.java` (register style), `res/lang/*.json`
- Test: `test/client/hero/PanelStylesTest.java`

**Interfaces:**
- `PanelStyle { void drawSlotBackground(...); void drawFrame(...); void drawCooldown(...); int accentColor(); }`, `PanelStyles.register(HeroId, PanelStyle)`, `PanelStyles.of(Player)` → default when not registered. Layout (6 vertical slots R,Y,Z,V,B,N + 3 flight slots, page indicator on Left Alt) does not move.
- Iron Man style (spec §15.1): holographic JARVIS frames, blue glow, scanline shimmer, corner brackets, cooldown = fill. Empty page-1 slots = dim "offline" cells.
- `IronManHud` (no helmet system yet → Stage 1 always shows): left — energy bar (cyan → amber < 30 → red blinking at 0, «ПЛАНИРОВАНИЕ» in glide) + 3 overheat pips (empty, wired in Stage 2); right, in flight — speed (blocks/s) and altitude. Hidden when the suit is off. Stage 3 moves this into the helmet HUD.
- Icon `suit.png` 16×16 in the panel art style.

- [ ] Step 1: Tests: `defaultStyleForUnregistered`, `registeredStyleReturned`.
- [ ] Step 2: FAIL → implement → PASS. Screenshot check: Homelander/Regulus panel identical to before.
- [ ] Step 3: Lang `hud.viltrumitecore.ironman.{energy,glide,speed,altitude}` all locales. Commit `feat(ironman): energy hud and holographic panel`.

### Task 12: Ship

- [ ] `NoHeroBranchTest` green, allowlist unchanged; `./gradlew build --no-daemon` green (JDK 17); `git diff --check` clean.
- [ ] `check_hero_resources.py --hero ironman` → 0 problems.
- [ ] Dedicated server: `./gradlew :viltrumitecore:runServer` reaches "Done" (accept EULA in run dir).
- [ ] In-game check if a client can run (race pick, reactor glow, suit on/off in 1st/3rd person, relog mid-wave, flight states, sonic ram, fly-by, glide at 0 energy, three landing types, Homelander regression); otherwise state it in the PR.
- [ ] Version bump: core minor, flight minor (new public seam). Update `.memory-bank/project.md` (flight profile seam, mouse seam, PlayerGeoLayer, PanelStyles), `add_hero` skill `hero-seam.md` (`flightProfile`, `mouseAction` hooks), `animation-system` skill, `CREDITS.md`, `SESSION.md` entry in Russian.
- [ ] PR: title «Железный человек — этап 1: Тони, нано-костюм, полёт», body starts with «Для игрока».

---

## Self-review

- Spec coverage (Stage 1 = §20.1 + acceptance §21 rows 1–5, energy row): Tony/race/reactor (T1), nano key/state/lifecycle (T3, §4.5 first two rows), wave + helmet last (T8–T9), flight profile accel/brake/hover/turn/sonic-on-Ctrl (T4–T5), sonic ram + fly-by (T6, §7.3–7.4), energy + glide (T2, T7), landings + air strike (T7, T10, §8.6), HUD/panel restyle (T11). Out of this plan: weapons, F block, nano damage visuals/repair (Stage 2), helmet/JARVIS (Stage 3), Veronica/marks (Stage 4), Hulkbuster (Stage 5).
- No hero branches: flight sees only `FlightProfile`; mouse only `mouseAction`; HUD panel only `PanelStyles`; skin/parts only providers.
- Risks: (a) geo layer after all pose mixins — verified in T8 step 4; (b) client profile prediction depends on snapshot sync — worst case 1 tick; (c) suit skin may need baking — T8 step 1 decides; (d) synthesized sounds may need replacing.

## Review log

Plan reviewed against spec §2–§8, §15–§21 and the code (reviewer pass, 2026-10-09). Fixed:
1. §3.2: body is a suit **skin** on the player model, not a full geo model → T8 rewritten (skin + pixel reveal; geo only for 3D parts; helmet last).
2. §8.6: air strike needs LMB near the ground in a dive (was automatic) → T7 + new mouse seam T6.
3. §7.3–7.4 sonic ram and fly-by punch were missing (acceptance row 4) → T6.
4. §8.6 heavy landing kneel pose was missing → T7/T10.
5. §4.1 Tony's glowing reactor was missing → T1.
6. `CleanupReason` has `DEATH, DISCONNECT, HERO_CHANGE` (no dimension) → T3 uses `onDimensionChange`; control interrupts the wave (§16).
7. §17 "own sounds, no stubs" → sounds generated by a committed script instead of "placeholders".
