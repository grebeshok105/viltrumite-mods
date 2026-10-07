# Hero seam

How a hero plugs into `viltrumitecore`. Paths are under `viltrumitecore/src/main/java/dev/baranhan/viltrumitecore/` unless noted.

## 1. `hero/HeroDefinition`

Required:

| Method | Meaning |
|---|---|
| `HeroId id()` | enum key |
| `allowsFlight(Player)` | mod flight allowed (also gated by `HeroRegistry.installFlightPolicy`) |
| `allowsLegacyAbilities(Player)` | Viltrumite ability pipeline (punch, clap, dash) |
| `ownsAbility(String id)` | ability id belongs to this hero |
| `allowsExternalControl(LivingEntity, ControlKind)` | can others grab/freeze/anchor this player |
| `canAct(ServerPlayer, HeroAction)` | gate for every input action |
| `tick(ServerPlayer)` | server tick |
| `handleInput(ServerPlayer, HeroAction, boolean pressed)` | key input (JUMP is handled by `HeroSuperJump`, not here) |
| `enter(ServerPlayer)` | hero chosen |
| `snapshot(Player)` | `HeroPublicSnapshot` synced to clients |
| `cleanup(ServerPlayer, CleanupReason)` | stop everything (death, logout, hero change …) |

Default hooks (override only when needed):

| Hook | Default |
|---|---|
| `allowsAbilityPages`, `hasAbilityPanel` | panel visibility |
| `meleeDamageFactor(Player)` | 1.0 |
| `cancelsFallDamage(Player)` | false |
| `onLanded(ServerPlayer, float fallDistance)` | nothing; called from `HeroEvents.onLivingFall` |
| `superJumpVelocity(Player)` | 0 (no super jump); both sides |
| `onSuperJump(ServerPlayer)` | nothing (hero sounds) |
| `preventsExhaustion(Player)` | false |
| `saveHeroState` / `loadHeroState` / `cloneHeroState` | drop state |

New behaviour that shared code must ask about → add a new default hook here. Do not branch on `HeroId` in shared code.

## 2. Touchpoints for a new hero

| What | File |
|---|---|
| Hero id | `hero/HeroId.java` (append) |
| Input actions | `hero/HeroAction.java` (append) |
| Registration | `hero/HeroRegistry.registerDefaults()` |
| Hero code | `hero/<id>/` (`<Id>Hero`, `<Id>Rules`, `<Id>State`, `<Id>Abilities`, one class per ability) |
| Ability panel slots, icons, grey-out | `ability/ViltrumiteAbilities.java` |
| Input sending | `client/AbilityInputManager.java`, `network/packet/HeroInputC2SPacket.java` |
| Public sync | `hero/HeroPublicSnapshot.java` |
| Choosing the hero | `client/gui/RaceSelectionScreen.java` (+ lang `gui.viltrumitecore.race_selection.become_<id>`), items like `item/EvangeliumItem.java` |
| Skin | `client/<id>/<Id>Client` → `HeroSkins.register`; texture `assets/viltrumitecore/textures/entity/hero/<id>.png` |
| Client init | `client/ViltrumiteCoreClient.java` |
| Poses / VFX | `client/<id>/`, `client/render/vfx/` (follow `animation-system`) |
| Key mappings | `client/ViltrumiteCoreClient.java` (lang `key.viltrumitecore.*`) |
| Sounds | `ViltrumiteCore.SOUND_EVENTS`, `assets/viltrumitecore/sounds.json`, `src/main/binassets/**.ogg.b64` |
| Lang | `assets/viltrumitecore/lang/*.json` (all locales) |
| Mixins | `viltrumitecore.mixins.json`, `viltrumitecore.client.mixins.json` (only if no hook exists) |
| Tests | `src/test/java/dev/baranhan/viltrumitecore/hero/<id>/` |
| Design | `docs/design/<date>-<id>-design.md` |

## 3. Known legacy debt (Regulus branches in shared code)

These files check `HeroId.REGULUS` directly. When a new hero needs the same behaviour, replace the branch with a `HeroDefinition` hook instead of adding a second branch:

- `ability/ViltrumiteAbilities` — Regulus slot registration and grey-out.
- `client/AbilityInputManager` — Regulus key routing.
- `client/ViltrumiteCoreClient` — Regulus key block.
- `client/gui/RaceSelectionScreen` — hero buttons and head preview.
- `client/mixin/GameRendererDashMixin`, `RegulusFovMixin`, `RegulusHudMixin`, `SilhouetteRendererMixin`.
- `client/render/vfx/RegulusActionVFXManager`, `RegulusLionVFXManager`.
- `hero/HeroDamage`, `hero/HeroEvents`, `item/EvangeliumItem`.
- `hero/HeroPublicSnapshot` — fields are Regulus-shaped (lion, madness, hearts, `COOLDOWN_COUNT = 6`).

Run `grep -rln "HeroId.REGULUS" viltrumitecore/src/main/java` to see the current list.
