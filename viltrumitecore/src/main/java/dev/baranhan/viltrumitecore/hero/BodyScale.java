package dev.baranhan.viltrumitecore.hero;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Pure space checks for a scaled hero body (Iron Man Hulkbuster, plan stage 5
 * Task 3). The real final box is the pose dimensions × scale at the feet
 * position; {@code free} answers whether a box has no colliding blocks.
 */
public final class BodyScale {
   public static final int SEARCH_RADIUS = 2;

   private BodyScale() {
   }

   public static AABB box(Vec3 feet, float width, float height) {
      double half = width / 2.0;
      return new AABB(feet.x - half, feet.y, feet.z - half, feet.x + half, feet.y + height, feet.z + half);
   }

   /** Feet position with room for the box: the spot itself or the nearest within {@link #SEARCH_RADIUS} blocks; null = no room. */
   @Nullable
   public static Vec3 findFree(Vec3 feet, float width, float height, Predicate<AABB> free) {
      if (free.test(box(feet, width, height))) {
         return feet;
      }

      Vec3 best = null;
      double bestDistance = Double.MAX_VALUE;
      for (int dy = 0; dy <= SEARCH_RADIUS; dy++) {
         for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
               Vec3 candidate = feet.add(dx, dy, dz);
               double distance = dx * dx + dy * dy + dz * dz;
               if (distance > 0 && distance < bestDistance && free.test(box(candidate, width, height))) {
                  best = candidate;
                  bestDistance = distance;
               }
            }
         }
      }

      return best;
   }
}
