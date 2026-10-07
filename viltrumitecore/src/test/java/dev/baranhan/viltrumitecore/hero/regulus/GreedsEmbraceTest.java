package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GreedsEmbraceTest {

   @Test
   void skyFallbackProbesNearGroundInFacingDirection() {
      net.minecraft.world.phys.Vec3 feet = new net.minecraft.world.phys.Vec3(2.0, 64.0, 3.0);
      assertEquals(new net.minecraft.world.phys.Vec3(2.0, 66.0, 15.0), GreedsEmbrace.nearAimPoint(feet, 0.0F));
      net.minecraft.world.phys.Vec3 west = GreedsEmbrace.nearAimPoint(feet, 90.0F);
      assertEquals(-10.0, west.x, 0.00001);
      assertEquals(66.0, west.y, 0.00001);
      assertEquals(3.0, west.z, 0.00001);
   }

   private static RegulusState embraceState() {
      RegulusState state = new RegulusState();
      state.beginAction(RegulusHero.ACTION_EMBRACE, RegulusRules.EMBRACE_RECOVER_TICK + 1, RegulusRules.EMBRACE_APPEAR_TICK, RegulusRules.EMBRACE_RECOVER_TICK);
      return state;
   }

   @Test
   void lockHappensBeforeTheEvent() {
      RegulusState state = embraceState();
      assertFalse(GreedsEmbrace.shouldLock(state));

      state.actionElapsed = RegulusRules.EMBRACE_LOCK_TICK - 1;
      assertFalse(GreedsEmbrace.shouldLock(state));

      state.actionElapsed = RegulusRules.EMBRACE_LOCK_TICK;
      assertTrue(GreedsEmbrace.shouldLock(state));

      // Once the point is stored it never re-locks (spec: the aim freezes).
      state.actionPoint = net.minecraft.world.phys.Vec3.ZERO;
      assertFalse(GreedsEmbrace.shouldLock(state));
   }

   @Test
   void domeAppearsOnlyAtTheEventTick() {
      RegulusState state = embraceState();
      state.actionElapsed = RegulusRules.EMBRACE_APPEAR_TICK - 1;
      assertFalse(GreedsEmbrace.shouldAppear(state));

      state.actionElapsed = RegulusRules.EMBRACE_APPEAR_TICK;
      assertTrue(GreedsEmbrace.shouldAppear(state));

      // Single appearance.
      state.eventFired = true;
      assertFalse(GreedsEmbrace.shouldAppear(state));
   }

   @Test
   void captureExcludesAlliesTamesControlledAndGrabbed() {
      // Every exclusion alone is enough to skip the target (spec 9.2).
      assertFalse(GreedsEmbrace.captureFilter(true, false, false, false, false));
      assertFalse(GreedsEmbrace.captureFilter(false, true, false, false, false));
      assertFalse(GreedsEmbrace.captureFilter(false, false, true, false, false));
      assertFalse(GreedsEmbrace.captureFilter(false, false, false, true, false));
      assertFalse(GreedsEmbrace.captureFilter(false, false, false, false, true));
      assertTrue(GreedsEmbrace.captureFilter(false, false, false, false, false));
   }

   @Test
   void specNumbersStayPinned() {
      assertEquals(13, RegulusRules.EMBRACE_LOCK_TICK);
      assertEquals(18, RegulusRules.EMBRACE_APPEAR_TICK);
      assertEquals(32, RegulusRules.EMBRACE_RECOVER_TICK);
      assertEquals(40.0, RegulusRules.EMBRACE_RANGE);
      assertEquals(8.0, RegulusRules.EMBRACE_RADIUS);
      assertEquals(80, RegulusRules.EMBRACE_DURATION_TICKS);
      assertEquals(700, RegulusRules.EMBRACE_COOLDOWN);
   }
}
