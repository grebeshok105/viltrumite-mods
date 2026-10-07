package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Landing shockwave scaling, punch/air-blade damage and madness multipliers. */
class RegulusPowerScalingTest {

   @Test
   void regulusLandingNumbers() {
      assertEquals(RegulusRules.LANDING.minRadius(), RegulusRules.LANDING.radius(8.0F, 1.0), 1.0E-9);
      assertEquals(12.0, RegulusRules.LANDING.radius(32.0F, 1.0), 1.0E-9, "max radius at a 32-block drop");
      assertEquals(14.0F, RegulusRules.LANDING.damage(200.0F, 1.0), 1.0E-6F);
   }

   @Test
   void madnessAmplifiesShockwaveAndPunch() {
      double m = RegulusRules.MADNESS_SHOCKWAVE_MULTIPLIER;
      assertEquals(RegulusRules.LANDING.radius(20.0F, 1.0) * 1.5, RegulusRules.LANDING.radius(20.0F, m), 1.0E-9);
      assertEquals(RegulusRules.LANDING.damage(20.0F, 1.0) * 1.5F, RegulusRules.LANDING.damage(20.0F, m), 1.0E-4F);
      assertEquals(RegulusRules.PUNCH_DAMAGE, RegulusRules.punchDamage(0, false), 1.0E-6F);
      assertEquals(RegulusRules.PUNCH_DAMAGE * 1.5F, RegulusRules.punchDamage(0, true), 1.0E-4F);
      assertTrue(RegulusRules.punchDamage(RegulusRules.MAX_HEARTS, false) > RegulusRules.punchDamage(0, false), "hearts boost the punch");
      assertEquals(RegulusRules.AIR_BLADE_DAMAGE, RegulusRules.airBladeDamage(0), 1.0E-6F);
   }

   @Test
   void madnessKickEruptionIsBigger() {
      dev.baranhan.viltrumitecore.hero.HeroDebris.Eruption mad = RegulusRules.KICK_ERUPTION.scaled(RegulusRules.MADNESS_ERUPT_SCALE);
      assertEquals(RegulusRules.KICK_ERUPTION.radius() * 1.4, mad.radius(), 1.0E-9);
      assertEquals(59, mad.maxFlying(), "thrown-block cap grows with the area");
      assertEquals(RegulusRules.KICK_ERUPTION.ahead(), mad.ahead(), 1.0E-9, "the crater stays in front of the foot");
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
