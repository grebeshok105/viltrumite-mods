package dev.baranhan.viltrumitecore.hero.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;
import org.junit.jupiter.api.Test;

class ControlManagerTest {

   @Test
   void freezeRecordDropsOnlyWhenProvablyGone() {
      // A resolved projectile keeps its freeze record.
      assertFalse(ControlManager.shouldDropFreezeRecord(true, true));
      assertFalse(ControlManager.shouldDropFreezeRecord(true, false));
      // An unloaded chunk keeps the record too: the projectile is still there
      // and re-resolves on reload, so its original gravity survives.
      assertFalse(ControlManager.shouldDropFreezeRecord(false, false));
      // Loaded chunk but no entity = removed or moved away; the record drops
      // and the gravity restore defers to the next entity join.
      assertTrue(ControlManager.shouldDropFreezeRecord(false, true));
   }

   // --- deferred queue cap math --------------------------------------------

   @Test
   void deferredQueueAccumulatesUpToTheCap() {
      // Freeze cap: 40% of max health (spec 8.3); dome cap: 35% (spec 9.2).
      assertEquals(8.0F, RegulusRules.freezeDeferredCap(20.0F));
      assertEquals(7.0F, RegulusRules.domeDeferredCap(20.0F));
      assertEquals(40.0F, RegulusRules.freezeDeferredCap(100.0F));
      assertEquals(35.0F, RegulusRules.domeDeferredCap(100.0F));
   }

   @Test
   void deferredAccumulateNeverCrossesTheCap() {
      float cap = RegulusRules.freezeDeferredCap(100.0F);
      assertEquals(10.0F, RegulusRules.deferredAccumulate(0.0F, 10.0F, cap));
      assertEquals(30.0F, RegulusRules.deferredAccumulate(10.0F, 20.0F, cap));
      assertEquals(cap, RegulusRules.deferredAccumulate(30.0F, 20.0F, cap));
      assertEquals(cap, RegulusRules.deferredAccumulate(cap, 999.0F, cap));
      // Garbage in never produces negative queue.
      assertEquals(0.0F, RegulusRules.deferredAccumulate(0.0F, -5.0F, cap));
   }

   // --- acquire precedence --------------------------------------------------

   @Test
   void aControlledTargetRejectsEveryNewAcquire() {
      for (ControlKind kind : ControlKind.values()) {
         assertFalse(ControlManager.mayAcquire(kind, true, false, true), kind + " must not stack controls");
      }
   }

   @Test
   void aGrabbedTargetRejectsEveryKind() {
      // Precedence: the legacy viltrumite grab owns the victim outright.
      for (ControlKind kind : ControlKind.values()) {
         assertFalse(ControlManager.mayAcquire(kind, false, true, true), kind + " must refuse grab-tagged targets");
      }
   }

   @Test
   void heroPolicyAndFreeTargets() {
      for (ControlKind kind : ControlKind.values()) {
         assertTrue(ControlManager.mayAcquire(kind, false, false, true));
         assertFalse(ControlManager.mayAcquire(kind, false, false, false));
      }
   }

   // --- caster cleanup ------------------------------------------------------

   @Test
   void casterDeathKeepsDomeLinkedStasisOnly() {
      // Spec 9.2: a dead caster's dome lives its full duration, victims included.
      assertFalse(ControlManager.releaseOnCasterCleanup(ControlKind.STASIS, true, CleanupReason.DEATH));
      assertTrue(ControlManager.releaseOnCasterCleanup(ControlKind.PULL, false, CleanupReason.DEATH));
      assertTrue(ControlManager.releaseOnCasterCleanup(ControlKind.FREEZE, false, CleanupReason.DEATH));
      // A STASIS record whose dome is already gone releases normally.
      assertTrue(ControlManager.releaseOnCasterCleanup(ControlKind.STASIS, false, CleanupReason.DEATH));
   }

   @Test
   void disconnectAndHeroChangeReleaseEverything() {
      for (ControlKind kind : new ControlKind[] {ControlKind.PULL, ControlKind.FREEZE, ControlKind.STASIS}) {
         assertTrue(ControlManager.releaseOnCasterCleanup(kind, true, CleanupReason.DISCONNECT));
         assertTrue(ControlManager.releaseOnCasterCleanup(kind, true, CleanupReason.HERO_CHANGE));
      }
   }

   @Test
   void cleanupReasonsMapOntoReleaseReasons() {
      assertEquals(ReleaseReason.CASTER_DEATH, ControlManager.cleanupReleaseReason(CleanupReason.DEATH));
      assertEquals(ReleaseReason.CASTER_DISCONNECT, ControlManager.cleanupReleaseReason(CleanupReason.DISCONNECT));
      assertEquals(ReleaseReason.HERO_CHANGE, ControlManager.cleanupReleaseReason(CleanupReason.HERO_CHANGE));
   }
}
