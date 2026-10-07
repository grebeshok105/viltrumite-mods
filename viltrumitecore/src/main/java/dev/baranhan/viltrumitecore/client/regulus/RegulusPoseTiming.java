package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;

/**
 * Client mirror of the server action ticks plus the keyframe math shared by
 * every Regulus animation layer. Every tick number comes from RegulusRules so
 * the presentation can never drift from the gameplay contract.
 *
 * <p>Keyframe rows are {@code {tick, pitchDeg, yawDeg, rollDeg, x, y, z}}. A
 * row built with {@link #under(float)} means "the pose underneath" (vanilla
 * locomotion plus earlier layers), so every timeline enters from and returns
 * to whatever the body was already doing.
 */
public final class RegulusPoseTiming {
   public static final int DEBRIS_RISE_TICK = RegulusRules.DEBRIS_RISE_TICK;
   public static final int EMBRACE_LOCK_TICK = RegulusRules.EMBRACE_LOCK_TICK;
   public static final int COUNTER_LIFT_TICKS = RegulusRules.COUNTER_LIFT_TICKS;
   public static final int RITUAL_TICKS = RegulusRules.RITUAL_TICKS;
   public static final int MANIA_CHANNEL_TICKS = RegulusRules.MANIA_CHANNEL_TICKS;
   /** Strike keys overshoot their settle pose, same as the Viltrumite punch S2 key. */
   public static final float OVERSHOOT = 1.15F;

   private RegulusPoseTiming() {
   }

   /**
    * Cast window in ticks: the event fires at eventTick, the server clears the
    * action at length; visualLength adds a client-only recovery tail.
    */
   public record Timing(int eventTick, int unlockTick, int length, int visualLength) {
   }

   public static Timing timing(HeroAction action) {
      return switch (action) {
         case LIONS_HEART -> new Timing(
            RegulusRules.LION_WINDUP_TICKS,
            RegulusRules.LION_WINDUP_TICKS,
            RegulusRules.LION_WINDUP_TICKS + 1,
            RegulusRules.LION_WINDUP_TICKS + 8
         );
         case DEBRIS_KICK -> new Timing(
            RegulusRules.DEBRIS_EVENT_TICK,
            RegulusRules.DEBRIS_EVENT_TICK,
            RegulusRules.DEBRIS_ANIM_TICKS,
            RegulusRules.DEBRIS_ANIM_TICKS
         );
         case MANIA -> new Timing(
            RegulusRules.MANIA_WINDUP_TICKS,
            RegulusRules.MANIA_WINDUP_TICKS,
            RegulusRules.MANIA_WINDUP_TICKS + 1,
            RegulusRules.MANIA_WINDUP_TICKS + 6
         );
         case GREEDS_EMBRACE -> new Timing(
            RegulusRules.EMBRACE_APPEAR_TICK,
            RegulusRules.EMBRACE_RECOVER_TICK,
            RegulusRules.EMBRACE_RECOVER_TICK + 1,
            RegulusRules.EMBRACE_RECOVER_TICK + 5
         );
         case COUNTER -> new Timing(
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS,
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS,
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS + 1,
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS + 9
         );
         default -> new Timing(0, 0, 0, 0);
      };
   }

   /** True once the event tick has passed (effect already applied). */
   public static boolean eventPassed(HeroAction action, int elapsed, float partialTick) {
      Timing timing = timing(action);
      return timing.eventTick() > 0 && (float)elapsed + partialTick >= (float)timing.eventTick();
   }

   public static float[] key(float tick, float pitch, float yaw, float roll, float x, float y, float z) {
      return new float[]{tick, pitch, yaw, roll, x, y, z};
   }

   public static float[] key(float tick, float pitch, float yaw, float roll) {
      return key(tick, pitch, yaw, roll, 0.0F, 0.0F, 0.0F);
   }

   /** Strike key: the settle values pushed {@link #OVERSHOOT} past the target. */
   public static float[] strike(float tick, float pitch, float yaw, float roll, float x, float y, float z) {
      return key(tick, pitch * OVERSHOOT, yaw * OVERSHOOT, roll * OVERSHOOT, x * OVERSHOOT, y * OVERSHOOT, z * OVERSHOOT);
   }

   /** "The pose underneath" row: enter from / return to locomotion. */
   public static float[] under(float tick) {
      return new float[]{tick, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN};
   }

   public static boolean isUnder(float[] row) {
      return Float.isNaN(row[1]);
   }

   /**
    * Fractional key index for {@code elapsed}: the integer part is the segment
    * start row, the fraction is the smoothstep-eased progress inside it.
    * Clamps to the first/last row outside the timeline.
    */
   public static float locate(float[][] keys, float elapsed) {
      int last = keys.length - 1;
      if (elapsed <= keys[0][0]) {
         return 0.0F;
      }
      for (int i = 0; i < last; i++) {
         float from = keys[i][0];
         float to = keys[i + 1][0];
         if (elapsed < to) {
            float t = clamp01((elapsed - from) / Math.max(1.0E-4F, to - from));
            return (float)i + t * t * (3.0F - 2.0F * t);
         }
      }
      return (float)last;
   }

   /**
    * Samples channel {@code channel} (0..5) of a timeline. {@code base} is the
    * value underneath; additive rows add to it, absolute rows replace it.
    */
   public static float sample(float[][] keys, float elapsed, int channel, float base, boolean additive) {
      float at = locate(keys, elapsed);
      int i = (int)at;
      float t = at - (float)i;
      float a = resolve(keys[i], channel, base, additive);
      if (t <= 0.0F || i + 1 >= keys.length) {
         return a;
      }
      float b = resolve(keys[i + 1], channel, base, additive);
      return a + (b - a) * t;
   }

   public static float resolve(float[] row, int channel, float base, boolean additive) {
      if (isUnder(row)) {
         return base;
      }
      float value = row[channel + 1];
      return additive ? base + value : value;
   }

   public static float clamp01(float value) {
      return value < 0.0F ? 0.0F : Math.min(1.0F, value);
   }
}
