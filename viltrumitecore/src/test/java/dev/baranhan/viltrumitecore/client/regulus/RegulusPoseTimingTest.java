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
