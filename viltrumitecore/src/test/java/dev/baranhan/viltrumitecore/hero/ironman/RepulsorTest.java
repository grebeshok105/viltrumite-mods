package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.combat.Repulsor;
import org.junit.jupiter.api.Test;

class RepulsorTest {
   private static Repulsor.Shot hold(Repulsor r, Energy e, int ticks) {
      assertTrue(r.press());
      for (int i = 0; i < ticks; i++) {
         r.tick();
      }
      return r.release(e);
   }

   private static void cool(Repulsor r) {
      for (int i = 0; i < IronManRules.REPULSOR_COOLDOWN; i++) {
         r.tick();
      }
   }

   @Test
   void tapIsShotCost2() {
      Energy e = new Energy();
      Repulsor.Shot shot = hold(new Repulsor(), e, 1);
      assertSame(Repulsor.Kind.SHOT, shot.kind());
      assertEquals(IronManRules.ENERGY_MAX - 2.0F, e.value(), 1.0E-4F);
      assertEquals(IronManRules.REPULSOR_SHOT, shot.damage());
   }

   @Test
   void chargeCost6() {
      Energy e = new Energy();
      Repulsor.Shot shot = hold(new Repulsor(), e, 10);
      assertSame(Repulsor.Kind.CHARGED, shot.kind());
      assertEquals(IronManRules.ENERGY_MAX - 6.0F, e.value(), 1.0E-4F);
      assertTrue(shot.damage() > IronManRules.REPULSOR_CHARGED_MIN && shot.damage() < IronManRules.REPULSOR_CHARGED_MAX);
   }

   @Test
   void fullChargeVolleyCost10() {
      Energy e = new Energy();
      Repulsor.Shot shot = hold(new Repulsor(), e, 40);
      assertSame(Repulsor.Kind.VOLLEY, shot.kind());
      assertEquals(IronManRules.ENERGY_MAX - 10.0F, e.value(), 1.0E-4F);
   }

   @Test
   void handsAlternate() {
      Energy e = new Energy();
      Repulsor r = new Repulsor();
      boolean first = hold(r, e, 0).rightHand();
      cool(r);
      assertNotEquals(first, hold(r, e, 0).rightHand());
   }

   @Test
   void cooldownBlocksPress() {
      Repulsor r = new Repulsor();
      hold(r, new Energy(), 0);
      assertFalse(r.press());
      cool(r);
      assertTrue(r.press());
   }

   @Test
   void cancelFiresNothing() {
      Energy e = new Energy();
      Repulsor r = new Repulsor();
      r.press();
      r.tick();
      r.cancel();
      assertSame(Repulsor.Kind.NONE, r.release(e).kind());
      assertEquals(IronManRules.ENERGY_MAX, e.value(), 1.0E-4F);
   }

   @Test
   void backShotBrakes() {
      assertTrue(Repulsor.brakes(true, -0.9, 1.0));
      assertFalse(Repulsor.brakes(true, 0.5, 1.0));
      assertFalse(Repulsor.brakes(false, -0.9, 1.0));
      assertEquals(0.5, Repulsor.brakeFactor(1.0F), 1.0E-6);
   }

   @Test
   void downShotInHoverLifts() {
      assertTrue(Repulsor.lifts(true, 75.0F));
      assertFalse(Repulsor.lifts(true, 30.0F));
      assertFalse(Repulsor.lifts(false, 80.0F));
   }

   @Test
   void groundFullChargeShockwave() {
      assertTrue(Repulsor.groundShockwave(true, Repulsor.Kind.VOLLEY, 80.0F));
      assertFalse(Repulsor.groundShockwave(true, Repulsor.Kind.CHARGED, 80.0F));
      assertFalse(Repulsor.groundShockwave(false, Repulsor.Kind.VOLLEY, 80.0F));
      assertFalse(Repulsor.groundShockwave(true, Repulsor.Kind.VOLLEY, 40.0F));
   }

   @Test
   void lockedEnergyFizzles() {
      Energy e = new Energy();
      e.drain(IronManRules.ENERGY_MAX);
      assertTrue(e.weaponsLocked());
      assertSame(Repulsor.Kind.FIZZLE, hold(new Repulsor(), e, 0).kind());
   }
}
