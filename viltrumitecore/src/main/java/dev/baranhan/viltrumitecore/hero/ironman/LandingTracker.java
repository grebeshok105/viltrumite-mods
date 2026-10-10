package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumiteflight.util.FlightState;

/**
 * Classifies a flight touchdown once (spec §8.6). Speed is the last airborne
 * tick's velocity; re-arms after {@link IronManRules#LANDING_REARM_TICKS}
 * airborne ticks. Only touchdowns out of mod flight classify: plain falls go
 * through onLanded with their real fall distance.
 */
public final class LandingTracker {
   private int airborne;
   private boolean flew;
   private double lastSpeed;
   private double lastImpactSpeed;

   public LandingKind tick(boolean onGround, double speed, double vy, FlightState state, boolean strikeArmed) {
      if (!onGround) {
         this.airborne++;
         this.lastSpeed = speed;
         this.flew |= state != null && state != FlightState.NONE;
         return LandingKind.NONE;
      }

      boolean armed = this.airborne >= IronManRules.LANDING_REARM_TICKS && this.flew;
      this.airborne = 0;
      this.flew = false;
      if (!armed) {
         return LandingKind.NONE;
      }

      this.lastImpactSpeed = this.lastSpeed;
      if (strikeArmed) {
         return LandingKind.AIR_STRIKE;
      }

      double fall = LandingKind.equivalentFall(this.lastSpeed, IronManRules.HEAVY_LANDING.fullPowerFall());
      return fall >= IronManRules.HEAVY_FALL ? LandingKind.HEAVY : LandingKind.SOFT;
   }

   /** Impact speed of the last classified touchdown. */
   public double impactSpeed() {
      return this.lastImpactSpeed;
   }

   public void reset() {
      this.airborne = 0;
      this.flew = false;
   }
}
