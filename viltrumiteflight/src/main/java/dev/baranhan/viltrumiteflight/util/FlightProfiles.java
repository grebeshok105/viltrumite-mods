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

   private FlightProfiles() {
   }

   public static void setResolver(Function<Player, FlightProfile> profileResolver) {
      resolver = Objects.requireNonNull(profileResolver, "profileResolver");
   }

   @Nullable
   public static FlightProfile of(@Nullable Player player) {
      return player == null ? null : resolver.apply(player);
   }
}
