package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import org.junit.jupiter.api.Test;

class HelmetAnimTest {
   @Test
   void stepsOverToggleTicks() {
      float p = 0.0F;
      for (int i = 0; i < IronManRules.HELMET_TOGGLE_TICKS; i++) {
         p = HelmetAnim.step(p, true);
      }

      assertEquals(1.0F, p, 1.0E-4F);
      assertEquals(1.0F, HelmetAnim.step(1.0F, true));
      assertEquals(0.0F, HelmetAnim.step(0.0F, false));
   }
}
