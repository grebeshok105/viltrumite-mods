package dev.baranhan.viltrumitecore.hero.homelander;

import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.hero.HeroDefinition;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.LegacyKit;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Homelander replaces the Viltrumite race (spec docs/design/2026-10-08-homelander-design.md).
 * Uses a subset of the shared legacy kit plus own abilities.
 */
public class HomelanderHero implements HeroDefinition {

   @Override
   public HeroId id() {
      return HeroId.HOMELANDER;
   }

   @Override
   public boolean allowsFlight(Player player) {
      return true;
   }

   @Override
   public boolean allowsLegacyAbilities(Player player) {
      return true;
   }

   @Override
   public boolean allowsLegacyAbility(Player player, String abilityId) {
      if (!HomelanderAbilities.ownsKit(abilityId)) {
         return false;
      }

      if (LegacyKit.DASH.equals(abilityId)) {
         return player instanceof ViltrumiteFlightPlayer flightPlayer && HomelanderAbilities.dashAllowed(flightPlayer.getFlightState());
      }

      return true;
   }

   @Override
   public boolean ownsAbility(String abilityId) {
      return HomelanderAbilities.owns(abilityId);
   }

   @Override
   public net.minecraft.resources.ResourceLocation abilityIcon(String abilityId) {
      return HomelanderAbilities.icon(abilityId);
   }

   @Override
   public String[] heroInputSlots() {
      return HomelanderAbilities.slotIds();
   }

   @Override
   public HeroAction heroActionFor(String abilityId) {
      return HomelanderAbilities.actionFor(abilityId);
   }

   @Override
   public String[] defaultLoadout() {
      return HomelanderAbilities.defaultLoadout();
   }

   @Override
   public boolean allowsExternalControl(LivingEntity target, ControlKind kind) {
      return true;
   }

   @Override
   public boolean canAct(ServerPlayer player, HeroAction action) {
      if (action == HeroAction.JUMP) {
         return true;
      }

      if (HeroDamage.isAnchored(player) || !player.isAlive()) {
         return false;
      }

      HomelanderState state = HomelanderState.ensure(player);
      return switch (action) {
         case LASERS, FOCUS -> state.heat.sourcesAllowed();
         case ROAR -> state.roarCooldown <= 0;
         default -> false;
      };
   }

   @Override
   public void tick(ServerPlayer player) {
      HomelanderState state = HomelanderState.ensure(player);
      if (state.roarCooldown > 0) {
         state.roarCooldown--;
      }

      if (state.roarAnim > 0) {
         state.roarAnim--;
      }

      EyeLasers.tick(player, state);
      Focus.tick(player, state);
      state.heat.tick(state.laserBeamOn(), state.focusOn);
      if (state.heat.justOverheated()) {
         this.onOverheat(player, state);
      }

      if (player.isAlive() && state.regen.tick() && player.getHealth() < player.getMaxHealth()) {
         player.heal(1.0F);
      }
   }

   /** Spec §6.3: both eye sources stop; smoke and hiss. */
   private void onOverheat(ServerPlayer player, HomelanderState state) {
      stopChannels(player, state);
      HomelanderFeedback.overheat(player);
   }

   @Override
   public void onHurt(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
      HomelanderState.ensure(player).regen.onDamaged();
   }

   @Override
   public void handleInput(ServerPlayer player, HeroAction action, boolean pressed) {
      HomelanderState state = HomelanderState.ensure(player);
      switch (action) {
         case LASERS -> {
            if (pressed) {
               EyeLasers.press(player, state, this.canAct(player, action));
            } else {
               EyeLasers.release(player, state);
            }
         }
         case FOCUS -> {
            if (pressed) {
               Focus.toggle(player, state, this.canAct(player, action));
            }
         }
         case ROAR -> {
            if (pressed) {
               Roar.fire(player, state, this.canAct(player, action));
            }
         }
         default -> {
         }
      }
   }

   @Override
   public void enter(ServerPlayer player) {
   }

   @Override
   public HeroPublicSnapshot snapshot(Player player) {
      HomelanderState state = HomelanderState.of(player);
      int[] cooldowns = new int[HeroPublicSnapshot.COOLDOWN_COUNT];
      if (state == null) {
         return new HeroPublicSnapshot(HeroId.HOMELANDER, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1, cooldowns, false);
      }

      cooldowns[HomelanderAbilities.ROAR_COOLDOWN_INDEX] = state.roarCooldown;
      int flags = (state.laserBeamOn() ? 1 << HomelanderAbilities.FLAG_LASER : 0) | (state.focusOn ? 1 << HomelanderAbilities.FLAG_FOCUS : 0);
      int roarElapsed = Roar.animElapsed(state.roarAnim);
      int actionId = roarElapsed < 0 ? -1 : HeroAction.ROAR.ordinal();
      return new HeroPublicSnapshot(HeroId.HOMELANDER, actionId, Math.max(0, roarElapsed), roarElapsed < 0 ? 0 : Roar.ANIM_TICKS, 0, false, 0, 0, false, false, 0, 0, -1, cooldowns, false,
         -1, null, state.heat.snapshotValue(), state.heat.locked(), flags);
   }

   @Override
   public void cleanup(ServerPlayer player, CleanupReason reason) {
      HomelanderState state = HomelanderState.of(player);
      if (state != null) {
         stopChannels(player, state);
         state.heat.reset();
         state.roarCooldown = 0;
         state.roarAnim = 0;
      }

      if (reason == CleanupReason.HERO_CHANGE) {
         LegacyKit.reset(player);
      }
   }

   @Override
   public void onDimensionChange(ServerPlayer player) {
      HomelanderState state = HomelanderState.of(player);
      if (state != null) {
         stopChannels(player, state);
      }
   }

   /** Stop lasers and focus (and the fear they cause). Heat and cooldowns stay. */
   static void stopChannels(ServerPlayer player, HomelanderState state) {
      EyeLasers.release(player, state);
      Focus.stop(player, state);
   }
}
