package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSpec;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PulseUnibeamTest {
   @Test
   void threePulses() {
      Set<Integer> pulses = new HashSet<>();
      for (int t = 0; t < SignatureRules.pulseTotal(); t++) {
         int pulse = SignatureRules.pulseAt(t);
         if (pulse >= 0) {
            pulses.add(pulse);
         }
      }

      assertEquals(Set.of(0, 1, 2), pulses);
      assertEquals(26, SignatureRules.pulseTotal());
      assertEquals(-1, SignatureRules.pulseAt(SignatureRules.pulseTotal()));
   }

   @Test
   void pulseBeamLastsSixTicksTenApart() {
      assertTrue(SignatureRules.pulseAt(5) >= 0);
      assertEquals(-1, SignatureRules.pulseAt(6), "the gap after a 6 tick beam");
      assertEquals(1, SignatureRules.pulseAt(10));
   }

   @Test
   void pulseHitsOncePerPulse() {
      assertTrue(SignatureRules.pulseHitsTick(0));
      assertFalse(SignatureRules.pulseHitsTick(3));
      assertTrue(SignatureRules.pulseHitsTick(10));
      assertTrue(SignatureRules.pulseHitsTick(20));
   }

   @Test
   void pulseCostAndCooldownMatchSpec() {
      assertEquals(35.0F, SignatureRules.PULSE_COST, 1.0E-9F);
      assertEquals(200, SignatureRules.PULSE_COOLDOWN);
   }

   @Test
   void normalUnibeamChargeStaysTenTicks() {
      assertEquals(10, MarkSpec.of(MarkId.MARK_17).unibeamCharge());
   }
}
