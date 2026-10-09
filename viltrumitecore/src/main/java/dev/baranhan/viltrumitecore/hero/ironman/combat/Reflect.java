package dev.baranhan.viltrumitecore.hero.ironman.combat;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/** Perfect-block projectile reflection (spec §8.5), pure. */
public final class Reflect {
   private Reflect() {
   }

   /**
    * New velocity: same speed (at least {@code minSpeed}) towards the
    * shooter, or straight back without one.
    */
   public static Vec3 velocity(Vec3 incoming, @Nullable Vec3 toShooter, double minSpeed) {
      double speed = Math.max(minSpeed, incoming.length());
      if (toShooter != null && toShooter.lengthSqr() > 1.0E-6) {
         return toShooter.normalize().scale(speed);
      }

      if (incoming.lengthSqr() < 1.0E-9) {
         return Vec3.ZERO;
      }

      return incoming.normalize().scale(-speed);
   }
}
