package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import org.junit.jupiter.api.Test;

class HulkbusterPosesTest {
   private static final int ACTIVE = HulkbusterLayer.Phase.ACTIVE.ordinal();
   private static final int EXITING = HulkbusterLayer.Phase.EXITING.ordinal();

   private static HulkbusterPoses.Input input(int phase, HeroAction action, int elapsed, int length, RightTool tool, boolean shotRight, double slamGround) {
      return new HulkbusterPoses.Input(phase, action.ordinal(), elapsed, length, 0.0F, tool, shotRight, slamGround, 0.0F, 0.0F, 0.0);
   }

   @Test
   void punchPlaysTheShotHandOnTheStrikeClock() {
      HulkbusterPoses.Pose pose = HulkbusterPoses.select(input(ACTIVE, HeroAction.PRIMARY_ATTACK, 6, 12, RightTool.REPULSOR, true, -1.0));
      assertEquals(HulkbusterPoses.Clip.PUNCH_RIGHT, pose.overlay());
      assertEquals(0.3, pose.overlaySeconds(), 1.0E-9);
      pose = HulkbusterPoses.select(input(ACTIVE, HeroAction.PRIMARY_ATTACK, 6, 12, RightTool.REPULSOR, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.PUNCH_LEFT, pose.overlay());
   }

   @Test
   void jackhammerLoopsOnTheSyncedClockAndChargeClamps() {
      HulkbusterPoses.Pose hammer = HulkbusterPoses.select(input(ACTIVE, HeroAction.SECONDARY_USE, 5, 60, RightTool.JACKHAMMER, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.JACKHAMMER, hammer.overlay());
      assertEquals(0.05, hammer.overlaySeconds(), 1.0E-9);

      HulkbusterPoses.Pose charge = HulkbusterPoses.select(input(ACTIVE, HeroAction.SECONDARY_USE, 40, 30, RightTool.HULK_REPULSOR, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.CHARGE, charge.overlay());
      assertEquals(HulkbusterPoses.Clip.CHARGE.length(), charge.overlaySeconds(), 1.0E-9);
      assertEquals(1.0F, charge.glow(), 1.0E-6F);
   }

   @Test
   void grabHoldUsesWallClockAndSlamHasThreeStages() {
      assertEquals(HulkbusterPoses.Clip.GRAB_HOLD, HulkbusterPoses.select(input(ACTIVE, HeroAction.UNIBEAM, 0, 1, RightTool.REPULSOR, false, -1.0)).overlay());

      HulkbusterPoses.Pose launch = HulkbusterPoses.select(input(ACTIVE, HeroAction.MISSILES, 2, 60, RightTool.REPULSOR, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.SLAM_LAUNCH, launch.overlay());
      assertTrue(launch.flames() > 0.0F);

      HulkbusterPoses.Pose air = HulkbusterPoses.select(input(ACTIVE, HeroAction.MISSILES, 10, 60, RightTool.REPULSOR, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.SLAM_AIR, air.overlay());
      assertEquals(1.0F, air.flames(), 1.0E-6F);

      HulkbusterPoses.Pose smash = HulkbusterPoses.select(input(ACTIVE, HeroAction.MISSILES, 12, 60, RightTool.REPULSOR, false, 0.1));
      assertEquals(HulkbusterPoses.Clip.SLAM_SMASH, smash.overlay());
      assertEquals(0.1, smash.overlaySeconds(), 1.0E-9);
   }

   @Test
   void hopFlamesRiseAndFall() {
      HulkbusterPoses.Pose start = HulkbusterPoses.select(input(ACTIVE, HeroAction.NANO_ARSENAL, 0, 10, RightTool.REPULSOR, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.HOP, start.overlay());
      assertEquals(0.0F, start.flames(), 1.0E-6F);
      HulkbusterPoses.Pose middle = HulkbusterPoses.select(input(ACTIVE, HeroAction.NANO_ARSENAL, 5, 10, RightTool.REPULSOR, false, -1.0));
      assertEquals(1.0F, middle.flames(), 1.0E-6F);
   }

   @Test
   void exitRunsItsClipAndNoBodyOutsideTheLayer() {
      HulkbusterPoses.Pose exit = HulkbusterPoses.select(input(EXITING, HeroAction.SUIT, 45, 30, RightTool.REPULSOR, false, -1.0));
      assertEquals(HulkbusterPoses.Clip.EXIT, exit.overlay());
      assertEquals(HulkbusterPoses.Clip.EXIT.length(), exit.overlaySeconds(), 1.0E-9);
      assertNull(HulkbusterPoses.select(input(HulkbusterLayer.Phase.DROPPING.ordinal(), HeroAction.SUIT, 3, 20, RightTool.REPULSOR, false, -1.0)));
      assertNull(HulkbusterPoses.select(input(HulkbusterLayer.Phase.PARTIAL.ordinal(), HeroAction.SUIT, 0, 0, RightTool.REPULSOR, false, -1.0)));
   }

   @Test
   void walkBlendsBySpeedAndCyclesWithTheLimbSwing() {
      HulkbusterPoses.Input still = new HulkbusterPoses.Input(ACTIVE, -1, 0, 0, 0.0F, RightTool.REPULSOR, false, -1.0, 0.0F, 0.0F, 0.0);
      assertEquals(0.0F, HulkbusterPoses.select(still).walk(), 1.0E-6F);
      HulkbusterPoses.Input running = new HulkbusterPoses.Input(ACTIVE, -1, 0, 0, 0.0F, RightTool.REPULSOR, false, -1.0, 0.8F, 0.0F, 0.0);
      assertEquals(1.0F, HulkbusterPoses.select(running).walk(), 1.0E-6F);

      double cycle = 2.0 * Math.PI / 0.6662;
      assertEquals(0.0, HulkbusterPoses.walkSeconds((float)cycle), 1.0E-4);
      assertEquals(0.5, HulkbusterPoses.walkSeconds((float)(cycle / 2.0)), 1.0E-4);
   }
}
