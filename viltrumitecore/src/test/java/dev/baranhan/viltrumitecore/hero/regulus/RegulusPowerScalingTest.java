package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Landing shockwave scaling, punch/air-blade damage and madness multipliers. */
class RegulusPowerScalingTest {

   @Test
   void shockwaveGrowsWithFallHeight() {
      assertEquals(0.0F, RegulusRules.shockwavePower(RegulusRules.SHOCKWAVE_MIN_FALL), 1.0E-6F);
      assertEquals(1.0F, RegulusRules.shockwavePower(200.0F), 1.0E-6F);
      assertEquals(RegulusRules.SHOCKWAVE_RADIUS, RegulusRules.shockwaveRadius(RegulusRules.SHOCKWAVE_MIN_FALL, false), 1.0E-9);
      assertEquals(RegulusRules.SHOCKWAVE_MAX_RADIUS, RegulusRules.shockwaveRadius(200.0F, false), 1.0E-9);
      assertEquals(RegulusRules.SHOCKWAVE_MAX_DAMAGE, RegulusRules.shockwaveDamage(200.0F, false), 1.0E-6F);
      assertTrue(RegulusRules.shockwaveRadius(20.0F, false) > RegulusRules.shockwaveRadius(10.0F, false));
   }

   @Test
   void madnessAmplifiesShockwaveAndPunch() {
      assertEquals(RegulusRules.shockwaveRadius(20.0F, false) * 1.5, RegulusRules.shockwaveRadius(20.0F, true), 1.0E-9);
      assertEquals(RegulusRules.shockwaveDamage(20.0F, false) * 1.5F, RegulusRules.shockwaveDamage(20.0F, true), 1.0E-4F);
      assertEquals(RegulusRules.PUNCH_DAMAGE, RegulusRules.punchDamage(0, false), 1.0E-6F);
      assertEquals(RegulusRules.PUNCH_DAMAGE * 1.5F, RegulusRules.punchDamage(0, true), 1.0E-4F);
      assertTrue(RegulusRules.punchDamage(RegulusRules.MAX_HEARTS, false) > RegulusRules.punchDamage(0, false), "hearts boost the punch");
      assertEquals(RegulusRules.AIR_BLADE_DAMAGE, RegulusRules.airBladeDamage(0), 1.0E-6F);
   }

   @Test
   void kickCraterIsAForwardHalfEllipsoid() {
      double r = RegulusRules.KICK_ERUPT_RADIUS;
      double d = RegulusRules.KICK_ERUPT_DEPTH;
      assertTrue(RegulusRules.inEruption(0.0, 0.0, 0.0, 0.0, 1.0, r, d), "centre surface block flies");
      assertTrue(RegulusRules.inEruption(0.0, 1.0, 0.0, 0.0, 1.0, r, d), "plants on top go too");
      assertTrue(RegulusRules.inEruption(0.0, -2.0, 0.0, 0.0, 1.0, r, d), "digs two layers down");
      assertTrue(!RegulusRules.inEruption(0.0, -3.0, 0.0, 0.0, 1.0, r, d), "not deeper than the crater");
      assertTrue(!RegulusRules.inEruption(0.0, 2.0, 0.0, 0.0, 1.0, r, d), "air above is untouched");
      assertTrue(!RegulusRules.inEruption(r + 0.5, 0.0, 0.0, 0.0, 1.0, r, d), "outside the rim");
      assertTrue(!RegulusRules.inEruption(0.0, 0.0, -3.0, 0.0, 1.0, r, d), "nothing behind the foot");
      assertTrue(!RegulusRules.inEruption(0.0, -2.0, 2.5, 0.0, 1.0, r, d), "the rim is shallow");
   }

   @Test
   void airBladeSegmentDistance() {
      Vec3 a = new Vec3(0.0, 0.0, 0.0);
      Vec3 b = new Vec3(0.0, 0.0, 10.0);
      assertEquals(2.0, RegulusAirBlade.distanceToSegment(new Vec3(2.0, 0.0, 5.0), a, b), 1.0E-9);
      assertEquals(3.0, RegulusAirBlade.distanceToSegment(new Vec3(0.0, 0.0, -3.0), a, b), 1.0E-9, "clamped to the start");
      assertEquals(1.0, RegulusAirBlade.distanceToSegment(new Vec3(0.0, 0.0, 11.0), a, b), 1.0E-9, "clamped to the end");
   }
}
