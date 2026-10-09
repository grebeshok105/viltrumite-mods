package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertSame;

import dev.baranhan.viltrumitecore.hero.FlightGrantDecision.Result;
import org.junit.jupiter.api.Test;

class FlightGrantDecisionTest {

   @Test
   void grantsWhenWanted() {
      assertSame(Result.GRANT, FlightGrantDecision.decide(true, false, false, false));
   }

   @Test
   void noDoubleGrant() {
      assertSame(Result.KEEP, FlightGrantDecision.decide(true, true, true, false));
   }

   @Test
   void grantsAgainWhenOurMayflyWasTakenAway() {
      // Survival game-mode switch clears mayfly; our marker is still set.
      assertSame(Result.GRANT, FlightGrantDecision.decide(true, true, false, false));
   }

   @Test
   void revokesOnlyOwnGrant() {
      assertSame(Result.REVOKE, FlightGrantDecision.decide(false, true, true, false));
      assertSame(Result.KEEP, FlightGrantDecision.decide(false, false, true, false));
      assertSame(Result.KEEP, FlightGrantDecision.decide(false, false, false, false));
   }

   @Test
   void revokeClearsAStaleMarkerEvenWithoutMayfly() {
      assertSame(Result.REVOKE, FlightGrantDecision.decide(false, true, false, false));
   }

   @Test
   void legacyMayflyUntouched() {
      // Legacy kit (Homelander) sets mayfly itself and never wants our grant.
      assertSame(Result.KEEP, FlightGrantDecision.decide(false, false, true, false));
      // Someone else already gave mayfly: we do not take ownership of it.
      assertSame(Result.KEEP, FlightGrantDecision.decide(true, false, true, false));
   }

   @Test
   void creativeKeeps() {
      for (boolean wants : new boolean[]{false, true}) {
         for (boolean ours : new boolean[]{false, true}) {
            assertSame(Result.KEEP_CLEAR_MARKER, FlightGrantDecision.decide(wants, ours, true, true));
         }
      }
   }

   @Test
   void spectatorKeeps() {
      assertSame(Result.KEEP_CLEAR_MARKER, FlightGrantDecision.decide(true, true, false, true));
      assertSame(Result.KEEP_CLEAR_MARKER, FlightGrantDecision.decide(false, true, false, true));
   }
}
