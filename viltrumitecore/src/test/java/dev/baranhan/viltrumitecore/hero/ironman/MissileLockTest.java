package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.combat.MissileLock;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MissileLockTest {
   private static MissileLock open() {
      MissileLock m = new MissileLock();
      assertTrue(m.press(false));
      for (int i = 0; i < IronManRules.MISSILE_FLAPS; i++) {
         m.tick();
      }
      return m;
   }

   @Test
   void flapsTake10() {
      MissileLock m = new MissileLock();
      m.press(false);
      for (int i = 0; i < IronManRules.MISSILE_FLAPS - 1; i++) {
         m.tick();
      }
      assertFalse(m.flapsOpen());
      assertFalse(m.offer(1, true), "no marks before the flaps are open");
      m.tick();
      assertTrue(m.flapsOpen());
   }

   @Test
   void marksUpToFour() {
      MissileLock m = open();
      for (int id = 1; id <= 6; id++) {
         m.offer(id, true);
      }
      assertEquals(List.of(1, 2, 3, 4), m.marks());
   }

   @Test
   void noDuplicateMarks() {
      MissileLock m = open();
      assertTrue(m.offer(7, true));
      assertFalse(m.offer(7, true));
      assertEquals(1, m.marks().size());
   }

   @Test
   void needsLineOfSight() {
      MissileLock m = open();
      assertFalse(m.offer(3, false));
      assertTrue(m.marks().isEmpty());
   }

   @Test
   void noMarksFiresStraight() {
      MissileLock m = open();
      MissileLock.Volley v = m.release();
      assertTrue(v.fire());
      assertTrue(v.targets().isEmpty());
      assertEquals(4, MissileLock.missileCount(0));
      assertEquals(2, MissileLock.missileCount(2));
   }

   @Test
   void releaseBeforeFlapsFiresNothing() {
      MissileLock m = new MissileLock();
      m.press(false);
      m.tick();
      assertFalse(m.release().fire());
   }

   @Test
   void cost15() {
      Energy e = new Energy();
      assertTrue(e.spend(IronManRules.COST_MISSILES));
      assertEquals(IronManRules.ENERGY_MAX - 15.0F, e.value(), 1.0E-4F);
   }

   @Test
   void lockedRefusesPress() {
      assertFalse(new MissileLock().press(true));
   }

   @Test
   void coneAndFan() {
      Vec3 eye = Vec3.ZERO;
      Vec3 look = new Vec3(0, 0, 1);
      assertTrue(MissileLock.inCone(eye, look, new Vec3(0, 0, 30)));
      assertFalse(MissileLock.inCone(eye, look, new Vec3(10, 0, 30)));
      assertFalse(MissileLock.inCone(eye, look, new Vec3(0, 0, 80)), "beyond 64 blocks");
      assertEquals(0.0F, MissileLock.fanOffset(0, 1));
      assertEquals(-MissileLock.fanOffset(0, 4), MissileLock.fanOffset(3, 4), 1.0E-5F);
   }
}
