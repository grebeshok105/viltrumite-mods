package dev.baranhan.viltrumiteflight.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class FlightMotionTest {
   private static final double EPS = 1.0E-6;
   /** Iron Man stage 1 numbers (IronManRules.profile mirrors these). */
   private static final FlightProfile PROFILE = new FlightProfile(0.67F, 1.0F / 30.0F, 1.0F / 20.0F, 0.79F, 0.85F, 9.0F, 2.5F, 0.8F, false, 0.12F);
   private static final FlightProfile GLIDE = PROFILE.withGlide(true);
   private static final FlightProfile NO_INERTIA = new FlightProfile(0.67F, 1.0F / 30.0F, 1.0F / 20.0F, 0.79F, 0.0F, 180.0F, 180.0F, 0.8F, false, 0.12F);

   @Test
   void nullProfileKeepsLegacyMotion() {
      assertNull(FlightProfiles.of(null));
      Vec3 look = new Vec3(0.6, -0.8, 0.0);
      Vec3 v = FlightMotion.legacyVelocity(look, 0.5F, 9.0F);
      assertEquals(0.6 * 4.5, v.x, EPS);
      assertEquals(-0.8 * 4.5, v.y, EPS);
      assertEquals(0.0, v.z, EPS);
   }

   @Test
   void accelReachesFullIn30Ticks() {
      float throttle = 0.0F;
      for (int i = 0; i < 29; i++) {
         throttle = FlightMotion.throttle(throttle, true, false, PROFILE);
      }

      assertTrue(throttle < 1.0F);
      assertTrue(throttle >= 0.8F, "sonic is reached before full throttle");
      throttle = FlightMotion.throttle(throttle, true, false, PROFILE);
      assertEquals(1.0F, throttle, 1.0E-5F);
      assertEquals(1.0F, FlightMotion.throttle(throttle, true, false, PROFILE), 1.0E-6F);
   }

   @Test
   void brakeStopsIn20Ticks() {
      float throttle = 1.0F;
      for (int i = 0; i < 19; i++) {
         throttle = FlightMotion.throttle(throttle, false, false, PROFILE);
      }

      assertTrue(throttle > 0.0F);
      throttle = FlightMotion.throttle(throttle, false, false, PROFILE);
      assertEquals(0.0F, throttle, 1.0E-5F);
   }

   @Test
   void speedLockCapsBelowSonic() {
      assertEquals(0.79F, FlightMotion.throttle(1.0F, false, true, PROFILE), 1.0E-6F);
      assertEquals(0.79F, FlightMotion.throttle(0.79F, true, true, PROFILE), 1.0E-6F);
      assertEquals(0.5F, FlightMotion.throttle(0.5F, false, true, PROFILE), 1.0E-6F);
      assertEquals(0.5F, FlightMotion.throttle(0.5F, true, true, PROFILE), 1.0E-6F, "the lock freezes thrust");
      assertTrue(FlightMotion.throttle(1.0F, true, true, PROFILE) < 0.8F);
   }

   @Test
   void glideOverridesSpeedLock() {
      assertEquals(0.0F, FlightMotion.throttle(0.6F, false, true, GLIDE), 1.0E-6F);
      assertEquals(0.0F, FlightMotion.throttle(0.6F, true, false, GLIDE), 1.0E-6F);
   }

   @Test
   void turnRateShrinksWithSpeed() {
      assertEquals(9.0F, FlightMotion.turnRateDeg(0.0F, PROFILE), 1.0E-5F);
      assertEquals(2.5F, FlightMotion.turnRateDeg(1.0F, PROFILE), 1.0E-5F);
      assertTrue(FlightMotion.turnRateDeg(0.5F, PROFILE) < FlightMotion.turnRateDeg(0.2F, PROFILE));
   }

   @Test
   void cannotTurnInstantlyAtFullSpeed() {
      Vec3 old = new Vec3(6.0, 0.0, 0.0);
      Vec3 look = new Vec3(0.0, 0.0, 1.0);
      Vec3 v = FlightMotion.velocity(old, look, 1.0F, 9.0F, PROFILE);
      double angle = Math.toDegrees(Math.acos(v.normalize().dot(old.normalize())));
      assertEquals(2.5, angle, 1.0E-3);
      // Straight ahead: no turn needed.
      Vec3 straight = FlightMotion.velocity(old, new Vec3(1.0, 0.0, 0.0), 1.0F, 9.0F, PROFILE);
      assertEquals(0.0, straight.normalize().cross(new Vec3(1.0, 0.0, 0.0)).length(), 1.0E-6);
   }

   @Test
   void turnsAroundFromOppositeDirection() {
      Vec3 v = FlightMotion.velocity(new Vec3(-3.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0), 0.5F, 9.0F, PROFILE);
      double angle = Math.toDegrees(Math.acos(v.normalize().dot(new Vec3(-1.0, 0.0, 0.0))));
      assertEquals(FlightMotion.turnRateDeg(0.5F, PROFILE), angle, 1.0E-3);
   }

   @Test
   void inertiaBlendsSpeed() {
      Vec3 v = FlightMotion.velocity(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), 1.0F, 9.0F, PROFILE);
      assertEquals(9.0 * 0.67 * (1.0 - 0.85), v.length(), 1.0E-4);
      assertEquals(1.0, v.normalize().x, EPS);
   }

   @Test
   void glideSinksAndHasNoThrust() {
      Vec3 v = FlightMotion.velocity(new Vec3(2.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0), 1.0F, 9.0F, GLIDE);
      assertEquals(-0.12, v.y, EPS);
      assertTrue(Math.abs(v.x) < 2.0, "horizontal drift slows");
      assertTrue(v.x > 0.0, "no thrust toward the look");
      Vec3 hover = FlightMotion.hover(new Vec3(0.2, 0.0, 0.0), Vec3.ZERO, GLIDE);
      assertEquals(-0.12, hover.y, EPS);
   }

   @Test
   void hoverDampsDrift() {
      Vec3 drift = new Vec3(0.5, 0.2, -0.4);
      Vec3 damped = FlightMotion.hover(drift, Vec3.ZERO, PROFILE);
      assertEquals(drift.scale(0.2).length(), damped.length(), 1.0E-6);
      Vec3 steered = FlightMotion.hover(drift, new Vec3(0.0, 0.0, 1.0), PROFILE);
      assertEquals(drift, steered, "with input vanilla flight steers");
   }

   @Test
   void speedScalesWithPlayerMax() {
      Vec3 look = new Vec3(0.0, 0.0, 1.0);
      Vec3 a = FlightMotion.velocity(Vec3.ZERO, look, 1.0F, 9.0F, NO_INERTIA);
      Vec3 b = FlightMotion.velocity(Vec3.ZERO, look, 1.0F, 4.5F, NO_INERTIA);
      assertEquals(9.0 * 0.67, a.length(), 1.0E-5);
      assertEquals(4.5 * 0.67, b.length(), 1.0E-5);
      assertEquals(0.5 * 9.0 * 0.67, FlightMotion.velocity(Vec3.ZERO, look, 0.5F, 9.0F, NO_INERTIA).length(), 1.0E-5);
   }
}
