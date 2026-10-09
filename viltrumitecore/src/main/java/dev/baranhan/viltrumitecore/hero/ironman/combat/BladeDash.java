package dev.baranhan.viltrumitecore.hero.ironman.combat;

import net.minecraft.world.phys.Vec3;

/**
 * Blade dash path (spec §8.4), pure: from the feet towards a target, stop
 * before the first solid step or {@code stopShort} before the target.
 */
public final class BladeDash {
   /** Solid test on block coordinates (the player's feet and head cells). */
   @FunctionalInterface
   public interface Solid {
      boolean at(int x, int y, int z);
   }

   private BladeDash() {
   }

   /** Furthest free point on the straight path; steps of 0.25 blocks. */
   public static Vec3 end(Vec3 from, Vec3 to, double stopShort, Solid solid) {
      Vec3 delta = to.subtract(from);
      double length = delta.length();
      if (length < 1.0E-3) {
         return from;
      }

      Vec3 dir = delta.scale(1.0 / length);
      double max = Math.max(0.0, length - stopShort);
      Vec3 last = from;
      for (double d = 0.25; d <= max + 1.0E-6; d += 0.25) {
         Vec3 p = from.add(dir.scale(d));
         int x = (int)Math.floor(p.x);
         int y = (int)Math.floor(p.y);
         int z = (int)Math.floor(p.z);
         if (solid.at(x, y, z) || solid.at(x, y + 1, z)) {
            return last;
         }

         last = p;
      }

      return max <= 0.0 ? from : from.add(dir.scale(max));
   }
}
