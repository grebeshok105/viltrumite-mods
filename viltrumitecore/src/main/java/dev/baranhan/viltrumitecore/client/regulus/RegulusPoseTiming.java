package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;

/**
 * Client-side mirror of the server action ticks for pose and VFX timelines.
 * Every number comes straight from RegulusRules, so the presentation layer can
 * never drift from the gameplay contract (spec 13.2: windup until the event,
 * free cancel before it).
 */
public final class RegulusPoseTiming {
   public static final int DEBRIS_RISE_TICK = RegulusRules.DEBRIS_RISE_TICK;
   public static final int EMBRACE_LOCK_TICK = RegulusRules.EMBRACE_LOCK_TICK;
   public static final int COUNTER_LIFT_TICKS = RegulusRules.COUNTER_LIFT_TICKS;
   public static final int RITUAL_TICKS = RegulusRules.RITUAL_TICKS;
   public static final int MANIA_CHANNEL_TICKS = RegulusRules.MANIA_CHANNEL_TICKS;

   private RegulusPoseTiming() {
   }

   public static float actionElapsed(int syncedElapsed, long ticksSinceSync, float partialTick, int length) {
      return Math.min(length, syncedElapsed + Math.min(6, Math.max(0, ticksSinceSync - 1)) + partialTick);
   }

   public static float keyedAngle(float elapsed, float vanilla, float[][] keys, int axis) {
      if (elapsed <= keys[0][0]) {
         return vanilla;
      }
      int last = keys.length - 1;
      for (int i = 0; i < last; i++) {
         if (elapsed < keys[i + 1][0]) {
            float from = i == 0 ? vanilla : keys[i][axis];
            float to = keys[i + 1][axis] + (i + 1 == last ? vanilla : 0.0F);
            float t = (elapsed - keys[i][0]) / (keys[i + 1][0] - keys[i][0]);
            return from + (to - from) * t;
         }
      }
      return vanilla + keys[last][axis];
   }

   /** Cast window in ticks: the event fires at eventTick, the anim ends at length. */
   public record Timing(int eventTick, int unlockTick, int length) {
   }

   public static Timing timing(HeroAction action) {
      return switch (action) {
         case LIONS_HEART -> new Timing(
            RegulusRules.LION_WINDUP_TICKS,
            RegulusRules.LION_WINDUP_TICKS,
            RegulusRules.LION_WINDUP_TICKS + 1
         );
         case DEBRIS_KICK -> new Timing(
            RegulusRules.DEBRIS_EVENT_TICK,
            RegulusRules.DEBRIS_EVENT_TICK,
            RegulusRules.DEBRIS_ANIM_TICKS
         );
         case MANIA -> new Timing(
            RegulusRules.MANIA_WINDUP_TICKS,
            RegulusRules.MANIA_WINDUP_TICKS,
            RegulusRules.MANIA_WINDUP_TICKS + 1
         );
         case GREEDS_EMBRACE -> new Timing(
            RegulusRules.EMBRACE_APPEAR_TICK,
            RegulusRules.EMBRACE_RECOVER_TICK,
            RegulusRules.EMBRACE_RECOVER_TICK + 1
         );
         case COUNTER -> new Timing(
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS,
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS,
            RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS + 1
         );
         default -> new Timing(0, 0, 0);
      };
   }

   /** True once the event tick has passed (effect already applied). */
   public static boolean eventPassed(HeroAction action, int elapsed, float partialTick) {
      Timing timing = timing(action);
      return timing.eventTick() > 0 && (float)elapsed + partialTick >= (float)timing.eventTick();
   }
}
