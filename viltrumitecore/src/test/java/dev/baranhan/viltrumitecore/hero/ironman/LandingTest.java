package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumiteflight.util.FlightState;
import org.junit.jupiter.api.Test;

class LandingTest {
   private static LandingKind fly(LandingTracker tracker, int airTicks, double speed, boolean strike) {
      for (int i = 0; i < airTicks; i++) {
         assertEquals(LandingKind.NONE, tracker.tick(false, speed, -speed, FlightState.CRUISE, strike));
      }
      return tracker.tick(true, 0.0, 0.0, FlightState.NONE, strike);
   }

   @Test
   void equivalentFallFromSpeed() {
      assertEquals(1.0 / (2 * 0.08), LandingKind.equivalentFall(1.0, 100.0), 1.0E-6);
      assertEquals(4.0 / (2 * 0.08), LandingKind.equivalentFall(2.0, 100.0), 1.0E-6);
      assertEquals(20.0, LandingKind.equivalentFall(5.0, 20.0), 1.0E-6, "clamped to fullPowerFall");
   }

   @Test
   void softLandingSlow() {
      assertEquals(LandingKind.SOFT, fly(new LandingTracker(), 10, 0.3, false));
   }

   @Test
   void heavyLandingFastFall() {
      // 1.2 b/t → 9 blocks equivalent ≥ HEAVY_FALL 8
      assertEquals(LandingKind.HEAVY, fly(new LandingTracker(), 10, 1.2, false));
   }

   @Test
   void airStrikeNeedsLmbInDive() {
      assertTrue(AirStrike.canArm(FlightState.CRUISE, 40.0F, 5.0));
      assertTrue(AirStrike.canArm(FlightState.SONIC, 35.0F, IronManRules.AIR_STRIKE_ARM_DIST));
      assertFalse(AirStrike.canArm(FlightState.HOVER, 60.0F, 3.0), "hover is not a dive");
      assertFalse(AirStrike.canArm(FlightState.NONE, 60.0F, 3.0));
      assertFalse(AirStrike.canArm(FlightState.CRUISE, 20.0F, 3.0), "too shallow");
      assertFalse(AirStrike.canArm(FlightState.CRUISE, 50.0F, IronManRules.AIR_STRIKE_ARM_DIST + 1.0), "too high");
      assertEquals(LandingKind.HEAVY, fly(new LandingTracker(), 10, 1.5, false), "no LMB → no strike");
   }

   @Test
   void airStrikeFiresOnce() {
      LandingTracker tracker = new LandingTracker();
      assertEquals(LandingKind.AIR_STRIKE, fly(tracker, 10, 1.5, true));
      for (int i = 0; i < 10; i++) {
         assertEquals(LandingKind.NONE, tracker.tick(true, 0.0, 0.0, FlightState.NONE, true), "once per touchdown");
      }
   }

   @Test
   void armDisarmsAfter30Ticks() {
      AirStrike strike = new AirStrike();
      strike.arm();
      for (int i = 0; i < IronManRules.AIR_STRIKE_ARM_TICKS - 1; i++) {
         strike.tick();
      }
      assertTrue(strike.armed());
      strike.tick();
      assertFalse(strike.armed());
      strike.arm();
      strike.consume();
      assertFalse(strike.armed());
   }

   @Test
   void rearmsAfterAirborne() {
      LandingTracker tracker = new LandingTracker();
      assertEquals(LandingKind.SOFT, fly(tracker, 10, 0.3, false));
      // a short hop (< 5 airborne ticks) does not re-arm
      assertEquals(LandingKind.NONE, fly(tracker, 3, 1.5, false));
      assertEquals(LandingKind.HEAVY, fly(tracker, 5, 1.5, false));
      // walking jumps without flight never classify
      LandingTracker walker = new LandingTracker();
      for (int i = 0; i < 10; i++) {
         walker.tick(false, 0.4, -0.4, FlightState.NONE, false);
      }
      assertEquals(LandingKind.NONE, walker.tick(true, 0.0, 0.0, FlightState.NONE, false));
   }
}
