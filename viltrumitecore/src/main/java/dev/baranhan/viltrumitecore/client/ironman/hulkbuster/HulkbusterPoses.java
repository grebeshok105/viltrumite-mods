package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Which Mark 48 clip plays and at which clip time (pure). Timeline clips (punch,
 * jackhammer, charge, hop, exit) follow the synced action clock. Loops (idle,
 * grab, slam air) follow wall time. The walk follows the limb swing distance.
 */
public final class HulkbusterPoses {
   /** Walk position units of one Sind Hulkbuster stride ({walk_time} = sin((limbSwing / 13) % 1)). */
   private static final double STRIDE_UNITS = 13.0;
   /** Limb swing amount that counts as a full walk. */
   private static final float WALK_FULL_SPEED = 0.4F;
   /** Slam windup ticks before the air loop (the jump has left the ground). */
   public static final int SLAM_LAUNCH_TICKS = 6;
   /** Slam landing needs the jump to be airborne for at least this many ticks. */
   public static final int SLAM_LAND_TICKS = 4;

   private HulkbusterPoses() {
   }

   /** Clips of mark48.animation.json. Lengths in seconds must match tools/assets/convert_ironman_sind.py. */
   public enum Clip {
      IDLE(5.0, true),
      WALK(1.0, true),
      PUNCH_LEFT(0.6, false),
      PUNCH_RIGHT(0.6, false),
      JACKHAMMER(0.25, true),
      CHARGE(1.5, false),
      GRAB_HOLD(1.0, true),
      SLAM_LAUNCH(0.3, false),
      SLAM_AIR(0.4, true),
      SLAM_SMASH(0.5, false),
      HOP(0.5, false),
      EXIT(1.5, false);

      private final double length;
      private final boolean loop;

      Clip(double length, boolean loop) {
         this.length = length;
         this.loop = loop;
      }

      public String animation() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      public double length() {
         return this.length;
      }

      public boolean loop() {
         return this.loop;
      }
   }

   /**
    * One body frame: idle/walk blend ({@code walk} 0..1, clip times), an overlay clip
    * over it, thruster strength 0..1 and the glow multiplier.
    */
   public record Pose(float walk, double idleSeconds, double walkSeconds, @Nullable Clip overlay, double overlaySeconds, float flames, float glow) {
   }

   /**
    * Synced inputs of one frame. {@code slamGroundSeconds} is the time since the slam
    * landed (-1 while airborne). {@code wallSeconds} is the render clock in seconds.
    */
   public record Input(int phase, int actionId, int elapsed, int length, float partialTick, RightTool tool, boolean shotRight,
      double slamGroundSeconds, float speed, float walkPosition, double wallSeconds) {
   }

   /** Pose of an ACTIVE or EXITING layer; null for every other phase (no body). */
   @Nullable
   public static Pose select(Input in) {
      HulkbusterLayer.Phase phase = phaseOf(in.phase());
      double seconds = (in.elapsed() + in.partialTick()) / 20.0;
      double wall = in.wallSeconds();
      double idle = loop(wall, Clip.IDLE.length());
      double stride = walkSeconds(in.walkPosition());
      float walk = Math.max(0.0F, Math.min(1.0F, in.speed() / WALK_FULL_SPEED));
      if (phase == HulkbusterLayer.Phase.EXITING) {
         return new Pose(0.0F, idle, stride, Clip.EXIT, clamp(seconds, Clip.EXIT.length()), 0.0F, 1.0F);
      }

      if (phase != HulkbusterLayer.Phase.ACTIVE) {
         return null;
      }

      Clip overlay = null;
      double overlaySeconds = 0.0;
      float flames = 0.0F;
      float glow = 1.0F;
      if (in.actionId() == HeroAction.PRIMARY_ATTACK.ordinal() && in.length() > 0) {
         overlay = in.shotRight() ? Clip.PUNCH_RIGHT : Clip.PUNCH_LEFT;
         overlaySeconds = clamp(seconds, overlay.length());
      } else if (in.actionId() == HeroAction.SECONDARY_USE.ordinal()) {
         if (in.tool() == RightTool.HULK_REPULSOR) {
            overlay = Clip.CHARGE;
            overlaySeconds = clamp(seconds, Clip.CHARGE.length());
            glow = 0.5F + 0.5F * (float)(overlaySeconds / Clip.CHARGE.length());
         } else {
            overlay = Clip.JACKHAMMER;
            overlaySeconds = loop(seconds, Clip.JACKHAMMER.length());
         }
      } else if (in.actionId() == HeroAction.UNIBEAM.ordinal()) {
         overlay = Clip.GRAB_HOLD;
         overlaySeconds = loop(wall, Clip.GRAB_HOLD.length());
      } else if (in.actionId() == HeroAction.MISSILES.ordinal()) {
         if (in.slamGroundSeconds() >= 0.0) {
            overlay = Clip.SLAM_SMASH;
            overlaySeconds = clamp(in.slamGroundSeconds(), Clip.SLAM_SMASH.length());
         } else if (in.elapsed() < SLAM_LAUNCH_TICKS) {
            overlay = Clip.SLAM_LAUNCH;
            overlaySeconds = clamp(seconds, Clip.SLAM_LAUNCH.length());
            flames = 0.8F;
         } else {
            overlay = Clip.SLAM_AIR;
            overlaySeconds = loop(wall, Clip.SLAM_AIR.length());
            flames = 1.0F;
         }
      } else if (in.actionId() == HeroAction.NANO_ARSENAL.ordinal()) {
         overlay = Clip.HOP;
         overlaySeconds = clamp(seconds, Clip.HOP.length());
         flames = (float)Math.sin(Math.PI * overlaySeconds / Clip.HOP.length());
      }

      return new Pose(walk, idle, stride, overlay, overlaySeconds, flames, glow);
   }

   /** Walk clip time: one walk clip is one Sind stride, so the feet keep pace with the movement. */
   public static double walkSeconds(float walkPosition) {
      double cycles = walkPosition / STRIDE_UNITS;
      return loop(cycles * Clip.WALK.length(), Clip.WALK.length());
   }

   static HulkbusterLayer.Phase phaseOf(int ordinal) {
      HulkbusterLayer.Phase[] values = HulkbusterLayer.Phase.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : HulkbusterLayer.Phase.NONE;
   }

   private static double clamp(double seconds, double length) {
      return Math.max(0.0, Math.min(length, seconds));
   }

   private static double loop(double seconds, double length) {
      return seconds - Math.floor(seconds / length) * length;
   }
}
