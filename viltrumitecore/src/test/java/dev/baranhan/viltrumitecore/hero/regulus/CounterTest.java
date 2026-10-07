package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * Counter: usable at any time (no madness gate), arms from the last living
 * attacker record or the gaze target, latches its target and spends the 800t
 * cooldown when the slam lands.
 */
class CounterTest {

   @Test
   void pressNoLongerNeedsMadness() {
      RegulusState state = new RegulusState();
      state.attackerId = UUID.randomUUID();
      state.attackerTick = 100L;

      assertTrue(Counter.canActivate(state, true, true, true), "counter works outside madness");
   }

   @Test
   void liftEasesOutFromLaunchToApex() {
      assertEquals(0.0, Counter.liftProgress(0), 1.0E-9);
      assertEquals(0.0, Counter.liftProgress(RegulusRules.COUNTER_LAUNCH_TICK), 1.0E-9);
      assertEquals(1.0, Counter.liftProgress(RegulusRules.COUNTER_LIFT_TICKS), 1.0E-9);
      assertEquals(1.0, Counter.liftProgress(RegulusRules.COUNTER_LIFT_TICKS + 5), 1.0E-9);
      int mid = (RegulusRules.COUNTER_LAUNCH_TICK + RegulusRules.COUNTER_LIFT_TICKS) / 2;
      assertTrue(Counter.liftProgress(mid) > 0.6, "ease-out covers most height early");
   }

   @Test
   void pressNeedsARealInRangeAttacker() {
      RegulusState state = new RegulusState();
      state.madnessTicksLeft = RegulusRules.MADNESS_TICKS;
      state.attackerId = UUID.randomUUID();

      assertTrue(Counter.canActivate(state, true, true, true));
      assertFalse(Counter.canActivate(state, false, true, true), "a stale record is no target");
      assertFalse(Counter.canActivate(state, true, false, true), "a dead attacker is no target");
      assertFalse(Counter.canActivate(state, true, true, false), "an out-of-range attacker is no target");

      state.attackerId = null;
      assertFalse(Counter.canActivate(state, true, true, true), "no record at all is no target");
   }

   @Test
   void castLatchesOffenderAndArmsLiftThenSlam() {
      RegulusState state = new RegulusState();
      UUID attacker = UUID.randomUUID();
      state.attackerId = attacker;
      Vec3 pos = new Vec3(3.0, 64.0, 3.0);

      Counter.beginCast(state, attacker, pos);

      int slamTick = RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS;
      assertEquals(RegulusHero.ACTION_COUNTER, state.actionId);
      assertEquals(slamTick + 1, state.actionLength);
      assertEquals(slamTick, state.actionEventTick, "slam fires 7t after the teleport");
      assertEquals(slamTick, state.actionUnlockTick);
      assertEquals(attacker, state.actionTargetId, "the offender is latched at cast time");
      assertEquals(pos, state.actionPoint, "last known position starts at the target");
      assertEquals(64.0, state.counterApexY, 1.0E-9);
   }

   @Test
   void slamHitsOnlyAliveInRangeTarget() {
      assertTrue(Counter.slamHitsTarget(true, true));
      assertFalse(Counter.slamHitsTarget(false, true), "dead target takes no slam damage");
      assertFalse(Counter.slamHitsTarget(true, false), "target past 40 blocks takes no slam damage");
   }

   @Test
   void behindSpotSitsOppositeTargetFacing() {
      // MC yaw 0 faces +Z (south): behind is -Z.
      Vec3 spot = Counter.behindSpot(new Vec3(10.0, 64.0, 10.0), 0.0F);
      assertEquals(10.0, spot.x, 1.0E-6);
      assertEquals(64.0, spot.y, 1.0E-6);
      assertEquals(10.0 - RegulusRules.COUNTER_BEHIND_DISTANCE, spot.z, 1.0E-6, "behind a south-facing target is -Z");

      // Yaw 90 faces -X (west): behind is +X.
      Vec3 west = Counter.behindSpot(new Vec3(10.0, 64.0, 10.0), 90.0F);
      assertEquals(10.0 + RegulusRules.COUNTER_BEHIND_DISTANCE, west.x, 1.0E-6);
      assertEquals(10.0, west.z, 1.0E-6);
   }

   @Test
   void slamCooldownSamplesHeartsAtSlam() {
      RegulusState state = new RegulusState();
      for (int i = 0; i < RegulusRules.MAX_HEARTS; i++) {
         state.carriers.add(UUID.randomUUID());
      }

      Counter.chargeSlamCooldown(state);

      assertEquals(
         RegulusRules.cooldown(RegulusRules.COUNTER_COOLDOWN, RegulusRules.MAX_HEARTS),
         state.cooldownOf(RegulusAbilities.COUNTER),
         "800 base scaled by hearts when the slam lands"
      );
   }
}
