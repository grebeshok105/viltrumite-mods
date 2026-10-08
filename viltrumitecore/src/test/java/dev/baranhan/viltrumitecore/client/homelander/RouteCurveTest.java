package dev.baranhan.viltrumitecore.client.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RouteCurveTest {
   @Test
   void smoothKeepsEnds() {
      List<Vec3> path = List.of(new Vec3(0, 0, 0), new Vec3(4, 0, 0), new Vec3(4, 0, 4));
      List<Vec3> out = RouteCurve.smooth(path, 3);
      assertEquals(path.get(0), out.get(0));
      assertEquals(path.get(2), out.get(out.size() - 1));
      assertTrue(out.size() > path.size());
   }

   @Test
   void resampleEvenSteps() {
      List<Vec3> out = RouteCurve.resample(List.of(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(2, 0, 0)), 0.5);
      assertEquals(5, out.size());
      assertEquals(1.5, out.get(3).x, 1.0E-9);
      assertEquals(2.0, out.get(4).x, 1.0E-9);
   }

   @Test
   void simplifyFlattensStairSteps() {
      List<Vec3> stairs = List.of(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1), new Vec3(2, 0, 1), new Vec3(2, 0, 2), new Vec3(3, 0, 2), new Vec3(3, 0, 3));
      List<Vec3> out = RouteCurve.simplify(stairs, 1.2);
      assertEquals(2, out.size());
   }
}
