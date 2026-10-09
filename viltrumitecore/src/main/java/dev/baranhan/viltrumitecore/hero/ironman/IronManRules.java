package dev.baranhan.viltrumitecore.hero.ironman;

/**
 * All Iron Man numbers (spec §5, §7, §8.6, §18). Pure constants: tuning
 * changes only this file. Per-tick values: 20 t = 1 s.
 */
public final class IronManRules {
   // Energy (spec §5)
   public static final float ENERGY_MAX = 100.0F;
   /** ~10 / s. */
   public static final float ENERGY_REGEN_PER_TICK = 0.5F;
   /** Regen pause after the last spend or drain (~1 s). */
   public static final int ENERGY_REGEN_DELAY = 20;
   /** Hover ~1 / s. */
   public static final float DRAIN_HOVER = 0.05F;
   /** Cruise ~1 / s. */
   public static final float DRAIN_CRUISE = 0.05F;
   /** Sonic ~6 / s. */
   public static final float DRAIN_SONIC = 0.3F;
   /** After hitting 0, weapons and shield stay off until this value (spec §5.3). */
   public static final float WEAPONS_UNLOCK = 20.0F;

   // Stage 2+ energy costs (spec §5.2), used by later plans.
   public static final float COST_REPULSOR = 2.0F;
   public static final float COST_REPULSOR_CHARGED = 6.0F;
   public static final float COST_REPULSOR_VOLLEY = 10.0F;
   public static final float COST_UNIBEAM = 35.0F;
   public static final float COST_MISSILES = 15.0F;
   public static final float COST_NANO_WEAPON = 8.0F;
   public static final float COST_SHIELD_HIT = 4.0F;
   /** War Machine gun ~5 / s. */
   public static final float DRAIN_WAR_MACHINE_GUN = 0.25F;

   // Nano suit (spec §4.2)
   public static final int SUIT_DEPLOY_TICKS = 20;
   public static final int SUIT_RETRACT_TICKS = 20;
   public static final double NANO_ARMOR = 14.0;
   public static final double NANO_TOUGHNESS = 4.0;
   public static final double NANO_KNOCKBACK_RES = 0.3;
   /** Nano punch on the ground (vanilla LMB damage factor). */
   public static final float NANO_MELEE_FACTOR = 1.5F;

   // Fly-by punch (spec §7.4): LMB in flight, player velocity untouched.
   public static final float FLYBY_BASE = 4.0F;
   /** Extra damage per block/tick of flight speed. */
   public static final float FLYBY_PER_SPEED = 1.5F;
   public static final float FLYBY_MAX = 12.0F;
   public static final double FLYBY_RANGE = 3.5;
   /** Target boxes are inflated by this for the ray (a cone-like reach). */
   public static final double FLYBY_RADIUS = 1.0;
   public static final double FLYBY_KNOCKBACK = 1.2;
   /** Minimum ticks between punches (packet spam guard, ~2 punches/s). */
   public static final int FLYBY_COOLDOWN = 10;

   // Sonic ram (spec §7.4): bodies in the swept box while SONIC.
   public static final float RAM_DAMAGE = 6.0F;
   public static final double RAM_KNOCKBACK = 1.5;
   public static final int RAM_REHIT_TICKS = 10;

   // Landings and air strike (spec §8.6).
   /** Airborne ticks before the next touchdown classifies again. */
   public static final int LANDING_REARM_TICKS = 5;
   /** Equivalent fall (blocks) from which a flight touchdown is HEAVY. */
   public static final float HEAVY_FALL = 8.0F;
   /** Kneel pose flag duration after a heavy landing / air strike. */
   public static final int HEAVY_POSE_TICKS = 20;
   public static final dev.baranhan.viltrumitecore.hero.HeroShockwave.Landing HEAVY_LANDING =
      new dev.baranhan.viltrumitecore.hero.HeroShockwave.Landing(HEAVY_FALL, 24.0F, 3.0, 6.0, 3.0F, 8.0F);
   public static final float AIR_STRIKE_PITCH = 35.0F;
   public static final double AIR_STRIKE_ARM_DIST = 8.0;
   public static final int AIR_STRIKE_ARM_TICKS = 30;
   /** Air strike shockwave: always full power (fall passed = fullPowerFall). */
   public static final dev.baranhan.viltrumitecore.hero.HeroShockwave.Landing AIR_STRIKE_LANDING =
      new dev.baranhan.viltrumitecore.hero.HeroShockwave.Landing(0.0F, 16.0F, 5.0, 8.0, 8.0F, 14.0F);
   public static final dev.baranhan.viltrumitecore.hero.HeroDebris.Eruption AIR_STRIKE_CRATER =
      new dev.baranhan.viltrumitecore.hero.HeroDebris.Eruption(3.0, 2.0, 0.0, 24, 1.1, 1.0);
   public static final float AIR_STRIKE_DEBRIS_DAMAGE = 4.0F;

   // Flight profile (spec §7.2, §18). speedMul scales the player's synced max
   // flight speed (server config, default 9.0 → ~6 b/t): below Homelander on purpose.
   public static final float FLIGHT_SPEED_MUL = 0.67F;
   /** Ctrl from hover to full throttle in ~1.5 s. */
   public static final float FLIGHT_THROTTLE_UP = 1.0F / 30.0F;
   /** Ctrl released: brake to hover in ~1 s. */
   public static final float FLIGHT_THROTTLE_DOWN = 1.0F / 20.0F;
   /** Shift speed lock holds at most this (sonic needs 0.8, so only Ctrl reaches it). */
   public static final float FLIGHT_LOCK_CAP = 0.79F;
   public static final float FLIGHT_INERTIA = 0.85F;
   public static final float FLIGHT_TURN_SLOW_DEG = 9.0F;
   public static final float FLIGHT_TURN_FAST_DEG = 2.5F;
   public static final float FLIGHT_HOVER_DAMPING = 0.8F;
   /** Energy 0: glide down at this speed (blocks per tick). */
   public static final float FLIGHT_GLIDE_SINK = 0.12F;

   private static final dev.baranhan.viltrumiteflight.util.FlightProfile PROFILE = new dev.baranhan.viltrumiteflight.util.FlightProfile(
      FLIGHT_SPEED_MUL, FLIGHT_THROTTLE_UP, FLIGHT_THROTTLE_DOWN, FLIGHT_LOCK_CAP, FLIGHT_INERTIA,
      FLIGHT_TURN_SLOW_DEG, FLIGHT_TURN_FAST_DEG, FLIGHT_HOVER_DAMPING, false, FLIGHT_GLIDE_SINK);
   private static final dev.baranhan.viltrumiteflight.util.FlightProfile GLIDE_PROFILE = PROFILE.withGlide(true);

   public static dev.baranhan.viltrumiteflight.util.FlightProfile profile(boolean glide) {
      return glide ? GLIDE_PROFILE : PROFILE;
   }

   private IronManRules() {
   }
}
