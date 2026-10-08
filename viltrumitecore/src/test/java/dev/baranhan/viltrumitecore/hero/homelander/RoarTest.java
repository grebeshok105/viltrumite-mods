package dev.baranhan.viltrumitecore.hero.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RoarTest {
   private static final Vec3 LOOK = new Vec3(0.0, 0.0, 1.0);

   private static Vec3 at(double degrees, double dist) {
      double r = Math.toRadians(degrees);
      return new Vec3(Math.sin(r) * dist, 0.0, Math.cos(r) * dist);
   }

   @Test
   void insideCone() {
      assertTrue(Roar.inCone(LOOK, at(0.0, 5.0)));
      assertTrue(Roar.inCone(LOOK, at(29.0, 11.9)));
   }

   @Test
   void outside30Degrees() {
      assertFalse(Roar.inCone(LOOK, at(31.0, 5.0)));
      assertFalse(Roar.inCone(LOOK, at(180.0, 3.0)));
   }

   @Test
   void beyond12Blocks() {
      assertFalse(Roar.inCone(LOOK, at(0.0, 12.1)));
   }

   @Test
   void snapshotTimeline() {
      assertEquals(-1, Roar.animElapsed(0));
      assertEquals(0, Roar.animElapsed(Roar.ANIM_TICKS));
      assertEquals(Roar.ANIM_TICKS - 1, Roar.animElapsed(1));
   }
}
