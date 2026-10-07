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

The following contracts are the phase handoff, not implemented APIs. A worker must record any necessary signature adjustment here before a dependent phase consumes it. All implementation checkboxes remain unchecked until code and gates prove completion.

### Hero, lifecycle and flight boundary

- `HeroPlayer`: `HeroId getHeroId()`, `HeroSession getHeroSession()`, `HeroPublicSnapshot getHeroSnapshot()`. `HeroSession` is immutable `(HeroId heroId, UUID sessionId, boolean totemConsumed)`; client snapshots never expose its mutation or the totem flag.
- `HeroRegistry`: `HeroDefinition get(Player player)`, `void changeHero(ServerPlayer player, HeroId id)`, `void restoreHero(ServerPlayer player, HeroSession session)`. Change creates a session only when the id actually changes; restore never enters a new session. `isViltrumite()` projects canonical identity; legacy `setViltrumite(boolean)` changes only HUMAN/VILTRUMITE identity and cannot convert/clear a different hero. Explicit hero changes/resets use `changeHero`; NBT load/clone uses restore, not the legacy setter. Coordinate the existing `readAdditionalSaveData`/clone calls so their boolean write cannot overwrite a restored new id; keep writing the legacy projection for old readers.
- `HeroDefinition`: `HeroId id()`, `boolean allowsFlight(Player player)`, `boolean allowsLegacyAbilities(Player player)`, `boolean allowsExternalControl(LivingEntity target, ControlKind kind)`, `boolean canAct(ServerPlayer player, HeroAction action)`, `void tick(ServerPlayer player)`, `void handleInput(ServerPlayer player, HeroAction action, boolean pressed)`, `void cleanup(ServerPlayer player, CleanupReason reason)`. Non-player control targets use the same control guard with neutral hero policy. Reasons distinguish DEATH, DISCONNECT and HERO_CHANGE; cleanup is idempotent and precedes transient-state discard.
- NBT adds `HeroData` with `Id` (stable string), `SessionId` (UUID), `TotemConsumed` (boolean). A valid new id wins over `IsViltrumite`; absent new data migrates the old boolean once and retains `HasChosenRace`. Restore consumed state on every clone (death and non-death) and load; never renew it in join/respawn hooks, totem activation or madness entry. Preserve all 18 slot keys and the chosen-race API. Missing session metadata is initialized once during migration, not each tick. Actual hero exit clears transient state; selecting the current hero is not an exit/re-entry exploit.
- Ordinary Regulus health is 20; melee uses vanilla attack and effects, not `ViltrumiteStatHolder.getBaseDamage()` (initialized to 19). Keep legacy stat data compatible without applying it to Regulus. Prevent ScourgeVirusEffect's legacy conversion calls from converting Regulus; explicit administrative race changes use the lifecycle boundary.
- Flight owns a core-independent `FlightPermission` contract: `boolean allowsModFlight(Player player)`; `FlightPermissions.setPolicy(FlightPermission policy)` registers the core adapter and `FlightPermissions.allowsModFlight(Player player)` queries it. Without core, preserve the flight module's existing `mayfly` behavior. Core delegates the policy to `HeroDefinition.allowsFlight` and the control guard. Do not use `mayfly` alone: creative/spectator and legacy grab temporarily grant it. Regulus never enters mod flight; do not break vanilla creative/spectator movement.
- Gate `FlightToggleC2SPacket`, `HoverInputC2SPacket`, `FlightAccelerateC2SPacket`, `FlightSpeedLockC2SPacket`, flight ticking/state entry and collision handling through that permission. Flight-command-granted `mayfly` cannot bypass it. Core's `CoreFlightHookMixin` (priority1500) must not cancel collision handling for a denied hero merely because stale throttle is >=.6. `PlayerEntityCoreMixin`'s flight/cruise collision/no-physics paths also require the hero capability. On hero change/load/clone clear mod state, throttle, acceleration, hover input, lock/takeoff counters and mod-owned noPhysics/flying flags; preserve legitimate vanilla permissions. Current `stopFlight()` alone does not establish this reset contract.
- Server validation covers stable-id race selection, equip slot0..17, page0..2, hero-owned ability ids, active equipped slot, press/release state and control/action locks; never trust client target/position/timer/heart count. Preserve legacy boolean packet decoding; add stable-id selection at an appended packet registration instead of changing existing wire layouts/ids. Gate old ability packets as well as the new packet and reject Scourge conversion into Viltrumite. Frozen/stasis targets cannot jump, attack, use items/abilities or move via forged input; PULL does not remove their actions. Lion permits normal movement/melee, its off-toggle and Evangelium, not other slots. Grab movement interrupts Mania and ritual outside Lion.
- Press validates equipped slot and start availability. Release addresses the server's active cast, even after page/equipment changes and while its action lock is set; it cannot start/select a different action. A stale/duplicate release is harmless. Normal Mania release must reach the channel after page swap; interrupted control uses its own end reason. Gate legacy server action ticks (punch/dash/drill/grab/block/superspeed/chop/thunderclap/barrage), not just packets; on hero exit clear their old flags/timers/owned modifiers so restored stale state cannot run. Leave world managers ticking independently of the player's legacy-action eligibility.

### Snapshot and generic skin boundary

- `HeroPublicSnapshot` is immutable and server-produced: hero id, monotonic sequence/server tick, action id/start/duration/event tick, action-lock status, public heart count, Lion active/window/overheat, ritual/madness deadlines, ability cooldown deadlines and current target entity id. `HeroOwnerSnapshot` carries only the owner's carrier UUID/entity-id list (<=12) and private HUD data. Public snapshots go to self and tracking players; owner packets go only to self. Clients derive elapsed time from the server epoch, clamp it, and never advance gameplay.
- Send a full baseline on login, respawn, dimension change and `PlayerEvent.StartTracking`; send changes while tracking and sequenced impact/removal events. Late trackers see the current skin/action, not only future deltas. Entity UUID plus dimension validates temporary entity ids; ignore stale sequences and clear caches on world/player removal. Packet counts/lengths are bounded, and no live server objects are serialized.
- World control/dome snapshots are separate from the caster snapshot: dimension, stable dome UUID, caster UUID, center/radius, creation/expiry ticks and controlled-target anchor/type/expiry. Send baselines to dimension/chunk observers and target clients even when the caster is dead/untracked. Do not make dome ticking, VFX or queue ownership depend on a live `ServerPlayer`. Control snapshots cover mobs and non-hero players, not just `HeroPlayer` targets.
- `HeroSkins.Provider` is client-only: `ResourceLocation defaultSkin()`, `String defaultModelName()`, `Optional<ResourceLocation> skinVariant(AbstractClientPlayer player)`, `Optional<ResourceLocation> handTexture(AbstractClientPlayer player)`, `Optional<String> modelName(AbstractClientPlayer player)`, `boolean suppressOverride(AbstractClientPlayer player)`, `boolean suppressCosmetics(AbstractClientPlayer player)`, `boolean suppressCape(AbstractClientPlayer player)`. `HeroSkins.resolve(AbstractClientPlayer player)` returns `Optional<ResolvedHeroSkin>` containing skin/hand/model and suppression policy. Register by hero id once; only Regulus client registration knows paths and madness state. Empty variant uses default, empty hand uses resolved skin, empty model uses provider default (Regulus `default`, broad arms). Suppressor predicates can temporarily disable the override; do not confuse that with suppressing legacy cosmetics/capes.
- Wrap the existing `AbstractClientPlayerMixin` skin/model/cloak paths and the vanilla `PlayerRenderer.renderHand` texture lookup for both arms through that resolver. Leave first-person pose/pivot fixes in flight intact. Suppress the separate cape path and cosmetic replacement while Regulus is active; no `HeroId.REGULUS` checks in render mixins, no client resource paths in common. Test default/variant/empty/suppressed fallback, both hands, broad model and legacy cosmetic/cape preservation.

### Damage ordering and totem boundary

- Current priorities are `PlayerStatsMixin`1, `BlockDamageMixin`1000 (default), `PlayerEntityCoreMixin`1200 and `PlayerAbilityMixin`1300. `LivingEntityStatsMixin` and optional `WroughtnautDamageBypassMixin` are default1000; Wroughtnaut directly calls `setHealth`/`die`. Player and LivingEntity `hurt` are different entry points; a LivingEntity HEAD hook alone cannot precede Player's early returns or the optional subclass bypass.
- Add narrow shared damage interceptors at Player/LivingEntity `hurt` with explicit priority4000, above these hooks, but verify the transformed callback order, not just priority numbers. Legacy block/stat modifiers must bypass processing while the shared route owns a controlled/internal/deferred hit. Uncontrolled legacy Viltrumite hits keep their old reduction/block order and visuals. Adapt Wroughtnaut's direct-health branch to call the same guard before changing health, rather than relying on the superclass injection. Inventory other owned direct-health/`die` routes; do not globally cancel unrelated healing or administrative health changes.
- `HeroDamage.route(LivingEntity target, DamageSource source, float amount, DamageKind kind)` returns `DamageResult` (PASS, BLOCKED, QUEUED, APPLIED); kinds are EXTERNAL, INTERNAL and DEFERRED_RELEASE. It is server-only, rejects invalid/nonpositive amounts and uses a scoped try/finally re-entry guard. Order: capture a real living attacker for an external attempt once (including blocked Lion hits); queue if FREEZE/STASIS; apply Lion's external block; pass ordinary external damage to the usual mitigations; apply clean internal/deferred damage through the shared lethal/totem path. Queue the normally payable HP loss: evaluate applicable vanilla/legacy/compat external mitigation once without HP loss, use unmitigated internal damage, and cap the sum (also clamp against current maxHP on release). A queued hit cannot also trigger legacy damage/death, hurt interruption or knockback. Release removes ownership first, then applies the capped sum once without re-queueing, double heart scaling or a second armor/Resistance pass. Preserve the contributing living source for death/Counter attribution; environmental/internal damage never invents a living attacker.
- `HeroDamage.applyInternal(ServerPlayer player, float amount, InternalDamageCause cause)` covers backlash, overheat and blood price; clean HP loss ignores armor, Resistance and Lion, but still queues under freeze/stasis. Actual immediate HP loss interrupts pre-event casts, not blocked/queued attempts; Mania channel ordinary damage does not interrupt. Ritual's >=4HP rule applies outside Lion; while Lion is active only movement/release cancels it (§16).
- `HeroDamage.tryHeroTotem(ServerPlayer player, DamageSource source)` is the common lethal boundary, including clean HP subtraction and deferred release. Wire ordinary lethal hurt via `LivingEntity.checkTotemDeathProtection(DamageSource)` and call the same boundary for owned direct-health routes before `die`. Check `FELL_OUT_OF_WORLD`/`GENERIC_KILL` before revival. Mark `HeroSession.TotemConsumed` before restoring health and vanilla-equivalent effects/status feedback; preserve the consumed value on save/clone. A successful rescue is not death cleanup and never creates another session. Prevent duplicate vanilla/custom revival for the same lethal hit. Tests must use production routing/transitions, not constant-only assertions.
- Counter attacker capture uses the living shooter/ability caster, not a projectile entity or vanilla last-hurt-mob fallback. `PunchImpactManager.MeteorData` currently carries velocity/ticks only and its later collision uses `flyIntoWall()` without an attacker: retain the launching UUID/source through grab/throw collision damage so Counter can identify the real offender. Internal ticks/backlash do not replace that record.

### Control, projectile and world ownership

- `ControlManager.get(ServerLevel level)`, `boolean tryAcquire(LivingEntity target, UUID caster, UUID effectId, ControlKind kind)`, `boolean transition(UUID target, UUID effectId, ControlKind next)`, `void release(UUID target, UUID effectId, ReleaseReason reason)`, `void cleanupCaster(UUID caster, CleanupReason reason)`, `void tick(ServerLevel level)`. One server-thread map keyed by target UUID owns PULL/FREEZE/STASIS plus a VILTRUMITE_GRAB adapter; transition/release require the matching effect id so stale cleanup cannot release another caster's control. No lockout after release; Mania then dome is legal. Lion immunity rejects acquisition before side effects.
- Precedence: any existing Regulus control rejects Mania/dome; legacy grab rejects Mania; FREEZE/STASIS reject legacy grab. PULL still permits legacy grab as counterplay: end the channel with no freeze and start its cooldown before handing ownership to grab. `ControlKind.IMPULSE` is a policy query only, not an acquired record; guard both direct grab latch and later throw/meteor/push/setDeltaMovement paths, not only `knockback()`. Apply guards on the victim and preserve voluntary movement. Do not call a generic `canAct` that accidentally disables all movement/melee while Lion is active.
- Each anchored record saves position/dimension, original noGravity, original Mob.noAi, owned input/movement flags, queue and source attribution. Stop AI goals/navigation during FREEZE/STASIS. Restore saved values, not unconditional `noAi=false`/`noGravity=false`; legacy release/failsafe paths must not overwrite a current control owner's flags. Remove stale launched-entity motion before anchoring. Normal expiry or explicit caster cleanup restores/replays once, then dome pushes surviving targets; despawn/unload/target disconnect discards the unavailable target's queue and does not retain it for reconnect. PULL does not queue damage or suppress target actions; frozen targets stay fixed in air.
- `DomeRecord(UUID id, UUID caster, ResourceKey<Level> dimension, Vec3 center, long createdAt, long expiresAt)` lives in the level manager, not RegulusState; target records link its id. Capture once at appearance18, excluding caster, tames, scoreboard teammates, controlled/grabbed and Lion-immune targets. One dome per caster is enforced in the world index across respawn, not a reset player boolean. DEATH cleans the caster's personal states/hearts/channel but keeps an existing dome and its targets through expiry; HERO_CHANGE/DISCONNECT removes it and restores/releases targets. Tick each server dimension even when the caster has no live entity. No persistence of transient domes across restart is needed.
- Projectile records key by projectile UUID with original noGravity and a set of freezing caster/session UUIDs. Multiple Lion owners cannot thaw each other's projectile. Final release leaves velocity zero and restores original gravity so ordinary projectiles fall; do not restore saved flight velocity. Death/hero exit/disconnect removes only that owner's membership, and unload discards the record. This is separate from living-target control ownership.

### Acceptance coverage and future regression gates

This table confirms plan coverage, not implemented acceptance. Phase workers add the named tests and observe RED/GREEN against production logic. Foundation sets up JUnit first; later phases run targeted `:viltrumitecore:test --tests '*.<TestClass>'` plus `:viltrumiteflight:compileJava :viltrumitecore:compileJava --no-daemon` as relevant. Parent alone repeats full build/resource/server gates.

| Spec §18 | Task | Required regression/contract evidence |
|---|---|---|
| 1 | 1 | HeroIdentityTest: third id, choice validation, legacy migration; head base/hat resource wiring |
| 2 | 1, 6 | HeroSkinContractTest: public late-tracker madness skin, both hands/model, cosmetic/cape suppression/fallback |
| 3 | 2 | RegulusHeartsTest: namespace/Enemy/heartless/12 cap, shared carrier death vs unload, owner-only ids |
| 4 | 2 | LionsHeartTest: shrink-only window, internal overheat curve, HP4 cutoff |
| 5 | 2, 4 | HeroDamageTest/ControlManagerTest: external block vs internal, no grab/push/throw latch |
| 6 | 3 | DebrisKickTest: all nine independent hits, 7-to-2 range formula, block quota/griefing |
| 7 | 4 | ManiaTest: 120t channel,80t freeze,40% queue, cruise escape vs hover |
| 8 | 4 | GreedsEmbraceTest: capture once,80t,35% queue, caster death/respawn world survival |
| 9 | 5 | CounterTest: madness/living-source240t/range40, blocked/projectile/throw attribution, controlled-target queue |
| 10 | 5 | EvangeliumTest:60t ritual,900t madness,.6/20t price,1800 cooldown at madness end |
| 11 | 1–5 | RegulusRulesTest: ceil formula and H sampled at every cooldown start, including book/forced Lion |
| 12 | 2–6 | Timeline tests: no effect/CD before event, no rollback after; ritual400 interruption is explicit exception |
| 13 | 6 | Resource/mixin/shader contracts: existing systems only, two-hand poses, uniform parity, no second chain |
| 14 | 2, 4, 6 | InputPolicyTest: Lion movement/melee/off-toggle/book allowed, other slots/control denied |
| 15 | 4 | ManiaTest: ordinary damage continues channel; all end reasons start CD, only normal release/timeout freezes |
| 16 | 1–5 | HeroLifecycleTest: idempotent death/disconnect/hero-exit, consumed-totem clone/reload, dome death exception |

## Task 1: Third race, hero seam, skins and passive base (§2–4)

**Modify:** PlayerEntityCoreMixin, PlayerStatsMixin, CoreEvents, race selection packet/validation, RaceSelectionScreen, PlayerAbilityMixin, AbilityInputManager, legacy ability packet validation, CoreMessages, AbstractClientPlayerMixin, first-person hand skin path, ability selection/HUD filters, all lang files, core build.gradle and mixin configs; flight permission/packet/state gates (without core imports).

**Create:** shared hero files, RegulusState/Rules/Hero/Passives, HeroSkins, hero input/snapshot packets, JUnit setup and `src/test/java/.../hero/HeroIdentityTest.java`, `RegulusRulesTest.java`.

- [ ] Write failing tests for legacy boolean → id, unknown id rejection, cooldown endpoints, consumed-totem clone/reload, hero capability separation; observe RED.
- [ ] Implement stable id with old `IsViltrumite` fallback; preserve old accessor and chosen-race API. Copy all 18 slots on clone, not only six. Clear incompatible slots on actual hero change.
- [ ] Introduce hero definition hooks at narrow points; preserve legacy visuals and behavior. Implement the flight-owned permission bridge and server gates/reset described above, not a `mayfly`-only gate.
- [ ] Validate third-choice C2S and add the head base/hat layers next to its button. Register default Regulus skin with broad arms; override cosmetics/cape through generic policy. Separate first-person hand provider; variant/suppressor fallback tests.
- [ ] Add base 20 HP, +70 armor, full knockback resistance and passive RegenerationI/SpeedII/StrengthII/JumpII/Fire Resistance; remove only own attribute modifiers/effects at cleanup. Never inherit base attack 19 or Viltrumite flight/grab.
- [ ] Server-valid jump charge up to ~10 blocks, no flight; landing threshold ~8, radius ~5, damage ~5 and knockback. Explicitly deny fall damage while preserving server fall-distance tracking for the landing wave; regression covers both. Send only input intent from client.
- [ ] Persistent one-use hero-session totem, no reset on respawn/reconnect; bypass for void/kill. Honor deferred control damage. Reset only on an actual new hero session.
- [ ] Observe GREEN for tests and compile changed classes. Commit `feat(hero): add race contract and Regulus foundation`; push handoff branch.

## Task 2: Hearts then Lion's Heart (§5–6)

**Create/modify:** RegulusHearts, LionsHeart, HeroDamage, RegulusState/Rules/Hero, ControlManager guard, narrow grab/exhaustion/damage hooks, owner snapshot fields; tests `RegulusHeartsTest`, `LionsHeartTest`.

- [ ] RED tests: H 0..12, ceil rounding, heart-loss window shrink/new-heart non-extension, overheat curve/HP4 cutoff, manual/forced cooldown, internal vs external classification, shared projectile ownership, hunger/debuff immunity and carrier death vs unload. Ordinary melee and ability damage both use the +2% per-heart multiplier once; counter/internal/released damage must not multiply again.
- [ ] Scan radius20 every20t: minecraft living, non-player/non-Enemy, no heartless tag, <=12; UUID ownership one per caster per carrier, retain beyond radius.
- [ ] Carrier death: internal 10% maxHP and WeaknessI60; unload no backlash; cleanup no backlash; owner-only gold pulse list.
- [ ] Lion windup14, interrupted by actual damage before event, no cooldown. Active locks ability slots but not normal walking/melee; ritual exception is explicit (§16).
- [ ] Every tick shrink window `60+40H`; freeze nearby projectiles radius4 and save/restore gravity across all owners. External control/grab does not latch; hunger does not drain and negative effects are removed. Preserve internal damage and attacker capture for blocked hits.
- [ ] Overheat1.5HP/s then +.5HP/s each40t; force off HP<=4, cooldown600; manual off100; push impulse~1.5 and restore projectiles.
- [ ] GREEN, targeted tests/compile, logical commit and push.

## Task 3: Debris Kick (§7)

**Create/modify:** DebrisKick, shared approved destruction helper, RegulusRules/State/Hero and tests.

- [ ] RED for ray damage endpoints7/2, nine independent hits, cone35°/range16, three-block quota, cancelled-before14/noCD and post-event unlocked recovery.
- [ ] Timeline44t, SlownessI until14, visual rise11, effect14. Nine block-aware rays; first living hit per ray, bypass hurt invulnerability only to realize all nine independent hits (scoped/restored per hit, not globally). Respect bedrock/unbreakable and mobGriefing through the approved shared destruction helper. Current PunchImpactManager destruction does not check mobGriefing: add that check for Regulus rather than assuming reuse already enforces it or changing legacy behavior unrelated to this phase.
- [ ] Multiply by heart bonus; knockback away; cooldown400 at event. Unlock action after14 while animation continues.
- [ ] GREEN and compile; commit/push.

## Task 4: Mania then Greed's Embrace (§8–9)

**Create/modify:** Mania, GreedsEmbrace, ControlManager, control S2C snapshot/action hooks, RegulusState/Rules/Hero; tests for transitions/queues.

- [ ] RED for control exclusivity across casters, queue caps40%/35%, normal release vs interruption, timeout120, freeze80, dome80 with one-shot capture, caster death vs race/disconnect cleanup, restore gravity/AI and despawn discard.
- [ ] Mania windup19 selects first living target within100 only at event; rejects controlled/grabbed/Lion targets. Pull is additive .6/t, channel caster slow/nojump. Cruise/sprint flight escapes; hover does not. Ordinary damage does not break channel.
- [ ] Normal release or timeout → freeze80. Death/range/grab/stun interruption → no freeze. Every channel end samples H for cooldown500.
- [ ] Freeze/stasis anchors position without gravity and disables attacks/abilities. Queue all damage routes and cannot die while controlled; at release apply once with source attribution then knockback if applicable. Counter damage joins the same queue; its slam cannot dislodge an anchored target. Normal flight actions may escape PULL, never bypass FREEZE/STASIS.
- [ ] Embrace locks point13 up to40, appears18/radius8/duration80, pose recovery32. Capture once excluding caster/tames/team/already-controlled/Lion; cooldown700 at appearance. Death leaves dome alive; explicit hero exit/disconnect removes it; one dome per caster.
- [ ] GREEN/compile and separate Mania/Embrace commits; push.

## Task 5: Counter then Evangelium/madness (§10–11)

**Create/modify:** Counter, Evangelium, item registration/class/model/texture, RegulusState/Rules/Hero and attacker/damage/lifecycle hooks; tests.

- [ ] RED for living attacker240t/range40, missing target/noCD/actionbar, lift20→slam7, last-position fallback/cooldown800, capped damage `min(45,15+.15*maxHP)*(1+.02H)`.
- [ ] Track actual living source including blocked/projectile/ability/grab-throw hits before madness. Counter only during madness; teleport behind, recompute at slam; invalid target retains last position/no target damage; visual explosion, crater depth8/radius3 using approved destruction; stop target flight.
- [ ] RED ritual60 movement/damage>=4/release cancellation400; Lion permits ritual, blocked hits do not interrupt; madness900, price .6/20t, cooldown1800 at madness END (not ritual completion), persistent totem not renewed. Sample H at interruption/end respectively; ritual cancellation400 is the explicit exception to free pre-event cancellation (§11/16).
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
