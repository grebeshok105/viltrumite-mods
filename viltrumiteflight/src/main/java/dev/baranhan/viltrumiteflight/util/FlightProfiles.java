package dev.baranhan.viltrumiteflight.util;

import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

/**
 * Flight-profile seam (same pattern as {@link FlightPermissions#setPolicy}).
 * The core mod installs a resolver; the default resolver returns {@code null}
 * for everyone, which keeps the original flight. Read on both sides.
 */
public final class FlightProfiles {
   private static volatile Function<Player, FlightProfile> resolver = player -> null;
   private static volatile Function<Player, Float> speedScale = player -> 1.0F;

   private FlightProfiles() {
   }

   public static void setResolver(Function<Player, FlightProfile> profileResolver) {
      resolver = Objects.requireNonNull(profileResolver, "profileResolver");
   }

   /** Factor on the legacy (no profile) CRUISE/SONIC speed; default 1. */
   public static void setSpeedScaleResolver(Function<Player, Float> scaleResolver) {
      speedScale = Objects.requireNonNull(scaleResolver, "scaleResolver");
   }

   public static float speedScale(@Nullable Player player) {
      if (player == null) {
         return 1.0F;
      }

      Float scale = speedScale.apply(player);
      return scale == null || !Float.isFinite(scale) || scale <= 0.0F ? 1.0F : scale;
   }

   @Nullable
   public static FlightProfile of(@Nullable Player player) {
      return player == null ? null : resolver.apply(player);
   }
}
