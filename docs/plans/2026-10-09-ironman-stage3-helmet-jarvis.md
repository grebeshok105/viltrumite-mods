# Iron Man — Stage 3 (Helmet, JARVIS, Scan, Countermeasures) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The helmet opens/closes with nanites (auto-closes on the first hit in combat) and gates the JARVIS layer: helmet HUD frame, threat frames and projectile arrows, low-energy/overheat warnings with JARVIS voice lines, missile auto-lock; a 1.5 s scan with a target card (HP, armor, effects, resists, stats, weak spot) and 10 s through-wall highlight; countermeasure flares that pull homing projectiles and make mobs lose the player for ~2 s.

**Architecture:** Server owns helmet state, scan progress/result, countermeasure timers and the threat list; the client draws. Helmet = `Helmet` pure class in `IronManState`. Threats and scan targets go to the owner through the Stage 2 typed owner sections (`HeroRegistry.pushOwnerSection` with `THREATS` and `SCAN` — independent from `MARKS`, `SCAN` carries a 200 t expiry) and a new `ScanCardS2CPacket`. The Homelander focus outline is extracted into a shared `client/render/vfx/OutlineTargets` (Homelander behaviour identical) and reused for the scan highlight. Hero protections and weak spots come from a generic descriptive hook `HeroDefinition.scanInfo(Player self)` that mirrors the real damage route; vanilla targets use a rule table of real vanilla damage rules. Analysis never deals damage or changes state. Countermeasures retarget anything implementing `core/entity/Homing` (added in Stage 2).

**Tech Stack:** Forge 47.3.0, Minecraft 1.20.1, Java 17, Mixin 0.8.5, JUnit 5.9.3.

**Spec:** `docs/design/2026-10-09-ironman-design.md` §10, §11, §15.2, §16, §17. Previous plans: Stage 1, Stage 2 (`docs/plans/2026-10-09-ironman-stage{1,2}-*.md`).

Paths as in Stage 1; `util/` = `core/hero/ironman/util/`.

## Global Constraints

- All Stage 1 / Stage 2 Global Constraints apply.
- Without the helmet: no JARVIS HUD, no scan, no missile marks (spec §10); clean view; only energy, durability and the ability panel.
- JARVIS is "small hints at screen edges", never a half-screen UI (spec §11.1).
- JARVIS (hints and voice) works only with the helmet closed.
- Voice lines: Codex JARVIS recordings `assets/superheroes/sounds/ironman/jarvis_*.ogg` (repo `grebeshok105/Codex-Superheroes`; fetch with sparse checkout of that folder; the local `/data/cs` clone does not contain it). Each line has a per-line cooldown and a global ≥ 4 s gap; never two at once. If the Codex folder has no JARVIS files, voice is replaced by subtitle + UI chime and it is stated in the PR (no synthetic voice).
- Scan works on any `LivingEntity` including other heroes' players; nothing in the card is computed on the client.
- Mark 15 (Stage 4) is hidden from scan and focus: reserve the hook now (`HeroDefinition.hiddenFromScan(Player)` default false).
- Scan shows only what the damage code really does. No invented weak spots; unknown → «не обнаружено». Head-hit bonuses are out of scope (separate design decision).

## Review Focus

1. Helmet open → missile marks refused on the server even if the client sends them (Task 1 test `openHelmetBlocksMarks`).
2. Scan target dies / leaves range / player opens the helmet mid-scan → scan cancelled, no card, no stuck highlight (Task 4 tests).
3. Countermeasures: mobs lose the player for ~40 t and then re-acquire normally; boss mobs (`WitherBoss`, `EnderDragon`) are not affected (Task 6 tests `mobsForgetFor40`, `bossesIgnore`).
4. JARVIS voice never stacks or spams (Task 3 test `globalGapAndPerLineCooldown`).
5. Outline extraction leaves Homelander focus pixel-identical (existing `test/client/homelander/*` green + screenshot).

---

### Task 1: Helmet state and gating

**Files:**
- Create: `core/hero/ironman/Helmet.java`
- Modify: `IronManState.java`, `IronManHero.java` (`handleInput(HELMET)`, `onHurt` auto-close, `canMarkTargets`), `HeroAction` append `HELMET, SCAN, COUNTERMEASURES`, `IronManAbilities` (`ironman:helmet` page 2 slot 4), `IronManFlags` bit 19 helmet closed
- Test: `test/hero/ironman/HelmetTest.java`

**Interfaces:**
- `Helmet`: `boolean closed()`, `void toggle()` (12 t nanite fold/unfold; in a mark Stage 4 → plates), `void onHit()` → closes if open; called from `onHurt` for damage with a living attacker or a projectile (combat), not for fall/fire/drowning (spec §10), `float progress()`, `save/load` key `HelmetOpen` (default closed). Suit put on → helmet closed; suit off → n/a.
- Gating: `canMarkTargets = helmet.closed()`; scan refused when open; JARVIS client layer hidden when open.

- [ ] Step 1: Tests: `defaultClosed`, `toggleTakes12`, `hitClosesOpenHelmet`, `openHelmetBlocksMarks`, `saveLoad`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): helmet state`.

### Task 2: Helmet visuals and helmet HUD

**Files:**
- Create: `core/client/ironman/HelmetHud.java` (frame at screen edges from Sind `hud__*` reference, redrawn), `res/textures/gui/ironman/helmet_frame.png` (+ `.b64`)
- Modify: `IronManHud.java` (split: always — energy, durability, panel; helmet closed — frame, left energy + 3 pips + durability (mark/Hulkbuster, filled in Stages 4–5), right speed/altitude in flight, centre crosshair from `IronManCrosshair`), `IronManSkinLayer.java` + `IronManPartsProvider.java` (helmet `head_mask` part folds by `Helmet.progress()`; Tony's face visible when open), `IronManPoser.java` (hand-to-face gesture during toggle)

- [ ] Step 1: Nanite fold/unfold of the mask (reuse `RevealMask` along the head bone), 1st person: frame fades in/out with a scanline sweep.
- [ ] Step 2: Without helmet: only energy, durability, panel (spec §15.2). Commit `feat(ironman): helmet visuals and hud`.

### Task 3: JARVIS hints and voice

**Files:**
- Create: `core/hero/ironman/jarvis/ThreatScan.java` (server: mobs whose target is the player, skeletons/pillagers drawing at the player, players looking at us with a weapon within 32 blocks; every 5 t → `THREATS` owner section), `core/client/ironman/jarvis/JarvisHints.java` (threat brackets at screen edge, projectile arrows for projectiles within 32 blocks whose path passes ≤ 2 blocks from the player, warnings), `core/client/ironman/jarvis/JarvisVoice.java`, `core/client/ironman/jarvis/VoiceGate.java` (pure), `res/sounds/ironman/jarvis_*.ogg` (+ `.b64`)
- Modify: `IronManHero.java` (missile auto-lock: if no manual marks and helmet closed → nearest threat gets the first mark, missiles only), `res/sounds.json`, lang
- Test: `test/hero/ironman/ThreatScanTest.java` (pure predicates), `test/client/ironman/VoiceGateTest.java`

**Interfaces:**
- Voice triggers: suit up, energy < 20, overheat, second overheat (spec §9.1 warning — replaces the Stage 2 text-only warning with voice + text), overdraft, scan complete, countermeasures, Veronica (Stage 4 uses). `VoiceGate.tryPlay(line, now)` → false if global gap < 80 t or the line's cooldown active.
- Map Codex file names to triggers in a table in `JarvisVoice` (inspect the fetched folder; unused lines skipped).

- [ ] Step 1: Tests: `mobTargetingIsThreat`, `drawingBowIsThreat`, `idleMobIsNot`, `projectilePathNearPlayer`, `globalGapAndPerLineCooldown`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): jarvis hints and voice`.

### Task 4: Scan (server)

**Files:**
- Create: `core/hero/ironman/scan/ScanProgress.java` (pure: hold aim 30 t on the same target, reset on target change), `core/hero/ironman/scan/ScanCard.java` (record), `core/hero/ironman/scan/ScanAnalyzer.java`, `core/hero/ironman/scan/WeakSpots.java`, `core/network/packet/ScanCardS2CPacket.java`
- Create: `core/hero/ScanInfo.java` (record: `List<Component> protections`, `List<Component> weakSpots`, `List<Component> conditions`)
- Modify: `core/hero/HeroDefinition.java` (`default ScanInfo scanInfo(Player self) { return ScanInfo.EMPTY; }`, `default boolean hiddenFromScan(Player self) { return false; }`), `core/hero/regulus/RegulusHero.java` (`scanInfo`: Lion's Heart active → «блокирует весь внешний урон» from the same state `HeroDamage.decide` reads, `HeroDamage.java:78-110`; inactive → nothing), `core/hero/homelander/HomelanderHero.java` (`scanInfo`: legacy damage reduction % and ignore threshold from the same values `PlayerStatsMixin.reduceIncomingDamage/ignoreWeakDamage` use, `PlayerStatsMixin.java:90-125`), `IronManHero.java` (`scanInfo`: nano armor / mark durability / shield), `IronManHero.java` (`SCAN` press → start; repeat → cancel), `IronManAbilities` (`ironman:scan` page 2 slot 1)
- Test: `test/hero/ironman/ScanTest.java`

**Interfaces:**
- `ScanCard(int entityId, Component name, float hp, float maxHp, float armor, float toughness, List<EffectLine> effects, List<Component> resists, float attackDamage, float moveSpeed, Component weakSpot)`.
- Resists (all read-only queries, no test hits): `fireImmune()`, active Resistance effect level (−20 % per level), Fire Resistance effect, armor + toughness attributes, knockback resistance attribute; for vanilla mobs also `isInvulnerableTo(src)` **queries** for explosion/projectile/magic sources built from `damageSources()` (no damage applied). For players the hero part comes only from `HeroRegistry.get(target).scanInfo(target)` — hero protections live in `HeroDamage`/`PlayerStatsMixin`, not in `isInvulnerableTo`.
- Weak spots (`WeakSpots`, only real vanilla damage rules): `MobType.UNDEAD` → «Небесная кара, зелья исцеления» (Smite bonus, instant health harms undead); `MobType.ARTHROPOD` → «Бич членистоногих»; `isSensitiveToWater()` → «вода»; players → `scanInfo(target).weakSpots()`; nothing matched → «не обнаружено». Rules are a data list keyed by vanilla properties, never hero ids.
- Done → card S2C to the owner, highlight via the `SCAN` owner section with a 200 t expiry (THREATS/MARKS updates do not touch it); cancelled on death, range > 48, LOS lost > 10 t, helmet open.

- [ ] Step 1: Tests: `needs30TicksOnSameTarget`, `targetChangeResets`, `cancelledWhenHelmetOpens`, `cancelledOnTargetDeath`, `fireImmuneListed`, `undeadWeakSpot`, `plainMobHasNoInventedWeakSpot` (cow, iron golem → «не обнаружено»), `playerWithoutHeroInfo`, `regulusLionActiveVsInactive`, `homelanderReductionListed`, `resistanceEffectListed`, `analysisDoesNotChangeHpOrState`, `hiddenFromScanRefused`, `highlightLasts200`, `threatUpdateKeepsScanExpiry`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): scan analysis`.

### Task 5: Scan (client) and shared outline

**Files:**
- Create: `core/client/render/vfx/OutlineTargets.java` (extracted from `client/homelander/FocusClient` outline code: per-entity colour, through walls via the vanilla outline buffer), `core/client/ironman/scan/ScanReticle.java`, `core/client/ironman/scan/ScanCardRenderer.java`
- Modify: `core/client/homelander/FocusClient.java` (use `OutlineTargets`, no behaviour change)

- [ ] Step 1: Extract outline; Homelander tests + screenshot unchanged.
- [ ] Step 2: Reticle: rotating brackets filling over 30 t on the target; card at the right edge (compact rows, icons for effects), fades after 10 s; highlight in JARVIS cyan for 10 s.
- [ ] Step 3: Commit `feat(ironman): scan card and highlight`.

### Task 6: Countermeasures

**Files:**
- Create: `core/entity/FlareEntity.java` (short-lived, bright pixel flare, gravity, 60 t), `core/hero/ironman/Countermeasures.java` (pure timers + selection), `core/client/ironman/FlareRenderer.java`
- Modify: `core/entity/MicroMissileEntity.java` (already `Homing`), `IronManHero.java`, `IronManAbilities` (`ironman:countermeasures` page 2 slot 2, cooldown 400 t in `extraCooldowns[1]`)
- Test: `test/hero/ironman/CountermeasuresTest.java`

**Interfaces:**
- Press → fan of 8 flares from back/shoulders; every `Homing` projectile within 24 blocks targeting the player → `retarget(nearest flare)`; vanilla homing (`ShulkerBullet`) → retargeted via its target setter; mobs (`Mob`) whose target is the player within 32 blocks → target cleared and re-cleared for 40 t (`Countermeasures.ForgetList`), then normal. Bosses ignored. Cooldown 400 t, no energy (spec §11.3).

- [ ] Step 1: Tests: `cooldown400`, `noEnergyCost`, `mobsForgetFor40`, `bossesIgnore`, `homingRetargeted`.
- [ ] Step 2: FAIL → implement → PASS. Commit `feat(ironman): countermeasures`.

### Task 7: Sounds, icons, lang, ship

- [ ] Sounds: helmet open/close, scan loop/complete, flares launch/burn (script `tools/sfx/ironman_stage3.sh`), JARVIS lines from Codex.
- [ ] Icons `scan, countermeasures, helmet` (16×16, panel style). Lang (all locales): scan card labels, weak spot texts, JARVIS subtitle texts, HUD.
- [ ] `NoHeroBranchTest`, build, checker, `runServer`; in-game list = spec §21 row "Шлем, JARVIS, сканирование…"; Homelander focus regression.
- [ ] Version bump, memory bank (`OutlineTargets`, `scanInfo`, `hiddenFromScan`), `hero-seam.md`, `SESSION.md`. PR «Железный человек — этап 3: шлем, JARVIS, сканирование, контрмеры», body starts with «Для игрока».

---

## Review log

Plan reviewed against spec §10, §11, §15.2, §16 and Stage 1–2 interfaces (reviewer pass, 2026-10-09). Fixed:
1. Helmet auto-close "в бою": defined as damage with a living attacker or projectile (fall/fire do not close it).
2. Weak-spot table was vague → ordered data rules on vanilla properties + hero hook.
3. JARVIS voice/hints explicitly gated by the closed helmet (spec §11.1 "работает при закрытом шлеме").
4. Fallback when the Codex JARVIS files are missing: subtitles + chime, stated in the PR.
5. Durability on the HUD is a placeholder until Stages 4–5.

Fixed after PR review (2026-10-09):
6. Fixed after PR review: threats/scan used the single owner `int[]` → Stage 2 typed sections `THREATS` and `SCAN` with expiry; test `threatUpdateKeepsScanExpiry` (Tasks 3–4).
7. Fixed after PR review: invented weak spots («голова (крит)», «холод / ближний бой», creeper «дальний бой») removed; only real vanilla damage rules, else «не обнаружено» (Task 4).
8. Fixed after PR review: hero resists cannot come from `isInvulnerableTo` → descriptive `HeroDefinition.scanInfo(Player)` mirroring `HeroDamage`/`PlayerStatsMixin`, Regulus and Homelander implement it; no test hits (Task 4 Files/tests).
9. Fixed after PR review: countermeasures cooldown goes through `extraCooldowns[1]` (Task 6).
