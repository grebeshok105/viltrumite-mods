package dev.baranhan.viltrumitecore.hero.ironman;

/** Iron Man touchdown classes (spec §8.6). Not "Landing": that is HeroShockwave.Landing. */
public enum LandingKind {
   NONE,
   SOFT,
   HEAVY,
   AIR_STRIKE;

   /** Vanilla gravity, blocks per tick². */
   public static final double GRAVITY = 0.08;

   /**
    * Flight has no fall distance: the height a free fall would need to reach
    * this impact speed, v² / (2·g), clamped to the shockwave's fullPowerFall.
    */
   public static double equivalentFall(double impactSpeed, double fullPowerFall) {
      return Math.min(fullPowerFall, impactSpeed * impactSpeed / (2.0 * GRAVITY));
   }
}
