package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;
import org.junit.jupiter.api.Test;

class HulkbusterAssemblyTest {
   private static final int DROPPING = HulkbusterLayer.Phase.DROPPING.ordinal();
   private static final int ASSEMBLING = HulkbusterLayer.Phase.ASSEMBLING.ordinal();
   private static final int PARTIAL = HulkbusterLayer.Phase.PARTIAL.ordinal();

   @Test
   void groupBitsMatchTheServerLockOrder() {
      assertTrue(HulkbusterAssembly.locked(0b0001, HulkbusterAssembly.LEGS));
      assertFalse(HulkbusterAssembly.locked(0b0001, HulkbusterAssembly.TORSO));
      assertTrue(HulkbusterAssembly.locked(0b1111, HulkbusterAssembly.HELMET));
      assertFalse(HulkbusterAssembly.locked(0, HulkbusterAssembly.ARMS));
      assertEquals(HulkbusterLayer.PARTS, HulkbusterAssembly.GROUPS);
   }

   @Test
   void flightArrivesAtTheLockTickOfEachGroup() {
      assertEquals(1.0F, HulkbusterAssembly.flightProgress(HulkbusterAssembly.LEGS, HulkbusterLayer.DROP_TICKS + 15), 1.0E-6F);
      assertEquals(0.4F, HulkbusterAssembly.flightProgress(HulkbusterAssembly.TORSO, HulkbusterLayer.DROP_TICKS), 1.0E-6F);
      assertEquals(0.0F, HulkbusterAssembly.flightProgress(HulkbusterAssembly.HELMET, 0.0), 1.0E-6F);
   }

   @Test
   void dropCountsBeforeTheAssembly() {
      assertEquals(5.0, HulkbusterAssembly.dropTicks(DROPPING, 5.0), 1.0E-9);
      assertEquals(HulkbusterLayer.DROP_TICKS + 5.0, HulkbusterAssembly.dropTicks(ASSEMBLING, 5.0), 1.0E-9);
   }

   @Test
   void wrapOpensAndClosesAfterTheLock() {
      assertEquals(0.0F, HulkbusterAssembly.wrapProgress(HulkbusterAssembly.LEGS, ASSEMBLING, 15.0), 1.0E-6F);
      assertEquals(0.5F, HulkbusterAssembly.wrapProgress(HulkbusterAssembly.LEGS, ASSEMBLING, 19.0), 1.0E-6F);
      assertEquals(1.0F, HulkbusterAssembly.wrapProgress(HulkbusterAssembly.LEGS, ASSEMBLING, 23.0), 1.0E-6F);
      assertEquals(1.0F, HulkbusterAssembly.wrapProgress(HulkbusterAssembly.LEGS, PARTIAL, 0.0), 1.0E-6F);
   }

   @Test
   void groupCentresStayInsideTheBodyHeight() {
      for (int group = 0; group < HulkbusterAssembly.GROUPS; group++) {
         double centre = HulkbusterAssembly.centreAboveFeet(group);
         assertTrue(centre > 0.0 && centre < 2.0, "group " + group);
      }
   }
}
