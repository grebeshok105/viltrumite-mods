package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.hero.HeroDefinition;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Tony Stark (spec docs/design/2026-10-09-ironman-design.md). No Viltrumite
 * kit: everything comes from the suit.
 */
public class IronManHero implements HeroDefinition {
   private static final UUID ARMOR_ID = UUID.fromString("7c3e9a51-2f6d-4b8e-a1c4-3d5f6e7a8b01");
   private static final UUID TOUGHNESS_ID = UUID.fromString("7c3e9a51-2f6d-4b8e-a1c4-3d5f6e7a8b02");
   private static final UUID KNOCKBACK_ID = UUID.fromString("7c3e9a51-2f6d-4b8e-a1c4-3d5f6e7a8b03");

   @Override
   public HeroId id() {
      return HeroId.IRON_MAN;
   }

   /**
    * Mod flight only while the suit is fully worn. Read on BOTH sides (the
    * flight mixin and input check the local player): the client reads the
    * synced snapshot flag, the server its own state.
    */
   @Override
   public boolean allowsFlight(Player player) {
      if (player != null && player.level().isClientSide()) {
         return suitWornFlag(player);
      }

      IronManState state = IronManState.of(player);
      return state != null && state.wantsFlight();
   }

   @Override
   public boolean grantsFlightAbility(Player player) {
      IronManState state = IronManState.of(player);
      return state != null && state.wantsFlight();
   }

   /**
    * Normal flight is the original (Homelander) flight: no profile. Only the
    * 0-energy glide uses a profile. Both sides: the client reads the synced
    * flags (≤ 1 tick behind).
    */
   @Override
   public dev.baranhan.viltrumiteflight.util.FlightProfile flightProfile(Player player) {
      if (player == null) {
         return null;
      }

      if (player.level().isClientSide()) {
         return profileFor(suitWornFlag(player), glideFlag(player));
      }

      IronManState state = IronManState.of(player);
      return state == null ? null : profileFor(state.wantsFlight(), state.glide);
   }

   @javax.annotation.Nullable
   static dev.baranhan.viltrumiteflight.util.FlightProfile profileFor(boolean worn, boolean glide) {
      return worn && glide ? IronManRules.glideProfile() : null;
   }

   static boolean glideFlag(Player player) {
      return player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer
         && heroPlayer.getHeroSnapshot().heroId() == HeroId.IRON_MAN
         && IronManFlags.is(heroPlayer.getHeroSnapshot().heroFlags(), IronManFlags.Field.GLIDE);
   }

   /** Synced SUIT_WORN bit of an Iron Man snapshot (any side). */
   static boolean suitWornFlag(Player player) {
      if (!(player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer)) {
         return false;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      return snapshot.heroId() == HeroId.IRON_MAN && IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SUIT_WORN);
   }

   @Override
   public boolean allowsLegacyAbilities(Player player) {
      return false;
   }

   @Override
   public boolean allowsLegacyAbility(Player player, String abilityId) {
      return false;
   }

   @Override
   public boolean hasAbilityPanel(Player player) {
      return true;
   }

   @Override
   public boolean allowsAbilityPages(Player player) {
      return true;
   }

   /** No fall damage while armored, including the retract wave (spec §8.6). */
   @Override
   public boolean cancelsFallDamage(Player player) {
      IronManState state = IronManState.of(player);
      return state != null && state.suit.armored();
   }

   @Override
   public boolean ownsAbility(String abilityId) {
      return IronManAbilities.owns(abilityId);
   }

   @Override
   public java.util.List<dev.baranhan.viltrumitecore.ability.ViltrumiteAbility> panelAbilities() {
      return IronManAbilities.panelAbilities();
   }

   @Override
   public net.minecraft.resources.ResourceLocation abilityIcon(String abilityId) {
      return IronManAbilities.icon(abilityId);
   }

   @Override
   public String[] heroInputSlots() {
      return IronManAbilities.slotIds();
   }

   @Override
   public HeroAction heroActionFor(String abilityId) {
      return IronManAbilities.actionFor(abilityId);
   }

   @Override
   public String[] defaultLoadout() {
      return IronManAbilities.defaultLoadout();
   }

   @Override
   public boolean allowsExternalControl(LivingEntity target, ControlKind kind) {
      return true;
   }

   /** Control (spec §16): nothing starts while anchored or dead. */
   static boolean controlled(ServerPlayer player) {
      return HeroDamage.isAnchored(player) || !player.isAlive();
   }

   @Override
   public boolean canAct(ServerPlayer player, HeroAction action) {
      if (controlled(player)) {
         return false;
      }

      return switch (action) {
         case SUIT -> true;
         case SECONDARY_USE, TOOL_CYCLE, UNIBEAM, MISSILES, NANO_ARSENAL, GUARD, HELMET, SCAN, COUNTERMEASURES -> {
            IronManState state = IronManState.of(player);
            yield state != null && state.suit.worn();
         }
         case INTERACT -> true;
         // Same gate as the claim: a forged HeroInputC2SPacket cannot punch on foot.
         case PRIMARY_ATTACK -> {
            IronManState state = IronManState.of(player);
            yield state != null && (claimsPrimary(state.suit.worn(), flightState(player)) || claimsWeapon(state.suit.worn(), state.arsenal.weapon() != null));
         }
         default -> false;
      };
   }

   /**
    * LMB: in flight (fly-by / air strike, spec §6.1) or with a formed nano
    * weapon; otherwise vanilla (nano punch via meleeDamageFactor). RMB in the
    * suit: hero interaction on a HeroInteractable target, else the current
    * RMB tool; Shift+RMB stays vanilla (blocks, doors). MMB cycles the tool.
    */
   @Override
   public HeroAction mouseAction(dev.baranhan.viltrumitecore.hero.MouseButton button, Player player) {
      if (player == null) {
         return null;
      }

      boolean worn;
      boolean weapon;
      if (player.level().isClientSide()) {
         HeroPublicSnapshot snapshot = ((dev.baranhan.viltrumitecore.hero.HeroPlayer)player).getHeroSnapshot();
         worn = suitWornFlag(player);
         weapon = worn && dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.byId(
            IronManFlags.get(snapshot.heroFlags(), IronManFlags.Field.RMB_TOOL)).nanoWeapon();
      } else {
         IronManState state = IronManState.of(player);
         worn = state != null && state.suit.worn();
         weapon = worn && state.arsenal.weapon() != null;
      }

      return switch (button) {
         case PRIMARY -> claimsPrimary(worn, flightState(player)) || claimsWeapon(worn, weapon) ? HeroAction.PRIMARY_ATTACK : null;
         case SECONDARY -> !worn || player.isShiftKeyDown() ? null : interactTarget(player) != null ? HeroAction.INTERACT : HeroAction.SECONDARY_USE;
         case MIDDLE -> worn ? HeroAction.TOOL_CYCLE : null;
         default -> null;
      };
   }

   static boolean claimsWeapon(boolean worn, boolean weaponFormed) {
      return worn && weaponFormed;
   }

   /** Entity under the crosshair (reach 4) that offers a hero interaction. */
   @javax.annotation.Nullable
   static dev.baranhan.viltrumitecore.entity.HeroInteractable interactTarget(Player player) {
      net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
      net.minecraft.world.phys.Vec3 end = eye.add(player.getLookAngle().scale(4.0));
      net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, eye, end,
         new net.minecraft.world.phys.AABB(eye, end).inflate(1.0),
         e -> e instanceof dev.baranhan.viltrumitecore.entity.HeroInteractable interactable && interactable.canHeroInteract(player), 16.0);
      return hit != null && hit.getEntity() instanceof dev.baranhan.viltrumitecore.entity.HeroInteractable interactable ? interactable : null;
   }

   /** F (guard key) raises the shield while any suit is on; the vanilla swap is blocked (spec §6.1, §8.5). */
   @Override
   public HeroAction guardAction(Player player) {
      return suitPresent(player) ? HeroAction.GUARD : null;
   }

   @Override
   public boolean blocksHandSwap(Player player) {
      return suitPresent(player);
   }

   static boolean suitPresent(Player player) {
      if (player == null) {
         return false;
      }

      if (player.level().isClientSide()) {
         return suitWornFlag(player) || player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer
            && heroPlayer.getHeroSnapshot().heroId() == HeroId.IRON_MAN
            && (IronManFlags.is(heroPlayer.getHeroSnapshot().heroFlags(), IronManFlags.Field.DEPLOYING)
               || IronManFlags.is(heroPlayer.getHeroSnapshot().heroFlags(), IronManFlags.Field.RETRACTING));
      }

      IronManState state = IronManState.of(player);
      return state != null && state.suit.state() != SuitState.NONE;
   }

   static boolean claimsPrimary(boolean worn, @javax.annotation.Nullable dev.baranhan.viltrumiteflight.util.FlightState state) {
      return worn && state != null && state != dev.baranhan.viltrumiteflight.util.FlightState.NONE;
   }

   @javax.annotation.Nullable
   static dev.baranhan.viltrumiteflight.util.FlightState flightState(Player player) {
      return player instanceof dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer flightPlayer ? flightPlayer.getFlightState() : null;
   }

   /**
    * Flight energy per tick (spec §5.1, §5.3): HOVER/CRUISE drain, SONIC
    * drains more, grounded regen (Energy.tick after its delay). At 0 the
    * flight turns into a glide (no thrust, no drain) until the bar is back
    * at WEAPONS_UNLOCK or the player lands.
    */
   static void flightEnergyTick(IronManState state, @javax.annotation.Nullable dev.baranhan.viltrumiteflight.util.FlightState flight) {
      boolean flying = state.suit.worn() && flight != null && flight != dev.baranhan.viltrumiteflight.util.FlightState.NONE;
      if (!flying) {
         state.glide = false;
      } else if (state.glide) {
         if (state.energy.value() >= IronManRules.WEAPONS_UNLOCK) {
            state.glide = false;
         }
      } else {
         state.energy.drain(switch (flight) {
            case SONIC -> IronManRules.DRAIN_SONIC;
            case CRUISE -> IronManRules.DRAIN_CRUISE;
            default -> IronManRules.DRAIN_HOVER;
         });
         state.glide = state.energy.empty();
      }

      state.energy.tick();
   }

   /** Fall without flight (e.g. after the suit's flight ended mid-air): real fall distance. */
   @Override
   public void onLanded(ServerPlayer player, float fallDistance) {
      IronManState state = IronManState.of(player);
      if (state != null && state.suit.armored() && fallDistance >= IronManRules.HEAVY_FALL
         && player.level().getGameTime() - state.landedAt > 1L) {
         IronManLandings.heavy(player, state, fallDistance);
      }
   }

   /** Nano punch on the ground (server-side melee). */
   @Override
   public float meleeDamageFactor(Player player) {
      IronManState state = IronManState.of(player);
      return state != null && state.suit.worn() ? IronManRules.NANO_MELEE_FACTOR : 1.0F;
   }

   @Override
   public void tick(ServerPlayer player) {
      IronManState state = IronManState.ensure(player);
      if (controlled(player)) {
         state.suit.interrupt();
      }

      state.suit.tick();
      dev.baranhan.viltrumiteflight.util.FlightState flight = flightState(player);
      flightEnergyTick(state, flight);
      syncArmor(player, state.suit.armored());
      state.airStrike.tick();
      if (state.heavyPoseTicks > 0) {
         state.heavyPoseTicks--;
      }

      net.minecraft.world.phys.Vec3 velocity = FlyBy.velocityOf(player);
      LandingKind kind = state.landing.tick(player.onGround(), velocity.length(), velocity.y, flight, state.airStrike.armed());
      long now = player.level().getGameTime();
      // onLanded (packet-time fall event) may already have played this touchdown.
      if (kind != LandingKind.NONE && state.suit.armored() && now - state.landedAt > 1L) {
         IronManLandings.land(player, state, kind, state.landing.impactSpeed());
      }
      if (!state.suit.worn()) {
         // Suit off / wave: every held mouse action ends without a shot (spec §16).
         dev.baranhan.viltrumitecore.hero.HeldInputs.releaseAll(player);
         IronManCombat.suitGone(player, state);
      }

      IronManCombat.tick(player, state, controlled(player));
      IronManJarvis.tick(player, state);

      if (state.flyByCooldown > 0) {
         state.flyByCooldown--;
      }

      if (state.suit.worn() && flightState(player) == dev.baranhan.viltrumiteflight.util.FlightState.SONIC) {
         SonicRam.tick(player, state);
      }
   }

   @Override
   public void handleInput(ServerPlayer player, HeroAction action, boolean pressed) {
      IronManState state = IronManState.ensure(player);
      if (!pressed) {
         release(player, state, action);
         return;
      }

      if (!this.canAct(player, action)) {
         if (action == HeroAction.SUIT && state.suit.nanoLocked()) {
            this.onInputRefused(player, action);
         }
         return;
      }

      switch (action) {
         case SUIT -> {
            Suit suit = state.suit;
            if (suit.state() == SuitState.NONE && suit.nanoLocked()) {
               this.onInputRefused(player, action);
            } else if (suit.toggle()) {
               IronManSounds.play(player, suit.state() == SuitState.DEPLOYING
                  ? dev.baranhan.viltrumitecore.ViltrumiteCore.IRONMAN_NANO_DEPLOY.get()
                  : dev.baranhan.viltrumitecore.ViltrumiteCore.IRONMAN_NANO_RETRACT.get(), 1.0F, 1.0F);
            }
         }
         case PRIMARY_ATTACK -> {
            if (state.arsenal.weapon() != null) {
               IronManCombat.strike(player, state);
               return;
            }

            dev.baranhan.viltrumiteflight.util.FlightState flight = flightState(player);
            if (AirStrike.canArm(flight, player.getXRot(), IronManLandings.groundAhead(player))) {
               // This LMB arms the air strike instead of a fly-by (spec §8.6).
               state.airStrike.arm();
            } else if (state.flyByCooldown <= 0) {
               state.flyByCooldown = IronManRules.FLYBY_COOLDOWN;
               FlyBy.hit(player);
            }
         }
         case SECONDARY_USE -> {
            state.heldTool = state.rightTool;
            switch (state.rightTool) {
               case REPULSOR -> state.repulsor.press();
               case NANO_BLADE -> IronManCombat.bladeDash(player, state);
               case NANO_HAMMER -> state.hammerCharge = state.arsenal.forming() ? -1 : 0;
               default -> {
               }
            }
         }
         case INTERACT -> {
            dev.baranhan.viltrumitecore.entity.HeroInteractable target = interactTarget(player);
            if (target != null) {
               target.heroInteract(player);
            }
         }
         case TOOL_CYCLE -> IronManCombat.toolCycle(player, state);
         case UNIBEAM -> IronManCombat.unibeamPress(player, state);
         case MISSILES -> IronManCombat.missilesPress(player, state);
         case NANO_ARSENAL -> IronManCombat.arsenalPress(player, state);
         case GUARD -> IronManCombat.guardPress(player, state);
         case HELMET -> IronManJarvis.helmetPress(player, state);
         case SCAN -> IronManJarvis.scanPress(player, state);
         case COUNTERMEASURES -> IronManJarvis.countermeasuresPress(player, state);
         default -> {
         }
      }
   }

   /** Releases always pass (channels must stop); without the suit they cancel without a shot. */
   private static void release(ServerPlayer player, IronManState state, HeroAction action) {
      boolean live = state.suit.worn() && !controlled(player);
      switch (action) {
         case SECONDARY_USE -> {
            dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool tool = state.heldTool;
            state.heldTool = null;
            if (tool == dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.REPULSOR) {
               if (live) {
                  IronManCombat.fire(player, state, state.repulsor.release(state.energy));
               } else {
                  state.repulsor.cancel();
               }
            } else if (tool == dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool.NANO_HAMMER) {
               if (live) {
                  IronManCombat.hammerLaunch(player, state);
               } else {
                  state.hammerCharge = -1;
               }
            }
         }
         case UNIBEAM -> IronManCombat.unibeamRelease(player, state);
         case MISSILES -> {
            if (live) {
               IronManCombat.missilesRelease(player, state);
            } else if (state.missiles.held()) {
               state.missiles.cancel();
               IronManCombat.pushMarks(player, java.util.List.of());
            }
         }
         case GUARD -> state.shield.lower();
         default -> {
         }
      }
   }

   /** Suit key while the nanites are still lost after a core explosion (spec §9.4). */
   @Override
   public void onInputRefused(ServerPlayer player, HeroAction action) {
      IronManState state = IronManState.of(player);
      if (action == HeroAction.SUIT && state != null && state.suit.nanoLocked()) {
         int seconds = (state.suit.nanoLockTicks() + 19) / 20;
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("hud.viltrumitecore.ironman.nano_lost", seconds), true);
      }
   }

   @Override
   public dev.baranhan.viltrumitecore.hero.DamageAbsorb absorbIncoming(ServerPlayer self, net.minecraft.world.damagesource.DamageSource source, float raw) {
      IronManState state = IronManState.of(self);
      return state == null || !state.suit.worn() ? dev.baranhan.viltrumitecore.hero.DamageAbsorb.PASS : IronManCombat.absorb(self, state, source);
   }

   @Override
   public float clampFinalDamage(ServerPlayer self, net.minecraft.world.damagesource.DamageSource source, float afterArmor) {
      IronManState state = IronManState.of(self);
      return state == null ? afterArmor : IronManCombat.clampCoreExplosion(self, state, source, afterArmor);
   }

   @Override
   public void onHurt(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
      IronManState state = IronManState.of(player);
      if (state != null) {
         IronManCombat.onHurt(player, state, amount);
         IronManJarvis.onHurt(player, state, source);
      }
   }

   @Override
   public void enter(ServerPlayer player) {
      IronManState.ensure(player);
   }

   @Override
   public HeroPublicSnapshot snapshot(Player player) {
      IronManState state = IronManState.of(player);
      return snapshotOf(state == null ? new IronManState() : state);
   }

   /**
    * Pure snapshot: flags, one channel timeline, energy x10, weapon lock,
    * the Unibeam end point and extra cooldowns ([0] = nano lock ticks).
    * Channel priority: suit wave > Unibeam > missiles > RMB hold (repulsor
    * charge, hammer charge, blade dash) > LMB weapon strike > weapon forming.
    */
   static HeroPublicSnapshot snapshotOf(IronManState state) {
      Suit suit = state.suit;
      int flags = 0;
      flags = IronManFlags.set(flags, IronManFlags.Field.SUIT_WORN, suit.worn());
      flags = IronManFlags.set(flags, IronManFlags.Field.DEPLOYING, suit.state() == SuitState.DEPLOYING);
      flags = IronManFlags.set(flags, IronManFlags.Field.RETRACTING, suit.state() == SuitState.RETRACTING);
      flags = IronManFlags.set(flags, IronManFlags.Field.GLIDE, state.glide);
      flags = IronManFlags.set(flags, IronManFlags.Field.HEAVY_LANDING, state.heavyPoseTicks > 0);
      flags = IronManFlags.set(flags, IronManFlags.Field.OVERHEAT_COUNT, state.overheat.count());
      flags = IronManFlags.set(flags, IronManFlags.Field.OVERHEAT_LOCK, state.unibeam.overheated());
      flags = IronManFlags.set(flags, IronManFlags.Field.RMB_TOOL, state.rightTool.ordinal());
      flags = IronManFlags.set(flags, IronManFlags.Field.SHIELD_UP, state.shield.raised());
      flags = IronManFlags.set(flags, IronManFlags.Field.DAMAGED_ZONES, state.damagedZones);
      flags = IronManFlags.set(flags, IronManFlags.Field.UNIBEAM_PHASE, state.unibeam.phase().ordinal());
      flags = IronManFlags.set(flags, IronManFlags.Field.MISSILE_FLAPS, state.missiles.flapsOpen());
      flags = IronManFlags.set(flags, IronManFlags.Field.OVERDRAFT_SPUTTER, state.overdraft.sputtering());
      flags = IronManFlags.set(flags, IronManFlags.Field.RECOIL, state.recoilTicks > 0);
      flags = IronManFlags.set(flags, IronManFlags.Field.SHOT_HAND, state.recoilRight);
      flags = IronManFlags.set(flags, IronManFlags.Field.OVERDRAFT, state.overdraft.active());
      flags = IronManFlags.set(flags, IronManFlags.Field.HELMET_CLOSED, suit.state() != SuitState.NONE && state.helmet.closed());
      flags = IronManFlags.set(flags, IronManFlags.Field.SCAN_ACTIVE, state.scan.active());
      int actionId = -1;
      int elapsed = 0;
      int length = 0;
      if (suit.transitioning()) {
         actionId = HeroAction.SUIT.ordinal();
         elapsed = suit.ticks();
         length = suit.waveLength();
      } else if (state.unibeam.active() || state.unibeam.overheated()) {
         actionId = HeroAction.UNIBEAM.ordinal();
         elapsed = state.unibeam.ticks();
         length = switch (state.unibeam.phase()) {
            case CHARGE -> IronManRules.UNIBEAM_CHARGE;
            case BEAM -> state.unibeam.overdraft() ? dev.baranhan.viltrumitecore.hero.ironman.combat.Overdraft.explodeAt() : IronManRules.UNIBEAM_MAX;
            default -> IronManRules.UNIBEAM_OVERHEAT_LOCK;
         };
      } else if (state.missiles.held()) {
         actionId = HeroAction.MISSILES.ordinal();
         elapsed = state.missiles.ticks();
         length = IronManRules.MISSILE_FLAPS;
      } else if (state.repulsor.charging()) {
         actionId = HeroAction.SECONDARY_USE.ordinal();
         elapsed = state.repulsor.chargeTicks();
         length = IronManRules.REPULSOR_CHARGE_MAX;
      } else if (state.hammerCharge >= 0) {
         actionId = HeroAction.SECONDARY_USE.ordinal();
         elapsed = state.hammerCharge;
         length = IronManRules.HAMMER_CHARGE_MAX;
      } else if (state.dashTicks > 0) {
         actionId = HeroAction.SECONDARY_USE.ordinal();
         elapsed = IronManRules.BLADE_DASH_TICKS - state.dashTicks;
         length = IronManRules.BLADE_DASH_TICKS;
      } else if (state.strikeTicks > 0 && state.strikeLength > 0) {
         actionId = HeroAction.PRIMARY_ATTACK.ordinal();
         elapsed = state.strikeLength - state.strikeTicks;
         length = state.strikeLength;
      } else if (state.arsenal.forming() || state.arsenal.dissolveTicks() > 0) {
         actionId = HeroAction.NANO_ARSENAL.ordinal();
         elapsed = IronManRules.NANO_FORM_TICKS - Math.max(state.arsenal.formTicks(), state.arsenal.dissolveTicks());
         length = IronManRules.NANO_FORM_TICKS;
      } else if (state.scan.active() && state.scan.targetId() >= 0) {
         // Lowest priority: the scan reticle fill (spec §11.2).
         actionId = HeroAction.SCAN.ordinal();
         elapsed = state.scan.ticks();
         length = IronManRules.SCAN_TICKS;
      }

      int[] extra = extraCooldowns(suit.nanoLockTicks(), state.countermeasures.cooldown());
      // controlTargetId = the entity being scanned (reticle), -1 otherwise.
      int scanTarget = state.scan.active() ? state.scan.targetId() : -1;
      return new HeroPublicSnapshot(HeroId.IRON_MAN, actionId, elapsed, length,
         0, false, 0, 0, false, false, 0, 0, scanTarget, new int[HeroPublicSnapshot.COOLDOWN_COUNT], false,
         -1, state.unibeam.phase() == dev.baranhan.viltrumitecore.hero.ironman.combat.UnibeamTimeline.Phase.BEAM ? state.beamEnd : null,
         Math.round(state.energy.value() * 10.0F), state.energy.weaponsLocked(), flags, extra);
   }

   /** extraCooldowns: [0] nano lock, [1] countermeasures; empty when both are 0. */
   static int[] extraCooldowns(int nanoLock, int countermeasures) {
      if (countermeasures > 0) {
         return new int[]{nanoLock, countermeasures};
      }

      return nanoLock > 0 ? new int[]{nanoLock} : new int[0];
   }

   /** Scan card hero part: only what the suit really does to damage (IronManCombat.absorb, nano armor attributes). */
   @Override
   public dev.baranhan.viltrumitecore.hero.ScanInfo scanInfo(Player self) {
      IronManState state = IronManState.of(self);
      return state == null ? dev.baranhan.viltrumitecore.hero.ScanInfo.EMPTY
         : scanInfoFor(state.suit.armored(), state.shield.raised(), state.overheat.count(), state.energy.weaponsLocked(), state.helmet.closed());
   }

   static dev.baranhan.viltrumitecore.hero.ScanInfo scanInfoFor(boolean armored, boolean shieldUp, int overheats, boolean weaponsOffline, boolean helmetClosed) {
      java.util.List<dev.baranhan.viltrumitecore.hero.ScanLine> protections = new java.util.ArrayList<>();
      java.util.List<dev.baranhan.viltrumitecore.hero.ScanLine> conditions = new java.util.ArrayList<>();
      if (armored) {
         protections.add(dev.baranhan.viltrumitecore.hero.ScanLine.of("scan.viltrumitecore.ironman.nano_armor",
            Math.round(IronManRules.NANO_ARMOR), Math.round(IronManRules.NANO_TOUGHNESS), Math.round(IronManRules.NANO_KNOCKBACK_RES * 100.0)));
         protections.add(dev.baranhan.viltrumitecore.hero.ScanLine.of("scan.viltrumitecore.ironman.no_fall"));
      }

      if (shieldUp) {
         protections.add(dev.baranhan.viltrumitecore.hero.ScanLine.of("scan.viltrumitecore.ironman.shield", Math.round(IronManRules.SHIELD_CONE_DEG)));
      }

      if (overheats > 0) {
         conditions.add(dev.baranhan.viltrumitecore.hero.ScanLine.of("scan.viltrumitecore.ironman.overheats", overheats));
      }

      if (weaponsOffline) {
         conditions.add(dev.baranhan.viltrumitecore.hero.ScanLine.of("scan.viltrumitecore.ironman.weapons_offline"));
      }

      if (armored && !helmetClosed) {
         conditions.add(dev.baranhan.viltrumitecore.hero.ScanLine.of("scan.viltrumitecore.ironman.helmet_open"));
      }

      return new dev.baranhan.viltrumitecore.hero.ScanInfo(protections, java.util.List.of(), conditions);
   }

   @Override
   public void saveHeroState(Player player, CompoundTag nbt) {
      IronManState state = IronManState.of(player);
      if (state != null) {
         state.save(nbt);
      }
   }

   @Override
   public void loadHeroState(Player player, CompoundTag nbt) {
      IronManState.ensure(player).load(nbt);
   }

   @Override
   public void cloneHeroState(Player original, Player clone) {
      IronManState previous = IronManState.of(original);
      if (previous != null && clone instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer) {
         heroPlayer.viltrumitecore$setHeroState(IronManState.cloneForRespawn(previous));
      }
   }

   @Override
   public void cleanup(ServerPlayer player, CleanupReason reason) {
      IronManState state = IronManState.of(player);
      if (state != null) {
         state.onCleanup(reason);
      }

      if (reason != CleanupReason.DISCONNECT) {
         syncArmor(player, false);
      }
   }

   /** Spec §16: the suit on the player stays through a dimension change. */
   @Override
   public void onDimensionChange(ServerPlayer player) {
   }

   /** Nano armor attributes with fixed ids: present exactly while armored. */
   static void syncArmor(ServerPlayer player, boolean armored) {
      modifier(player, Attributes.ARMOR, ARMOR_ID, "Iron Man nano armor", IronManRules.NANO_ARMOR, armored);
      modifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "Iron Man nano toughness", IronManRules.NANO_TOUGHNESS, armored);
      modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Iron Man nano stability", IronManRules.NANO_KNOCKBACK_RES, armored);
   }

   private static void modifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount, boolean present) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance == null) {
         return;
      }

      boolean has = instance.getModifier(id) != null;
      if (present && !has) {
         instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
      } else if (!present && has) {
         instance.removeModifier(id);
      }
   }
}
