package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;

/** Pure rules of Tony's reactor glow (spec §4.1). */
final class ReactorGlow {
   private ReactorGlow() {
   }

   /** The suit (worn or mid-wave) covers the chest from the first tick: glow only without it. */
   static boolean visible(int heroFlags) {
      return !IronManFlags.is(heroFlags, IronManFlags.Field.SUIT_WORN)
         && !IronManFlags.is(heroFlags, IronManFlags.Field.DEPLOYING)
         && !IronManFlags.is(heroFlags, IronManFlags.Field.RETRACTING);
   }

   /** Gentle pulse, 0.75..1, period ~3 s. */
   static float pulse(float ageInTicks) {
      return 0.875F + 0.125F * (float)Math.sin(ageInTicks * (Math.PI * 2.0 / 60.0));
   }
}
