package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class HelmetTest {
   @Test
   void defaultClosed() {
      Helmet helmet = new Helmet();
      assertTrue(helmet.closed());
      assertEquals(1.0F, helmet.progress());
      assertFalse(helmet.moving());
   }

   @Test
   void toggleTakes12() {
      Helmet helmet = new Helmet();
      helmet.toggle();
      assertFalse(helmet.closed(), "gating flips at once");
      for (int i = 0; i < IronManRules.HELMET_TOGGLE_TICKS - 1; i++) {
         helmet.tick();
         assertTrue(helmet.moving());
      }

      helmet.tick();
      assertFalse(helmet.moving());
      assertEquals(0.0F, helmet.progress());
      assertEquals(12, IronManRules.HELMET_TOGGLE_TICKS);
   }

   @Test
   void reverseMidFoldContinuesFromPosition() {
      Helmet helmet = new Helmet();
      helmet.toggle();
      for (int i = 0; i < 4; i++) {
         helmet.tick();
      }

      float before = helmet.progress();
      helmet.toggle();
      assertTrue(helmet.closed());
      assertEquals(before, helmet.progress(), 1.0E-4F);
   }

   @Test
   void hitClosesOpenHelmet() {
      Helmet helmet = new Helmet();
      assertFalse(helmet.onHit(), "already closed");
      helmet.toggle();
      assertTrue(helmet.onHit());
      assertTrue(helmet.closed());
   }

   @Test
   void onlyCombatHitsClose() {
      assertTrue(Helmet.combatHit(true, false));
      assertTrue(Helmet.combatHit(false, true));
      assertFalse(Helmet.combatHit(false, false), "fall / fire / drowning");
   }

   @Test
   void openHelmetBlocksMarks() {
      IronManState state = new IronManState();
      assertTrue(state.canMarkTargets());
      state.helmet.toggle();
      assertFalse(state.canMarkTargets());
      // A forged press still opens the flaps, but no offer is made while the helmet is open:
      // IronManCombat.missilesTick checks canMarkTargets before MissileLock.offer.
      assertTrue(state.missiles.press(false));
      for (int i = 0; i < IronManRules.MISSILE_FLAPS; i++) {
         state.missiles.tick();
      }

      if (state.canMarkTargets()) {
         state.missiles.offer(42, true);
      }

      assertTrue(state.missiles.marks().isEmpty());
   }

   @Test
   void saveLoad() {
      Helmet helmet = new Helmet();
      helmet.toggle();
      CompoundTag tag = new CompoundTag();
      helmet.save(tag);
      Helmet loaded = new Helmet();
      loaded.load(tag);
      assertFalse(loaded.closed());
      assertFalse(loaded.moving(), "no animation after login");
      Helmet fresh = new Helmet();
      fresh.load(new CompoundTag());
      assertTrue(fresh.closed(), "default closed");
   }

   @Test
   void heroChangeClosesHelmetAndClearsCountermeasures() {
      IronManState state = new IronManState();
      state.helmet.toggle();
      state.countermeasures.fire();
      state.onCleanup(dev.baranhan.viltrumitecore.hero.CleanupReason.HERO_CHANGE);
      assertTrue(state.helmet.closed());
      assertTrue(state.countermeasures.ready());
   }

   @Test
   void deathKeepsCountermeasuresCooldown() {
      IronManState state = new IronManState();
      state.countermeasures.fire();
      IronManState respawned = IronManState.cloneForRespawn(state);
      assertEquals(IronManRules.COUNTERMEASURES_COOLDOWN, respawned.countermeasures.cooldown());
   }
}
