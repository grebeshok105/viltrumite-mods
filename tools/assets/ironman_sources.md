# Iron Man asset sources

One row per committed asset. Raw archives are never committed.

| Committed file | Source archive | Original path | Author | Conversion |
|---|---|---|---|---|
| `viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero/ironman.png.b64` | `grebeshok105/Codex-Superheroes` (GitHub, branch `main`) | `src/main/resources/assets/superheroes/textures/entity/hero/ironman.png` | grebeshok105 | none (64×64 RGBA, classic arms); base64 for `decodeBinaryAssets` |
| `viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero/ironman_reactor_glow.png.b64` | own (generated) | — | this project | `python3 tools/assets/gen_reactor_glow.py` (emissive mask matching the reactor pixels of `ironman.png`); base64 |
| `viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero/ironman_mark_50.png.b64` | `IronMan_Hulkbuster_models.zip` → `source_original/Satsu/satsu_geo_textures_animations.zip` | `textures/models/iron_man/no_light/mark_50_0.png` | Satsu (Iron Man Addon 3.6.1) | none: UVs of `geo/armor_models/iron_man/full_body/main.geo.json` are the vanilla 64×64 skin layout, used as the player skin; base64 |
| `viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero/ironman_mark_50_glow.png.b64` | same | `textures/models/iron_man/light/mark_50_0.png` | Satsu | none; drawn with `RenderType.eyes`; base64 |
