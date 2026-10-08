package dev.baranhan.viltrumitecore.hero.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EyeLasersTest {

   @Test
   void chargeDelays4Ticks() {
      for (int age = 0; age < 4; age++) {
         assertFalse(EyeLasers.damageTick(age), "age " + age);
      }
      assertTrue(EyeLasers.damageTick(4));
   }

   @Test
   void damageEvery4Ticks() {
      int hits = 0;
      for (int age = 0; age < 4 + 20; age++) {
         if (EyeLasers.damageTick(age)) {
            hits++;
         }
      }
      // 20 beam ticks = 1 s -> 5 hits x 1.5 = 7.5 health (3.75 hearts) per second.
      assertEquals(5, hits);
      assertFalse(EyeLasers.damageTick(5));
      assertTrue(EyeLasers.damageTick(8));
      assertFalse(EyeLasers.damageTick(-1));
   }
}
