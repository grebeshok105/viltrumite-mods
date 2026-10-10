package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSpec;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import org.junit.jupiter.api.Test;

class ShoulderGunTest {
   @Test
   void firesEvery2Ticks() {
      for (int t = 0; t < 10; t++) {
         assertEquals(t % 2 == 0, SignatureRules.gunFires(t), "tick " + t);
      }
   }

   @Test
   void drainsEnergy() {
      Energy energy = new Energy();
      float before = energy.value();
      for (int t = 0; t < 4; t++) {
         energy.drain(SignatureRules.GUN_COST_PER_TICK);
      }

      assertEquals(1.0F, before - energy.value(), 1.0E-4F);
   }

   @Test
   void eightMarks() {
      MarkSpec spec = MarkSpec.of(MarkId.WAR_MACHINE_MK2);
      assertEquals(8, spec.missileMarks());
      assertEquals(1.4F, spec.missileMul(), 1.0E-6F);
   }
}
