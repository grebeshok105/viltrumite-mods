package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Shard fan geometry, kick timing and shared gaze collision. */
class DebrisKickTest {

   @Test
   void pointBlankEyeInsideTheTargetBoxStillHits() {
      AABB box = new AABB(0.0, 60.0, 0.0, 1.0, 62.0, 1.0);
      Vec3 inside = new Vec3(0.5, 61.0, 0.5);
      assertEquals(0.0, DebrisKick.rayDistance(inside, new Vec3(0.5, 61.0, 17.0), box));

      // A normal box entry keeps its distance; a miss reports MAX_VALUE.
      AABB farBox = new AABB(9.0, 60.0, 0.0, 10.0, 62.0, 1.0);
      assertEquals(8.5, DebrisKick.rayDistance(new Vec3(0.5, 61.0, 0.5), new Vec3(16.5, 61.0, 0.5), farBox), 1.0E-9);
      assertEquals(Double.MAX_VALUE, DebrisKick.rayDistance(new Vec3(0.5, 70.0, 0.5), new Vec3(16.5, 70.0, 0.5), box));
   }

   @Test
   void castLocksActionsUntilEventThenFreesWhileAnimRuns() {
      RegulusState state = new RegulusState();
      state.beginAction(RegulusHero.ACTION_DEBRIS, RegulusRules.DEBRIS_ANIM_TICKS, RegulusRules.DEBRIS_EVENT_TICK, RegulusRules.DEBRIS_EVENT_TICK);

      state.actionElapsed = RegulusRules.DEBRIS_EVENT_TICK - 1;
      assertTrue(state.busy(), "pre-event the cast locks other actions");

      // The event fired but the 44t animation keeps running for the snapshot.
      state.eventFired = true;
      state.actionElapsed = RegulusRules.DEBRIS_EVENT_TICK;
      assertFalse(state.busy(), "after the 14t event Regulus acts freely");
      assertNotNull(state.actionId, "the animation keeps playing to tick 44");
   }

   @Test
   void cancelBeforeEventIsFreeAndStartsNoCooldown() {
      RegulusState state = new RegulusState();
      state.beginAction(RegulusHero.ACTION_DEBRIS, RegulusRules.DEBRIS_ANIM_TICKS, RegulusRules.DEBRIS_EVENT_TICK, RegulusRules.DEBRIS_EVENT_TICK);
      state.actionElapsed = RegulusRules.DEBRIS_EVENT_TICK - 4;

      // Real damage before the event cancels the cast for free (spec 7.1).
      assertTrue(state.busy() && !state.eventFired);
      state.clearAction();
      assertNull(state.actionId);
      assertEquals(0, state.cooldownOf(RegulusAbilities.DEBRIS_KICK), "free cancel starts no cooldown");
   }

   @Test
   void cooldownSamplesHeartsAtEventStart() {
      RegulusState state = new RegulusState();
      state.startCooldown(RegulusAbilities.DEBRIS_KICK, RegulusRules.DEBRIS_COOLDOWN);
      assertEquals(400, state.cooldownOf(RegulusAbilities.DEBRIS_KICK));

      for (int i = 0; i < RegulusRules.MAX_HEARTS; i++) {
         state.carriers.add(java.util.UUID.randomUUID());
      }

      state.startCooldown(RegulusAbilities.DEBRIS_KICK, RegulusRules.DEBRIS_COOLDOWN);
      assertEquals(256, state.cooldownOf(RegulusAbilities.DEBRIS_KICK)); // ceil(400 * 0.64)
   }

   @Test
   void shardsFanAcrossTheConeAndStayForward() {
      assertEquals(-22.0, DebrisKick.shardYawOffset(0, 14), 1.0E-9);
      assertEquals(22.0, DebrisKick.shardYawOffset(13, 14), 1.0E-9);
      assertEquals(0.0, DebrisKick.shardYawOffset(0, 1), 1.0E-9);
      // yaw 0 faces +Z in Minecraft; a flat shard goes straight forward.
      Vec3 straight = DebrisKick.shardDirection(0.0F, 0.0F, 0.0, 0.0);
      assertEquals(1.0, straight.z, 1.0E-9);
      assertEquals(0.0, straight.y, 1.0E-9);
      Vec3 rising = DebrisKick.shardDirection(0.0F, 0.0F, 0.0, 9.0);
      assertTrue(rising.y > 0.0 && rising.z > 0.9, "positive elevation lifts the shard");
   }

   @Test
   void kickPitchFollowsGazeOnlyMildly() {
      assertEquals(0.0F, DebrisKick.aimPitch(0.0F), 1.0E-6);
      assertEquals(15.0F, DebrisKick.aimPitch(80.0F), 1.0E-6, "looking at feet: clamped then halved");
      assertEquals(-15.0F, DebrisKick.aimPitch(-90.0F), 1.0E-6);
   }

   @Test
   void shardMaterialIsTheBlockActuallyUnderTheFeet() {
      // Full block: feet at y=65.0 stand on the block at 64.
      assertEquals(new net.minecraft.core.BlockPos(0, 64, 0), DebrisKick.feetSurface(0.5, 65.0, 0.5));
      // Bottom slab at y=64: feet at 64.5 -> the slab itself, not the block below it.
      assertEquals(new net.minecraft.core.BlockPos(0, 64, 0), DebrisKick.feetSurface(0.5, 64.5, 0.5));
      // Carpet on 64: feet at 65.0625 -> the carpet cell 65.
      assertEquals(new net.minecraft.core.BlockPos(0, 65, 0), DebrisKick.feetSurface(0.5, 65.0625, 0.5));
   }
}
