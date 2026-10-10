package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.ironman.combat.Overdraft;
import org.junit.jupiter.api.Test;

class OverdraftTest {
   private static int run(Overdraft o, Overdraft.Event until) {
      for (int t = 1; t <= 500; t++) {
         if (o.tick() == until) {
            return t;
         }
      }
      return -1;
   }

   @Test
   void thirdOverheatStartsOverdraft() {
      IronManState state = new IronManState();
      state.overheat.add();
      state.overheat.add();
      assertTrue(state.overheat.nextIsOverdraft());
   }

   @Test
   void sputterAt40() {
      Overdraft o = new Overdraft();
      o.start();
      assertEquals(40, run(o, Overdraft.Event.SPUTTER));
      assertTrue(o.sputtering());
   }

   @Test
   void explodesAt70() {
      Overdraft o = new Overdraft();
      o.start();
      assertEquals(70, run(o, Overdraft.Event.EXPLODE));
      assertFalse(o.active());
   }

   @Test
   void releaseStillExplodesWeaker() {
      Overdraft o = new Overdraft();
      o.start();
      for (int i = 0; i < 10; i++) {
         o.tick();
      }
      o.release();
      assertEquals(60, run(o, Overdraft.Event.EXPLODE), "same moment: 70 t after the start");
      assertEquals(IronManRules.CORE_EXPLOSION_POWER_RELEASED, o.power());
      Overdraft full = new Overdraft();
      full.start();
      assertEquals(IronManRules.CORE_EXPLOSION_POWER, full.power());
   }

   @Test
   void explosionLeavesTwoHearts() {
      assertEquals(2.0F, Overdraft.survivalClamp(6.0F, 50.0F), 1.0E-5F, "6 HP, no armor: 2 HP lost, 4 HP left");
      assertEquals(1.0F, Overdraft.survivalClamp(20.0F, 1.0F), 1.0E-5F, "small hits pass unchanged");
      assertEquals(0.0F, Overdraft.survivalClamp(3.0F, 10.0F), 1.0E-5F, "below the floor nothing is lost");
   }

   @Test
   void deferredPayoutOfOwnExplosionClamped() {
      // The rule is keyed by the source and the window, not by the blast tick.
      assertTrue(Overdraft.isOwnCoreExplosion(true, true, IronManRules.CORE_EXPLOSION_WINDOW - 30));
      assertFalse(Overdraft.isOwnCoreExplosion(true, true, 0));
   }

   @Test
   void otherDamageStillLethal() {
      assertFalse(Overdraft.isOwnCoreExplosion(false, true, 20), "not an explosion");
      assertFalse(Overdraft.isOwnCoreExplosion(true, false, 20), "someone else's explosion");
   }

   @Test
   void nanoLocked30s() {
      Suit suit = new Suit();
      suit.toggle();
      for (int i = 0; i < 40; i++) {
         suit.tick();
      }
      suit.scatter(IronManRules.NANO_LOST_TICKS);
      assertSame(SuitState.NONE, suit.state());
      assertTrue(suit.nanoLocked());
      assertEquals(600, IronManRules.NANO_LOST_TICKS);
      for (int i = 0; i < IronManRules.NANO_LOST_TICKS; i++) {
         suit.tick();
      }
      assertFalse(suit.nanoLocked());
   }

   @Test
   void deathCancelsPendingExplosion() {
      IronManState state = new IronManState();
      state.overheat.add();
      state.overheat.add();
      state.overdraft.start();
      state.onCleanup(CleanupReason.DEATH);
      assertFalse(state.overdraft.active());
      assertEquals(0, state.overheat.count());
      IronManState respawned = IronManState.cloneForRespawn(state);
      assertFalse(respawned.overdraft.active());
   }

   @Test
   void controlDoesNotCancelOverdraft() {
      IronManState state = new IronManState();
      state.unibeam.press(false);
      for (int i = 0; i < IronManRules.UNIBEAM_CHARGE; i++) {
         state.unibeam.tick();
      }
      state.unibeam.startBeam(true);
      state.overdraft.start();
      assertFalse(state.unibeam.interrupt());
      state.stopCombat();
      assertTrue(state.overdraft.active());
      assertTrue(state.unibeam.overdraft());
   }
}
