package dev.baranhan.viltrumitecore.hero.ironman.mark;

/**
 * Damage on a worn mark (spec §4.3): while durability lasts the whole hit is
 * absorbed, Tony loses nothing; the hit that empties it does not spill over.
 * Armor only slows the loss.
 */
public final class MarkDamage {
   /** Loss factor never goes below this, whatever the armor. */
   public static final float MIN_LOSS = 0.4F;

   private MarkDamage() {
   }

   public record Result(float durability, boolean absorbed, boolean broke) {
   }

   public static float lossFactor(double armor) {
      return (float)Math.max(MIN_LOSS, 1.0 - armor * 0.02);
   }

   public static Result apply(float durability, float amount, double armor) {
      if (durability <= 0.0F) {
         return new Result(0.0F, false, false);
      }

      float left = durability - Math.max(0.0F, amount) * lossFactor(armor);
      return left <= 0.0F ? new Result(0.0F, true, true) : new Result(left, true, false);
   }
}
