package dev.baranhan.viltrumitecore.client.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pure visual-curve seams behind the world VFX and screen uniforms. All
 * numbers are presentation-only approximations; gameplay stays server-side.
 */
class RegulusVfxMathTest {

   @Test
   void debrisImpactIsTiedToGroundKickEvent() {
      assertEquals(0.0F, RegulusVfxMath.debrisImpactEnvelope(13.99F));
      assertEquals(1.0F, RegulusVfxMath.debrisImpactEnvelope(14.0F));
      assertEquals(0.0F, RegulusVfxMath.debrisImpactEnvelope(24.0F));
      assertEquals(0.0F, RegulusVfxMath.debrisImpactEnvelope(44.0F));
   }

   @Test
   void debrisImpactFallsOffThroughRecovery() {
      float near = RegulusVfxMath.debrisImpactEnvelope(15.0F);
      float far = RegulusVfxMath.debrisImpactEnvelope(23.0F);
      assertTrue(near > far && far > 0.0F);
   }

   @Test
   void overheatRampsOnlyWhileOverheating() {
      assertEquals(0.0F, RegulusVfxMath.overheatFactor(0));
      assertTrue(RegulusVfxMath.overheatFactor(10) > 0.0F);
      assertEquals(1.0F, RegulusVfxMath.overheatFactor(20));
      assertEquals(1.0F, RegulusVfxMath.overheatFactor(999));
   }

   @Test
   void heartFlashDecaysToZero() {
      assertEquals(1.0F, RegulusVfxMath.heartFlashFactor(0.0F), 1.0E-6);
      assertTrue(RegulusVfxMath.heartFlashFactor(6.0F) > 0.0F);
      assertEquals(0.0F, RegulusVfxMath.heartFlashFactor(12.0F), 1.0E-6);
      assertEquals(0.0F, RegulusVfxMath.heartFlashFactor(100.0F), 1.0E-6);
   }

   @Test
   void madnessBeatIsTwoThumpsPerCycle() {
      // Lub-dub: a strong beat early in the period and a softer echo, with a
      // quiet tail so the overlay visibly pulses instead of staying constant.
      float peak = 0.0F;
      float echoPeak = 0.0F;
      for (int i = 0; i < 100; i++) {
         float phase = i / 100.0F;
         float value = RegulusVfxMath.madnessPulse(phase);
         if (phase < 0.2F) {
            peak = Math.max(peak, value);
         }
         if (phase > 0.2F && phase < 0.5F) {
            echoPeak = Math.max(echoPeak, value);
         }
      }
      assertEquals(1.0F, peak, 0.05F);
      assertTrue(echoPeak > 0.3F && echoPeak < peak, "second thump must be softer");
      assertTrue(RegulusVfxMath.madnessPulse(0.9F) < 0.2F, "tail should decay");
   }

   @Test
   void domeAlphaFadesAtBothEnds() {
      long created = 1000L;
      long expires = 1080L;
      assertTrue(RegulusVfxMath.domeAlpha(created, created, expires) < 0.1F);
      assertEquals(1.0F, RegulusVfxMath.domeAlpha(1040L, created, expires), 1.0E-6);
      assertTrue(RegulusVfxMath.domeAlpha(expires, created, expires) < 0.1F);
      assertTrue(RegulusVfxMath.domeAlpha(expires - 2, created, expires) < 1.0F);
   }

   @Test
   void hashOffsetsAreDeterministicAndInRange() {
      for (int seed = 0; seed < 64; seed++) {
         for (int axis = 0; axis < 3; axis++) {
            float value = RegulusVfxMath.hashOffset(seed, axis);
            assertTrue(value >= 0.0F && value < 1.0F, "hash out of range");
            assertEquals(value, RegulusVfxMath.hashOffset(seed, axis), "hash must be stable");
         }
      }
      assertNotEquals(RegulusVfxMath.hashOffset(1, 0), RegulusVfxMath.hashOffset(2, 0));
   }

   @Test
   void runeAnglesOrbitOnceAround() {
      // 8 runes evenly spread; a slow continuous drift on the time term.
      float step = RegulusVfxMath.runeAngle(1, 0.0F) - RegulusVfxMath.runeAngle(0, 0.0F);
      assertEquals((float)(Math.PI * 2.0 / 8.0), step, 1.0E-4);
      float later = RegulusVfxMath.runeAngle(0, 1.0F);
      assertTrue(later != RegulusVfxMath.runeAngle(0, 0.0F), "runes must orbit over time");
   }
}
