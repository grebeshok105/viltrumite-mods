package dev.baranhan.viltrumitecore.client.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The cast clock follows synced ticks without ever running ahead of the server. */
class RegulusActionClockTest {
   private static final int KICK = 1;

   @Test
   void startsAtTheSyncedTickAndAddsPartialTick() {
      RegulusActionClock clock = new RegulusActionClock();
      clock.tick(KICK, 3, 44, 44);
      assertTrue(clock.isPlaying(KICK));
      assertEquals(3.5F, clock.time(0.5F));
   }

   @Test
   void followsSyncedTicksWithoutExtrapolating() {
      RegulusActionClock clock = new RegulusActionClock();
      clock.tick(KICK, 0, 44, 44);
      clock.tick(KICK, 0, 44, 44);
      clock.tick(KICK, 0, 44, 44);
      assertEquals(0, clock.ticks(), "a stalled server holds the clock");
      clock.tick(KICK, 9, 44, 44);
      assertEquals(9, clock.ticks());
   }

   @Test
   void naturalEndPlaysTheRecoveryTailThenStops() {
      RegulusActionClock clock = new RegulusActionClock();
      for (int elapsed = 0; elapsed <= 27; elapsed++) {
         clock.tick(4, elapsed, 28, 36);
      }
      for (int i = 0; i < 8; i++) {
         clock.tick(-1, 0, 28, 36);
         assertTrue(clock.isPlaying(4));
      }
      clock.tick(-1, 0, 28, 36);
      assertFalse(clock.isPlaying(4));
      assertEquals(36.0F, clock.time(0.7F));
   }

   @Test
   void cancelMidCastFreezesForTheFadeOut() {
      RegulusActionClock clock = new RegulusActionClock();
      for (int elapsed = 0; elapsed <= 8; elapsed++) {
         clock.tick(KICK, elapsed, 44, 44);
      }
      clock.tick(-1, 0, 44, 44);
      assertFalse(clock.isPlaying(KICK));
      assertEquals(KICK, clock.actionId());
      assertEquals(8.0F, clock.time(0.9F));
   }

   @Test
   void recastRestartsFromTheSyncedTick() {
      RegulusActionClock clock = new RegulusActionClock();
      for (int elapsed = 0; elapsed <= 20; elapsed++) {
         clock.tick(KICK, elapsed, 44, 44);
      }
      clock.tick(KICK, 0, 44, 44);
      assertEquals(0, clock.ticks());
   }
}
