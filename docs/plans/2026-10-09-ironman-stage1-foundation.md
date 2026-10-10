# Iron Man — Stage 1a (Foundation: hero, suit, energy, flight, seams) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> Stage 1 is split in two plans and two PRs: **1a** (this file: gameplay core and shared seams) and **1b** (`docs/plans/2026-10-09-ironman-stage1b-visuals.md`: suit skin reveal, flames, poses, sounds, HUD, panel). A jar is shipped only after 1b, because a hero without animations is not done (`AGENTS.md` §3.4).

**Goal:** Playable Tony Stark core: pick Iron Man on the race screen, put on / take off the nano Mark 50 with one key (~1 s state wave), mod flight granted while the suit is worn, Iron Man flight profile (inertia, braking, hover stabilization, sonic only while Ctrl is held, sonic ram, fly-by punch), energy spend/regen with glide at 0, soft / heavy / air-strike landings. Shared seams that every later stage needs: flight-grant contract, damage-layer contract, mouse/hold-input contract, `IronManFlags`.

**Architecture:** `HeroId.IRON_MAN("ironman")` with `hero/ironman/` (server) and `client/ironman/` (client). New **generic** seams, all as `HeroDefinition` default methods or shared helpers (no hero branches):
- **Flight grant** — `HeroDefinition.grantsFlightAbility(Player)` + shared `hero/HeroFlightGrant` that gives/revokes vanilla `mayfly` for heroes outside the legacy kit (Task 3).
- **Damage layers** — `HeroDefinition.absorbIncoming(...)`, `clampFinalDamage(...)`, `modifyOutgoingDamage(...)` wired into `LivingAttackEvent`, `LivingDamageEvent`, `LivingHurtEvent` **and** the direct `HeroDamage.applyCleanDamage` path used by `ControlManager` payouts (Task 4). The notify-only `onHurt` stays unchanged.
- **Flight profile** — `FlightProfile` + `FlightProfiles.setResolver` in `viltrumiteflight` (same pattern as `FlightPermissions.setPolicy`); `null` profile → the old flight code (Tasks 6–7).
- **Mouse and held input** — `HeroDefinition.mouseAction(MouseButton, Player)` + shared `hero/HeldInputs` (press-only gating, release always routed, forced release); the client `HeroMouseInput` replaces `client/regulus/RegulusInputPriority` (Task 8).

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3 (`viltrumitecore`; added to `viltrumiteflight` in Task 6).

**Spec:** `docs/design/2026-10-09-ironman-design.md` (§ numbers below refer to it). Later stages get their own plans (`docs/plans/2026-10-09-ironman-stage{2..6}-*.md`).

Paths: `core/` = `viltrumitecore/src/main/java/dev/baranhan/viltrumitecore/`, `res/` = `viltrumitecore/src/main/resources/assets/viltrumitecore/`, `test/` = `viltrumitecore/src/test/java/dev/baranhan/viltrumitecore/`, `fl/` = `viltrumiteflight/src/main/java/dev/baranhan/viltrumiteflight/`, `fltest/` = `viltrumiteflight/src/test/java/dev/baranhan/viltrumiteflight/`.

## Global Constraints

- `AGENTS.md` §8: gameplay is server-authoritative (suit state, energy, glide, profile parameters, damage). Exception that already exists in the code: **local player movement is computed on the client** (the flight mixin sets `deltaMovement` on both sides, the server does not own a `ServerPlayer`'s velocity). So inertia and `FlightMotion` run on the client; the server decides only states and parameters. Client classes only in `client` packages and `*.client.mixins.json`; dedicated server must start.
- `AGENTS.md` §7: no `HeroId.IRON_MAN` branch in shared code; add `HeroDefinition` default hooks. `NoHeroBranchTest` allowlist must not grow (Task 8 shrinks it by one file).
- `viltrumiteflight` must not depend on `viltrumitecore`. Only generic seams (`FlightProfile`, resolver) live in flight.
- Other heroes (Human, Homelander, Regulus): flight, landing, damage, mouse input unchanged. Proof: Task 6 test `nullProfileKeepsLegacyMotion` (the mixin's legacy branch calls the tested `FlightMotion.legacyVelocity`), Task 4 tests `noLayersMeansVanilla`, Task 8 test `otherHeroesNeverClaim`, manual regression list in Task 10.
- `AGENTS.md` §8: no renames of registry ids, NBT keys, synced data, key-mapping ids. Append to enums (`HeroId`, `HeroAction`), never reorder.
- All numbers live in `IronManRules` (spec §5, §7, §8.6, §18). Tuning later only changes constants.
- Lang: every new key in every file of `res/lang/`.
- Assets: the user's archive `IronMan_Hulkbuster_models.zip` (Satsu Iron Man addon + Sind Iron Man pack, sent by the user in chat) is **not** in the repository and must not be committed raw. Only the files we use, after conversion, are committed under `res/` (binaries as `viltrumitecore/src/main/binassets/<path>.b64`). `tools/assets/ironman_sources.md` lists for every committed asset: source archive name, original path, author, conversion step. Authors' permissions (Satsu, Sind) are recorded in a new `CREDITS.md` (Create) with author, what is used, date of permission; the permission text the user received is stored in `docs/licenses/ironman/` (open question: the user must provide it).
- Commits in English conventional style; PR title in Russian, body starts with «Для игрока».

## Review Focus

1. Human/Homelander/Regulus flight is unchanged: resolver returns `null`, the mixin takes the old branch (Task 6 test `nullProfileKeepsLegacyMotion`, Task 7 manual check).
2. Flight grant: a new survival player picks Iron Man, puts on nano (never was Homelander) and takes off; relog restores; suit off / hero change leaves no extra `mayfly`; creative unchanged (Task 3 tests + manual).
3. Damage layers: no layers → exactly vanilla; deferred `ControlManager` payout goes through the layers once; final clamp works on both paths (Task 4 tests).
4. Held input: release always reaches the active action; a repeated press packet does not restart it; screen open / suit off / control / hero change force a release (Task 8 tests).
5. Ctrl/W/Shift: sonic only while Ctrl is held; speed lock cannot hold ≥ 0.8; glide beats speed lock (Task 6–7 tests `speedLockCapsBelowSonic`, `glideOverridesSpeedLock`).
6. Landing classification by impact speed (flight has no fall distance), once per touchdown; air strike only from a dive with LMB near the ground (Task 9 tests).

---

### Task 1: Hero id, skeleton, race button, Tony skin

**Files:**
- Modify: `core/hero/HeroId.java`, `core/hero/HeroAction.java`, `core/hero/HeroRegistry.java` (`registerDefaults`), `core/client/gui/RaceSelectionScreen.java`, `core/client/ViltrumiteCoreClient.java` (init call), `res/lang/*.json`
- Create: `core/hero/ironman/IronManHero.java`, `core/hero/ironman/IronManAbilities.java`, `core/client/ironman/IronManClient.java`, `res/textures/entity/hero/ironman.png` (+ `.b64`, from Codex `assets/superheroes/textures/entity/hero/ironman.png`), `CREDITS.md`, `tools/assets/ironman_sources.md`
- Test: `test/hero/ironman/IronManHeroTest.java`, extend `test/hero/HeroIdMigrationTest.java`

**Interfaces:**
- `HeroId.IRON_MAN("ironman")` appended last. `HeroAction` append `SUIT`.
- `IronManAbilities`: id `ironman:suit` (page 2, slot 5 = key B; page 2 order per spec §6.2: Scan, Countermeasures, Veronica, Helmet, **Suit**, Legion reserve). `defaultLoadout()` 18 slots: page 1 empty, page 2 index 4 = `ironman:suit`, flight slots unchanged.
- `IronManHero`: `allowsLegacyAbilities` false (no Viltrumite kit, so `PlayerStatsMixin` never grants `mayfly` — Task 3 solves flight), `ownsAbility` only own ids, `allowsFlight(p)` = suit worn (Task 5), `heroInputSlots()` = `{ironman:suit}`, `heroActionFor("ironman:suit") = SUIT`, `hasAbilityPanel` true, `allowsAbilityPages` true.
- Client: `HeroSkins.register(IRON_MAN, …)` with Tony skin (check arm-width pixels → classic/slim), race button `become_ironman` sending `HeroChoiceC2SPacket("ironman")`, head preview with the shared head helper.

- [ ] Step 1: Tests: `ironManIsLastOrdinal`, `fromKeyIronman`, `ownsOnlyOwnIds`, `noLegacyKit`, `loadoutPutsSuitOnPage2SlotB`, `noFlightWithoutSuit`.
- [ ] Step 2: `./gradlew :viltrumitecore:test --tests '*IronMan*' --no-daemon` → FAIL.
- [ ] Step 3: Implement + register; lang keys `gui.viltrumitecore.race_selection.become_ironman`, `hero.viltrumitecore.ironman`, `ability.viltrumitecore.ironman_suit.name/.desc` in all locales. `CREDITS.md`: Codex skin (own project), Satsu, Sind sections.
- [ ] Step 4: Tests PASS; `./gradlew build --no-daemon` green; `python3 .agents/skills/add_hero/scripts/check_hero_resources.py --hero ironman` → 0 new problems.
- [ ] Step 5: Commit `feat(ironman): hero id, race button, tony skin`.

### Task 2: Flags, rules and energy

**Files:**
- Create: `core/hero/ironman/IronManFlags.java`, `core/hero/ironman/IronManRules.java`, `core/hero/ironman/Energy.java`
- Test: `test/hero/ironman/IronManFlagsTest.java`, `test/hero/ironman/EnergyTest.java`

**Interfaces:**
- `IronManFlags` = the single bit layout of `HeroPublicSnapshot.heroFlags` for all stages: 0 suit worn, 1 deploying, 2 retracting, 3 glide, 4 heavy-landing pose (Stage 1); 5–6 overheat count, 7 overheat lock, 8–10 RMB tool, 11 shield up, 12–14 damaged zones, 15–16 Unibeam phase, 17 missile flaps, 18 overdraft sputter (Stage 2); 19 helmet closed, 20 scan active (Stage 3); 21 mark camo, 22–23 equip/exit phase (Stage 4); 24–26 Hulkbuster phase (Stage 5); 27–31 reserved. Static `get/set(int flags, Field)` helpers.
- `IronManRules` constants (ticks, per tick): `ENERGY_MAX=100`, `ENERGY_REGEN_PER_TICK=0.5f` (10/s), `ENERGY_REGEN_DELAY=20`, `DRAIN_HOVER=0.05f`, `DRAIN_CRUISE=0.05f` (1/s), `DRAIN_SONIC=0.3f` (6/s), `WEAPONS_UNLOCK=20`, `SUIT_DEPLOY_TICKS=20`, `SUIT_RETRACT_TICKS=20`, `NANO_ARMOR=14`, `NANO_TOUGHNESS=4`, `NANO_KNOCKBACK_RES=0.3`, `NANO_MELEE_FACTOR=1.5f` (nano punch on the ground), flight profile values (Task 7), landing / ram / fly-by values (Tasks 8–9). Stage 2+ energy costs from spec §5.2 (repulsor 2, charged 6, volley 10, Unibeam 35, missiles 15, nano weapon 8, shield 4/hit, War Machine gun 0.25/t) are added now so later plans only use them.
- `Energy`: `float value()`, `boolean spend(float)` (false if not enough, no partial), `void drain(float)` (continuous, clamps at 0), `void tick()` (regen after `ENERGY_REGEN_DELAY` ticks without spend/drain), `boolean empty()`, `boolean weaponsLocked()` (true from hitting 0 until `value ≥ WEAPONS_UNLOCK`), `save/load(CompoundTag)` key `Energy`.

- [ ] Step 1: Tests: `flagsDoNotOverlap`, `flagsRoundTrip`, `startsFull`, `spendRefusesWhenShort`, `drainClampsAtZero`, `regenWaits20Ticks`, `regensTenPerSecond`, `zeroLocksWeaponsUntil20`, `saveLoadRoundTrip`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): flags, rules and energy`.

### Task 3: Flight-grant seam (vanilla `mayfly` for non-legacy heroes)

Fact: `HeroRegistry.installFlightPolicy` (`HeroRegistry.java:29-38`) requires `allowsFlight && getAbilities().mayfly && !preventsFlight`. Only `PlayerStatsMixin.onTick` (`PlayerStatsMixin.java:151-159`) sets `mayfly`, and only when `isViltrumite()` (legacy kit). `FlightToggleC2SPacket.java:36-43` refuses take-off without the policy. So Iron Man cannot fly without a new grant.

**Files:**
- Create: `core/hero/HeroFlightGrant.java` (shared server helper), `core/hero/FlightGrantDecision.java` (pure)
- Modify: `core/hero/HeroDefinition.java` (`default boolean grantsFlightAbility(Player p) { return false; }`), `core/hero/HeroRegistry.java` (call `HeroFlightGrant.sync` from the hero tick loop, on login, after `changeHero`, after respawn), `core/hero/HeroEvents.java` (login/respawn/hero-change hooks, `PlayerEvent.PlayerChangeGameModeEvent` → re-sync next tick), `core/mixin/PlayerHeroMixin.java` (NBT key `HeroGrantedMayfly`, boolean, new key — no rename)
- Test: `test/hero/FlightGrantDecisionTest.java`

**Interfaces:**
- `FlightGrantDecision.decide(boolean wants, boolean grantedByUs, boolean mayflyNow, boolean creativeOrSpectator)` → `GRANT | REVOKE | KEEP`. Rules: creative/spectator → `KEEP` always (vanilla owns `mayfly`; we only clear our marker); `wants && !mayflyNow` → `GRANT` (set `mayfly`, mark `grantedByUs`); `!wants && grantedByUs` → `REVOKE` (clear `mayfly`, `flying=false`, `FlightPermissions.resetModFlight`, clear marker); otherwise `KEEP`. Never revoke a `mayfly` we did not grant (other mods, legacy kit).
- `HeroFlightGrant.sync(ServerPlayer)`: applies the decision, calls `onUpdateAbilities()` only on change (client sync). Legacy kit path in `PlayerStatsMixin` stays as is.
- Control: the grant does not bypass `ControlManager.preventsFlight` (policy still checks it).
- `IronManHero.grantsFlightAbility(p)` = suit worn (Task 5).

- [ ] Step 1: Tests: `grantsWhenWanted`, `revokesOnlyOwnGrant`, `creativeKeeps`, `spectatorKeeps`, `noDoubleGrant`, `legacyMayflyUntouched`.
- [ ] Step 2: FAIL → implement → PASS.
- [ ] Step 3: Manual (Task 10 list): new survival world → Iron Man → nano → double jump takes off; relog in suit → can fly; suit off → `mayfly` false; change hero to Human → false; creative → flight as before; anchored by Regulus → no take-off.
- [ ] Step 4: Commit `feat(hero): flight grant seam for non-legacy heroes`.

### Task 4: Damage-layer seam

Facts: `HeroDefinition.onHurt(...)` returns `void`; `HeroEvents.onLivingHurt` (`HeroEvents.java:236-242`) only notifies. `HeroDamage.applyCleanDamage` (`HeroDamage.java:182-203`) sets HP directly, and `ControlManager.release` (`ControlManager.java:223-248`) pays queued damage through it — both bypass Forge damage events.

**Files:**
- Create: `core/hero/HeroDamageLayers.java` (shared pipeline), `core/hero/DamageAbsorb.java` (record `(boolean absorbed, float passOn)`)
- Modify: `core/hero/HeroDefinition.java` (three new defaults, see below), `core/hero/HeroEvents.java` (`LivingAttackEvent` HIGH, `LivingDamageEvent` LOWEST, `LivingHurtEvent` for outgoing), `core/hero/HeroDamage.java` (`applyCleanDamage` runs absorb + clamp for `ServerPlayer` targets and outgoing for player attackers)
- Test: `test/hero/HeroDamageLayersTest.java`

**Interfaces:**
- `default DamageAbsorb absorbIncoming(ServerPlayer self, DamageSource src, float raw) { return DamageAbsorb.PASS; }` — runs in `LivingAttackEvent` (before armor, knockback and hurt animation). `absorbed=true` → event cancelled: no HP loss, no vanilla knockback, no hurt flash (the hero plays its own hit FX/knockback). Layers spend durability/energy here.
- `default float clampFinalDamage(ServerPlayer self, DamageSource src, float afterArmor) { return afterArmor; }` — runs in `LivingDamageEvent` (after armor/enchant/resistance) → `event.setAmount`. Used for HP floors (Stage 2 core explosion).
- `default float modifyOutgoingDamage(ServerPlayer attacker, LivingEntity target, DamageSource src, float amount) { return amount; }` — runs in `LivingHurtEvent` when `src.getEntity()` is a `ServerPlayer` (Stage 4 camo ×2, mark `weaponMul`).
- Direct path: `applyCleanDamage(target, src, amount, flinch)` for a `ServerPlayer` target → `absorbIncoming` (absorbed → return) → `clampFinalDamage` → set HP. For a `ServerPlayer` attacker → `modifyOutgoingDamage` first. No Forge event fires on this path, so nothing is charged twice.
- Fixed order (documented in `HeroDamageLayers` javadoc and used by every hero): **control** (`HeroDamage.route` queues while anchored; queued hits are not charged to any layer) → **shield** → **Hulkbuster** → **mark** → **inner suit / nano armor** (vanilla armor attributes) → **Tony's HP** (`clampFinalDamage`). The hero implements shield→Hulkbuster→mark inside its own `absorbIncoming`; a breaking hit is absorbed fully and does not spill to the next layer. A deferred payout enters at the shield step once.
- `onHurt` stays notify-only and unchanged.

- [ ] Step 1: Tests (pure pipeline with fake hero layers): `noLayersMeansVanilla`, `absorbedCancelsWholeHit`, `breakingHitDoesNotSpill`, `clampAppliesAfterArmor`, `cleanPathRunsLayersOnce`, `deferredPayoutChargedOnce`, `outgoingModifiedOnce`, `otherSourcesStillLethal`, `voidNeverAbsorbed` (`bypassesInvulnerability` sources skip layers).
- [ ] Step 2: FAIL → implement → PASS. Regulus Lion's Heart (`HeroDamage.decide`) and Homelander unchanged (their tests green).
- [ ] Step 3: Commit `feat(hero): damage layer seam for incoming, final and outgoing damage`.

### Task 5: Suit state machine, "Костюм" key, persistence, lifecycle

**Files:**
- Create: `core/hero/ironman/SuitState.java` (enum `NONE, DEPLOYING, NANO, RETRACTING`; later stages append — never reorder), `core/hero/ironman/Suit.java`, `core/hero/ironman/IronManState.java`
- Modify: `IronManHero.java` (`handleInput`, `tick`, `save/load/cloneHeroState`, `cleanup`, `onDimensionChange`, `snapshot`, `canAct`, `allowsFlight`, `grantsFlightAbility`, `cancelsFallDamage`)
- Test: `test/hero/ironman/SuitTest.java`

**Interfaces:**
- `Suit`: `boolean toggle()` (NONE→DEPLOYING, NANO→RETRACTING; ignored while DEPLOYING/RETRACTING), `void tick()` (DEPLOYING→NANO after `SUIT_DEPLOY_TICKS`, RETRACTING→NONE after `SUIT_RETRACT_TICKS`), `void interrupt()` (control: DEPLOYING→NONE, RETRACTING→NONE), `float progress()` 0..1, `SuitState state()`, `boolean worn()` (NANO only), `boolean armored()` (NANO or RETRACTING — no fall damage while the wave goes back), `save/load` key `Suit` (mid-deploy saves as NANO, mid-retract as NONE).
- `IronManState` = `Suit suit`, `Energy energy`, `LandingTracker landing` (Task 9), `HeldInputs held` (Task 8); `of/ensure` through `HeroPlayer.viltrumitecore$getHeroState` like Homelander; `cloneHeroState` on death → suit off, energy full, cooldowns kept (spec §16).
- Armor while worn: attribute modifiers with fixed UUIDs (`NANO_ARMOR`, `NANO_TOUGHNESS`, `NANO_KNOCKBACK_RES`), added at NANO, removed at NONE and in every `cleanup`. Damage goes to Tony (no nano durability, spec §4.2).
- Snapshot through `IronManFlags`: worn, deploying, retracting, glide; `resource` = energy ×10; `actionElapsed/actionLength` = wave progress; `resourceLocked` = `weaponsLocked`.
- Lifecycle (spec §16; `CleanupReason` = `DEATH, DISCONNECT, HERO_CHANGE`): `DEATH` → NONE; `DISCONNECT` → resolve to target state (saved); `HERO_CHANGE` → NONE + modifiers removed + flight grant revoked (Task 3); `onDimensionChange` → suit kept; control (`canAct` false) → `Suit.interrupt()` on an active wave + `HeldInputs.releaseAll`.
- Undress stops flight via the grant revoke; `armored()` keeps fall damage off until the wave ends.

- [ ] Step 1: Tests: `toggleStartsDeploy`, `deployTakes20Ticks`, `suitToggleIgnoredWhileTransitioning`, `retractTakes20Ticks`, `interruptCancelsWave`, `saveLoadMidDeployResolvesToTarget`, `cleanupDeathRemovesSuit`, `heroChangeClearsAll`, `grantFollowsSuit`, `snapshotFlags`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): nano suit state and key`.

### Task 6: Flight profile seam (viltrumiteflight)

**Files:**
- Create: `fl/util/FlightProfile.java`, `fl/util/FlightProfiles.java`, `fl/util/FlightMotion.java`
- Modify: `viltrumiteflight/build.gradle` (JUnit block copied from `viltrumitecore/build.gradle:59-70`, without core deps), `fl/mixin/PlayerEntityMixin.java` (legacy velocity line ~293 replaced by a call to `FlightMotion.legacyVelocity` — same math, so the test covers the real code)
- Test: `fltest/util/FlightMotionTest.java`

**Input model (current code, kept):** W/A/S/D = direction in HOVER (`HoverInputC2SPacket`), **Ctrl** (`options.keySprint`, `FlightInputHandler.java:87`) = thrust: throttle rises while held, falls ×2 when released; **Shift** = speed lock (`FlightSpeedLockC2SPacket`): while locked the mixin does not change throttle (`PlayerEntityMixin.java:240-268`). State: throttle ≥ 0.8 SONIC, > 0 CRUISE, else HOVER. Sonic boom sound plays when throttle crosses **0.6** (as now).

**Iron Man table (profile present):**

| Situation | Result |
|---|---|
| Ctrl held | throttle → 1 at `throttleUpPerTick`; ≥ 0.8 = SONIC |
| Ctrl released, no lock | throttle → 0 at `throttleDownPerTick` (~1 s brake, spec §7.2 "после отпускания Ctrl") |
| Shift lock | throttle frozen but capped at `lockCap` = 0.79 → lock can hold CRUISE, never SONIC |
| Glide (energy 0) | lock ignored, throttle forced → 0, no thrust, sink `glideSink` |
| W released (HOVER) | drift damped by `hoverDamping` (stable hover) |

**Interfaces:**
- `record FlightProfile(float speedMul, float throttleUpPerTick, float throttleDownPerTick, float lockCap, float inertia, float turnRateSlowDeg, float turnRateFastDeg, float hoverDamping, boolean glide, float glideSink)`. **`speedMul` multiplies the player's synced `getMaxFlightSpeed()`** (server config `ViltrumiteConfig.maxFlightSpeed`, set per player through `FlightConfigSyncC2SPacket`), so operators keep control of speed.
- `FlightProfiles.setResolver(Function<Player, FlightProfile>)`, `FlightProfiles.of(Player)` → profile or `null` (default resolver returns `null`).
- `FlightMotion` (pure; only `Vec3`): `throttle(float cur, boolean ctrl, boolean locked, FlightProfile p)`, `velocity(Vec3 oldVel, Vec3 look, float throttle, float maxSpeed, FlightProfile p)` (direction rotated toward look by at most the turn rate, lerped slow→fast by throttle; magnitude blended with `inertia`; glide → sink + slow drift), `hover(Vec3 oldVel, Vec3 input, FlightProfile p)`, `legacyVelocity(look, throttle, maxSpeed)` (exact old formula).

- [ ] Step 1: Tests: `nullProfileKeepsLegacyMotion`, `accelReachesFullIn30Ticks`, `brakeStopsIn20Ticks`, `speedLockCapsBelowSonic`, `glideOverridesSpeedLock`, `turnRateShrinksWithSpeed`, `cannotTurnInstantlyAtFullSpeed`, `glideSinksAndHasNoThrust`, `hoverDampsDrift`, `speedScalesWithPlayerMax`.
- [ ] Step 2: `./gradlew :viltrumiteflight:test --no-daemon` → FAIL → implement → PASS.
- [ ] Step 3: Commit `feat(flight): per-player flight profile seam`.

### Task 7: Wire the profile into flight

**Files:**
- Modify: `fl/mixin/PlayerEntityMixin.java` (tick TAIL throttle + velocity block, lines ~240-295), `fl/client/FlightInputHandler.java` (local throttle prediction uses `FlightMotion.throttle` when a profile exists), `core/hero/HeroDefinition.java` (`default FlightProfile flightProfile(Player p) { return null; }`), `core/hero/HeroRegistry.java` (`installFlightPolicy` also calls `FlightProfiles.setResolver(p -> get(p).flightProfile(p))`), `IronManHero.java`
- Test: `test/hero/ironman/IronManFlightTest.java`

**Interfaces:**
- Mixin: `p == null` → old code untouched; else throttle via `FlightMotion.throttle` (both sides, server for state), velocity via `FlightMotion.velocity/hover` **applied on the client for the local player** (movement is client-side); the server uses the same throttle for state, SONIC, drain, ram. Collision handling and the 0.6 boom stay.
- The profile is resolved on both sides from synced data: hero id + `IronManFlags` glide bit in `HeroPublicSnapshot` (verify `HeroRegistry.get(player)` on the client reads the synced hero; it does for the local player through `HeroPlayer.getHeroId`). Worst case mismatch = 1 tick.
- `IronManRules.profile(boolean glide)`: `speedMul 0.67` (≈ 6 blocks/t at the default config 9.0), `up 1/30`, `down 1/20`, `lockCap 0.79`, `inertia 0.85`, `turn 9°→2.5°`, `hoverDamping 0.8`; glide `glide=true, glideSink 0.12`. Below the shared default on purpose (spec §18 "слабее Homelander в лоб"; Homelander flies at the same shared config speed ×1).

- [ ] Step 1: Tests: `ironManProfileValues`, `profileNullForOtherHeroes`, `profileNullWithoutSuit`, `glideProfileWhenEmpty`.
- [ ] Step 2: FAIL → implement → PASS. `./gradlew build` green.
- [ ] Step 3: Manual: W release with and without Ctrl; Ctrl release with Shift lock; glide at 0 energy with lock on; Homelander and Human fly exactly as before.
- [ ] Step 4: Commit `feat(ironman): flight profile with inertia and turn radius`.

### Task 8: Mouse and held-input seam, fly-by punch, sonic ram

**Files:**
- Create: `core/hero/MouseButton.java` (`PRIMARY, SECONDARY, MIDDLE`), `core/hero/HeldInputs.java` (shared, pure core + server glue), `core/network/packet/HeroMouseC2SPacket.java`, `core/client/hero/HeroMouseInput.java`, `core/hero/ironman/FlyBy.java`, `core/hero/ironman/SonicRam.java`
- Modify: `core/hero/HeroDefinition.java` (`default HeroAction mouseAction(MouseButton b, Player p) { return null; }`), `core/network/CoreMessages.java`, `core/client/AbilityInputManager.java` (`heartKeyOwns` → generic `heroKeyOwns`, no `HeroId.REGULUS`), `core/hero/regulus/RegulusHero.java` (`mouseAction(MIDDLE)` = its heart action), `IronManHero.java`, `test/architecture/NoHeroBranchTest.java` (allowlist shrinks)
- Delete: `core/client/regulus/RegulusInputPriority.java` (its pick-block cancel moves into `HeroMouseInput`)
- Test: `test/hero/HeldInputsTest.java`, `test/hero/ironman/FlyByTest.java`

**Interfaces:**
- Client `HeroMouseInput` (Forge `InputEvent.InteractionKeyMappingTriggered`, HIGHEST): if `mouseAction(button, player) != null` → cancel vanilla (attack / use / **pick-block**, `setSwingHand(false)`) and send edges. MMB in survival is vanilla pick-block (it changes the hotbar slot), so cancelling it is required (spec §6.1 corrected). Releases are sent from a client tick that watches `isDown()` transitions; when a screen opens, the window loses focus, or the claim disappears while held → send release.
- `HeldInputs` (per player, server): `press(action)` → only if not already held (repeated press packets ignored), `canAct` and the claim are checked **only here**; `release(action)` → always routed to the action that was started (even if `canAct`/`mouseAction` now return null, or the RMB tool changed); `releaseAll(reason)` → forced on suit off, control, hero change, death, disconnect. Same pattern as `HeroInputC2SPacket.java:54-60`.
- Regulus heart key: behaviour unchanged (default MMB still assigns hearts and still beats pick-block), now through the shared seam.
- Iron Man Stage 1a claims only LMB while flying in the suit. On the ground LMB stays vanilla with `meleeDamageFactor = NANO_MELEE_FACTOR`. Stage 2 widens the claims.
- `FlyBy.hit(ServerPlayer)`: ray/cone 3.5 blocks along velocity + look, damage `FLYBY_BASE + speed × FLYBY_PER_SPEED` via `HeroDamage.route`, knockback along velocity; player velocity untouched (spec §7.4).
- `SonicRam.tick(ServerPlayer)` in SONIC: entities in this tick's swept box get damage + knockback once per 10 t each (`BoundedMap`); no extra block destruction.

- [ ] Step 1: Tests: `releaseAlwaysRouted`, `repeatedPressIgnored`, `pressCheckedOnlyOnce`, `forcedReleaseOnSuitOff`, `forcedReleaseOnControl`, `releaseAfterToolChangeGoesToStartedAction`, `otherHeroesNeverClaim`, `noMouseClaimWithoutSuit`, `lmbClaimedOnlyInFlight`, `flyByDamageGrowsWithSpeed`, `flyByKeepsVelocity`, `ramHitsEachTargetOncePer10Ticks`.
- [ ] Step 2: FAIL → implement → PASS. Manual: Regulus MMB hearts as before; open inventory while holding a mouse button → release arrives.
- [ ] Step 3: Commit `feat(hero): mouse and held-input seam; feat(ironman): fly-by, sonic ram`.

### Task 9: Energy in flight, glide, landings, air strike

**Files:**
- Create: `core/hero/ironman/LandingTracker.java`, `core/hero/ironman/LandingKind.java` (enum `NONE, SOFT, HEAVY, AIR_STRIKE`; not `Landing` — that name is `HeroShockwave.Landing`), `core/hero/ironman/AirStrike.java`
- Modify: `IronManHero.java` (`tick`, `cancelsFallDamage`, `onLanded`, `handleInput(PRIMARY_ATTACK)`), `HeroAction` append `PRIMARY_ATTACK`
- Test: `test/hero/ironman/LandingTest.java`, `test/hero/ironman/FlightEnergyTest.java`

**Interfaces:**
- Drain per tick by `FlightState`: HOVER/CRUISE `DRAIN_CRUISE`, SONIC `DRAIN_SONIC`; NONE → regen. `Energy.empty()` → glide bit → glide profile (spec §5.3).
- `LandingTracker.tick(boolean onGround, double speed, double vy, FlightState state, boolean strikeArmed)` → `LandingKind` once per touchdown; re-arms after ≥ 5 airborne ticks. Speed is the last airborne tick's velocity (server reads the client-synced position delta).
- `HeroShockwave.land(player, fallDistance, spec, …)` takes a fall distance, but flight has `fallDistance = 0`. `LandingKind.equivalentFall(double impactSpeed)` = `v² / (2 · 0.08)` blocks (vanilla gravity 0.08 b/t²), clamped to the spec's `fullPowerFall`; Iron Man passes this value. Landings without flight (falling after undress) keep the real `fallDistance` from `onLanded`.
- HEAVY when `equivalentFall ≥ HEAVY_FALL` (8), else SOFT. Air strike (spec §8.6): CRUISE/SONIC, pitch ≥ 35° down, ground ≤ `AIR_STRIKE_ARM_DIST` (raycast), LMB arms it (consumes this LMB instead of fly-by); next touchdown = `AIR_STRIKE`; disarms after 30 t.
- Effects (server part; visuals in 1b): SOFT — small dust (`HeroFx.launch`); HEAVY — `HeroShockwave.land` small spec + `CameraShake` through `HeroFx.shockwave` + flag bit 4 for the kneel pose; AIR_STRIKE — `HeroShockwave.land` big + `HeroDebris.erupt` crater, blocks only through `HeroDestruction.canDestroy` (mobGriefing). Fall damage cancelled while `armored()`.

- [ ] Step 1: Tests: `hoverDrainsOnePerSecond`, `sonicDrainsSixPerSecond`, `emptyEnergyForcesGlide`, `glideBlocksSonic`, `equivalentFallFromSpeed`, `softLandingSlow`, `heavyLandingFastFall`, `airStrikeNeedsLmbInDive`, `airStrikeFiresOnce`, `armDisarmsAfter30Ticks`, `rearmsAfterAirborne`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): flight energy, glide and landings`.

### Task 10: Ship 1a

- [ ] `NoHeroBranchTest` green, allowlist one file shorter; `./gradlew build --no-daemon` green (JDK 17); `git diff --check` clean; checker 0 problems.
- [ ] Dedicated server: `./gradlew :viltrumitecore:runServer` reaches "Done" (accept EULA in run dir).
- [ ] In-game check if a client can run: flight grant list (Task 3), damage seam smoke test (`/damage` while anchored then released), held input list (Task 8), flight table (Task 6), landings; Homelander/Regulus/Human regression (flight, punch, Regulus MMB hearts). Otherwise state it in the PR.
- [ ] No jar/version bump (shipped after 1b). Update `.memory-bank/project.md` (flight grant, damage layers, mouse/held input, flight profile), `add_hero` skill `hero-seam.md` (new hooks: `grantsFlightAbility`, `absorbIncoming`, `clampFinalDamage`, `modifyOutgoingDamage`, `mouseAction`, `flightProfile`; remove the `RegulusInputPriority`/`AbilityInputManager` debt line), `SESSION.md` entry in Russian.
- [ ] PR: «Железный человек — этап 1a: Тони, нано-костюм, полёт, общие швы», body starts with «Для игрока».

---

## Self-review

- Spec coverage of 1a: Tony/race (T1), nano key/state/lifecycle (T5, §4.5 rows 1–2), flight grant (T3), flight profile accel/brake/hover/turn/sonic-on-Ctrl (T6–T7, §7.1–7.2), sonic ram + fly-by (T8, §7.3–7.4), energy + glide (T2, T9, §5), landings + air strike (T9, §8.6). Visuals of all of it: Stage 1b.
- No hero branches: flight sees only `FlightProfile`/grant; damage only layer hooks; mouse only `mouseAction`.
- Risks: (a) client-side motion means a modified client can ignore inertia — same as today for all flight; server still owns energy/state; (b) `LivingAttackEvent` cancel must not swallow `/kill` or void (`bypassesInvulnerability` sources are never absorbed — add to tests as `voidNeverAbsorbed`).

## Review log

Plan reviewed against spec §2–§8, §15–§21 and the code (reviewer pass, 2026-10-09). Fixed:
1. §3.2: body is a suit **skin** on the player model, not a full geo model → T8 rewritten (skin + pixel reveal; geo only for 3D parts; helmet last).
2. §8.6: air strike needs LMB near the ground in a dive (was automatic) → T7 + new mouse seam T6.
3. §7.3–7.4 sonic ram and fly-by punch were missing (acceptance row 4) → T6.
4. §8.6 heavy landing kneel pose was missing → T7/T10.
5. §4.1 Tony's glowing reactor was missing → T1.
6. `CleanupReason` has `DEATH, DISCONNECT, HERO_CHANGE` (no dimension) → T3 uses `onDimensionChange`; control interrupts the wave (§16).
7. §17 "own sounds, no stubs" → sounds generated by a committed script instead of "placeholders".

Fixed after PR review (2026-10-09):
8. Fixed after PR review: no way to change incoming damage (`onHurt` is void; `applyCleanDamage` and `ControlManager` payouts bypass events) → new damage-layer seam (Task 4) with fixed layer order, clean-path support, outgoing hook, tests.
9. Fixed after PR review: Iron Man could not take off (`installFlightPolicy` needs `mayfly`, granted only to the legacy kit) → flight-grant seam (Task 3) with grant/revoke rules and manual list.
10. Fixed after PR review: W/Ctrl/Shift mixed up → input table; spec §7.2 now says "отпускание Ctrl"; speed lock capped below sonic; glide beats lock (Task 6).
11. Fixed after PR review: absolute `maxSpeed 6.0` and "Homelander 9.0" were wrong (9.0 is the shared per-player config) → `speedMul 0.67` of the player's max speed (Tasks 6–7).
12. Fixed after PR review: movement is client-side → inertia runs on the client, server owns state only (Global Constraints, Task 7).
13. Fixed after PR review: `nullProfileKeepsLegacyMotion` tested a copy → the mixin legacy branch calls `FlightMotion.legacyVelocity` (Task 6).
14. Fixed after PR review: sonic boom plays at 0.6, not 0.8 — stated "as now" (Task 6).
15. Fixed after PR review: held input — press-only gating, release always routed, repeated press ignored, forced release; `RegulusInputPriority` absorbed by `HeroMouseInput`; MMB is vanilla pick-block in survival (spec §6.1 fixed) (Task 8).
16. Fixed after PR review: `IronManFlags` moved here from Stage 2 (bits 0–4 are used in Stage 1) (Task 2).
17. Fixed after PR review: Stage 1 split into 1a (this) and 1b (visuals, `2026-10-09-ironman-stage1b-visuals.md`); the shader-vs-frames spike lives in 1b.
18. Fixed after PR review: `CREDITS.md` does not exist → Create; asset sources/permissions storage defined; raw archives are not committed (Global Constraints, Task 1).
19. Fixed after PR review: `NANO_MELEE_FACTOR` added to `IronManRules` (Task 2).
20. Fixed after PR review: `ironman.Landing` → `LandingKind` (name clash with `HeroShockwave.Landing`); fall distance recomputed from impact speed for `HeroShockwave.land` (Task 9).
