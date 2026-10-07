---
name: animation-system
description: Use for ALL work on animations, poses, visual effects, screen shaders, particles or animated items/entities in viltrumite-mods. Maps the existing animation and VFX system and gives the only allowed way to add an animation or effect for a new hero or ability.
---

# Animation system (viltrumite-mods)

Read this file before you change or add any animation, pose or visual effect.
The code is the source of truth. If this file and the code disagree, trust the code and update this file.

## 1. Hard rules

1. Do not break current visual effects. Every existing animation, VFX and shader must look the same after your change. Check in `./gradlew runClient`.
2. Use ONLY the systems in this file. Do not add PlayerAnimator, Emotecraft, GeckoLib animation, Veil, a new shader pipeline or any second animation engine. GeckoLib is `compileOnly` for one grab mixin only.
3. Every hero and every active ability MUST have animations: a third-person body pose, a first-person pose, and a visual effect. A hero without animations is not done.
4. Extend the existing classes and patterns. Do not copy a system to make a "better" one.
5. If a new effect needs a capability the system does not have, stop and ask the user before you add it.

## 2. System map

The mod has several layers. Each layer has one job. Pick the layer by what you animate.

| What you animate | Layer | Where |
|---|---|---|
| Player body pose (third person) | `PlayerModel.setupAnim` TAIL mixins | `viltrumitecore/client/mixin/*ModelMixin*`, `viltrumiteflight/client/mixin/SlowFlyingModelMixin`, `PlayerModelMixin` |
| Whole-body lunge/spin/tilt | `PlayerRenderer.setupRotations` TAIL mixins | `*RendererCoreMixin*`, `SlowFlyingRendererMixin` |
| First-person arms/hands | `FirstPersonXMixin` + `PoseDataManager.FP` | `client/mixin/FirstPerson*Mixin`, flight `ItemInHandRendererMixin` |
| Smooth on/off blend of a pose | Weight managers | `client/render/animation/*AnimationManager`, `SilhouetteManager` |
| Animated items and geo entities | Anim core (Blockbench geo + animation JSON) | `viltrumitecore/client/anim/**`, `InfinityGunRenderer` |
| World effects (rings, beams, sparks, blood) | VFX managers on `RenderLevelStageEvent` | `client/render/vfx/*VFXManager`, `MeltedTunnelRenderer` |
| Screen effects (shake, blur, tint) | Post shader `dash_impact` | `ViltrumiteShaders`, `GameRendererDashMixin`, `shaders/post/dash_impact.json`, `assets/minecraft/shaders/program/dash_impact.*` |
| Flight screen effects | Post shader `sonic_boom` | flight `GameRendererMixin` |
| Overlay on the player model | `RenderLayer` | `AtmosphericHeatFeatureRenderer` via `PlayerFeatureRendererMixin` |
| Particles, entity renderers | Forge registration | `ClientModEvents` |
| Hand position for VFX/grab | Tracker mixins | `HandPositionTrackerMixin`, `ChopHandTrackerMixin` |

Ability state comes from synced player data: `ViltrumiteCorePlayer` (core) and `ViltrumiteFlightPlayer` (flight). All visual layers READ this state. They do not own it.

## 3. Player body poses (third person)

Pattern: one mixin per ability on `PlayerModel.setupAnim(LivingEntity;FFFFF)V` at `@At("TAIL")`.

### 3.1 Order (mixin priority)

| Mixin | Priority |
|---|---|
| `DashModelMixin` | 1100 |
| `PunchModelMixin` | 1150 |
| `ChopModelMixin` | 1160 |
| `ChopModelMixin2`, `ThunderclapModelMixin` | 1170 |
| `BarrageModelMixin` | 1175 |
| `GrabModelMixin` | 2000 |
| `BlockModelMixin` | 3000 |

A higher priority applies later and wins. Grab and Block lerp from the pose that earlier mixins left. Give a new ability a free priority that fits this order.

### 3.2 Rules for every pose mixin

1. Act only when the entity implements `ViltrumiteCorePlayer` and the ability is active (synced ticks > 0).
2. Skip when `entity == mc.player` and the camera is first person, EXCEPT when `ShaderCompat.isShadowPass()` is true.
3. Write pose constants in degrees. Convert with `Math.toRadians`.
4. Add position offsets to the base pivots:

| Part | X | Y standing | Y sneaking |
|---|---|---|---|
| Arms | ±5 | 2.0 | 5.2 |
| Body | 0 | 0 | 3.2 |
| Head | 0 | 0 | 4.2 |
| Legs | ±1.9 | 12 | 12 |

5. Mirror left/right with `m = isLeft ? -1 : 1` on yaw, roll and X.
6. Before you pose, set `head.zRot`, `body.yRot`, `body.zRot` to 0.
7. After you pose, ALWAYS call `copyFrom` for `hat`, `jacket`, `rightSleeve`, `leftSleeve`, `rightPants`, `leftPants`. If you skip this, the outer skin layer does not move.
8. Null-check `cloak` before you use it.

### 3.3 Timing

- Abilities use a 20-tick countdown. `t = clamp((20 - (ticks - partialTick)) / 20, 0, 1)`.
- Interpolate with piecewise `Mth.lerp`. Overshoot is `×1.15`.
- Standard 2-keyframe timeline (Punch, Chop):

| t | Segment |
|---|---|
| 0–0.25 | vanilla → S1 (wind-up) |
| 0.25–0.35 | S1 → S2×1.15 (strike) |
| 0.35–0.5 | settle → S2 |
| 0.5–0.65 | hold S2 |
| 0.65–0.9 | S2 → vanilla |

- Thunderclap and Barrage have their own timelines. Read their mixins if you need a multi-hit or charge ability.
- Use `Minecraft.getFrameTime()` for partial ticks in new code. (Old mixins mix `getFrameTime()` and `getPartialTick()`. Do not change them.)

### 3.4 Flight poses

`viltrumiteflight` poses run on the same `setupAnim` TAIL.
- `SlowFlyingRendererMixin` (`setupRotations` TAIL) computes `FlightAnimManager.AnimState.deltaSeconds` and body tilt. `SlowFlyingModelMixin` uses that value.
- Per-player state lives in `FlightAnimManager.AnimState`. Add a new factor field there. Move it with `±rate * deltaSeconds`. Blend with smoothstep `x²(3-2x)`.
- GUI frames must not advance state (`deltaSeconds = 0`).
- A core ability that runs in flight must look correct in BOTH grounded and flying states. Chop and Barrage use additive leg blending when grounded. Copy that pattern.

## 4. Whole-body motion

Use a `PlayerRenderer.setupRotations` TAIL mixin (`PunchRendererCoreMixin` 1500, `ChopRendererCoreMixin` 1510, `BarrageRendererCoreMixin` 1515, `ChopRendererCoreMixin2` 1520).
- Translate to a pivot Y, rotate, translate back.
- Lunge curve for strikes: 0 → -0.3 (t 0–0.25) → 1 (–0.35) → hold (–0.65) → 0 (–0.85).
- Existing lunges apply only in flight (`getFlightState() != NONE`).

## 5. First person

- Each ability has a `FirstPersonXMixin` (Barrage, Block, Chop, Chop2, Grab, Gun, Punch, Thunderclap). Read the nearest one before you write a new one. Copy its structure.
- Flight applies `PoseDataManager.FP` in `ItemInHandRendererMixin` and resets arm pivots in `PlayerRendererMixin`. Do not fight these transforms.
- Blocking uses `FirstPersonBlockAnimationManager`, a separate weight manager. A pose that renders in both views needs a separate first-person weight.

## 6. Weight managers

Use one when a pose turns on and off by a boolean (block, grab, gun).
- Copy `GrabAnimationManager`: `static WeakHashMap<LivingEntity, State>`, `calculateWeight(entity, active)`.
- Wall-clock delta, clamped to 0.1 s. `weight = lerp(1 - exp(-k*dt), weight, target)`.
- k ≈ 10 soft fade, 12–15 body pose, 25 snappy weapon pose.
- Call it ONCE per frame per entity per view. The call also advances the state.
- The weight starts at 0 and never reaches exactly 1. Compare with an epsilon.
- Use from the render thread only.

## 7. Anim core (geo items and entities)

Use for Blockbench models: items, projectiles, summons, gadgets. Do not use it for the player body.

Assets:
- `assets/<ns>/geo/**.geo.json` (format `minecraft:geometry`, 1.12.0).
- `assets/<ns>/animations/**.animation.json` (format 1.8.0). Times are in seconds.
- `AnimCache` loads all namespaces on resource reload. Item helpers: `itemGeo(name)`, `itemAnimations(name)`, `itemTexture(name)`.

API:
- `AnimCache.model(loc)` returns null if missing. `AnimCache.animation(loc, name)`.
- `new AnimationController("main").transitionLength(ticks).onEvent(e -> ...)`.
- `play(anim)` does not restart the same name. `restart(anim)` always restarts.
- Per frame, in this order: `controller.play(anim)` → `controller.apply(model, AnimRenderer.time(pt))` → `AnimRenderer.render(..., boneHook)`.
- `BoneHook` runs for every bone, also for hidden bones. Use it to attach effects or vanilla arms.

JSON support:
- `loop`: `true` → LOOP, `"hold_on_last_frame"`, otherwise PLAY_ONCE. Use `hold_on_last_frame` for one-shot actions.
- Keyframes: arrays, numbers, `{pre, post, easing, lerp_mode: "catmullrom"}`.
- Easings: `linear`, `step`, `easeIn/Out/InOut` + `Sine|Quad|Cubic|Quart|Quint|Expo|Circ|Back|Bounce`. Elastic: in and out only.
- Events: `sound_effects`, `particle_effects`, `timeline` → `Event.Type` SOUND, PARTICLE, INSTRUCTION.
- NO Molang. A Molang string becomes the default value and logs a warning. Bake numbers in Blockbench.
- Animation bone names must equal geo bone names. A missing bone is skipped without an error.

Invariants:
- One `BakedGeoModel` instance is shared per resource. Call `apply` directly before `render`.
- Do not keep model or animation references across a resource reload.
- `AnimRenderer.time` is wall clock × 20. It runs during pause.

Template: `InfinityGunRenderer` (BEWLR). It selects `shoot` / `reload` / `idle` from NBT tick timers and hides the `right_arm` / `left_arm` bones to draw vanilla arms in first person.

## 8. World VFX

Pattern: a `@EventBusSubscriber(value = Dist.CLIENT, bus = FORGE)` class that renders in `RenderLevelStageEvent` at `AFTER_LEVEL` or `AFTER_PARTICLES`.
- Geometry: `Tesselator`, `QUADS`, `POSITION_COLOR`, position-color shader.
- Usual blend: additive (`SRC_ALPHA, ONE`), cull off, depth test on, `depthMask(false)`.
- Camera space: push the model-view stack, `XP(camXRot)`, `YP(camYRot + 180)`, translate `worldPos - cameraPos`.
- Style: the "paint pixel" ring (`drawPaintPixel`) and billboard crosses. New effects MUST use this pixel style to match the mod.
- ALWAYS restore GL state: `depthMask(true)`, `enableCull`, `defaultBlendFunc`, `disableBlend`, pop the stack.

Two driver types:
- One-shot: static list + `spawnX(pos, ...)` (`BlockVFXManager`, `MeteorImpactVFXManager`).
- State-driven: read synced ticks every frame (`PunchVFXManager`, `ChopSweepVFXManager`), or detect a tick threshold in `ClientTickEvent` (`ThunderclapVFXManager`).

For new effects, age by ticks with partialTick (Thunderclap pattern). Do not use per-frame `age++`: it depends on FPS.

### Regulus pose and world-space helpers

- Use `RegulusAnimationManager.actionTime(entity, snapshot, partialTick)` in both views. It uses client game time and limits extrapolation to six ticks and the action length. Do not use raw snapshot ticks for one view and the shared clock for the other.
- Use `calculateWeight` for third person. Use `calculateFirstPersonWeight` for first person. For a held-book pose, pass the fourth `mainHand` argument so each hand has its own weight. Tick inactive weights too, so they fade out.
- Call `RegulusAnimationManager.reset(entity)` when the hero changes. It clears third-person weights, both first-person hands, and the action clock.
- `RegulusPoseTiming.keyedAngle` recovers to the supplied base rotation. End transient keys at zero. Add head/body offsets to vanilla aim and crouch. Mirror action limbs and yaw/roll for the main arm. Restore position offsets at `setupAnim` HEAD before vanilla uses the model again. Copy hat, jacket, sleeves and pants; do not copy body pivots to the cloak.
- `RegulusPixelVfx` emits camera-relative vertices. Call `rotateCamera` once per render pass. Do not translate by the camera again. Use `billboardPixel`, `domeShell` or `sphereShell` for floating points. Shell shimmer changes alpha, not position. Apply owner-only carrier filtering and local first-person aura filtering in the managers, not in the shape helper.

## 9. Screen effects

Core shader: `viltrumitecore:shaders/post/dash_impact.json`, set in `GameRendererDashMixin` (`renderLevel` TAIL).
Current uniforms: `DashIntensity, PunchIntensity, ShakeIntensity, GrabIntensity, LockIntensity, Time, ChopIntensity, ChopType, ChopIsLeft, BarrageIntensity, BarrageIsLeft, ScourgeIntensity, ScourgeTurn`.

To add a screen effect:
1. Declare the uniform in `program/dash_impact.json` AND in the pass `uniforms` of `post/dash_impact.json`.
2. Clamp it in `dash_impact.fsh`.
3. Add it to the early-out check. If you do not, the effect never shows alone.
4. Colour effects are an exclusive chain: chop > barrage > dash/punch. Put a new one in the chain or as an overlay after it, like lock.
5. Compute the intensity in `GameRendererDashMixin`. Use distance falloff for other players.
6. Do not create a second post chain. Flight `sonic_boom` already replaces other post effects when it loads.

## 10. Recipe: new ability animation

1. Add synced state to `ViltrumiteCorePlayer` (20-tick countdown + side flag if needed).
2. Third-person pose: new `XModelMixin` (sections 3.1–3.3).
3. Whole-body motion if the move needs it: `XRendererCoreMixin` (section 4).
4. First-person pose: `FirstPersonXMixin` (section 5).
5. World effect: state-driven VFX manager (section 8).
6. Screen effect if the hit is heavy: new uniform (section 9).
7. Register every mixin in `viltrumitecore.client.mixins.json`. `defaultRequire` is 1, so a bad target crashes the game.
8. Test in `./gradlew runClient`: third person, first person, grounded, flying, sneaking, left and right side, shaders on (shadow pass). Check that old abilities look the same.

## 11. Known pitfalls

- `PlayerModelMixin` (flight) and `SlowFlyingModelMixin` inject at the same TAIL without priority. Do not depend on their order.
- `ChopRendererCoreMixin` does not check chop type, so it also runs for type 1.
- Many VFX lists are plain `ArrayList` and are not cleared on world change. Touch them only on the render thread.
- `MeteorImpactVFXManager` removes impacts at 600 ms, so its 2500 ms shake window is cut.
- `tickGrab` in `PlayerEntityCoreMixin` was rewritten by hand after decompile and is not confirmed in-game. Compare grab behaviour with the original JAR.