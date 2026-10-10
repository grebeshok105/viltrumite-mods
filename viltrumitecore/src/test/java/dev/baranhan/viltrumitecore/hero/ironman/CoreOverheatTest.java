package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.ironman.combat.CoreOverheat;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class CoreOverheatTest {
   @Test
   void thirdBeamIsOverdraft() {
      CoreOverheat c = new CoreOverheat();
      assertFalse(c.nextIsOverdraft());
      c.add();
      assertFalse(c.nextIsOverdraft());
      c.add();
      assertTrue(c.nextIsOverdraft(), "the beam after two overheats is the third: overdraft on this beam");
   }

   @Test
   void secondWarns() {
      CoreOverheat c = new CoreOverheat();
      c.add();
      assertFalse(c.warn());
      c.add();
      assertTrue(c.warn());
   }

   @Test
   void suitChangeKeepsCount() {
      IronManState state = new IronManState();
      state.overheat.add();
      state.overheat.add();
      state.suit.toggle();
      for (int i = 0; i < 40; i++) {
         state.suit.tick();
      }
      state.suit.toggle();
      for (int i = 0; i < 40; i++) {
         state.suit.tick();
      }
      state.onCleanup(CleanupReason.DISCONNECT);
      CompoundTag tag = new CompoundTag();
      state.save(tag);
      IronManState loaded = new IronManState();
      loaded.load(tag);
      assertEquals(2, loaded.overheat.count());
   }

   @Test
   void deathResets() {
      IronManState state = new IronManState();
      state.overheat.add();
      state.onCleanup(CleanupReason.DEATH);
      assertEquals(0, state.overheat.count());
      state.overheat.add();
      assertEquals(0, IronManState.cloneForRespawn(state).overheat.count());
   }

   @Test
   void explosionResets() {
      CoreOverheat c = new CoreOverheat();
      c.add();
      c.add();
      c.resetByExplosion();
      assertEquals(0, c.count());
   }

   @Test
   void neverAbove3() {
      CoreOverheat c = new CoreOverheat();
      for (int i = 0; i < 10; i++) {
         c.add();
      }
      assertEquals(3, c.count());
      CompoundTag tag = new CompoundTag();
      tag.putInt(CoreOverheat.KEY, 99);
      c.load(tag);
      assertEquals(3, c.count());
   }
}
