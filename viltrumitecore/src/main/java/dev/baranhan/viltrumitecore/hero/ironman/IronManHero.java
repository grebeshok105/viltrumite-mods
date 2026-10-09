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
    * Iron Man flight profile while the suit is worn; glide at 0 energy.
    * Both sides: the client reads the synced flags (≤ 1 tick behind).
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
      return worn ? IronManRules.profile(glide) : null;
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
         // Same gate as the claim: a forged HeroInputC2SPacket cannot punch on foot.
         case PRIMARY_ATTACK -> {
            IronManState state = IronManState.of(player);
            yield state != null && claimsPrimary(state.suit.worn(), flightState(player));
         }
         default -> false;
      };
   }

   /**
    * Stage 1a claims only LMB while flying in the suit (spec §6.1); on the
    * ground LMB stays vanilla (nano punch via meleeDamageFactor).
    */
   @Override
   public HeroAction mouseAction(dev.baranhan.viltrumitecore.hero.MouseButton button, Player player) {
      if (button != dev.baranhan.viltrumitecore.hero.MouseButton.PRIMARY || player == null) {
         return null;
      }

      boolean worn;
      if (player.level().isClientSide()) {
         worn = suitWornFlag(player);
      } else {
         IronManState state = IronManState.of(player);
         worn = state != null && state.suit.worn();
      }

      return claimsPrimary(worn, flightState(player)) ? HeroAction.PRIMARY_ATTACK : null;
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
         // Suit off / wave: every held mouse action ends (spec §16).
         dev.baranhan.viltrumitecore.hero.HeldInputs.releaseAll(player);
      }

      if (state.flyByCooldown > 0) {
         state.flyByCooldown--;
      }

      if (state.suit.worn() && flightState(player) == dev.baranhan.viltrumiteflight.util.FlightState.SONIC) {
         SonicRam.tick(player, state);
      }
   }

   @Override
   public void handleInput(ServerPlayer player, HeroAction action, boolean pressed) {
      if (action == HeroAction.SUIT && pressed && this.canAct(player, action)) {
         IronManState.ensure(player).suit.toggle();
      } else if (action == HeroAction.PRIMARY_ATTACK && pressed && this.canAct(player, action)) {
         IronManState state = IronManState.ensure(player);
         dev.baranhan.viltrumiteflight.util.FlightState flight = flightState(player);
         if (AirStrike.canArm(flight, player.getXRot(), IronManLandings.groundAhead(player))) {
            // This LMB arms the air strike instead of a fly-by (spec §8.6).
            state.airStrike.arm();
         } else if (state.flyByCooldown <= 0) {
            state.flyByCooldown = IronManRules.FLYBY_COOLDOWN;
            FlyBy.hit(player);
         }
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

   /** Pure snapshot: flags, wave timeline, energy x10 and the weapon lock. */
   static HeroPublicSnapshot snapshotOf(IronManState state) {
      Suit suit = state.suit;
      int flags = 0;
      flags = IronManFlags.set(flags, IronManFlags.Field.SUIT_WORN, suit.worn());
      flags = IronManFlags.set(flags, IronManFlags.Field.DEPLOYING, suit.state() == SuitState.DEPLOYING);
      flags = IronManFlags.set(flags, IronManFlags.Field.RETRACTING, suit.state() == SuitState.RETRACTING);
      flags = IronManFlags.set(flags, IronManFlags.Field.GLIDE, state.glide);
      flags = IronManFlags.set(flags, IronManFlags.Field.HEAVY_LANDING, state.heavyPoseTicks > 0);
      boolean wave = suit.transitioning();
      return new HeroPublicSnapshot(HeroId.IRON_MAN, wave ? HeroAction.SUIT.ordinal() : -1, wave ? suit.ticks() : 0, wave ? suit.waveLength() : 0,
         0, false, 0, 0, false, false, 0, 0, -1, new int[HeroPublicSnapshot.COOLDOWN_COUNT], false,
         -1, null, Math.round(state.energy.value() * 10.0F), state.energy.weaponsLocked(), flags);
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
