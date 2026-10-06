package dev.baranhan.viltrumiteflight.util;

import java.util.Objects;
import net.minecraft.world.entity.player.Player;

public final class FlightPermissions {
   private static volatile FlightPermission policy = player -> player.getAbilities().mayfly;

   private FlightPermissions() {
   }

   public static void setPolicy(FlightPermission permission) {
      policy = Objects.requireNonNull(permission, "permission");
   }

   public static boolean allowsModFlight(Player player) {
      return player != null && policy.allowsModFlight(player);
   }

   public static void resetModFlight(Player player) {
      if (player == null) {
         return;
      }

      if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
         flightPlayer.resetModFlight();
      }

      // Grab ownership tags are server-only; clients keep the server's ability flags.
      if (player.level().isClientSide()) {
         return;
      }

      boolean grabbed = player.getTags().contains("ViltrumiteGrabbed");
      if (!player.isSpectator() && !grabbed) {
         player.noPhysics = false;
      }

      if (!player.isCreative() && !player.isSpectator() && !grabbed && player.getAbilities().flying) {
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
      }
   }
}
