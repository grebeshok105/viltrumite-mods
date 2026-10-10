package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.HeroShockwave;
import net.minecraft.world.phys.Vec3;

/**
 * Numbers and pure rules of the seven mark signatures (spec §13, plan Tasks
 * 10-16). Starting values, tuned after the in-game check. Time is in ticks.
 */
public final class SignatureRules {
   // Mark 7 micro-lasers
   public static final double LASER_RANGE = 24.0;
   public static final float LASER_DAMAGE = 1.0F;
   public static final float LASER_COST = 0.15F;
   public static final int LASER_SHIELD_COOLDOWN = 100;
   public static final int LASER_HOLD_MAX = 600;
   public static final int LASER_SOUND_INTERVAL = 10;

   // Mark 42 rocket fist
   public static final double FIST_RANGE = 32.0;
   public static final float FIST_DAMAGE = 8.0F;
   public static final double FIST_SPEED = 1.5;
   public static final double FIST_RETURN_SPEED = 1.2;
   public static final double FIST_KNOCKBACK = 0.9;
   public static final double FIST_KNOCKBACK_UP = 0.35;
   public static final double FIST_REATTACH_RANGE = 1.2;
   public static final int FIST_MAX_TICKS = 200;

   // Mark 15 camo
   public static final int CAMO_TICKS = 160;
   public static final int CAMO_COOLDOWN = 400;
   public static final double CAMO_MOB_RANGE = 8.0;
   public static final int CAMO_MOB_CHECK = 10;
   public static final float CAMO_FIRST_HIT = 2.0F;

   // Mark 39 starboost
   public static final float BOOST_COST = 15.0F;
   public static final int BOOST_COOLDOWN = 100;
   public static final int BOOST_BLAST_TICKS = 12;

   // Mark 17 pulse unibeam
   public static final float PULSE_COST = 35.0F;
   public static final int PULSE_COOLDOWN = 200;
   public static final int PULSE_COUNT = 3;
   public static final int PULSE_BEAM_TICKS = 6;
   public static final int PULSE_SPACING = 10;
   public static final float PULSE_DAMAGE = 12.0F;

   // War Machine shoulder gun
   public static final float GUN_COST_PER_TICK = 0.25F;
   public static final int GUN_INTERVAL = 2;
   public static final float GUN_DAMAGE = 2.0F;
   public static final double GUN_SPREAD_DEG = 1.2;
   public static final double GUN_RANGE = 48.0;
   public static final int GUN_HOLD_MAX = 600;

   // Iron Heart slam
   public static final double SLAM_JUMP_HEIGHT = 6.0;
   public static final double SLAM_DIVE_SPEED = 2.6;
   public static final int SLAM_COOLDOWN = 240;
   public static final int SLAM_MAX_TICKS = 200;
   public static final int SLAM_DIVE_GRACE = 2;
   public static final double SLAM_RADIUS = 7.0;
   public static final HeroShockwave.Landing SLAM_LANDING = new HeroShockwave.Landing(2.0F, 6.0F, SLAM_RADIUS, SLAM_RADIUS, 6.0F, 10.0F);

   /** Vanilla living-entity physics per tick: move, then drag and gravity on the vertical speed. */
   private static final double GRAVITY = 0.08;
   private static final double DRAG = 0.98;

   private SignatureRules() {
   }

   /** Mark 17 pulse index whose beam is on at {@code tick}, or -1 in a gap or after the last pulse. */
   public static int pulseAt(int tick) {
      if (tick < 0) {
         return -1;
      }

      int pulse = tick / PULSE_SPACING;
      return pulse < PULSE_COUNT && tick % PULSE_SPACING < PULSE_BEAM_TICKS ? pulse : -1;
   }

   /** Each pulse hits once, on its first beam tick. */
   public static boolean pulseHitsTick(int tick) {
      return pulseAt(tick) >= 0 && tick % PULSE_SPACING == 0;
   }

   public static int pulseTotal() {
      return (PULSE_COUNT - 1) * PULSE_SPACING + PULSE_BEAM_TICKS;
   }

   public static boolean gunFires(int tick) {
      return tick >= 0 && tick % GUN_INTERVAL == 0;
   }

   public static boolean camoLosesPlayer(double distance) {
      return distance > CAMO_MOB_RANGE;
   }

   public static float camoOutgoing(boolean camoOn) {
      return camoOn ? CAMO_FIRST_HIT : 1.0F;
   }

   /** One step of a steering flight: moves at most {@code speed} towards {@code goal}. */
   public static Vec3 homingStep(Vec3 position, Vec3 goal, double speed) {
      Vec3 delta = goal.subtract(position);
      double distance = delta.length();
      if (distance <= speed) {
         return goal;
      }

      return position.add(delta.scale(speed / distance));
   }

   public static boolean reached(Vec3 from, Vec3 to, double radius) {
      return from.distanceToSqr(to) <= radius * radius;
   }

   /** Vertical distance a jump with this start speed reaches (vanilla tick order). */
   public static double apexHeight(double startSpeed) {
      double height = 0.0;
      double speed = startSpeed;
      for (int i = 0; i < 200 && speed > 0.0; i++) {
         height += speed;
         speed = speed * DRAG - GRAVITY;
      }

      return height;
   }

   /** Ticks from the take-off to the apex of a jump with this start speed. */
   public static int apexTicks(double startSpeed) {
      int ticks = 0;
      double speed = startSpeed;
      while (speed > 0.0 && ticks < 200) {
         speed = speed * DRAG - GRAVITY;
         ticks++;
      }

      return ticks;
   }

   /** Start speed that reaches {@code height} blocks (bisection over {@link #apexHeight}). */
   public static double jumpSpeed(double height) {
      double low = 0.0;
      double high = 3.0;
      for (int i = 0; i < 60; i++) {
         double mid = (low + high) / 2.0;
         if (apexHeight(mid) < height) {
            low = mid;
         } else {
            high = mid;
         }
      }

      return (low + high) / 2.0;
   }

   public static double slamJumpSpeed() {
      return jumpSpeed(SLAM_JUMP_HEIGHT);
   }
}
