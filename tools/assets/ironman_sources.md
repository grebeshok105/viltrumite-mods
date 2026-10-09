# Iron Man asset sources

One row per committed asset. Raw archives are never committed.

| Committed file | Source archive | Original path | Author | Conversion |
|---|---|---|---|---|
| `viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero/ironman.png.b64` | `grebeshok105/Codex-Superheroes` (GitHub, branch `main`) | `src/main/resources/assets/superheroes/textures/entity/hero/ironman.png` | grebeshok105 | none (64×64 RGBA, classic arms); base64 for `decodeBinaryAssets` |
| `viltrumitecore/src/main/binassets/assets/viltrumitecore/textures/entity/hero/ironman_reactor_glow.png.b64` | own (generated) | — | this project | `python3 tools/assets/gen_reactor_glow.py` (emissive mask matching the reactor pixels of `ironman.png`); base64 |
