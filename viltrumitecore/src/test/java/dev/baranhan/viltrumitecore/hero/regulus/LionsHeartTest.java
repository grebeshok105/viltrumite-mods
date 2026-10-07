package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LionsHeartTest {

   @Test
   void windupFiresEventOnlyAtTickFourteen() {
      RegulusState state = new RegulusState();
      state.beginAction(RegulusHero.ACTION_LION, RegulusRules.LION_WINDUP_TICKS + 1, RegulusRules.LION_WINDUP_TICKS, RegulusRules.LION_WINDUP_TICKS);

      state.actionElapsed = RegulusRules.LION_WINDUP_TICKS - 1;
      assertFalse(LionsHeart.shouldActivate(state), "no protection before the windup event");

      state.actionElapsed = RegulusRules.LION_WINDUP_TICKS;
      assertTrue(LionsHeart.shouldActivate(state));

      state.eventFired = true;
      assertFalse(LionsHeart.shouldActivate(state), "the event fires once");
   }

   @Test
   void windowRecomputesEveryTickShrinkOnly() {
      RegulusState state = new RegulusState();
      int full = RegulusRules.lionWindow(10);
      state.lionWindowMax = full;
      state.lionWindowFloorHearts = 10;

      // A lost heart shortens the running window by 40t each.
      LionsHeart.tickWindow(state, 6);
      assertEquals(full - 4 * RegulusRules.LION_WINDOW_PER_HEART, state.lionWindowMax);
      assertEquals(6, state.lionWindowFloorHearts);

      // A new heart while active never extends the window.
      LionsHeart.tickWindow(state, 12);
      assertEquals(full - 4 * RegulusRules.LION_WINDOW_PER_HEART, state.lionWindowMax);
      assertEquals(6, state.lionWindowFloorHearts);

      // Another loss after the gain keeps shrinking from the floor.
      LionsHeart.tickWindow(state, 4);
      assertEquals(full - 6 * RegulusRules.LION_WINDOW_PER_HEART, state.lionWindowMax);
   }

   @Test
   void windowMayShrinkToZeroUnderMassLoss() {
      RegulusState state = new RegulusState();
      state.lionWindowMax = RegulusRules.lionWindow(3); // 180
      state.lionWindowFloorHearts = 3;
      LionsHeart.tickWindow(state, 0);
      assertEquals(60, state.lionWindowMax);
   }

   @Test
   void overheatStartsOnlyPastTheWindow() {
      RegulusState state = new RegulusState();
      state.lionWindowMax = 100;
      state.lionElapsed = 100;
      assertFalse(LionsHeart.overheating(state));
      state.lionElapsed = 101;
      assertTrue(LionsHeart.overheating(state));
   }

   @Test
   void overheatDamagePerTickFollowsTheCurve() {
      // 1.5 HP/s base, +0.5 HP/s per 40t of overheat; applied per tick.
      assertEquals(1.5F / 20.0F, RegulusRules.overheatDps(1) / 20.0F, 1.0E-7);
      assertEquals(2.0F / 20.0F, RegulusRules.overheatDps(40) / 20.0F, 1.0E-7);
      assertEquals(2.5F / 20.0F, RegulusRules.overheatDps(80) / 20.0F, 1.0E-7);
   }

   @Test
   void forcedOffAtOrBelowFourHp() {
      assertTrue(RegulusRules.lionForcedOff(4.0F));
      assertTrue(RegulusRules.lionForcedOff(3.5F));
      assertFalse(RegulusRules.lionForcedOff(4.1F));
   }

   @Test
   void manualOffCostsHundredForcedOffCostsSixHundred() {
      assertEquals(RegulusRules.LION_MANUAL_COOLDOWN, RegulusRules.lionCooldownBase(false));
      assertEquals(RegulusRules.LION_FORCED_COOLDOWN, RegulusRules.lionCooldownBase(true));

      // Cooldowns scale by hearts sampled at start (spec 5.5 applies to all).
      RegulusState state = new RegulusState();
      for (int i = 0; i < RegulusRules.MAX_HEARTS; i++) {
         state.carriers.add(java.util.UUID.randomUUID());
      }
      state.startCooldown(RegulusAbilities.LIONS_HEART, RegulusRules.lionCooldownBase(true));
      assertEquals(384, state.cooldownOf(RegulusAbilities.LIONS_HEART)); // ceil(600 * 0.64)
   }
}
