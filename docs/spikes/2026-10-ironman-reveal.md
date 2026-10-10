# Spike: Iron Man suit reveal technique (Stage 1b, Task 2)

Date: 2026-10-09. Plan: `docs/plans/2026-10-09-ironman-stage1b-visuals.md` Task 2.

## Question

How to show the Mark 50 nanosuit growing over Tony pixel by pixel (spec §4.2,
helmet last) so that it works with Oculus shader packs, in the shadow pass,
in first person and for other players?

- **A. Shader `discard`:** a core shader `ironman_reveal` (entity cutout +
  `discard` by distance from a reactor uniform, `Progress`).
- **B. Pre-baked frames:** 16 masked textures, frame picked by progress, drawn
  with vanilla render types.

## Result: B, baked at runtime

Chosen: **B**, but the 16 frames are baked on the client (render thread,
lazily, rebuilt after a resource reload) into `DynamicTexture`s instead of
committed PNGs. Same output as a Python bake, no 64 extra binary files, and a
skin change needs no re-bake.

Why not A:
- Oculus replaces entity shaders with the pack's gbuffers programs. A custom
  core shader is ignored or breaks (no `discard` → full suit at once, or the
  entity disappears), and the shadow pass needs its own program. Making A
  work needs per-pack handling — not possible to verify or maintain here.
- B uses only vanilla render types (`entityCutoutNoCull`)
  and `eyes` for emissive parts. Shader packs draw them like any entity
  texture; the worst case is that glow is drawn as normal colour.

Not verified: an in-game run with Oculus + BSL/Complementary was not possible
in this environment (no GPU client). By construction B has no custom shader,
so the expected fallback is "suit drawn normally". **Owner check needed.**

## How it works

- `client/anim/render/RevealMask` (pure, tested): every texel of the 64×64
  classic player skin layout (inner + outer layer) gets a normalized distance
  0..1 from the arc reactor (model point 0, 20, −2) measured on the 3D surface
  of the six boxes. Head texels come after all body texels (they grow from the
  neck), so the helmet closes last. A small stable jitter makes the front
  ragged like scales.
- `client/ironman/IronManSuitTextures` bakes per frame 1..16:
  - `skin`: suit where revealed, Tony elsewhere, the head stays Tony —
    used as the player skin (`HeroSkins.skinVariant`), so the model, every
    pose mixin and the first-person arm show it with no extra layer;
  - `cut`: suit where revealed, transparent elsewhere — the helmet geo part;
  - `glow`: Mark 50 light map where revealed (premultiplied for additive);
  - `rim`: the cyan wave front with a scale pattern.
- Frame = `RevealMask.frame(IronManView.reveal(snapshot, pt))`; reveal comes
  from the synced wave timeline (`actionElapsed/actionLength`), so a relog or
  a second client mid-wave shows the right frame.
- Helmet: `geo/ironman/mark_50/head_mask.geo.json` on the head through
  `PlayerGeoLayer`, drawn from `RevealMask.field().headStartFrame()` on, with
  `cut` + `glow` + `rim`. Tony's hat layer is hidden from that frame on.
- Emissive passes are skipped in the shadow pass.

## Suit skin source

Satsu `textures/models/iron_man/no_light/mark_50_0.png` (+ `light/` glow map)
is already a 64×64 vanilla player skin layout with classic arms (checked by
rendering the front view against the player UV map), so it is used as is —
no geo-to-skin bake script was needed.

## Side finding

The Stage 1a "Tony" skin (from Codex-Superheroes) was actually an Iron Man
suit skin. It is replaced by an own drawn Tony skin
(`tools/assets/make_tony_skin.py`: hair, goatee, black tee with the reactor,
jeans).
