package dev.baranhan.viltrumitecore.hero.ironman.scan;

import dev.baranhan.viltrumitecore.hero.ScanInfo;

/**
 * Read-only facts about a scan target, collected by {@link ScanAnalyzer#traits}
 * with queries only (no damage, no state change). Pure input of the rules.
 *
 * @param resistance Resistance effect level (amplifier + 1), 0 = none
 * @param hero hero part from HeroDefinition.scanInfo (players), else EMPTY
 */
public record ScanTraits(
   boolean player,
   boolean fireImmune,
   int resistance,
   boolean fireResistance,
   double knockbackResistance,
   boolean immuneExplosion,
   boolean immuneProjectile,
   boolean immuneMagic,
   boolean undead,
   boolean arthropod,
   boolean waterSensitive,
   ScanInfo hero
) {
   public ScanTraits {
      hero = hero == null ? ScanInfo.EMPTY : hero;
   }

   /** A plain mob with no special rule (tests, defaults). */
   public static ScanTraits plain(boolean player) {
      return new ScanTraits(player, false, 0, false, 0.0, false, false, false, false, false, false, ScanInfo.EMPTY);
   }
}
