package dev.baranhan.viltrumitecore.client.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class GroundRouteTest {
   /** Flat floor at y=0 inside |x|,|z| <= 20. */
   private static boolean floor(int x, int y, int z) {
      return y == 0 && Math.abs(x) <= 20 && Math.abs(z) <= 20;
   }

   @Test
   void straightCorridor() {
      List<BlockPos> path = GroundRoute.find(new BlockPos(0, 0, 0), new BlockPos(6, 0, 0), (x, y, z) -> y == 0 && z == 0 && x >= 0 && x <= 6, 4000);
      assertEquals(7, path.size());
      assertEquals(new BlockPos(0, 0, 0), path.get(0));
      assertEquals(new BlockPos(6, 0, 0), path.get(6));
   }

   @Test
   void goesAroundWall() {
      // Wall at x=3 for z in -5..5.
      GroundRoute.Walkable walkable = (x, y, z) -> floor(x, y, z) && !(x == 3 && Math.abs(z) <= 5);
      List<BlockPos> path = GroundRoute.find(new BlockPos(0, 0, 0), new BlockPos(6, 0, 0), walkable, 4000);
      assertFalse(path.isEmpty());
      assertTrue(path.stream().noneMatch(p -> p.getX() == 3 && Math.abs(p.getZ()) <= 5));
      assertTrue(path.stream().anyMatch(p -> Math.abs(p.getZ()) >= 6));
      assertEquals(new BlockPos(6, 0, 0), path.get(path.size() - 1));
   }

   @Test
   void climbsStep() {
      // Ground y=0 for x<3, y=1 for x>=3 (a one-block step).
      GroundRoute.Walkable walkable = (x, y, z) -> z == 0 && x >= 0 && x <= 6 && y == (x < 3 ? 0 : 1);
      List<BlockPos> path = GroundRoute.find(new BlockPos(0, 0, 0), new BlockPos(6, 1, 0), walkable, 4000);
      assertEquals(7, path.size());
      assertEquals(new BlockPos(3, 1, 0), path.get(3));
   }

   @Test
   void noRouteInSealedRoom() {
      // Room |x|,|z| <= 2 with the target outside.
      GroundRoute.Walkable walkable = (x, y, z) -> y == 0 && Math.abs(x) <= 2 && Math.abs(z) <= 2 || y == 0 && x == 10 && z == 0;
      assertTrue(GroundRoute.find(new BlockPos(0, 0, 0), new BlockPos(10, 0, 0), walkable, 4000).isEmpty());
   }

   @Test
   void nodeLimitReturnsEmpty() {
      assertTrue(GroundRoute.find(new BlockPos(-20, 0, -20), new BlockPos(20, 0, 20), GroundRouteTest::floor, 10).isEmpty());
      assertFalse(GroundRoute.find(new BlockPos(-20, 0, -20), new BlockPos(20, 0, 20), GroundRouteTest::floor, 4000).isEmpty());
   }
}
