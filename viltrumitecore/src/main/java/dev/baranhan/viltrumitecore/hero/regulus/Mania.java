package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.control.ReleaseReason;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mania: pull channel + freeze. Filled in plan Task 4 together with the
 * control manager producers.
 */
public final class Mania {
   private Mania() {
   }

   public static void start(ServerPlayer player, RegulusState state) {
      // Task 4 implements the channel.
   }

   public static void tick(ServerPlayer player, RegulusState state) {
   }

   public static void endChannel(ServerPlayer player, RegulusState state, ReleaseReason reason) {
      state.channelTargetId = null;
      state.channelTicks = 0;
   }
}
