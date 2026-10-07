package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusState;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * Lifecycle contract: change is a no-op on the same id, restore resets state
 * only when the id differs, cleanup wipes every transient combat field, and
 * the action lock survives everything except a hero change.
 */
class HeroLifecycleTest {

   @Test
   void changeIsANoOpOnTheSameId() {
      assertFalse(HeroRegistry.isRealTransition(HeroId.REGULUS, HeroId.REGULUS), "re-selecting the current hero is never an exit/re-entry");
      assertTrue(HeroRegistry.isRealTransition(HeroId.REGULUS, HeroId.VILTRUMITE));
      assertTrue(HeroRegistry.isRealTransition(HeroId.HUMAN, HeroId.REGULUS));
   }

   @Test
   void restoreResetsStateOnlyWhenIdDiffers() {
      assertFalse(HeroRegistry.shouldResetState(HeroId.REGULUS, HeroId.REGULUS), "same-id restore keeps the loaded state");
      assertTrue(HeroRegistry.shouldResetState(HeroId.HUMAN, HeroId.REGULUS));
      assertTrue(HeroRegistry.shouldResetState(HeroId.REGULUS, HeroId.VILTRUMITE));
   }

   @Test
   void cleanupOnDeathWipesTransientCombatState() {
      RegulusState state = filled();

      state.resetTransient(CleanupReason.DEATH);
      assertClean(state);
      assertEquals(RegulusHero.ACTION_COUNTER, state.actionId, "the cast lock belongs to the entity, not the death");
   }

   @Test
   void heroChangeAlsoClearsTheCastLock() {
      RegulusState state = filled();

      state.resetTransient(CleanupReason.HERO_CHANGE);
      assertClean(state);
      assertNull(state.actionId);
      assertEquals(0, state.actionElapsed);
      assertNull(state.actionTargetId);
      assertNull(state.actionPoint);
   }

   @Test
   void resetIsIdempotent() {
      RegulusState state = filled();
      state.resetTransient(CleanupReason.HERO_CHANGE);
      state.resetTransient(CleanupReason.DEATH);
      assertClean(state);
      assertNull(state.actionId);
   }

   private static RegulusState filled() {
      RegulusState state = new RegulusState();
      state.beginAction(RegulusHero.ACTION_COUNTER, 28, 27, 27);
      state.actionElapsed = 12;
      state.actionTargetId = UUID.randomUUID();
      state.actionPoint = new Vec3(1.0, 64.0, 1.0);
      state.carriers.add(UUID.randomUUID());
      state.carriers.add(UUID.randomUUID());
      state.pushedCarriers.addAll(state.carriers);
      state.nextCarrierScan = 999L;
      state.lionActive = true;
      state.lionWindowMax = 460;
      state.lionElapsed = 30;
      state.overheatTicks = 7;
      state.lionStartTick = 4242L;
      state.lionWindowFloorHearts = 2;
      state.attackerId = UUID.randomUUID();
      state.attackerTick = 50L;
      state.attackerLastPos = new Vec3(2.0, 64.0, 2.0);
      state.ritualTicks = 40;
      state.madnessTicksLeft = 500;
      state.channelTargetId = UUID.randomUUID();
      state.channelTicks = 66;
      state.jumpHeld = true;
      state.jumpCharge = 40;
      state.lastFallDistance = 12.5F;
      state.lastSeenHealth = 17.0F;
      state.internalDamage = 3.0F;
      return state;
   }

   private static void assertClean(RegulusState state) {
      assertTrue(state.carriers.isEmpty());
      assertTrue(state.pushedCarriers.isEmpty());
      assertEquals(0L, state.nextCarrierScan);
      assertFalse(state.lionActive);
      assertEquals(0, state.lionWindowMax);
      assertEquals(0, state.lionElapsed);
      assertEquals(0, state.overheatTicks);
      assertEquals(0L, state.lionStartTick);
      assertEquals(-1, state.lionWindowFloorHearts);
      assertNull(state.attackerId);
      assertEquals(Long.MIN_VALUE, state.attackerTick);
      assertNull(state.attackerLastPos);
      assertEquals(-1, state.ritualTicks);
      assertEquals(0, state.madnessTicksLeft);
      assertNull(state.channelTargetId);
      assertEquals(0, state.channelTicks);
      assertFalse(state.jumpHeld);
      assertEquals(0, state.jumpCharge);
      assertTrue(state.wasOnGround);
      assertEquals(0.0F, state.lastFallDistance);
      assertEquals(-1.0F, state.lastSeenHealth);
      assertEquals(0.0F, state.internalDamage);
   }
}
