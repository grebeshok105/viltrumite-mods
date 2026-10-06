package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Lion's Heart: windup -> shrink-only window -> overheat, full external-damage
 * immunity, impulse escape and projectile freeze while active.
 */
public final class LionsHeart {
   private LionsHeart() {
   }

   public static void toggle(ServerPlayer player, RegulusState state) {
      if (state.lionActive) {
         deactivate(player, state, false);
         return;
      }

      if (state.busy() || state.madnessTicksLeft > 0) {
         return;
      }

      state.beginAction(RegulusHero.ACTION_LION, RegulusRules.LION_WINDUP_TICKS + 1, RegulusRules.LION_WINDUP_TICKS, RegulusRules.LION_WINDUP_TICKS);
   }

   /** Called each tick: windup event opens the window, then window + overheat. */
   public static void tick(ServerPlayer player, RegulusState state) {
      if (RegulusHero.ACTION_LION.equals(state.actionId) && !state.eventFired && state.actionElapsed >= state.actionEventTick) {
         activate(player, state);
      }

      if (!state.lionActive) {
         return;
      }

      state.lionElapsed++;
      int hearts = state.hearts();
      if (state.lionWindowFloorHearts < 0) {
         state.lionWindowFloorHearts = hearts;
      }

      state.lionWindowMax = RegulusRules.shrinkLionWindow(state.lionWindowMax, state.lionWindowFloorHearts, hearts);
      state.lionWindowFloorHearts = Math.min(state.lionWindowFloorHearts, hearts);

      if (state.lionElapsed > state.lionWindowMax) {
         state.overheatTicks++;
         // 1.5 HP/s, +0.5 every 40 ticks of overheat.
         float perTick = RegulusRules.overheatDps(state.overheatTicks) / 20.0F;
         HeroDamage.applyInternal(player, perTick);
         if (player.getHealth() <= RegulusRules.LION_FORCED_OFF_HP) {
            deactivate(player, state, true);
            return;
         }
      }
   }

   private static void activate(ServerPlayer player, RegulusState state) {
      state.eventFired = true;
      state.clearAction();
      state.lionActive = true;
      state.lionWindowMax = RegulusRules.lionWindow(state.hearts());
      state.lionElapsed = 0;
      state.overheatTicks = 0;
      state.lionStartTick = player.serverLevel().getGameTime();
      state.lionWindowFloorHearts = state.hearts();
      player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   /** forced=true on overheat-collapse; forced=false on manual toggle-off. */
   public static void deactivate(ServerPlayer player, RegulusState state, boolean forced) {
      if (!state.lionActive) {
         return;
      }

      state.lionActive = false;
      state.lionElapsed = 0;
      state.overheatTicks = 0;
      state.lionWindowFloorHearts = -1;
      state.startCooldown(RegulusAbilities.LIONS_HEART, forced ? RegulusRules.LION_FORCED_COOLDOWN : RegulusRules.LION_MANUAL_COOLDOWN);
      if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
         dev.baranhan.viltrumitecore.hero.control.ControlManager.get(level)
            .releaseProjectilesFor(player.getUUID(), player.position(), RegulusRules.LION_PROJECTILE_RADIUS, player.getLookAngle());
      }
   }

   /** Cleanup/hero-change path: no cooldown bookkeeping, just hard off. */
   public static void forceOff(ServerPlayer player, RegulusState state, boolean keepCooldown) {
      boolean wasActive = state.lionActive;
      state.lionActive = false;
      state.lionElapsed = 0;
      state.overheatTicks = 0;
      state.lionWindowFloorHearts = -1;
      if (wasActive && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
         dev.baranhan.viltrumitecore.hero.control.ControlManager.get(level)
            .releaseProjectilesFor(player.getUUID(), player.position(), RegulusRules.LION_PROJECTILE_RADIUS, player.getLookAngle());
      }
   }
}
