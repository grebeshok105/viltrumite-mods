package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.ironman.veronica.DropPoint;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class DropPointTest {
   @Test
   void pointWithin30OnDryGround() {
      Vec3 origin = new Vec3(100.5, 70.0, -40.5);
      RandomSource random = RandomSource.create(7L);
      for (int i = 0; i < 200; i++) {
         Vec3 point = DropPoint.pick(random, origin, (x, z) -> 64.0);
         assertTrue(Math.hypot(point.x - origin.x, point.z - origin.z) <= DropPoint.RADIUS + 1.5);
         assertEquals(64.0, point.y, 1.0E-9);
      }
   }

   @Test
   void neverWater() {
      // Water everywhere except columns with x divisible by 7.
      RandomSource random = RandomSource.create(3L);
      for (int i = 0; i < 100; i++) {
         Vec3 point = DropPoint.pick(random, Vec3.ZERO, (x, z) -> Math.floorMod(x, 7) == 0 ? 62.0 : null);
         assertTrue(Math.floorMod((int)Math.floor(point.x), 7) == 0 || point.equals(Vec3.ZERO));
      }
   }

   @Test
   void fallbackWhenNoDryGround() {
      Vec3 origin = new Vec3(10.2, 80.0, 10.7);
      Vec3 point = DropPoint.pick(RandomSource.create(1L), origin, (x, z) -> null);
      assertEquals(origin, point);
      Vec3 column = DropPoint.pick(RandomSource.create(1L), origin, (x, z) -> x == 10 && z == 10 ? 66.0 : null);
      assertEquals(new Vec3(10.5, 66.0, 10.5), column);
   }
}
