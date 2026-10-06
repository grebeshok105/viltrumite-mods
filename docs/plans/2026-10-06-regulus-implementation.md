# Regulus Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Implement tasks in dependency order. Use test-first development for state transitions and numeric rules.

**Goal:** Implement all Regulus acceptance criteria without changing the approved design or existing Viltrumite visuals.

**Architecture:** Add a shared hero contract at the existing Player mixin boundary. Keep Viltrumite logic and data compatible; route new hero ticks, input, damage policies and skin resolution through the contract. Regulus owns server state, while clients consume a read-only synchronized visual snapshot. World-owned control and projectile records are separate from player lifetime.

**Tech Stack:** Forge 47.3.0 / Minecraft 1.20.1, Java 17, Mojang mappings, existing Mixin/player-pose/VFX systems, JUnit 5.

**Spec:** `docs/design/2026-10-05-regulus-design.md` (copied unchanged from PR #2).

## Global Constraints

- PvP balance only. Mobs remain valid carriers and ability targets as the spec allows.
- Server owns damage, attributes, timers, target selection, control, block destruction and cooldowns.
- No core imports in flight. No client imports in common code.
- No EMF, Veil, Photon, PlayerAnimator, Emotecraft or GeckoLib animation. No second post chain.
- Preserve registry ids, synced legacy data and NBT migration. Register added members as `@Unique`.
- Cooldown starts use `ceil(base * (1 - .03 * H))`, with H sampled at that moment.
- In-game/UI verification is explicitly excluded by the user. Run code gates and a dedicated server smoke check instead.
- PR title is Russian; description starts with “Для игрока”, followed by English technical notes. Do not merge.
- Work in the fresh feature-branch clone; do not create another worktree or modify other repositories.

## Review Focus

1. Reconnect and respawn must preserve the consumed totem and hero identity, but not live casts/hearts.
2. Multiple Regulus players must not double-control a target or prematurely thaw each other's projectiles.
3. Direct-health/internal damage, totems and deferred damage must share death handling without bypassing controlled-target protection.
4. Forged C2S input must not equip/use another hero's powers, fly as Regulus, or act while frozen.
5. A caster death preserves an existing dome; race change/disconnect explicitly remove that caster's dome.

## Structure and contracts

Root below means `viltrumitecore/src/main/java/dev/baranhan/viltrumitecore/`.

- `hero/HeroId.java`: stable HUMAN, VILTRUMITE, REGULUS ids; legacy boolean migration.
- `hero/HeroDefinition.java`, `hero/HeroRegistry.java`: shared tick/input/lifecycle and capability policies. Common code asks these contracts, not concrete-character branches.
- `hero/HeroPlayer.java`, `mixin/PlayerHeroMixin.java`: synchronized hero id and display snapshot; persistent hero-session metadata. Existing `ViltrumiteCorePlayer.isViltrumite/setViltrumite` are compatibility adapters.
- `hero/regulus/RegulusState.java`: server-only action lock, countdowns, hearts, attacker record, ritual/madness, persistent totem flag. Do not persist transient world effects.
- `hero/regulus/RegulusRules.java`: pure numeric/transition functions for JUnit, used by production.
- `hero/regulus/RegulusHero.java`: hero implementation; delegates cohesive mechanics, never adds Regulus branches throughout the old 1500-line mixin.
- `hero/regulus/RegulusHearts.java`, `RegulusPassives.java`, `LionsHeart.java`: carriers, jump/landing/totem, immunity/projectile/overheat mechanics.
- `hero/regulus/DebrisKick.java`, `Mania.java`, `GreedsEmbrace.java`, `Counter.java`, `Evangelium.java`: ability timelines and effects in specified order.
- `hero/control/ControlManager.java`: server-level target UUID ownership, PULL/FREEZE/STASIS, fixed position, saved gravity/AI, capped deferred damage. World-owned domes survive caster death.
- `hero/HeroDamage.java`: normal/deferred/internal damage routing with proper source ownership, death/totem handling and heart scaling.
- `network/packet/HeroInputC2SPacket.java`: bounded action id plus press/release/jump intent. Validate sender, race, equipped active slot and control/action state.
- `client/hero/HeroSkins.java`: default skin, variant provider, separate hand/model selection and suppressor predicates. Only client hero registration knows the Regulus texture paths.
- `client/hero/RegulusClient.java`: consumes server snapshot and drives existing animation/VFX patterns. Poses remain in `client/mixin/`; world VFX remain in `client/render/vfx/`.

The foundation worker fixes exact snapshot accessors and module signatures in this plan before dependent tasks start. Required payload: action id, action elapsed/length/event, busy status, heart count, own carrier ids, Lion active/window/overheat, madness/ritual timers, cooldowns, target id, dome center/lifetime, impact events. Private carrier ids go only to their owner, never public tracking metadata. Public fields go to tracking clients and self. Transient VFX use sequenced S2C events where entity data is insufficient.

Shared entry points: `HeroRegistry.get(Player)` returns a definition; definition supplies `allowsFlight`, `allowsLegacyAbilities`, `allowsExternalControl`, `canAct`, `tick(ServerPlayer)`, `handleInput(ServerPlayer, action, pressed)`, and lifecycle cleanup. A client uses snapshot queries only. The implementation may use immutable typed records instead of a CompoundTag for display state; keep packet size bounded and do not sync server object references.

## Task 1: Third race, hero seam, skins and passive base (§2–4)

**Modify:** PlayerEntityCoreMixin, PlayerStatsMixin, CoreEvents, RaceChoiceC2SPacket, RaceSelectionScreen, PlayerAbilityMixin, AbilityInputManager, legacy ability packet validation, CoreMessages, AbstractClientPlayerMixin, first-person hand skin path, ability selection/HUD filters, all lang files, core build.gradle and mixin configs.

**Create:** shared hero files, RegulusState/Rules/Hero/Passives, HeroSkins, hero input/snapshot packets, JUnit setup and `src/test/java/.../hero/HeroIdentityTest.java`, `RegulusRulesTest.java`.

- [ ] Write failing tests for legacy boolean → id, unknown id rejection, cooldown endpoints, consumed-totem clone/reload, hero capability separation; observe RED.
- [ ] Implement stable id with old `IsViltrumite` fallback; preserve old accessor and chosen-race API. Copy all 18 slots on clone, not only six. Clear incompatible slots on actual hero change.
- [ ] Introduce hero definition hooks at narrow points; preserve legacy visuals and behavior. Flight remains dependent on core-granted permissions, not core classes.
- [ ] Validate third-choice C2S and add the head base/hat layers next to its button. Register default Regulus skin with broad arms; override cosmetics/cape through generic policy. Separate first-person hand provider; variant/suppressor fallback tests.
- [ ] Add base 20 HP, +70 armor, full knockback resistance and passive effects; remove only own attribute modifiers/effects at cleanup. Never inherit base attack 19 or Viltrumite flight/grab.
- [ ] Server-valid jump charge up to ~10 blocks, no flight; landing threshold ~8, radius ~5, damage ~5 and knockback. Send only input intent from client.
- [ ] Persistent one-use hero-session totem, no reset on respawn/reconnect; bypass for void/kill. Honor deferred control damage. Reset only on an actual new hero session.
- [ ] Observe GREEN for tests and compile changed classes. Commit `feat(hero): add race contract and Regulus foundation`; push handoff branch.

## Task 2: Hearts then Lion's Heart (§5–6)

**Create/modify:** RegulusHearts, LionsHeart, HeroDamage, RegulusState/Rules/Hero, ControlManager guard, narrow grab/exhaustion/damage hooks, owner snapshot fields; tests `RegulusHeartsTest`, `LionsHeartTest`.

- [ ] RED tests: H 0..12, ceil rounding, heart-loss window shrink/new-heart non-extension, overheat curve/HP4 cutoff, manual/forced cooldown, internal vs external classification, shared projectile ownership and carrier death vs unload.
- [ ] Scan radius20 every20t: minecraft living, non-player/non-Enemy, no heartless tag, <=12; UUID ownership one per caster per carrier, retain beyond radius.
- [ ] Carrier death: internal 10% maxHP and WeaknessI60; unload no backlash; cleanup no backlash; owner-only gold pulse list.
- [ ] Lion windup14, interrupted by actual damage before event, no cooldown. Active locks ability slots but not normal walking/melee; ritual exception is explicit (§16).
- [ ] Every tick shrink window `60+40H`; freeze nearby projectiles radius4 and save/restore gravity across all owners. External control/grab does not latch. Preserve internal damage and attacker capture for blocked hits.
- [ ] Overheat1.5HP/s then +.5HP/s each40t; force off HP<=4, cooldown600; manual off100; push impulse~1.5 and restore projectiles.
- [ ] GREEN, targeted tests/compile, logical commit and push.

## Task 3: Debris Kick (§7)

**Create/modify:** DebrisKick, shared approved destruction helper, RegulusRules/State/Hero and tests.

- [ ] RED for ray damage endpoints7/2, nine independent hits, cone35°/range16, three-block quota, cancelled-before14/noCD and post-event unlocked recovery.
- [ ] Timeline44t, SlownessI until14, visual rise11, effect14. Nine block-aware rays; first living hit per ray, bypass hurt invulnerability only to realize all nine independent hits. Respect bedrock/unbreakable and mobGriefing using existing Viltrumite destruction rules.
- [ ] Multiply by heart bonus; knockback away; cooldown400 at event. Unlock action after14 while animation continues.
- [ ] GREEN and compile; commit/push.

## Task 4: Mania then Greed's Embrace (§8–9)

**Create/modify:** Mania, GreedsEmbrace, ControlManager, control S2C snapshot/action hooks, RegulusState/Rules/Hero; tests for transitions/queues.

- [ ] RED for control exclusivity across casters, queue caps40%/35%, normal release vs interruption, timeout120, freeze80, dome80 with one-shot capture, caster death vs race/disconnect cleanup, restore gravity/AI and despawn discard.
- [ ] Mania windup19 selects first living target within100 only at event; rejects controlled/grabbed/Lion targets. Pull is additive .6/t, channel caster slow/nojump. Cruise/sprint flight escapes; hover does not. Ordinary damage does not break channel.
- [ ] Normal release or timeout → freeze80. Death/range/grab/stun interruption → no freeze. Every channel end samples H for cooldown500.
- [ ] Freeze/stasis anchors position without gravity and disables attacks/abilities. Queue all damage routes and cannot die while controlled; at release apply once with source attribution then knockback if applicable.
- [ ] Embrace locks point13 up to40, appears18/radius8/duration80, pose recovery32. Capture once excluding caster/tames/team/already-controlled/Lion; cooldown700 at appearance. Death leaves dome alive; explicit hero exit/disconnect removes it; one dome per caster.
- [ ] GREEN/compile and separate Mania/Embrace commits; push.

## Task 5: Counter then Evangelium/madness (§10–11)

**Create/modify:** Counter, Evangelium, item registration/class/model/texture, RegulusState/Rules/Hero and attacker/damage/lifecycle hooks; tests.

- [ ] RED for living attacker240t/range40, missing target/noCD/actionbar, lift20→slam7, last-position fallback/cooldown800, capped damage `min(45,15+.15*maxHP)*(1+.02H)`.
- [ ] Track actual living source including blocked/projectile/ability/grab-throw hits before madness. Counter only during madness; teleport behind, recompute at slam; invalid target retains last position/no target damage; visual explosion, crater depth8/radius3 using approved destruction; stop target flight.
- [ ] RED ritual60 movement/damage>=4/release cancellation400; Lion permits ritual, blocked hits do not interrupt; madness900, price .6/20t, completion cooldown1800, persistent totem not renewed.
- [ ] Grant non-droppable/non-throwable Evangelium on race choice and respawn if absent; server item use holds ritual. No active-madness reentry. Use item class plus lifecycle/drop hooks, not new sounds.
- [ ] Madness modifiers +10armor/+20%maxHP/+.4attack, upgraded Speed/Strength/JumpIII, Regeneration/ResistanceI; price is clean internal damage. Remove owned modifiers when ends and restore ordinary skin through variant provider.
- [ ] GREEN/compile, separate Counter and book commits, push.

## Task 6: Complete visual/readability layer (§13–15)

**Create/modify:** Regulus model/first-person mixins and weight manager; Regulus VFX in existing packages; GameRendererDashMixin and both uniform JSONs/fragment shader; Regulus HUD and five icons, blood sprite textures, heart/window textures; ability registry/input screens/lang; animation-system skill if extended.

- [ ] Add code-level contract tests for all six action/event timings and client/common boundaries, resource existence, icon dimensions, shader uniform parity and mixin registration. Observe failures before implementation.
- [ ] Implement distinct first/third-person chest/king, kick, grasp/pull, dome hand raise/open/lower, lift/slam and two-hand book poses, with shadow-pass handling, sleeves/hat/pants copying and correct pivots. Recovery visuals must not re-lock server actions.
- [ ] Pixel-style world feedback: Lion distortion/white frozen projectiles, owner-only carrier pulse, debris rise/dust/cone, gold tether/target outline, translucent one-shot dome and burst, teleport flash/speed lines/crater dust, book runes, landing wave.
- [ ] Extend existing dash_impact only: overheat cracks/noise, heart-loss flash, impact shake, madness black bands/glitch/light zoom/FOV. Use existing/vanilla heartbeat sound. No banned symbol.
- [ ] Existing six-key/three-page panel shows five hero icons/cooldowns/disabled predicates; additional sprite heart counter, Lion window (red pulse overheat), madness timer and sprite blood overlay (no rectangles).
- [ ] Age VFX by ticks+partialTick, clear world caches, restore GL state; no new framework or animation engine. GREEN resource/contract checks and compile, commit/push.

## Task 7: Integration, adversarial review, gates and delivery

- [ ] Integrate stage outputs with fast-forward ancestry; no duplicate file ownership or hidden merge conflict resolutions. Audit §18 all16 criteria plus §16 interactions against source/test evidence.
- [ ] Run independent review wave: spec/gameplay; architecture/network/security/lifecycle; tests; visuals/resources/docs. Reviewers read code only and do not runClient/UI. Cross-check findings and fix confirmed ones with regression tests.
- [ ] Remove temporary scaffolding/no-op hooks/TODO implementations. Update README, existing memory-bank relations and append SESSION.md. Bump core feature version to1.11.0; bump flight to1.7.0 only if its code changes.
- [ ] Run fresh `./gradlew build --no-daemon` for both modules and all JUnit tests, `git diff --check`, resource JSON validation, static/common-client checks. Existing project has no separate lint/typecheck task: javac/annotation processing and explicit checks are the relevant equivalents.
- [ ] Prove dedicated server startup with both modules and new mixins, then stop server. Do not run any game client or capture UI.
- [ ] Create one focused Russian-title PR against main (spec and two skins included; original spec PR is not merged). Follow fetched template; document any HOW deviation and in-game unverified status.
- [ ] Settle CI and review comments; never merge. Deliver brief Russian summary and built jars if useful. Parent independently reruns integrated gates; worker reports are not completion proof.

## Orchestration

The mutable combat state requires serial feature phases, matching the requested ability order. A deterministic workflow passes each pushed branch + exact contracts to the next separate-VM worker: foundation → hearts/Lion → Debris → Mania/Embrace → Counter/book → visuals → integration. Plan review precedes implementation. Each stage returns branch, commit, contract notes, checks and limitations; failures abort, never silently skip. Four independent final reviewers use separate sessions, and the coordinator applies/final-verifies their findings. This uses more ACU than inline work but isolates phase context and gives resumable handoffs. No shared-VM workers or in-game verification.
