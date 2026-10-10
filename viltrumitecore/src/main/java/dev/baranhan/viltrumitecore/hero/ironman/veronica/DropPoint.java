package dev.baranhan.viltrumitecore.hero.ironman.veronica;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Veronica capsule lands (spec §12.1): a random point within
 * {@link #RADIUS} blocks on solid dry ground, never in water or lava. After
 * {@link #TRIES} failed tries the point falls back to the player's column.
 */
public final class DropPoint {
   public static final double RADIUS = 30.0;
   public static final int TRIES = 16;

   /** World access: y of a solid, dry top surface at a column, or null (water, lava, unloaded, no ground). */
   public interface Surface {
      @Nullable
      Double dryTop(int x, int z);
   }

   private DropPoint() {
   }

   public static Vec3 pick(RandomSource random, Vec3 origin, Surface surface) {
      for (int i = 0; i < TRIES; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double radius = Math.sqrt(random.nextDouble()) * RADIUS;
         int x = (int)Math.floor(origin.x + Math.cos(angle) * radius);
         int z = (int)Math.floor(origin.z + Math.sin(angle) * radius);
         Double y = surface.dryTop(x, z);
         if (y != null) {
            return new Vec3(x + 0.5, y, z + 0.5);
         }
      }

      int x = (int)Math.floor(origin.x);
      int z = (int)Math.floor(origin.z);
      Double y = surface.dryTop(x, z);
      return y != null ? new Vec3(x + 0.5, y, z + 0.5) : origin;
   }
}
