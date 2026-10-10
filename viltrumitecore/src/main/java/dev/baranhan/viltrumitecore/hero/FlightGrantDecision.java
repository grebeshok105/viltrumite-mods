package dev.baranhan.viltrumitecore.hero;

/**
 * Pure rule of the flight-grant seam: when a hero outside the legacy kit gets
 * or loses vanilla {@code mayfly}. We only ever revoke a {@code mayfly} that we
 * granted ourselves (marker {@code HeroGrantedMayfly}).
 */
public final class FlightGrantDecision {
   public enum Result {
      /** Set mayfly, set our marker. */
      GRANT,
      /** Clear mayfly and flying, reset mod flight, clear our marker. */
      REVOKE,
      /** Nothing to do. */
      KEEP,
      /** Creative / spectator: vanilla owns mayfly; only our marker is dropped. */
      KEEP_CLEAR_MARKER
   }

   private FlightGrantDecision() {
   }

   /**
    * @param wants the hero wants flight now ({@code HeroDefinition.grantsFlightAbility})
    * @param grantedByUs our marker is set
    * @param mayflyNow current vanilla mayfly
    * @param creativeOrSpectator vanilla game mode owns mayfly
    */
   public static Result decide(boolean wants, boolean grantedByUs, boolean mayflyNow, boolean creativeOrSpectator) {
      if (creativeOrSpectator) {
         return Result.KEEP_CLEAR_MARKER;
      }

      if (wants) {
         return mayflyNow ? Result.KEEP : Result.GRANT;
      }

      return grantedByUs ? Result.REVOKE : Result.KEEP;
   }
}
