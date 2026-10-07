package dev.baranhan.viltrumitecore.client.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;
import org.junit.jupiter.api.Test;

/**
 * The client pose timeline must key on the same server-authoritative ticks the
 * abilities use (spec 13.2/13.3): windup until the event tick, then release.
 */
class RegulusPoseTimingTest {

   @Test
   void timelineEntersFromAndReturnsToThePoseUnderneath() {
      float[][] keys = {RegulusPoseTiming.under(0), RegulusPoseTiming.key(10, 60, 0, 0), RegulusPoseTiming.under(20)};
      assertEquals(20.0F, RegulusPoseTiming.sample(keys, 0, 0, 20, false));
      assertEquals(40.0F, RegulusPoseTiming.sample(keys, 5, 0, 20, false), 1.0E-4F);
      assertEquals(60.0F, RegulusPoseTiming.sample(keys, 10, 0, 20, false));
      assertEquals(40.0F, RegulusPoseTiming.sample(keys, 15, 0, 20, false), 1.0E-4F);
      assertEquals(20.0F, RegulusPoseTiming.sample(keys, 20, 0, 20, false));
      assertEquals(20.0F, RegulusPoseTiming.sample(keys, 99, 0, 20, false));
   }

   @Test
   void additiveRowsAddToTheBaseAndSegmentsAreEased() {
      float[][] keys = {RegulusPoseTiming.key(0, 0, 0, 0), RegulusPoseTiming.key(10, 10, 0, 0)};
      assertEquals(15.0F, RegulusPoseTiming.sample(keys, 5, 0, 10, true), 1.0E-4F);
      assertTrue(RegulusPoseTiming.sample(keys, 2, 0, 0, true) < 2.0F);
      assertTrue(RegulusPoseTiming.sample(keys, 8, 0, 0, true) > 8.0F);
   }

   @Test
   void strikeKeyOvershootsBy115Percent() {
      float[] row = RegulusPoseTiming.strike(14, -80, 10, 0, 0, 0, -2);
      assertEquals(14.0F, row[0]);
      assertEquals(-92.0F, row[1], 1.0E-4F);
      assertEquals(11.5F, row[2], 1.0E-4F);
      assertEquals(-2.3F, row[6], 1.0E-4F);
   }

   @Test
   void visualLengthNeverEndsBeforeTheServerCast() {
      for (HeroAction action : new HeroAction[]{HeroAction.LIONS_HEART, HeroAction.DEBRIS_KICK, HeroAction.MANIA, HeroAction.GREEDS_EMBRACE, HeroAction.COUNTER}) {
         RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(action);
         assertTrue(timing.visualLength() >= timing.length(), action.name());
      }
   }

   @Test
   void lionWindupMatchesSpec() {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.LIONS_HEART);
      assertEquals(RegulusRules.LION_WINDUP_TICKS, timing.eventTick());
      assertEquals(RegulusRules.LION_WINDUP_TICKS + 1, timing.length());
      assertEquals(RegulusRules.LION_WINDUP_TICKS, timing.unlockTick());
   }

   @Test
   void debrisKickTimingMatchesSpec() {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.DEBRIS_KICK);
      assertEquals(RegulusRules.DEBRIS_EVENT_TICK, timing.eventTick());
      assertEquals(RegulusRules.DEBRIS_ANIM_TICKS, timing.length());
      assertEquals(RegulusRules.DEBRIS_EVENT_TICK, timing.unlockTick());
      assertEquals(RegulusRules.DEBRIS_RISE_TICK, RegulusPoseTiming.DEBRIS_RISE_TICK);
   }

   @Test
   void maniaTimingMatchesSpec() {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.MANIA);
      assertEquals(RegulusRules.MANIA_WINDUP_TICKS, timing.eventTick());
      assertEquals(RegulusRules.MANIA_WINDUP_TICKS + 1, timing.length());
      assertEquals(RegulusRules.MANIA_WINDUP_TICKS, timing.unlockTick());
   }

   @Test
   void embraceTimingMatchesSpec() {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.GREEDS_EMBRACE);
      assertEquals(RegulusRules.EMBRACE_APPEAR_TICK, timing.eventTick());
      assertEquals(RegulusRules.EMBRACE_RECOVER_TICK + 1, timing.length());
      assertEquals(RegulusRules.EMBRACE_RECOVER_TICK, timing.unlockTick());
      assertEquals(RegulusRules.EMBRACE_LOCK_TICK, RegulusPoseTiming.EMBRACE_LOCK_TICK);
   }

   @Test
   void counterTimingMatchesSpec() {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.COUNTER);
      assertEquals(RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS, timing.eventTick());
      assertEquals(RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS + 1, timing.length());
      assertEquals(RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS, timing.unlockTick());
      assertEquals(RegulusRules.COUNTER_LIFT_TICKS, RegulusPoseTiming.COUNTER_LIFT_TICKS);
   }

   @Test
   void ritualHoldConstantMatchesSpec() {
      assertEquals(RegulusRules.RITUAL_TICKS, RegulusPoseTiming.RITUAL_TICKS);
   }

   @Test
   void eventPassedFlipsAtEventTick() {
      assertFalse(RegulusPoseTiming.eventPassed(HeroAction.LIONS_HEART, 13, 0.9F));
      assertTrue(RegulusPoseTiming.eventPassed(HeroAction.LIONS_HEART, 14, 0.0F));
      assertTrue(RegulusPoseTiming.eventPassed(HeroAction.LIONS_HEART, 15, 0.0F));
   }
}
