package dev.baranhan.viltrumitecore.hero.regulus;

import net.minecraft.server.level.ServerPlayer;

/**
 * Debris Kick: 44-tick cast (slowness -> rise event at 11 -> debris event at
 * 14), 9 rays in a 35° cone, up to 3 blocks per ray. Filled in plan Task 3.
 */
public final class DebrisKick {
   private DebrisKick() {
   }

   public static void start(ServerPlayer player, RegulusState state) {
      // Task 3 implements the cast; until then the input is a validated no-op.
   }

   public static void tick(ServerPlayer player, RegulusState state) {
   }
}
