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

   /** Douglas-Peucker: drop points within {@code epsilon} of the chord, so block steps become straight runs. */
   public static List<Vec3> simplify(List<Vec3> points, double epsilon) {
      if (points.size() < 3) {
         return new ArrayList<>(points);
      }

      boolean[] keep = new boolean[points.size()];
      keep[0] = true;
      keep[points.size() - 1] = true;
      mark(points, 0, points.size() - 1, epsilon, keep);
      List<Vec3> out = new ArrayList<>();
      for (int i = 0; i < points.size(); i++) {
         if (keep[i]) {
            out.add(points.get(i));
         }
      }

      return out;
   }

   private static void mark(List<Vec3> points, int from, int to, double epsilon, boolean[] keep) {
      if (to - from < 2) {
         return;
      }

      Vec3 a = points.get(from);
      Vec3 ab = points.get(to).subtract(a);
      double len2 = ab.lengthSqr();
      int best = -1;
      double bestDist = epsilon;
      for (int i = from + 1; i < to; i++) {
         Vec3 ap = points.get(i).subtract(a);
         double t = len2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ap.dot(ab) / len2));
         double d = ap.subtract(ab.scale(t)).length();
         if (d > bestDist) {
            bestDist = d;
            best = i;
         }
      }

      if (best >= 0) {
         keep[best] = true;
         mark(points, from, best, epsilon, keep);
         mark(points, best, to, epsilon, keep);
      }
   }
}
