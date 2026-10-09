# Spike: Iron Man suit reveal — shader `discard` vs pre-baked frames

Date: 2026-10-09. Plan: `docs/plans/2026-10-09-ironman-stage1b-visuals.md` Task 2.

## Options

- **A. Core shader `ironman_reveal`** — entity cutout + `discard` by distance from a uniform origin and `Progress`. Needs a custom `ShaderInstance` and render type.
- **B. 16 pre-baked frames** — reveal masks cut from the suit skin by a distance field, the frame is picked by wave progress. Uses only vanilla render types.

## Facts found

- Satsu `geo/armor_models/iron_man/full_body/main.geo.json` and `nano_armors/marks/*` use the **vanilla 64×64 player skin UV layout** (head 0,0 / hat 32,0, body 16,16 / jacket 16,32, arms 40,16 and 32,48, sleeves 40,32 and 48,48, legs 0,16 / 16,48, pants 0,32 / 0,48). So `textures/models/iron_man/{no_light,light}/mark_50_0.png` are usable as a player skin as is (no bake script needed).
- `head_mask/main.geo.json` is exactly the vanilla head + hat cubes (inflate 0.25 / 0.5) with the same UVs, so the helmet is part of the skin too.
- Oculus replaces vanilla entity shaders with the pack's `gbuffers_entities`; a custom core shader (A) is either ignored or drawn without the pack's lighting/shadows. Vanilla render types (B) are always translated by Oculus.

## Decision: B, frames composited into the player skin

- 16 frames are baked **at runtime on the client** (`DynamicTexture`, once per resource reload) from Tony's skin + the suit skin by `RevealMask` (body-space distance from the reactor; helmet last; 1-texel cyan rim on the wave front). Frame 16 = the suit skin itself.
- The frame is returned by the Iron Man `HeroSkins` provider as the player's skin **and hand** texture: one draw, no overlay, no z-fighting, first person included, shadow pass included (the skin is drawn by the vanilla player renderer).
- Glow (`light/mark_50_0.png`) is masked the same way (16 glow frames) and drawn with `RenderType.eyes` in `IronManSkinLayer`.
- A was not prototyped: it needs a second shader pipeline (animation-system §1.2 forbids it) and is unsafe with Oculus.

## Not verified

The runtime checks of step 2 (vanilla, Oculus + BSL/Complementary, shadow pass, first person, other players) were **not run**: the agent machine has no display/GPU and no Oculus. The choice rests on B using only vanilla render types and the vanilla skin path. To check in game: put the suit on with a shader pack enabled; the suit must be drawn at least fully revealed.
