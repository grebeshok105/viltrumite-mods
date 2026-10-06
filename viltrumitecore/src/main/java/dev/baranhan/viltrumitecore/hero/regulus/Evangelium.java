package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import net.minecraft.server.level.ServerPlayer;

/**
 * Evangelium book: the ritual and the madness state. Filled in plan Task 5.
 */
public final class Evangelium {
   private Evangelium() {
   }

   /** Ensure the book is in the player's inventory (enter + respawn grant). */
   public static void grant(ServerPlayer player) {
      // Task 5 adds the item; grant is a no-op until then.
   }

   public static void tick(ServerPlayer player, RegulusState state) {
   }

   /** Ritual interrupted by damage, moving or releasing the item early. */
   public static void interrupt(ServerPlayer player, RegulusState state) {
      state.ritualTicks = -1;
   }

   public static void cleanup(ServerPlayer player, RegulusState state, CleanupReason reason) {
      state.ritualTicks = -1;
   }
}
