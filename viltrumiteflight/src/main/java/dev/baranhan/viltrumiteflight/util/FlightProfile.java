package dev.baranhan.viltrumiteflight.util;

/**
 * Per-player tuning of mod flight. The flight logic (HOVER / CRUISE / SONIC,
 * Ctrl thrust, Shift speed lock, sonic at 0.8) stays the same; a profile only
 * changes numbers. No profile ({@code null}) = the original flight code.
 *
 * @param speedMul multiplier on the player's synced {@code getMaxFlightSpeed()} (server config)
 * @param throttleUpPerTick throttle gain per tick while Ctrl is held
 * @param throttleDownPerTick throttle loss per tick after Ctrl is released
 * @param lockCap highest throttle the Shift speed lock can hold (below sonic)
 * @param inertia 0..1, share of the old speed kept per tick
 * @param turnRateSlowDeg max turn per tick at throttle 0
 * @param turnRateFastDeg max turn per tick at throttle 1
 * @param hoverDamping 0..1, share of hover drift removed per tick without input
 * @param glide no thrust: sink at {@code glideSink} with slow drift
 * @param glideSink sink speed in blocks per tick
 */
public record FlightProfile(
   float speedMul,
   float throttleUpPerTick,
   float throttleDownPerTick,
   float lockCap,
   float inertia,
   float turnRateSlowDeg,
   float turnRateFastDeg,
   float hoverDamping,
   boolean glide,
   float glideSink
) {
   public FlightProfile withGlide(boolean glide) {
      return new FlightProfile(this.speedMul, this.throttleUpPerTick, this.throttleDownPerTick, this.lockCap, this.inertia,
         this.turnRateSlowDeg, this.turnRateFastDeg, this.hoverDamping, glide, this.glideSink);
   }
}
