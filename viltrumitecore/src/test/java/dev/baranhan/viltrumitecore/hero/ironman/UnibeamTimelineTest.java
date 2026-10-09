package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.combat.UnibeamTimeline;
import java.util.Set;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class UnibeamTimelineTest {
   private static UnibeamTimeline beaming() {
      UnibeamTimeline u = new UnibeamTimeline();
      assertTrue(u.press(false));
      UnibeamTimeline.Event last = UnibeamTimeline.Event.NONE;
      for (int i = 0; i < IronManRules.UNIBEAM_CHARGE; i++) {
         last = u.tick();
      }
      assertSame(UnibeamTimeline.Event.CHARGED, last);
      u.startBeam(false);
      return u;
   }

   @Test
   void chargeTakes20() {
      UnibeamTimeline u = new UnibeamTimeline();
      u.press(false);
      for (int i = 0; i < IronManRules.UNIBEAM_CHARGE - 1; i++) {
         assertSame(UnibeamTimeline.Event.NONE, u.tick());
      }
      assertSame(UnibeamTimeline.Event.CHARGED, u.tick());
   }

   @Test
   void beamMax60() {
      UnibeamTimeline u = beaming();
      for (int i = 0; i < IronManRules.UNIBEAM_MAX - 1; i++) {
         assertSame(UnibeamTimeline.Event.NONE, u.tick());
      }
      assertSame(UnibeamTimeline.Event.BEAM_MAX, u.tick());
      assertTrue(u.overheated());
   }

   @Test
   void overdraftHasNoCap() {
      UnibeamTimeline u = new UnibeamTimeline();
      u.press(false);
      for (int i = 0; i < IronManRules.UNIBEAM_CHARGE; i++) {
         u.tick();
      }
      u.startBeam(true);
      for (int i = 0; i < 200; i++) {
         assertSame(UnibeamTimeline.Event.NONE, u.tick());
      }
      assertSame(UnibeamTimeline.Phase.BEAM, u.phase());
   }

   @Test
   void releaseOverheats40() {
      UnibeamTimeline u = beaming();
      u.tick();
      assertTrue(u.release(), "a beam that ran counts as an overheat");
      assertTrue(u.overheated());
      assertFalse(u.press(false));
      for (int i = 0; i < IronManRules.UNIBEAM_OVERHEAT_LOCK - 1; i++) {
         u.tick();
      }
      assertSame(UnibeamTimeline.Event.OVERHEAT_END, u.tick());
      assertTrue(u.press(false));
   }

   @Test
   void releaseInChargeIsFree() {
      UnibeamTimeline u = new UnibeamTimeline();
      u.press(false);
      u.tick();
      assertFalse(u.release());
      assertSame(UnibeamTimeline.Phase.IDLE, u.phase());
   }

   @Test
   void overheatAddsCounter() {
      IronManState state = new IronManState();
      state.unibeam.press(false);
      for (int i = 0; i < IronManRules.UNIBEAM_CHARGE; i++) {
         state.unibeam.tick();
      }
      state.unibeam.startBeam(false);
      if (state.unibeam.release()) {
         state.overheat.add();
      }
      assertEquals(1, state.overheat.count());
   }

   @Test
   void turnRateLimited() {
      Vec3 out = UnibeamTimeline.turn(new Vec3(1, 0, 0), new Vec3(0, 0, 1), 3.0F);
      double deg = Math.toDegrees(Math.acos(out.dot(new Vec3(1, 0, 0))));
      assertEquals(3.0, deg, 1.0E-3);
      assertEquals(new Vec3(0, 0, 1), UnibeamTimeline.turn(new Vec3(0, 0, 1), new Vec3(0, 0, 1), 3.0F));
   }

   @Test
   void damageFallsWithDistance() {
      assertEquals(IronManRules.UNIBEAM_HIT, UnibeamTimeline.damageAt(4.0, false), 1.0E-5F);
      assertEquals(IronManRules.UNIBEAM_HIT_FAR, UnibeamTimeline.damageAt(IronManRules.UNIBEAM_RANGE, false), 1.0E-5F);
      assertTrue(UnibeamTimeline.damageAt(28.0, false) < IronManRules.UNIBEAM_HIT);
      assertEquals(IronManRules.UNIBEAM_HIT * 1.5F, UnibeamTimeline.damageAt(4.0, true), 1.0E-5F);
      // Per beam at most 22.5 (60 t / 4 t = 15 hits x 1.5).
      int hits = 0;
      for (int t = 0; t < IronManRules.UNIBEAM_MAX; t++) {
         if (UnibeamTimeline.damageTick(t)) {
            hits++;
         }
      }
      assertEquals(22.5F, hits * IronManRules.UNIBEAM_HIT, 1.0E-4F);
   }

   @Test
   void onlyGlassAndLeavesBreak() {
      assertTrue(UnibeamTimeline.breaksBlock(Set.of("minecraft:leaves")));
      assertTrue(UnibeamTimeline.breaksBlock(Set.of("forge:glass")));
      assertTrue(UnibeamTimeline.breaksBlock(Set.of("forge:glass_panes")));
      assertFalse(UnibeamTimeline.breaksBlock(Set.of("minecraft:mineable/pickaxe", "minecraft:base_stone_overworld")));
      assertFalse(UnibeamTimeline.breaksBlock(Set.of()));
   }

   @Test
   void controlEndsChannel() {
      UnibeamTimeline u = beaming();
      assertTrue(u.interrupt());
      assertTrue(u.overheated());
   }

   @Test
   void blindingRules() {
      assertTrue(UnibeamTimeline.looksInto(new Vec3(1, 0, 0), new Vec3(5, 0.5, 0), 5.0));
      assertFalse(UnibeamTimeline.looksInto(new Vec3(-1, 0, 0), new Vec3(5, 0, 0), 5.0));
      assertFalse(UnibeamTimeline.looksInto(new Vec3(1, 0, 0), new Vec3(20, 0, 0), 20.0));
      assertEquals(IronManRules.FLASH_PVP_MAX_TICKS, UnibeamTimeline.flashTicks(100, true));
      assertEquals(100, UnibeamTimeline.flashTicks(100, false));
   }
}
