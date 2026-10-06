package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * Debris Kick rules (spec 7): 44t cast, event at 14t, nine rays in a 35° cone,
 * range 16, <=3 blocks pierced per ray, damage 7 -> 2 by distance, heart bonus.
 */
class DebrisKickTest {

   @Test
   void rayFanIsNineRaysInsideThirtyFiveDegreeCone() {
      double[][] offsets = RegulusRules.debrisRayOffsets();
      assertEquals(RegulusRules.DEBRIS_RAYS, offsets.length);
      assertEquals(9, offsets.length);

      // Center ray travels along the view direction.
      assertEquals(0.0, offsets[0][0], 1.0E-9);
      assertEquals(0.0, offsets[0][1], 1.0E-9);

      // Every other ray sits exactly on the cone surface at the half-angle.
      double half = RegulusRules.DEBRIS_CONE_DEGREES / 2.0;
      for (int i = 1; i < offsets.length; i++) {
         double angular = Math.hypot(offsets[i][0], offsets[i][1]);
         assertEquals(half, angular, 1.0E-9, "ray " + i + " must sit on the 35° cone edge");
      }

      // The fan spreads evenly: 8 distinct azimuths around the center ray.
      for (int i = 1; i < offsets.length; i++) {
         for (int j = i + 1; j < offsets.length; j++) {
            boolean distinct = Math.abs(offsets[i][0] - offsets[j][0]) > 1.0E-9
               || Math.abs(offsets[i][1] - offsets[j][1]) > 1.0E-9;
            assertTrue(distinct, "rays " + i + " and " + j + " must not overlap");
         }
      }
   }

   @Test
   void rayDamageFallsFromSevenToTwoAndNineHitsReachSixtyThree() {
      assertEquals(7.0F, RegulusRules.debrisDamage(0.0, 0), 1.0E-6);
      assertEquals(7.0F, RegulusRules.debrisDamage(4.0, 0), 1.0E-6);
      assertEquals(4.5F, RegulusRules.debrisDamage(10.0, 0), 1.0E-6);
      assertEquals(2.0F, RegulusRules.debrisDamage(RegulusRules.DEBRIS_RANGE, 0), 1.0E-6);

      // Spec 7.2: point blank a target may take all nine rays = 63.
      assertEquals(63.0F, RegulusRules.DEBRIS_RAYS * RegulusRules.debrisDamage(0.0, 0), 1.0E-5);
   }

   @Test
   void pointBlankEyeInsideTheTargetBoxStillHits() {
      // Spec 7.2 showcases the melee-hug 63 damage; AABB.clip returns empty when
      // the eye is already inside the box, so containment must hit at distance 0.
      AABB box = new AABB(0.0, 60.0, 0.0, 1.0, 62.0, 1.0);
      Vec3 inside = new Vec3(0.5, 61.0, 0.5);
      assertEquals(0.0, DebrisKick.rayDistance(inside, new Vec3(0.5, 61.0, 17.0), box));

      // A normal box entry keeps its distance; a miss reports MAX_VALUE.
      AABB farBox = new AABB(9.0, 60.0, 0.0, 10.0, 62.0, 1.0);
      assertEquals(8.5, DebrisKick.rayDistance(new Vec3(0.5, 61.0, 0.5), new Vec3(16.5, 61.0, 0.5), farBox), 1.0E-9);
      assertEquals(Double.MAX_VALUE, DebrisKick.rayDistance(new Vec3(0.5, 70.0, 0.5), new Vec3(16.5, 70.0, 0.5), box));
   }

   @Test
   void rayPiercesAtMostThreeBlocks() {
      int B = RegulusRules.DEBRIS_BLOCK_BREAKABLE;
      int U = RegulusRules.DEBRIS_BLOCK_UNBREAKABLE;
      int far = Integer.MAX_VALUE; // no entity on the path

      assertEquals(0, RegulusRules.debrisRayTraversal(new int[0], far));
      assertEquals(3, RegulusRules.debrisRayTraversal(new int[]{B, B, B, B, B}, far), "quota caps the ray at 3 blocks");
      assertEquals(2, RegulusRules.debrisRayTraversal(new int[]{B, B}, far));
      assertEquals(0, RegulusRules.debrisRayTraversal(new int[]{U, B, B}, far), "unbreakable stops the ray");
      assertEquals(1, RegulusRules.debrisRayTraversal(new int[]{B, U, B}, far));
      assertEquals(3, RegulusRules.debrisRayTraversal(new int[]{B, B, B, U}, far));
   }

   @Test
   void entityHitStopsTheRayAndBlocksBehindItSurvive() {
      int B = RegulusRules.DEBRIS_BLOCK_BREAKABLE;
      int U = RegulusRules.DEBRIS_BLOCK_UNBREAKABLE;

      // Entity behind the second block: only two blocks are on the path.
      assertEquals(2, RegulusRules.debrisRayTraversal(new int[]{B, B, B, B}, 2));
      // Entity behind the fifth block: the quota still caps the ray at 3.
      assertEquals(3, RegulusRules.debrisRayTraversal(new int[]{B, B, B, B, B}, 5));
      // Entity behind an unbreakable block is never reached.
      assertEquals(1, RegulusRules.debrisRayTraversal(new int[]{B, U, B}, 3));
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
}
