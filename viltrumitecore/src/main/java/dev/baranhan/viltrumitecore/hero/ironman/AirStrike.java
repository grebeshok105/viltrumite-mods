package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumiteflight.util.FlightState;

/**
 * Air strike arming (spec §8.6): LMB in a fast dive near the ground arms it
 * (instead of a fly-by); the next touchdown is an AIR_STRIKE; disarms after
 * {@link IronManRules#AIR_STRIKE_ARM_TICKS}.
 */
public final class AirStrike {
   private int armedTicks;

   /** CRUISE/SONIC, pitch ≥ 35° down, ground within AIR_STRIKE_ARM_DIST. */
   public static boolean canArm(FlightState state, float pitchDown, double groundDist) {
      return (state == FlightState.CRUISE || state == FlightState.SONIC)
         && pitchDown >= IronManRules.AIR_STRIKE_PITCH
         && groundDist <= IronManRules.AIR_STRIKE_ARM_DIST;
   }

   public void arm() {
      this.armedTicks = IronManRules.AIR_STRIKE_ARM_TICKS;
   }

   public void tick() {
      if (this.armedTicks > 0) {
         this.armedTicks--;
      }
   }

   public boolean armed() {
      return this.armedTicks > 0;
   }

   public void consume() {
      this.armedTicks = 0;
   }
}
