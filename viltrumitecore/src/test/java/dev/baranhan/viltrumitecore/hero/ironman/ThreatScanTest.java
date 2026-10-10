package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.jarvis.ThreatScan;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ThreatScanTest {
   @Test
   void mobTargetingIsThreat() {
      assertTrue(ThreatScan.mobThreat(true, false));
   }

   @Test
   void drawingBowIsThreat() {
      assertTrue(ThreatScan.drawing(true, true, true));
      assertTrue(ThreatScan.mobThreat(false, ThreatScan.drawing(true, true, true)));
      assertFalse(ThreatScan.drawing(true, true, false), "drawing at someone else");
      assertFalse(ThreatScan.drawing(false, true, true), "not a ranged mob");
   }

   @Test
   void idleMobIsNot() {
      assertFalse(ThreatScan.mobThreat(false, false));
   }

   @Test
   void armedPlayerLookingAtUs() {
      assertTrue(ThreatScan.playerThreat(0.99, true, true, false));
      assertTrue(ThreatScan.playerThreat(0.99, true, false, true));
      assertFalse(ThreatScan.playerThreat(0.5, true, true, false), "looking away");
      assertFalse(ThreatScan.playerThreat(0.99, false, true, false), "behind a wall");
      assertFalse(ThreatScan.playerThreat(0.99, true, false, false), "empty hands");
   }

   @Test
   void projectilePathNearPlayer() {
      Vec3 player = new Vec3(0, 1, 0);
      assertTrue(ThreatScan.projectilePassesNear(new Vec3(10, 1, 0), new Vec3(-1, 0, 0), player, 32, 2));
      assertTrue(ThreatScan.projectilePassesNear(new Vec3(10, 2.5, 0), new Vec3(-1, 0, 0), player, 32, 2), "passes 1.5 above");
      assertFalse(ThreatScan.projectilePassesNear(new Vec3(10, 5, 0), new Vec3(-1, 0, 0), player, 32, 2), "passes 4 above");
      assertFalse(ThreatScan.projectilePassesNear(new Vec3(10, 1, 0), new Vec3(1, 0, 0), player, 32, 2), "flying away");
      assertFalse(ThreatScan.projectilePassesNear(new Vec3(40, 1, 0), new Vec3(-1, 0, 0), player, 32, 2), "out of range");
      assertFalse(ThreatScan.projectilePassesNear(new Vec3(5, 1, 0), Vec3.ZERO, player, 32, 2), "lying still");
   }
}
