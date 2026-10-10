package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import org.junit.jupiter.api.Test;

class MicroLaserTest {
   @Test
   void costsEnergyPerTick() {
      Energy energy = new Energy();
      float before = energy.value();
      for (int i = 0; i < 10; i++) {
         energy.drain(SignatureRules.LASER_COST);
      }

      assertEquals(1.5F, before - energy.value(), 1.0E-4F);
   }

   @Test
   void breaksShieldAndReachesTwentyFour() {
      assertEquals(100, SignatureRules.LASER_SHIELD_COOLDOWN);
      assertEquals(24.0, SignatureRules.LASER_RANGE, 1.0E-9);
      assertEquals(1.0F, SignatureRules.LASER_DAMAGE, 1.0E-9F);
   }
}
