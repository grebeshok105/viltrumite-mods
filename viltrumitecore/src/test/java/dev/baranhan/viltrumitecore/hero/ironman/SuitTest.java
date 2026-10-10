package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SuitTest {

   private static Suit worn() {
      Suit suit = new Suit();
      suit.toggle();
      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS; i++) {
         suit.tick();
      }

      return suit;
   }

   @Test
   void toggleStartsDeploy() {
      Suit suit = new Suit();
      assertSame(SuitState.NONE, suit.state());
      assertTrue(suit.toggle());
      assertSame(SuitState.DEPLOYING, suit.state());
      assertFalse(suit.worn());
      assertFalse(suit.armored());
   }

   @Test
   void deployTakes20Ticks() {
      Suit suit = new Suit();
      suit.toggle();
      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS - 1; i++) {
         suit.tick();
      }

      assertSame(SuitState.DEPLOYING, suit.state());
      assertEquals((IronManRules.SUIT_DEPLOY_TICKS - 1) / (float)IronManRules.SUIT_DEPLOY_TICKS, suit.progress(), 1.0E-5F);
      suit.tick();
      assertSame(SuitState.NANO, suit.state());
      assertTrue(suit.worn());
      assertTrue(suit.armored());
      assertEquals(1.0F, suit.progress(), 1.0E-5F);
   }

   @Test
   void suitToggleIgnoredWhileTransitioning() {
      Suit suit = new Suit();
      suit.toggle();
      suit.tick();
      assertFalse(suit.toggle());
      assertSame(SuitState.DEPLOYING, suit.state());
      Suit nano = worn();
      nano.toggle();
      assertFalse(nano.toggle());
      assertSame(SuitState.RETRACTING, nano.state());
   }

   @Test
   void retractTakes20Ticks() {
      Suit suit = worn();
      assertTrue(suit.toggle());
      assertSame(SuitState.RETRACTING, suit.state());
      assertFalse(suit.worn());
      assertTrue(suit.armored(), "no fall damage while the wave goes back");
      for (int i = 0; i < IronManRules.SUIT_RETRACT_TICKS - 1; i++) {
         suit.tick();
      }

      assertSame(SuitState.RETRACTING, suit.state());
      suit.tick();
      assertSame(SuitState.NONE, suit.state());
      assertFalse(suit.armored());
   }

   @Test
   void interruptCancelsWave() {
      Suit deploying = new Suit();
      deploying.toggle();
      deploying.tick();
      deploying.interrupt();
      assertSame(SuitState.NONE, deploying.state());

      Suit retracting = worn();
      retracting.toggle();
      retracting.interrupt();
      assertSame(SuitState.NONE, retracting.state());

      Suit nano = worn();
      nano.interrupt();
      assertSame(SuitState.NANO, nano.state(), "a worn suit is not a wave");
   }

   @Test
   void saveLoadMidDeployResolvesToTarget() {
      Suit deploying = new Suit();
      deploying.toggle();
      CompoundTag tag = new CompoundTag();
      deploying.save(tag);
      Suit loaded = new Suit();
      loaded.load(tag);
      assertSame(SuitState.NANO, loaded.state());

      Suit retracting = worn();
      retracting.toggle();
      tag = new CompoundTag();
      retracting.save(tag);
      loaded = worn();
      loaded.load(tag);
      assertSame(SuitState.NONE, loaded.state());

      loaded = worn();
      loaded.load(new CompoundTag());
      assertSame(SuitState.NONE, loaded.state());
   }

   @Test
   void cleanupDeathRemovesSuit() {
      IronManState state = new IronManState();
      state.suit.toggle();
      state.energy.drain(60.0F);
      state.onCleanup(CleanupReason.DEATH);
      assertSame(SuitState.NONE, state.suit.state());
   }

   @Test
   void disconnectResolvesToTarget() {
      IronManState state = new IronManState();
      state.suit.toggle();
      state.onCleanup(CleanupReason.DISCONNECT);
      assertSame(SuitState.NANO, state.suit.state());
   }

   @Test
   void heroChangeClearsAll() {
      IronManState state = new IronManState();
      state.suit.toggle();
      state.energy.drain(100.0F);
      state.onCleanup(CleanupReason.HERO_CHANGE);
      assertSame(SuitState.NONE, state.suit.state());
      assertEquals(IronManRules.ENERGY_MAX, state.energy.value(), 1.0E-5F);
      assertFalse(state.energy.weaponsLocked());
   }

   @Test
   void deathCloneKeepsNothingButCooldowns() {
      IronManState original = new IronManState();
      original.suit.toggle();
      original.energy.drain(70.0F);
      IronManState clone = IronManState.cloneForRespawn(original);
      assertSame(SuitState.NONE, clone.suit.state());
      assertEquals(IronManRules.ENERGY_MAX, clone.energy.value(), 1.0E-5F);
   }

   @Test
   void grantFollowsSuit() {
      IronManState state = new IronManState();
      assertFalse(state.wantsFlight());
      state.suit.toggle();
      assertFalse(state.wantsFlight(), "no flight while the wave is still forming");
      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS; i++) {
         state.suit.tick();
      }

      assertTrue(state.wantsFlight());
      state.suit.toggle();
      assertFalse(state.wantsFlight(), "undress revokes flight at once");
   }

   @Test
   void snapshotFlags() {
      IronManState state = new IronManState();
      HeroPublicSnapshot none = IronManHero.snapshotOf(state);
      assertSame(HeroId.IRON_MAN, none.heroId());
      assertEquals(0, none.heroFlags());
      assertEquals(-1, none.actionId());
      assertEquals(1000, none.resource());

      state.suit.toggle();
      for (int i = 0; i < 5; i++) {
         state.suit.tick();
      }

      HeroPublicSnapshot deploying = IronManHero.snapshotOf(state);
      assertTrue(IronManFlags.is(deploying.heroFlags(), IronManFlags.Field.DEPLOYING));
      assertFalse(IronManFlags.is(deploying.heroFlags(), IronManFlags.Field.SUIT_WORN));
      assertEquals(HeroAction.SUIT.ordinal(), deploying.actionId());
      assertEquals(5, deploying.actionElapsed());
      assertEquals(IronManRules.SUIT_DEPLOY_TICKS, deploying.actionLength());

      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS; i++) {
         state.suit.tick();
      }

      state.energy.drain(100.0F);
      state.glide = true;
      HeroPublicSnapshot nano = IronManHero.snapshotOf(state);
      assertTrue(IronManFlags.is(nano.heroFlags(), IronManFlags.Field.SUIT_WORN));
      assertTrue(IronManFlags.is(nano.heroFlags(), IronManFlags.Field.GLIDE));
      assertFalse(IronManFlags.is(nano.heroFlags(), IronManFlags.Field.DEPLOYING));
      assertEquals(0, nano.resource());
      assertTrue(nano.resourceLocked());

      state.suit.toggle();
      assertTrue(IronManFlags.is(IronManHero.snapshotOf(state).heroFlags(), IronManFlags.Field.RETRACTING));
   }
}
