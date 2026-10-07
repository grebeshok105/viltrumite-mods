package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.control.ReleaseReason;
import dev.baranhan.viltrumiteflight.util.FlightState;
import org.junit.jupiter.api.Test;

class ManiaTest {

   @Test
   void grabFiresOnlyOnTheWindupEvent() {
      RegulusState state = new RegulusState();
      state.beginAction(RegulusHero.ACTION_MANIA, RegulusRules.MANIA_WINDUP_TICKS, RegulusRules.MANIA_WINDUP_TICKS, RegulusRules.MANIA_WINDUP_TICKS);
      assertFalse(Mania.shouldGrab(state));

      state.actionElapsed = RegulusRules.MANIA_WINDUP_TICKS - 1;
      assertFalse(Mania.shouldGrab(state));

      state.actionElapsed = RegulusRules.MANIA_WINDUP_TICKS;
      assertTrue(Mania.shouldGrab(state));

      // The event fires once: after it, no second grab this cast.
      state.eventFired = true;
      assertFalse(Mania.shouldGrab(state));
   }

   @Test
   void grabEventSurvivesTheActionLockSweep() {
      // Regression: RegulusHero.tickActionLock runs before Mania.tick in the
      // same tick. If the action length equals the windup tick the lock clears
      // the cast at elapsed == windup and the grab event can never observe it.
      RegulusState state = new RegulusState();
      Mania.start(null, state);   // start() only writes the action lock; player is unused

      for (int i = 0; i < RegulusRules.MANIA_WINDUP_TICKS; i++) {
         // Production ordering: actionElapsed++ then the length sweep.
         state.actionElapsed++;
         if (state.actionElapsed >= state.actionLength) {
            state.clearAction();
         }
      }

      assertTrue(Mania.shouldGrab(state), "the 19t grab event must still see a live action after the lock sweep");
   }

   @Test
   void grabRequiresTheManiaAction() {
      RegulusState state = new RegulusState();
      state.actionElapsed = RegulusRules.MANIA_WINDUP_TICKS;
      assertFalse(Mania.shouldGrab(state));
   }

   @Test
   void onlyNormalEndFreezes() {
      assertTrue(Mania.endsAsFreeze(ReleaseReason.NORMAL_END));
      for (ReleaseReason reason : ReleaseReason.values()) {
         if (reason != ReleaseReason.NORMAL_END) {
            assertFalse(Mania.endsAsFreeze(reason), reason + " must never freeze");
         }
      }
   }

   @Test
   void channelTimeoutIsOneTwenty() {
      assertFalse(RegulusRules.maniaChannelDone(RegulusRules.MANIA_CHANNEL_TICKS - 1));
      assertTrue(RegulusRules.maniaChannelDone(RegulusRules.MANIA_CHANNEL_TICKS));
      assertTrue(RegulusRules.maniaChannelDone(RegulusRules.MANIA_CHANNEL_TICKS + 40));
      assertEquals(120, RegulusRules.MANIA_CHANNEL_TICKS);
   }

   @Test
   void onlyFastFlightEscapesThePull() {
      assertTrue(RegulusRules.pullEscapes(FlightState.CRUISE));
      assertTrue(RegulusRules.pullEscapes(FlightState.SONIC));
      assertFalse(RegulusRules.pullEscapes(FlightState.HOVER));
      assertFalse(RegulusRules.pullEscapes(FlightState.NONE));
   }

   @Test
   void specNumbersStayPinned() {
      assertEquals(19, RegulusRules.MANIA_WINDUP_TICKS);
      assertEquals(100.0, RegulusRules.MANIA_TARGET_RANGE);
      assertEquals(0.6, RegulusRules.MANIA_PULL_PER_TICK);
      assertEquals(80, RegulusRules.MANIA_FREEZE_TICKS);
      assertEquals(500, RegulusRules.MANIA_COOLDOWN);
   }
}
