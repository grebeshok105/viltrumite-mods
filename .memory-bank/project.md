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
- Flight profile: `viltrumiteflight` `FlightProfile`/`FlightProfiles`/`FlightMotion`; `HeroDefinition.flightProfile` (null = legacy flight unchanged). Movement is client-side: inertia runs on the local client, server owns throttle/state.
- Super-jump key id stays `key.viltrumitecore.regulus_super_jump` (player bindings).
- Binary assets pushed through GitHub MCP live as base64 in `viltrumitecore/src/main/binassets/**.b64`; Gradle `decodeBinaryAssets` writes them to resources.

## Iron Man visuals (Stage 1b)
- 3D parts on player parts: `client/anim/render/PlayerGeoLayer` (+ `PlayerBoneMap`, first person via `PlayerGeoHandMixin`); providers register once (`IronManPartsProvider`: helmet + flames). Skill §7.1.
- Suit reveal: no custom shader (Oculus). `RevealMask` distance field → 16 frames baked at runtime into `DynamicTexture`s (`IronManSuitTextures`, reload listener in `ViltrumiteCoreClient`); `skin` frame is the player skin via `HeroSkins.skinVariant`. Decision: `docs/spikes/2026-10-ironman-reveal.md`. Skill §7.2.
- Ability panel looks: `client/hero/PanelStyle`/`PanelStyles` (default = original drawing, `IronManPanelStyle` holographic).
- Iron Man poses `IronManModelMixin` priority 1190 → `IronManPoser`; suit sounds own synthesis `tools/sfx/ironman_stage1.sh`; asset conversion `tools/assets/convert_ironman_stage1b.py`.
- Static init order: a `static final INSTANCE = new X()` must come after the static fields its constructor reads (crashed the client once).

## Rendering gotchas
- Through-wall entity highlights: use the vanilla glowing path. Per-entity colour, client-only: mixin `Minecraft.shouldEntityAppearGlowing` → true and `Entity.getTeamColor` → colour (guard `level().isClientSide`), see `HomelanderGlowMixin`. Drawing into `outlineBufferSource()` by hand is unreliable (the outline post pass runs only when some entity glows). `RenderStateShard` constants (`NO_DEPTH_TEST` etc.) are protected and unavailable to mod code.
- `SilhouetteManager.getState(entity, shouldDraw)` consumes a per-frame delta (max 0.1 s). Call it at most once per entity per frame; a second caller zeros the first caller's alpha lerp.

## Topic files
- `decompile.md`: decompile artifacts and their fixes.