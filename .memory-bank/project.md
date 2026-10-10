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
- Super-jump key id stays `key.viltrumitecore.regulus_super_jump` (player bindings).
- Hero migration by id alias does not move ability slots: a legacy Viltrumite save loads as Homelander with the old slots. `PlayerHeroMixin` marks a legacy save; on login `HeroRegistry.resetLoadout` writes the default loadout, otherwise `repairLoadout` resets slots that hold abilities the hero does not own.
- `isViltrumite()` == `allowsLegacyAbilities()`, so every kit hero also gets the old Viltrumite chassis from `PlayerStatsMixin`/`PlayerEntityCoreMixin`: base strength, damage reduction/ignore threshold, forced `mayfly`, heal every 40 t. A hero with its own regen opts out through `HeroDefinition.usesLegacyRegen` (Homelander does).
- `setViltrumite` on a `ServerPlayer` runs the full `HeroRegistry.changeHero` lifecycle (commands grant/revoke, scourge virus). NBT load uses the identity-only path (`viltrumitecore$setLegacyIdentity`). The race screen's «Человек» sends `HeroChoiceC2SPacket("human")`.
- Hero panel abilities are registered from `HeroDefinition.panelAbilities()` inside `HeroRegistry.register`, not from `ViltrumiteAbilities`.
- `HeroInputC2SPacket` accepts a press only if the action's slot ability is equipped on the active page (releases always pass).
- The shared dash (`PlayerEntityCoreMixin.startDash`) starts only from `FlightState.NONE` or `HOVER`; in CRUISE/SONIC it is a silent no-op. Homelander allows it only in HOVER.
- Thunderclap (`ThunderClapManager`) follows the full look vector (3D), tears exposed blocks on any side; the client rings tilt by pitch.
- Binary assets pushed through GitHub MCP live as base64 in `viltrumitecore/src/main/binassets/**.b64`; Gradle `decodeBinaryAssets` writes them to resources.

## Rendering gotchas
- Through-wall entity highlights: use the vanilla glowing path. Per-entity colour, client-only: mixin `Minecraft.shouldEntityAppearGlowing` → true and `Entity.getTeamColor` → colour (guard `level().isClientSide`), see `HomelanderGlowMixin`. Drawing into `outlineBufferSource()` by hand is unreliable (the outline post pass runs only when some entity glows). `RenderStateShard` constants (`NO_DEPTH_TEST` etc.) are protected and unavailable to mod code.
- World position of a model part (eyes for beams): a `RenderLayer` on `PlayerRenderer` (`EntityRenderersEvent.AddLayers`), `model.head.translateAndRotate(poseStack)`, transform the point by the pose, undo the view with `RenderSystem.getInverseViewRotationMatrix()`, add the camera position. Skip `ShaderCompat.isShadowPass()`. Valid for the current frame only (see `HomelanderEyesLayer`). `getEyePosition` is wrong in flight poses.
- Emissive bits on the model (glowing eyes): draw quads in that layer with `RenderType.lightning()` (position+color, additive), just in front of the hat layer (z -4.6 px on the face).
- Soft trails that must not look neon: `SRC_ALPHA, ONE_MINUS_SRC_ALPHA`, ribbons with alpha 0 at the edges, slow motion; route geometry simplified (Douglas-Peucker) and Chaikin-smoothed, recomputed rarely with a cross-fade (see `FocusRouteRenderer`, `RouteCurve`).
- Client decals on blocks (scorch) must be dropped when the block is gone: check the collision shape behind the face periodically.
- `SilhouetteManager.getState(entity, shouldDraw)` consumes a per-frame delta (max 0.1 s). Call it at most once per entity per frame; a second caller zeros the first caller's alpha lerp.

## Environment and shipping
- The sandbox Gradle cache can be wiped between turns. If `./gradlew build --offline` fails on the foojay plugin, run one build online (no `--offline`), then offline works again.
- No git push credentials: push with GitHub MCP `push_files` (text only; binaries as `binassets/**.b64`).
- Releases: a temporary step in `.github/workflows/build.yml` (`permissions: contents: write`, `gh release create` with the CI-built jars, guarded by a `[release-vX]` marker in the commit message) pushed to `main`, then a follow-up commit restores the workflow. Used for v1.13.0-regulus and v1.14.6-homelander.
- Codex PR review has a usage limit; when it is reached it only posts a limit notice.

## Topic files
- `decompile.md`: decompile artifacts and their fixes.