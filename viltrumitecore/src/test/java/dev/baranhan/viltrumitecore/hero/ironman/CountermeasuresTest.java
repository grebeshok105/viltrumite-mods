package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CountermeasuresTest {
   @Test
   void cooldown400() {
      Countermeasures cm = new Countermeasures();
      assertTrue(cm.fire());
      assertFalse(cm.fire());
      for (int i = 0; i < 399; i++) {
         cm.tick();
      }

      assertFalse(cm.ready());
      cm.tick();
      assertTrue(cm.ready());
      assertEquals(400, IronManRules.COUNTERMEASURES_COOLDOWN);
   }

   @Test
   void noEnergyCost() {
      assertEquals(0.0F, Countermeasures.energyCost());
   }

   @Test
   void mobsForgetFor40() {
      Countermeasures cm = new Countermeasures();
      cm.forget(7);
      for (int i = 0; i < 39; i++) {
         assertTrue(cm.forgetting(7), "tick " + i);
         cm.tick();
      }

      assertTrue(cm.forgetting(7));
      cm.tick();
      assertFalse(cm.forgetting(7), "re-acquires normally after 40 t");
   }

   @Test
   void bossesIgnore() {
      assertFalse(Countermeasures.affects(true));
      assertTrue(Countermeasures.affects(false));
   }

   @Test
   void homingRetargeted() {
      assertTrue(Countermeasures.retargets(5, 5), "homing on the player");
      assertFalse(Countermeasures.retargets(9, 5), "homing on someone else");
      assertFalse(Countermeasures.retargets(-1, 5), "no target");
   }

   @Test
   void cooldownInExtraIndex1() {
      assertEquals(0, IronManHero.extraCooldowns(0, 0).length);
      int[] extra = IronManHero.extraCooldowns(0, 120);
      assertEquals(2, extra.length);
      assertEquals(0, extra[0]);
      assertEquals(120, extra[1]);
      assertEquals(1, IronManHero.extraCooldowns(30, 0).length);
   }
}
