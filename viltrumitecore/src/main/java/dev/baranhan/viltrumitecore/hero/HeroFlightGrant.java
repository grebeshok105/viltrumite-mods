package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumiteflight.util.FlightPermissions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side of the flight-grant seam. Heroes outside the legacy kit get
 * vanilla {@code mayfly} through {@link HeroDefinition#grantsFlightAbility}
 * (the flight policy in {@code HeroRegistry.installFlightPolicy} still needs
 * {@code mayfly}). Runs every server hero tick, so game-mode changes, relog
 * and respawn settle within one tick; the explicit calls (login, respawn,
 * hero change) only remove that one-tick delay. Control
 * ({@code ControlManager.preventsFlight}) is still checked by the policy.
 */
public final class HeroFlightGrant {
   private HeroFlightGrant() {
   }

   public static void sync(ServerPlayer player) {
      if (!(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      boolean wants = HeroRegistry.get(player).grantsFlightAbility(player);
      boolean ours = heroPlayer.viltrumitecore$isMayflyGranted();
      boolean mayfly = player.getAbilities().mayfly;
      switch (FlightGrantDecision.decide(wants, ours, mayfly, player.isCreative() || player.isSpectator())) {
         case GRANT -> {
            player.getAbilities().mayfly = true;
            heroPlayer.viltrumitecore$setMayflyGranted(true);
            player.onUpdateAbilities();
         }
         case REVOKE -> {
            heroPlayer.viltrumitecore$setMayflyGranted(false);
            boolean changed = mayfly || player.getAbilities().flying;
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            FlightPermissions.resetModFlight(player);
            if (changed) {
               player.onUpdateAbilities();
            }
         }
         case KEEP_CLEAR_MARKER -> heroPlayer.viltrumitecore$setMayflyGranted(false);
         case KEEP -> {
            // Self-heal a client that lost the flag (packet order, other mods): resend our grant now and then.
            if (wants && ours && player.tickCount % 40 == 0) {
               player.onUpdateAbilities();
            }
         }
      }
   }
}
