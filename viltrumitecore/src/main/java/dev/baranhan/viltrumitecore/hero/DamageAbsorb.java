package dev.baranhan.viltrumitecore.hero;

/**
 * Result of {@link HeroDefinition#absorbIncoming}. {@code absorbed} cancels the
 * whole hit (no HP loss, no vanilla knockback, no hurt flash). Otherwise
 * {@code passOn} is the amount that continues to armor and HP; a negative
 * value means "unchanged". A layer can lower a hit, never raise it.
 */
public record DamageAbsorb(boolean absorbed, float passOn) {
   public static final DamageAbsorb PASS = new DamageAbsorb(false, -1.0F);
   public static final DamageAbsorb ABSORBED = new DamageAbsorb(true, 0.0F);

   public static DamageAbsorb reduceTo(float amount) {
      return new DamageAbsorb(false, Math.max(0.0F, amount));
   }
}
