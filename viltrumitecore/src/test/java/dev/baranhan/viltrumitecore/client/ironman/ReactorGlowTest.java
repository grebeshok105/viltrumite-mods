package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import org.junit.jupiter.api.Test;

class ReactorGlowTest {
   @Test
   void visibleOnlyWithoutSuit() {
      assertTrue(ReactorGlow.visible(0));
      assertFalse(ReactorGlow.visible(IronManFlags.set(0, IronManFlags.Field.SUIT_WORN, true)));
      assertFalse(ReactorGlow.visible(IronManFlags.set(0, IronManFlags.Field.DEPLOYING, true)));
      assertFalse(ReactorGlow.visible(IronManFlags.set(0, IronManFlags.Field.RETRACTING, true)));
      assertTrue(ReactorGlow.visible(IronManFlags.set(0, IronManFlags.Field.GLIDE, true)));
   }

   @Test
   void pulseIsGentle() {
      for (float t = 0; t < 200; t += 0.37F) {
         float p = ReactorGlow.pulse(t);
         assertTrue(p >= 0.75F && p <= 1.0F, "pulse " + p);
      }
   }
}
