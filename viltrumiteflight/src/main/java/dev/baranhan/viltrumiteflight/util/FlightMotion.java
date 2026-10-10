package dev.baranhan.viltrumiteflight.util;

import net.minecraft.world.phys.Vec3;

/** Pure flight math. {@link #legacyVelocity} is the original formula used when no profile is set. */
public final class FlightMotion {
   private static final double MIN_SPEED = 1.0E-4;
   /** Horizontal drift kept per tick while gliding. */
   private static final double GLIDE_DRIFT = 0.9;

   private FlightMotion() {
   }

   /** Original CRUISE/SONIC velocity: straight along the look, no inertia. */
   public static Vec3 legacyVelocity(Vec3 look, float throttle, float maxSpeed) {
      double speed = throttle * maxSpeed;
      return new Vec3(look.x * speed, look.y * speed, look.z * speed);
   }

   /**
    * Next throttle. Glide forces 0 and beats the lock; the Shift lock freezes
    * the throttle but never above {@code lockCap}; Ctrl raises, release lowers.
    */
   public static float throttle(float current, boolean ctrl, boolean locked, FlightProfile p) {
      if (p.glide()) {
         return 0.0F;
      }

      if (locked) {
         return Math.min(current, p.lockCap());
      }

      return ctrl ? Math.min(1.0F, current + p.throttleUpPerTick()) : Math.max(0.0F, current - p.throttleDownPerTick());
   }

   /** Max turn per tick, from slow (throttle 0) to fast (throttle 1). */
   public static float turnRateDeg(float throttle, FlightProfile p) {
      float t = Math.max(0.0F, Math.min(1.0F, throttle));
      return p.turnRateSlowDeg() + (p.turnRateFastDeg() - p.turnRateSlowDeg()) * t;
   }

   /** CRUISE/SONIC velocity with a profile: limited turn, inertia on speed, glide. */
   public static Vec3 velocity(Vec3 oldVel, Vec3 look, float throttle, float maxSpeed, FlightProfile p) {
      if (p.glide()) {
         return glide(oldVel, p);
      }

      Vec3 target = look.lengthSqr() < MIN_SPEED ? oldVel : look.normalize();
      double oldSpeed = oldVel.length();
      Vec3 dir = oldSpeed < MIN_SPEED ? target : rotateToward(oldVel.scale(1.0 / oldSpeed), target, Math.toRadians(turnRateDeg(throttle, p)));
      double targetSpeed = throttle * maxSpeed * p.speedMul();
      double speed = oldSpeed * p.inertia() + targetSpeed * (1.0 - p.inertia());
      return dir.scale(speed);
   }

   /** HOVER: without input the drift is damped (stable hover); with input vanilla flight steers. */
   public static Vec3 hover(Vec3 oldVel, Vec3 input, FlightProfile p) {
      if (p.glide()) {
         return glide(oldVel, p);
      }

      if (input.lengthSqr() > MIN_SPEED) {
         return oldVel;
      }

      return oldVel.scale(1.0 - p.hoverDamping());
   }

   private static Vec3 glide(Vec3 oldVel, FlightProfile p) {
      return new Vec3(oldVel.x * GLIDE_DRIFT, -p.glideSink(), oldVel.z * GLIDE_DRIFT);
   }

   /** Rotate unit vector {@code from} toward unit vector {@code to} by at most {@code maxRad}. */
   static Vec3 rotateToward(Vec3 from, Vec3 to, double maxRad) {
      double dot = Math.max(-1.0, Math.min(1.0, from.dot(to)));
      double angle = Math.acos(dot);
      if (angle <= maxRad) {
         return to;
      }

      double sin = Math.sin(angle);
      if (sin < 1.0E-6) {
         // Opposite directions: turn around the vertical axis (or X when flying straight up/down).
         Vec3 axis = Math.abs(from.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
         Vec3 side = axis.cross(from).normalize();
         return from.scale(Math.cos(maxRad)).add(side.scale(Math.sin(maxRad)));
      }

      double a = Math.sin(angle - maxRad) / sin;
      double b = Math.sin(maxRad) / sin;
      return from.scale(a).add(to.scale(b)).normalize();
   }
}
