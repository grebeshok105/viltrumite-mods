package dev.baranhan.viltrumitecore.hero.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RegenTest {

   private static int heals(Regen regen, int ticks) {
      int heals = 0;
      for (int i = 0; i < ticks; i++) {
         if (regen.tick()) {
            heals++;
         }
      }
      return heals;
   }

   @Test
   void healsEvery40Ticks() {
      Regen regen = new Regen();
      assertEquals(0, heals(regen, 39));
      assertEquals(1, heals(regen, 1));
      assertEquals(2, heals(regen, 80));
   }

   @Test
   void damagePauses100Ticks() {
      Regen regen = new Regen();
      heals(regen, 30);
      regen.onDamaged();
      assertEquals(0, heals(regen, 100 + 39));
      assertEquals(1, heals(regen, 1));
   }

   @Test
   void repeatedDamageRestartsPause() {
      Regen regen = new Regen();
      regen.onDamaged();
      heals(regen, 90);
      regen.onDamaged();
      assertEquals(0, heals(regen, 100 + 39));
      assertEquals(1, heals(regen, 1));
   }
}
