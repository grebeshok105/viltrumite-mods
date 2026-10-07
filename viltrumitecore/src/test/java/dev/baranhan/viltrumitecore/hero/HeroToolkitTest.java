package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pure parts of the shared hero toolkit: landing shockwave, ground eruption, super jump. */
class HeroToolkitTest {
   private static final HeroShockwave.Landing LANDING = new HeroShockwave.Landing(8.0F, 32.0F, 5.0, 12.0, 5.0F, 14.0F);

   @Test
   void shockwaveScalesWithFallHeight() {
      assertEquals(0.0F, LANDING.power(8.0F), 1.0E-6F);
      assertEquals(0.5F, LANDING.power(20.0F), 1.0E-6F);
      assertEquals(1.0F, LANDING.power(500.0F), 1.0E-6F, "clamped at full power");
      assertEquals(0.0F, LANDING.power(2.0F), 1.0E-6F, "never negative");
      assertEquals(5.0, LANDING.radius(8.0F, 1.0), 1.0E-9);
      assertEquals(12.0, LANDING.radius(32.0F, 1.0), 1.0E-9);
      assertEquals(14.0F, LANDING.damage(32.0F, 1.0), 1.0E-6F);
      assertEquals(LANDING.radius(20.0F, 1.0) * 2.0, LANDING.radius(20.0F, 2.0), 1.0E-9, "multiplier scales radius");
      assertEquals(LANDING.damage(20.0F, 1.0) * 2.0F, LANDING.damage(20.0F, 2.0), 1.0E-5F, "multiplier scales damage");
   }

   @Test
   void eruptionIsAForwardHalfEllipsoid() {
      double r = 3.4;
      double d = 2.2;
      double ahead = 1.8;
      assertTrue(HeroDebris.inEruption(0.0, 0.0, 0.0, 0.0, 1.0, r, d, ahead), "centre surface block flies");
      assertTrue(HeroDebris.inEruption(0.0, 1.0, 0.0, 0.0, 1.0, r, d, ahead), "plants on top go too");
      assertTrue(HeroDebris.inEruption(0.0, -2.0, 0.0, 0.0, 1.0, r, d, ahead), "digs two layers down");
      assertFalse(HeroDebris.inEruption(0.0, -3.0, 0.0, 0.0, 1.0, r, d, ahead), "not deeper than the crater");
      assertFalse(HeroDebris.inEruption(0.0, 2.0, 0.0, 0.0, 1.0, r, d, ahead), "air above is untouched");
      assertFalse(HeroDebris.inEruption(r + 0.5, 0.0, 0.0, 0.0, 1.0, r, d, ahead), "outside the rim");
      assertFalse(HeroDebris.inEruption(0.0, 0.0, -3.0, 0.0, 1.0, r, d, ahead), "nothing behind the source");
      assertFalse(HeroDebris.inEruption(0.0, -2.0, 2.5, 0.0, 1.0, r, d, ahead), "the rim is shallow");
   }

   @Test
   void eruptionScalesSizeAndForce() {
      HeroDebris.Eruption base = new HeroDebris.Eruption(3.0, 2.0, 1.5, 20, 1.0, 1.0);
      HeroDebris.Eruption big = base.scaled(2.0);
      assertEquals(6.0, big.radius(), 1.0E-9);
      assertEquals(4.0, big.depth(), 1.0E-9);
      assertEquals(1.5, big.ahead(), 1.0E-9, "offset in front of the source does not scale");
      assertEquals(80, big.maxFlying(), "cap grows with the area (scale²)");
      assertEquals(2.0, big.speed(), 1.0E-9);
      assertEquals(2.0, big.power(), 1.0E-9);
   }

   @Test
   void superJumpApexMatchesVanillaPhysics() {
      assertEquals(1.25, HeroSuperJump.apex(0.42), 0.1, "vanilla hop is ~1.25 blocks");
      double apex = HeroSuperJump.apex(2.0);
      assertTrue(apex > 19.5 && apex < 21.5, "2.0 peaks ~20 blocks: " + apex);
      assertEquals(0.0, HeroSuperJump.apex(0.0), 1.0E-9);
   }
}
