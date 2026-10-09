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

   private IronManRules() {
   }
}
