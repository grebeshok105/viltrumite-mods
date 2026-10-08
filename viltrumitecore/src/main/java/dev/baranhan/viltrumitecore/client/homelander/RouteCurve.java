package dev.baranhan.viltrumitecore.client.homelander;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Pure curve helpers for the focus stream: Chaikin smoothing and even resampling. */
public final class RouteCurve {
   private RouteCurve() {
   }

   /** Chaikin corner cutting, end points kept. */
   public static List<Vec3> smooth(List<Vec3> points, int iterations) {
      List<Vec3> current = points;
      for (int it = 0; it < iterations && current.size() > 2; it++) {
         List<Vec3> next = new ArrayList<>(current.size() * 2);
         next.add(current.get(0));
         for (int i = 0; i < current.size() - 1; i++) {
            Vec3 a = current.get(i);
            Vec3 b = current.get(i + 1);
            next.add(a.lerp(b, 0.25));
            next.add(a.lerp(b, 0.75));
         }

         next.add(current.get(current.size() - 1));
         current = next;
      }

      return current;
   }

   /** Points every {@code step} blocks along the polyline, end point included. */
   public static List<Vec3> resample(List<Vec3> points, double step) {
      List<Vec3> out = new ArrayList<>();
      if (points.isEmpty()) {
         return out;
      }

      out.add(points.get(0));
      double carry = 0.0;
      for (int i = 0; i < points.size() - 1; i++) {
         Vec3 a = points.get(i);
         Vec3 b = points.get(i + 1);
         double len = a.distanceTo(b);
         double d = step - carry;
         while (d <= len) {
            out.add(a.lerp(b, d / len));
            d += step;
         }

         carry = len - (d - step);
      }

      Vec3 last = points.get(points.size() - 1);
      if (out.get(out.size() - 1).distanceToSqr(last) > 1.0E-6) {
         out.add(last);
      }

      return out;
   }
}
