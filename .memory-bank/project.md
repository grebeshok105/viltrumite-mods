# Project memory

Read this file before every task. Keep it short. See `AGENTS.md` §6 for the rules.

## Origin
- The original author (package prefix `dev.baranhan`) gave the mods to the repository owner. Only the release JARs existed. The sources in this repo were decompiled from them.
- Original JARs: `original-jars/viltrumitecore-forge-1.10.3.jar`, `original-jars/viltrumiteflight-forge-1.6.7.jar`.

## Direction
- Goal: revival on a new architecture, then many heroes and characters.
- Heroes: Human, Homelander (replaced Viltrumite in 1.14.0), Regulus. `HeroId.VILTRUMITE` stays in the enum for save compatibility only: `fromKey("viltrumite")` and the legacy `IS_VILTRUMITE` boolean map to `HOMELANDER`.
- `grebeshok105/Codex-Superheroes` (Fabric) is dropped. It is an idea source only.

## Architecture relations
- `viltrumitecore` depends on `viltrumiteflight`. Core visual effects read flight state.
- `./gradlew runClient` loads both mods together.
- Per-player state lives on `Player` through mixin interfaces: `ViltrumiteCorePlayer` (core) and `ViltrumiteFlightPlayer` (flight, FlightState).
- `PlayerEntityCoreMixin` (~1500 lines) holds most core player logic: synced data (`IS_VILTRUMITE`, `HAS_CHOSEN_RACE`, `IS_DASHING`, `DASH_TICKS`, `PUNCH_TICKS`, `IS_LEFT_ARM_PUNCH`, ...), dash, punch, chop, grab. Client model mixins read this synced data to pose the model. It is a refactor target for the hero seam.
- Impact effects are driven by `PunchImpactManager`, `ChopImpactManager`, `ThunderClapManager`.
- Core has its own geo/animation loader in `client/anim/` for JSON in `assets/viltrumitecore/geo/` and `assets/viltrumitecore/animations/`. Details: `.agents/skills/animation-system/SKILL.md`.

## Hero seam
- Heroes implement `hero/HeroDefinition`; shared code asks hooks (`cancelsFallDamage`, `onLanded`, `superJumpVelocity`, `onSuperJump`, `onHurt`, `onDimensionChange`, `abilityIcon`, ...), not `HeroId`. Regulus still has legacy `HeroId.REGULUS` branches in shared code; `test/architecture/NoHeroBranchTest` fails on new ones outside hero-named places.
- Legacy Viltrumite kit (punch, dash, thunderclap, grab, ...) is gated per ability: `hero/LegacyKit.allows(player, id)` → `HeroDefinition.allowsLegacyAbility`. `isViltrumite()` now means "may use the kit".
- Generic hero input: `HeroDefinition.heroInputSlots()/heroActionFor()` drive `HeroInputC2SPacket` from slot keys (no per-hero client branch). Generic snapshot fields: `resource`, `resourceLocked`, `heroFlags` (bits). `HeroOwnerSnapshot` ids = owner-only entity ids, read per hero (Regulus carriers, Homelander focus targets).
- Shared hero toolkit: `hero/fx/HeroFx` → `HeroFxS2CPacket` → `client/render/vfx/HeroImpactFx`; `CameraShake`, `PixelVfx`, `hero/HeroDebris`, `hero/HeroShockwave`, `hero/HeroSuperJump`, `client/hero/SuperJumpClient`. Procedure: `.agents/skills/add_hero/`.
- Flight grant: `HeroDefinition.grantsFlightAbility` → `hero/HeroFlightGrant.sync` (every server tick + login/respawn/hero change) sets vanilla `mayfly` for non-legacy heroes; NBT `HeroGrantedMayfly` marks our grant, never revokes a foreign `mayfly`; creative/spectator untouched.
- Damage layers: `hero/HeroDamageLayers`; hooks `absorbIncoming` (LivingAttackEvent HIGH), `clampFinalDamage` (LivingDamageEvent LOWEST), `modifyOutgoingDamage` (LivingHurtEvent HIGH); `HeroDamage.applyCleanDamage` runs them too. Order: control → shield → Hulkbuster → mark → nano armor → HP.
- Mouse / held input: `HeroDefinition.mouseAction(MouseButton, Player)` (both sides) → client `client/hero/HeroMouseInput` cancels vanilla attack/use/pick-block and sends `HeroMouseC2SPacket` edges → server `hero/HeldInputs` (claim + canAct only on press, release always to the started action, forced release on control/death/logout/hero change). MIDDLE = heart key mapping (default MMB). Replaced `RegulusInputPriority`.
- Flight profile: `viltrumiteflight` `FlightProfile`/`FlightProfiles`/`FlightMotion`; `HeroDefinition.flightProfile` (null = legacy flight unchanged). Movement is client-side: inertia runs on the local client, server owns throttle/state. Iron Man normal flight = null profile (same as Homelander, user decision after play test); only the 0-energy glide uses `IronManRules.glideProfile()`.
- Super-jump key id stays `key.viltrumitecore.regulus_super_jump` (player bindings).
- Knockback of any target by a hero goes through `HeroRegistry.allowsImpulse(target)` (IMPULSE policy + not anchored) and only after `hurt(...)` returned true.
- Owner sections are deduplicated by content (`pushOwnerSection`). To restart a client timer with the same content (re-scan of the same target), push `Section.EMPTY` first.
- Binary assets pushed through GitHub MCP live as base64 in `viltrumitecore/src/main/binassets/**.b64`; Gradle `decodeBinaryAssets` writes them to resources.

## Iron Man visuals (Stage 1b)
- 3D parts on player parts: `client/anim/render/PlayerGeoLayer` (+ `PlayerBoneMap`, first person via `PlayerGeoHandMixin`); providers register once (`IronManPartsProvider`: helmet + flames). Skill §7.1.
- Suit reveal: no custom shader (Oculus). `RevealMask` distance field → 16 frames baked at runtime into `DynamicTexture`s (`IronManSuitTextures`, reload listener in `ViltrumiteCoreClient`); `skin` frame is the player skin via `HeroSkins.skinVariant`. Decision: `docs/spikes/2026-10-ironman-reveal.md`. Skill §7.2.
- Ability panel looks: `client/hero/PanelStyle`/`PanelStyles` (default = original drawing, `IronManPanelStyle` holographic).
- Iron Man poses `IronManModelMixin` priority 1190 → `IronManPoser`; suit sounds own synthesis `tools/sfx/ironman_stage1.sh`; asset conversion `tools/assets/convert_ironman_stage1b.py`.
- Static init order: a `static final INSTANCE = new X()` must come after the static fields its constructor reads (crashed the client once).

## Iron Man combat (Stage 2)
- Server driver `hero/ironman/IronManCombat` (repulsors, unibeam + overdraft, missiles with lock, nano arsenal, shield); input routing in `IronManHero`. Rules are pure classes with tests (`CoreOverheat`, tool cycle, unibeam timeline, missile lock, nano arsenal, shield).
- Guard key seam: `HeroDefinition.guardAction` (F → `MouseButton.GUARD`, client `HeroGuardInput`) + `blocksHandSwap` (server cancels `LivingSwapItemsEvent`).
- Owner snapshot sections: `HeroRegistry.pushOwnerSection` / `ClientHeroData.section`; Iron Man uses `MARKS` (missile locks). `extraCooldowns[0]` = nano-lost lock.
- `IronManFlags` bits 27-29 = stage 2 pose bits, 30-31 reserved.
- `client/render/vfx/ScorchRenderer` + `ScorchBuffer` are shared (beam scorch marks).
- Client: `IronManCrosshair` (per tool), `IronManCombatParts` (own nano blade/hammer/shield/missile pod geo in `geo/ironman/nano/`, made by `tools/assets/make_ironman_stage2_parts.py`; no Satsu sources), `NanoDamageVisuals` (baked damage masks, reload listener). Missiles are pixel VFX only. Arm swing poses are third person only.
- Sounds: `tools/sfx/ironman_stage2.sh`; icons/crosshairs: `tools/assets/make_ironman_stage2_icons.py`.

## Iron Man helmet and JARVIS (Stage 3)
- Helmet: `hero/ironman/Helmet` (toggle 12 t, auto-close on a combat hit), saved in NBT (`HelmetOpen`); flag `HELMET_CLOSED` only with the suit on. Client `HelmetAnim` (fold via `RevealMask` head frames, hand-to-face pose), `HelmetHud` frame. Helmet open = HUD shows only energy, vanilla crosshair, no JARVIS, no scan.
- Server driver `hero/ironman/IronManJarvis` (helmet, scan, countermeasures); pure rules `jarvis/ThreatScan`, `scan/*` (`ScanProgress`, `ScanTraits`, `ScanAnalyzer`, `WeakSpots`, `ScanCard`), `Countermeasures`. Threat ids go in owner section `THREATS`, scan highlights in `SCAN`; scan card = `ScanCardS2CPacket`.
- Scan seam: `HeroDefinition.scanInfo(Player)` (extra `ScanLine`s) and `hiddenFromScan(Player)`. Regulus/Homelander/Iron Man add lines via static `scanInfoFor(...)`.
- Scan progress on the client: snapshot `controlTargetId` + lowest-priority channel `SCAN`.
- Countermeasures: `FlareEntity` (no damage, no model, `FlareRenderer` pixels), retarget homing (`Countermeasures.retargets`) and shulker bullets (`ShulkerBulletAccessor`), mobs forget the player 40 t, Wither/Ender Dragon ignored. `extraCooldowns[1]` = cooldown.
- Client JARVIS: `client/ironman/jarvis/JarvisVoice` + `VoiceGate` (one line at a time, global gap, per-line cooldown), `JarvisHints` (threat brackets, edge arrows, projectile marks, subtitle); scan UI `client/ironman/scan/ScanReticle`, `ScanCardRenderer`. World-to-screen: `client/render/vfx/ScreenProjector`.
- Voice: 4 CC0 recordings from Codex-Superheroes (suit up, threat, critical threat, scan done); other lines = subtitle + `jarvis_warning` chime. Sounds `tools/sfx/ironman_stage3.sh` (`--jarvis DIR` re-encodes the recordings); art `tools/assets/make_ironman_stage3_assets.py`.

## Iron Man Veronica and marks (Stage 4)
- One mark = one instance: `mark/MarkRoster` location `STORED/WORN/EMPTY/IN_DELIVERY`, changed only by the guarded `move(id, from, to)`; durability lives only in the roster (NBT `MarkRoster`). `IronManState.reconcileMark` makes only the mark on the body WORN; load and death recall world instances to STORED.
- `Suit` holds the mark states (`MARK`, `EQUIPPING`, `EXITING`, `MARK_PARTIAL`, appended to `SuitState`); `Suit.tick()` returns `Event.MARK_ON/EXITED`, `interrupt()` keeps locked parts as `MARK_PARTIAL`. Saved EQUIPPING/EXITING become NONE.
- Every system reads `IronManState.spec()` (`SuitSpec`: nano defaults or `MarkSpec` + Mark 42 lost parts), never nano constants for armor, missiles, Unibeam charge, shield cost, recoil, drain, weapon and flight speed.
- Damage order: shield (`IronManCombat.absorb`) → mark durability (`IronManMarks.absorb`, whole hit, no spill) → nano armor → Tony. A worn mark breaks on the next tick: debris + `Suit.autoNano()` (nano on at once, wave is visual only).
- Pod and empty suit (`VeronicaPodEntity`, `EmptySuitEntity`) are not saved and leave by themselves when the owner is offline, dead, in another level, far (> 96) or the owner's `podId`/`emptySuitId` no longer names them.
- Part flight is client-only: server syncs `EQUIP_PHASE`, the SUIT timeline (`EquipTimeline`, 50 t delivery / 20 t enter), `variant` (mark + parts) and the parts source as `actionTarget`.
- Signatures: `mark/MarkSignature` per mark (`MarkSignatures`), state in `IronManState.signature`; slot 3 in a mark and the RMB `SIGNATURE` tool route to it; cooldown in `extraCooldowns[3]`.

## Rendering gotchas
- Through-wall entity highlights: use the vanilla glowing path. Per-entity colour, client-only: mixin `Minecraft.shouldEntityAppearGlowing` → true and `Entity.getTeamColor` → colour (guard `level().isClientSide`), see `OutlineGlowMixin` / `OutlineTeamColorMixin`. Sources register in `client/render/vfx/OutlineTargets` (first registered source that returns a colour wins; Homelander focus first, Iron Man scan highlight next). Drawing into `outlineBufferSource()` by hand is unreliable (the outline post pass runs only when some entity glows). `RenderStateShard` constants (`NO_DEPTH_TEST` etc.) are protected and unavailable to mod code.
- `SilhouetteManager.getState(entity, shouldDraw)` consumes a per-frame delta (max 0.1 s). Call it at most once per entity per frame; a second caller zeros the first caller's alpha lerp.

## Topic files
- `decompile.md`: decompile artifacts and their fixes.