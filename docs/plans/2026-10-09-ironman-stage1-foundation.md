# Iron Man — Stage 1 (Foundation) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Playable Tony Stark: pick Iron Man on the race screen, put on / take off the nano Mark 50 with one key (wave from the reactor ~1 s), fly with an Iron Man flight profile (inertia, braking, hover stabilization, sonic on Ctrl), spend and regenerate energy, land softly / heavily / with an air strike, see an energy HUD and a restyled holographic panel. No weapons yet (Stage 2).

**Architecture:** `HeroId.IRON_MAN("ironman")` with `hero/ironman/` (server) and `client/ironman/` (client). Flight gets a generic per-player **flight profile** seam in `viltrumiteflight` (`FlightProfile` + `FlightProfiles.setResolver`, same pattern as `FlightPermissions.setPolicy`); no profile → the current flight code path is byte-for-byte the same. Core installs the resolver and asks `HeroDefinition.flightProfile(Player)` (default `null`). Suit, energy and landing logic are pure classes with JUnit tests; the hero only wires them. The 3D Mark 50 is drawn by a new generic `client/anim` player layer that renders a `BakedGeoModel` on the player model parts (approved extension of the animation core — update the skill in the same task).

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3 (`viltrumitecore`; added to `viltrumiteflight` in Task 4).

**Spec:** `docs/design/2026-10-09-ironman-design.md` (§ numbers below refer to it). Later stages (nano weapons + F block; helmet/JARVIS/scan/countermeasures; Veronica + marks; Hulkbuster; Legion) get their own plans.

Paths: `core/` = `viltrumitecore/src/main/java/dev/baranhan/viltrumitecore/`, `res/` = `viltrumitecore/src/main/resources/assets/viltrumitecore/`, `test/` = `viltrumitecore/src/test/java/dev/baranhan/viltrumitecore/`, `fl/` = `viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/`, `fltest/` = `viltrumiteflight/src/test/java/dev/baranhan/viltrumiteflight/`.

## Global Constraints

- `AGENTS.md`: server is authoritative (suit state, energy, flight profile values decided on the server, client only displays/predicts). Client classes only in `client` packages and `*.client.mixins.json`; dedicated server must start.
- `AGENTS.md` §7: no `HeroId.IRON_MAN` branch in shared code; add `HeroDefinition` default hooks. `NoHeroBranchTest` allowlist must not grow.
- `viltrumiteflight` must not depend on `viltrumitecore`. Only generic seams (`FlightProfile`, resolver) live in flight.
- Other heroes (Human, Homelander, Regulus): flight, landing, HUD and panel unchanged. Test for it in Task 4 (`nullProfileKeepsLegacyMotion`).
- `AGENTS.md` §8: no renames of registry ids, NBT keys, synced data, key-mapping ids. Append to enums (`HeroId`, `HeroAction`), never reorder.
- Animations/VFX only through `client/anim/`, model mixins at `setupAnim` TAIL with priorities, weight managers, `client/render/vfx/` managers on `RenderLevelStageEvent`, pixel style. Read `.agents/skills/animation-system/SKILL.md` before Tasks 7–9.
- All numbers live in `IronManRules` (values from spec: energy §, flight §, landings §). Tuning later only changes constants.
- Lang: every new key in every file of `res/lang/`.
- Binaries (png/ogg) go to `viltrumitecore/src/main/binassets/<path>.b64` when pushed through GitHub MCP. Geo/animation JSON are text and go straight to `res/geo/ironman/…`, `res/animations/ironman/…`.
- Assets: Satsu Mark 50 (`geo/armor_models/iron_man/full_body/main.geo.json`, `nano_armors/marks/head_mask/main.geo.json`, `nano_armors/marks/full_body_without_head_mask/main.geo.json`, textures `textures/models/iron_man/{no_light,light}/mark_50_0.png`), flames `geo/flames/below_flames_both_feet/flames_down.geo.json`, `geo/flames/upper_flames_both_arms/flames.geo.json`, `geo/flames/stabilizer_flames/main.geo.json`; Tony skin = Codex `textures/entity/hero/ironman.png`. Authors' permission obtained (Satsu, Sind) — credit in `CREDITS.md`.
- Commits in English conventional style; PR title in Russian, body starts with «Для игрока».

## Review Focus

1. Human/Homelander/Regulus flight is unchanged: `FlightProfiles` resolver returns `null` for them and the mixin takes the old branch (Task 4 test `nullProfileKeepsLegacyMotion`, Task 5 manual check).
2. Relog / death / dimension change during the nano wave → suit ends in a stable state (NANO or NONE), no half-drawn armor or stuck sound on any client (Task 3 tests `saveLoadMidDeployResolvesToTarget`, `cleanupDeathRemovesSuit`).
3. Energy hits 0 in sonic flight → profile switches to glide in the same tick, no sonic, player descends, no fall damage spike (Task 6 tests `emptyEnergyForcesGlide`, `glideBlocksSonic`).
4. Forged `HeroInputC2SPacket(SUIT)` while deploying, while dead or from a non-Iron-Man → ignored (Task 3 test `suitToggleIgnoredWhileTransitioning`, hero `canAct`).
5. Landing classification by impact speed, not only `fallDistance` (flight has no fall distance): cruise into ground → air strike once, not every tick (Task 6 test `airStrikeFiresOnce`).

---

### Task 1: Hero id, skeleton, race button, Tony skin

**Files:**
- Modify: `core/hero/HeroId.java`, `core/hero/HeroAction.java`, `core/hero/HeroRegistry.java` (`registerDefaults`), `core/client/gui/RaceSelectionScreen.java`, `core/client/ViltrumiteCoreClient.java` (init call), `res/lang/*.json`, `CREDITS.md`
- Create: `core/hero/ironman/IronManHero.java`, `core/hero/ironman/IronManAbilities.java`, `core/client/ironman/IronManClient.java`, `res/textures/entity/hero/ironman.png` (+ `.b64`)
- Test: `test/hero/ironman/IronManHeroTest.java`, extend `test/hero/HeroIdMigrationTest.java`

**Interfaces:**
- `HeroId.IRON_MAN("ironman")` appended last. `HeroAction` append `SUIT`.
- `IronManAbilities`: ids `ironman:suit` (page 2, slot 5 = key B per spec panel order Scan, Countermeasures, Veronica, Helmet, **Suit**, Legion), placeholders for later stages are not registered yet. `defaultLoadout()` 18 slots: page 1 empty, page 2 index 4 = `ironman:suit`, flight slots unchanged.
- `IronManHero`: `allowsLegacyAbilities` false (no Viltrumite kit), `ownsAbility` only own ids, `allowsFlight(p)` = suit is `NANO`, `heroInputSlots()` = `{ironman:suit}`, `heroActionFor("ironman:suit") = SUIT`, `hasAbilityPanel` true, `allowsAbilityPages` true.
- Client: `HeroSkins.register(IRON_MAN, …)` with Tony skin (check arm width → classic/slim), race button `become_ironman` sending `HeroChoiceC2SPacket("ironman")`, head preview with the shared head helper.

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
- `IronManRules` constants (ticks, per tick): `ENERGY_MAX=100`, `ENERGY_REGEN_PER_TICK=0.5f` (10/s), `ENERGY_REGEN_DELAY=20`, `DRAIN_HOVER=0.05f`, `DRAIN_CRUISE=0.05f` (1/s), `DRAIN_SONIC=0.3f` (6/s), `WEAPONS_UNLOCK=20`, `SUIT_DEPLOY_TICKS=20`, `SUIT_RETRACT_TICKS=20`, flight profile values (Task 4), landing thresholds (Task 6). Stage 2+ costs (repulsor 2, Unibeam 35 …) added as constants now so later plans only use them.
- `Energy`: `float value()`, `boolean spend(float)` (false if not enough, no partial), `void drain(float)` (continuous, clamps at 0), `void tick()` (regen after `ENERGY_REGEN_DELAY` ticks without spend/drain), `boolean empty()`, `boolean weaponsLocked()` (true from hitting 0 until `value ≥ WEAPONS_UNLOCK`), `save/load(CompoundTag)` key `Energy`.

- [ ] Step 1: Tests: `startsFull`, `spendRefusesWhenShort`, `drainClampsAtZero`, `regenWaits20Ticks`, `regensTenPerSecond`, `zeroLocksWeaponsUntil20`, `saveLoadRoundTrip`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): energy rules`.

### Task 3: Suit state machine, "Костюм" key, persistence

**Files:**
- Create: `core/hero/ironman/SuitState.java` (enum `NONE, DEPLOYING, NANO, RETRACTING`), `core/hero/ironman/Suit.java`, `core/hero/ironman/IronManState.java`
- Modify: `IronManHero.java` (`handleInput`, `tick`, `save/load/cloneHeroState`, `cleanup`, `snapshot`, `canAct`)
- Test: `test/hero/ironman/SuitTest.java`

**Interfaces:**
- `Suit`: `boolean toggle()` (NONE→DEPLOYING, NANO→RETRACTING; ignored while DEPLOYING/RETRACTING), `void tick()` (DEPLOYING→NANO after `SUIT_DEPLOY_TICKS`, RETRACTING→NONE after `SUIT_RETRACT_TICKS`), `float progress()` 0..1 (for the wave), `SuitState state()`, `boolean worn()` (NANO only), `save/load` key `Suit` (stores target state: mid-deploy saves as NANO, mid-retract as NONE).
- `IronManState` = `Suit suit`, `Energy energy`, `LandingTracker landing` (Task 6); `of/ensure` through `HeroPlayer.viltrumitecore$getHeroState` like Homelander.
- Armor while worn: attribute modifiers with fixed UUIDs (armor, toughness, knockback resistance — values in `IronManRules`), added at NANO, removed at NONE and in `cleanup`.
- Snapshot: `heroFlags` bit 0 = suit worn, bit 1 = deploying, bit 2 = retracting, bit 3 = glide; `resource` = energy ×10; `actionElapsed/actionLength` = wave progress; `resourceLocked` = `weaponsLocked`.
- `cleanup(DEATH)` → NONE (nano returns to the reactor), `LOGOUT`/`DIMENSION` → resolve to target state. Undress stops flight (`FlightPermissions.resetModFlight`) and the player falls with normal rules (soft landing still from suit while RETRACTING).

- [ ] Step 1: Tests: `toggleStartsDeploy`, `deployTakes20Ticks`, `suitToggleIgnoredWhileTransitioning`, `retractTakes20Ticks`, `saveLoadMidDeployResolvesToTarget`, `cleanupDeathRemovesSuit`, `snapshotFlags`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): nano suit state and key`.

### Task 4: Flight profile seam (viltrumiteflight)

**Files:**
- Create: `fl/util/FlightProfile.java`, `fl/util/FlightProfiles.java`, `fl/util/FlightMotion.java`
- Modify: `viltrumiteflight/build.gradle` (JUnit block copied from `viltrumitecore/build.gradle:59–70`, without core deps)
- Test: `fltest/util/FlightMotionTest.java`

**Interfaces:**
- `record FlightProfile(float maxSpeed, float throttleUpPerTick, float throttleDownPerTick, float inertia, float turnRateSlowDeg, float turnRateFastDeg, float hoverDamping, boolean glide, float glideSink)`. `inertia` 0..1 = share of old velocity kept per tick; turn rate in degrees per tick, lerped from slow (throttle 0) to fast (throttle 1) — bigger radius at high speed; `glide` = no thrust, throttle forced down, sink speed `glideSink`.
- `FlightProfiles.setResolver(Function<Player, FlightProfile>)`, `FlightProfiles.of(Player)` → profile or `null` (default resolver returns `null`).
- `FlightMotion` (pure, no Minecraft types except `Vec3`): `static float throttle(float cur, boolean accelerating, FlightProfile p)`, `static Vec3 velocity(Vec3 oldVel, Vec3 look, float throttle, FlightProfile p)` — target = look × throttle × maxSpeed; direction rotated from old direction toward target by at most the turn rate; magnitude blended with `inertia`; glide → gravity-like sink + slow forward drift. `static Vec3 hover(Vec3 oldVel, FlightProfile p)` → damped drift.
- Legacy path: `FlightMotion.legacyVelocity(look, throttle, maxSpeed)` = exact old formula.

- [ ] Step 1: Tests: `nullProfileKeepsLegacyMotion` (legacy formula equals the old mixin math), `accelReachesFullIn30Ticks` (throttle 0→1 with `1/30`), `brakeStopsIn20Ticks`, `turnRateShrinksWithSpeed`, `cannotTurnInstantlyAtFullSpeed`, `glideSinksAndHasNoThrust`, `hoverDampsDrift`.
- [ ] Step 2: `./gradlew :viltrumiteflight:test --no-daemon` → FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(flight): per-player flight profile seam`.

### Task 5: Wire the profile into flight

**Files:**
- Modify: `fl/mixin/PlayerEntityMixin.java` (tick TAIL throttle + velocity block, lines ~240–295), `fl/client/FlightInputHandler.java` (local throttle prediction uses the same `FlightMotion.throttle`), `core/hero/HeroDefinition.java` (`default FlightProfile flightProfile(Player p) { return null; }`), `core/hero/HeroRegistry.java` (`installFlightPolicy` also calls `FlightProfiles.setResolver(p -> get(p).flightProfile(p))`), `IronManHero.java`
- Test: `test/hero/ironman/IronManFlightTest.java`

**Interfaces:**
- Mixin: `FlightProfile p = FlightProfiles.of(player)`; `p == null` → old code untouched; else throttle and velocity through `FlightMotion`, HOVER through `FlightMotion.hover`. State thresholds (≥0.8 SONIC, >0 CRUISE) and sonic boom sound stay.
- Sonic only with Ctrl held: Iron Man profile `throttleDownPerTick` brings throttle below 0.8 within a few ticks of releasing Ctrl (existing accelerate key = Ctrl), so SONIC = "Ctrl held at full throttle".
- Client resolves the profile from the synced hero snapshot (`HeroRegistry.get(player)` on client uses snapshot hero id + flags), so prediction matches server.
- `IronManRules.profile(boolean glide)` → normal `(maxSpeed 6.0, up 1/30, down 1/20, inertia 0.85, turn 9°→2.5°, hoverDamping 0.8, glide false)`; glide variant `glide=true, glideSink 0.12`.

- [ ] Step 1: Tests: `ironManProfileValues`, `profileNullForOtherHeroes` (Human/Homelander/Regulus definitions return null), `glideProfileWhenEmpty`.
- [ ] Step 2: FAIL → implement → PASS. `./gradlew build` green.
- [ ] Step 3: Manual: Homelander flies exactly like before (accel, sonic, boom, collision); Iron Man has inertia and wide high-speed turns.
- [ ] Step 4: Commit `feat(ironman): flight profile with inertia and turn radius`.

### Task 6: Energy in flight, glide, landings, air strike

**Files:**
- Create: `core/hero/ironman/LandingTracker.java`, `core/hero/ironman/Landing.java` (enum `NONE, SOFT, HEAVY, AIR_STRIKE`)
- Modify: `IronManHero.java` (`tick`, `cancelsFallDamage`, `onLanded`)
- Test: `test/hero/ironman/LandingTest.java`, `test/hero/ironman/FlightEnergyTest.java`

**Interfaces:**
- Drain per tick by `FlightState`: HOVER/CRUISE `DRAIN_CRUISE`, SONIC `DRAIN_SONIC`; NONE → regen. `Energy.empty()` → snapshot bit 3 glide → profile glide.
- `LandingTracker.tick(boolean onGround, double speed, double vy, FlightState state)` → `Landing` once per touchdown: speed in last airborne tick ≥ `AIR_STRIKE_SPEED` while CRUISE/SONIC aimed down → `AIR_STRIKE`; `vy ≤ -HEAVY_VY` → `HEAVY`; else `SOFT`. Re-arms only after the player was airborne ≥ 5 ticks.
- Effects: SOFT — small dust, quiet thud; HEAVY — crack ring `HeroShockwave.land` small radius + `CameraShake` light; AIR_STRIKE — `HeroShockwave.land` big + `HeroDebris` + damage/knockback to entities in radius via `HeroDamage.route`, blocks via `HeroDestruction.canDestroy` (mobGriefing). Fall damage always cancelled while suit worn or retracting.

- [ ] Step 1: Tests: `hoverDrainsOnePerSecond`, `sonicDrainsSixPerSecond`, `emptyEnergyForcesGlide`, `glideBlocksSonic`, `softLandingSlow`, `heavyLandingFastFall`, `airStrikeFromCruiseDive`, `airStrikeFiresOnce`, `rearmsAfterAirborne`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(ironman): flight energy, glide and landings`.

### Task 7: Geo armor layer on player bones (animation core extension)

**Files:**
- Create: `core/client/anim/render/PlayerGeoLayer.java`, `core/client/anim/render/PlayerBoneMap.java`, `core/client/anim/render/RevealMask.java`, `core/client/ironman/IronManArmorRenderer.java`, `res/geo/ironman/mark_50/{full_body,head_mask,body_without_mask}.geo.json`, `res/textures/models/ironman/mark_50{,_glow}.png` (+ `.b64`)
- Modify: `core/client/ViltrumiteCoreClient.java` (layer registration on `EntityRenderersEvent.AddLayers` for both skins), `.agents/skills/animation-system/SKILL.md` (new capability: "geo model on player parts")
- Test: `test/client/anim/PlayerBoneMapTest.java`, `test/client/anim/RevealMaskTest.java`

**Interfaces:**
- `PlayerBoneMap`: `armorHead→head`, `armorBody→body`, `armorRightArm→rightArm`, `armorLeftArm→leftArm`, `armorRightLeg→rightLeg`, `armorLeftLeg→leftLeg` (GeckoLib armor names; child bones follow parents). Unknown top bone → skipped with one log warning.
- `PlayerGeoLayer<T>` generic: `Provider { BakedGeoModel model(AbstractClientPlayer); ResourceLocation texture(...); ResourceLocation glow(...); float reveal(AbstractClientPlayer, float pt); Vec3 revealOrigin(); }` registered per hero. Copies each `ModelPart` pose (incl. flight/anim mixin poses, so it runs after `setupAnim` TAIL) into the geo bone, renders base + emissive glow (`RenderType.eyes`). Hides vanilla armor layer while active.
- `RevealMask.visible(cubeCenterDistance, progress, maxDistance)` — nano wave: cube visible when its distance from the reactor (chest centre) ≤ progress × maxDistance; retract = reverse. Edge cubes get a bright cyan rim.
- First person: arms drawn with the same geo bones through the existing first-person arm hook (`ItemInHandRenderer`/hand mixin) — right/left arm only.

- [ ] Step 1: Tests: `mapsAllSixArmorBones`, `unknownBoneSkipped`, `revealGrowsFromChest`, `fullProgressShowsAll`, `zeroShowsNone`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Visual check in game (third + first person, sneaking, swimming, Viltrumite flight poses): armor follows limbs, no z-fighting with skin, glow on reactor/eyes.
- [ ] Step 4: Update `animation-system` skill (new section + allowed usage). Commit `feat(anim): geo model layer on player parts`.

### Task 8: Nano wave VFX and sounds

**Files:**
- Create: `core/client/ironman/NanoWaveVfx.java` (VFX manager on `RenderLevelStageEvent`), `res/sounds/ironman/{nano_deploy,nano_retract,thruster_loop,landing_heavy}.ogg` (+ `.b64`), sound registration in core sound registry + `res/sounds.json`
- Modify: `IronManArmorRenderer.java` (reveal from snapshot `actionElapsed/actionLength`)

**Interfaces / behaviour:**
- Deploy: reactor glow pulse → pixel nanite particles flowing outward on the wave front → armor revealed by `RevealMask` in ~1 s; retract: reverse, particles flow back into the reactor. Pixel style, cyan-gold palette.
- Sounds: no sounds in the packs. Use Codex Iron Man sounds if present in the full Codex repo (`assets/superheroes/sounds/ironman/`); else make short placeholders with `ffmpeg` (synth sweep/noise) and list them in the PR as "to replace".
- Thruster loop: client `TickableSoundInstance` while flying in suit, pitch by throttle; stops on any state change (no stuck loop).

- [ ] Step 1: Implement; verify: relog mid-wave on second client → armor shown in the final state, no loop left.
- [ ] Step 2: Commit `feat(ironman): nano wave vfx and suit sounds`.

### Task 9: Thruster flames and hover pose

**Files:**
- Create: `core/client/ironman/IronManPoser.java` (model mixin hook at `setupAnim` TAIL, priority after flight's `SlowFlyingModelMixin`), `core/client/ironman/ThrusterFlames.java`, `res/geo/ironman/flames/{feet,palms,stabilizer}.geo.json`
- Modify: `core/client/ironman/IronManClient.java`

**Behaviour:**
- HOVER: arms down along the body, palms back/down, slight elbow bend, legs together; small idle sway; feet + palm flames (stabilizer geo). CRUISE/SONIC: keep the existing flight pose, feet flames long, palm flames along the body. Glide: no flames, arms a bit out.
- Flames rendered with the `PlayerGeoLayer` on `armorRightArm/LeftArm/RightLeg/LeftLeg` bones, emissive, flicker by time, length by throttle; ground light (dust + glow) under hover when ground ≤ 4 blocks.
- First person: palm flames visible on the arms in hover.

- [ ] Step 1: Implement through `animation-system` (weights, no direct `ModelPart` overrides outside the poser).
- [ ] Step 2: Visual check all states + sprint/sneak transitions. Commit `feat(ironman): thruster flames and hover pose`.

### Task 10: HUD and holographic panel

**Files:**
- Create: `core/client/ironman/IronManHud.java`, `core/client/hero/PanelStyle.java`, `core/client/hero/PanelStyles.java`, `res/textures/gui/ironman/{panel_frame,energy_bar}.png` (+ `.b64`), `res/textures/gui/ability/ironman/suit.png` (+ `.b64`)
- Modify: `core/client/mixin/ViltrumiteInGameHudMixin.java` (draw frame/background through `PanelStyles.of(player)`; default style = current drawing, unchanged), `core/client/hero/HeroSkins` or the client hero registry (register style), `res/lang/*.json`
- Test: `test/client/hero/PanelStylesTest.java`

**Interfaces:**
- `PanelStyle { void drawSlotBackground(...); void drawFrame(...); int accentColor(); }`, `PanelStyles.register(HeroId, PanelStyle)`, `PanelStyles.of(Player)` → default when not registered. Layout (6 vertical slots R,Y,Z,V,B,N + 3 flight slots, page indicator on Left Alt) does not move.
- Iron Man style: translucent cyan glass, thin bright edge, scanline shimmer, corner brackets; empty page-1 slots shown as dim "offline" cells (Stage 2 fills them).
- `IronManHud`: energy arc/bar (colour cyan → amber < 30 → red blinking at 0, «ГЛАЙД» label in glide), 3 overheat pips (all empty in Stage 1, wired to `heroFlags` later), speed (blocks/s) and altitude in flight. Hidden when suit is off.
- Icon `suit.png` 16×16 in the panel art style.

- [ ] Step 1: Tests: `defaultStyleForUnregistered`, `registeredStyleReturned`.
- [ ] Step 2: FAIL → implement → PASS. Screenshot check: Homelander panel identical to before.
- [ ] Step 3: Lang `hud.viltrumitecore.ironman.{energy,glide,speed,altitude}` all locales. Commit `feat(ironman): energy hud and holographic panel`.

### Task 11: Ship

- [ ] `NoHeroBranchTest` green, allowlist unchanged; `./gradlew build --no-daemon` green (JDK 17); `git diff --check` clean.
- [ ] `check_hero_resources.py --hero ironman` → 0 problems.
- [ ] Dedicated server: `./gradlew :viltrumitecore:runServer` reaches "Done" (accept EULA in run dir).
- [ ] In-game check if a client can run (race pick, suit on/off, relog mid-wave, flight states, glide at 0 energy, three landing types, Homelander regression); otherwise state it in the PR.
- [ ] Version bump: core minor, flight minor (new public seam). Update `.memory-bank/project.md` (flight profile seam, PlayerGeoLayer, PanelStyles), `add_hero` skill `hero-seam.md` (`flightProfile` hook), `animation-system` skill, `CREDITS.md`, `SESSION.md` entry in Russian.
- [ ] PR: title «Железный человек — этап 1: Тони, нано-костюм, полёт», body starts with «Для игрока».

---

## Self-review

- Spec coverage (Stage 1 = Tony + nano + flight profile + landings + energy + HUD/panel): Tony/race (T1), nano key/state (T3), wave VFX (T8), flight profile accel/brake/hover/turn/sonic-on-Ctrl (T4–T5), flames + hover pose (T9), energy + glide (T2, T6), landings + air strike (T6), HUD/panel restyle (T10). Weapons, F block, helmet, JARVIS, Veronica, marks, Hulkbuster — explicitly out of this plan.
- No hero branches: flight sees only `FlightProfile`; HUD panel sees only `PanelStyles`; armor layer sees only a provider.
- Risks: (a) the geo layer must run after all pose mixins — order verified in T7 step 3; (b) client prediction of the profile depends on snapshot sync — glide bit is in the snapshot, so a 1-tick mismatch is the worst case; (c) placeholder sounds may be needed — listed in the PR.
