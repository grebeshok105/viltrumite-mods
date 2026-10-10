package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NanoDamageVisualsTest {
   private static int holes(int mask, int level) {
      int n = 0;
      for (int y = 0; y < 64; y++) {
         for (int x = 0; x < 64; x++) {
            if (NanoDamageVisuals.hole(mask, level, x, y)) {
               n++;
            }
         }
      }
      return n;
   }

   @Test
   void holesOnlyInsideTheZone() {
      assertTrue(holes(4, 0) > 0);
      // Chest zone: nothing on the legs (y >= 48 below the jacket rows).
      for (int x = 0; x < 16; x++) {
         assertFalse(NanoDamageVisuals.hole(4, 0, x, 60));
      }
      assertEquals(0, holes(0, 0));
   }

   @Test
   void repairShrinksHoles() {
      int last = Integer.MAX_VALUE;
      for (int level = 0; level < NanoDamageVisuals.LEVELS; level++) {
         int n = holes(7, level);
         assertTrue(n < last, "level " + level);
         last = n;
      }
   }

   @Test
   void levelFollowsRepairTicks() {
      assertEquals(0, NanoDamageVisuals.level(0));
      assertEquals(NanoDamageVisuals.LEVELS - 1, NanoDamageVisuals.level(IronManRules.NANO_REPAIR_TICKS * 2L));
   }

   @Test
   void weaponGrowsFromWristAndShrinks() {
      assertEquals(0.05F, IronManCombatParts.weaponScale(true, false, 0.0F), 1.0E-6F);
      assertEquals(1.0F, IronManCombatParts.weaponScale(true, false, 1.0F), 1.0E-6F);
      assertEquals(1.0F, IronManCombatParts.weaponScale(false, false, 0.3F), 1.0E-6F);
      assertEquals(0.25F, IronManCombatParts.weaponScale(false, true, 0.75F), 1.0E-6F);
   }

   @Test
   void crosshairPerTool() {
      Set<String> names = new HashSet<>();
      for (RightTool tool : RightTool.values()) {
         names.add(IronManCrosshair.textureName(tool));
      }
      assertEquals(RightTool.values().length, names.size(), "one crosshair per tool");
      assertEquals("repulsor", IronManCrosshair.textureName(RightTool.REPULSOR));
   }
}
