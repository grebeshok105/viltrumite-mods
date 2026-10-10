package dev.baranhan.viltrumitecore.client.ironman.veronica;

import net.minecraft.world.phys.Vec3;

/**
 * Path of a flying suit part (spec §12.4 step 2). The part eases from its
 * launch point to the live anchor on the moving player with an arc, and
 * tumbles until it reaches the body orientation. Pure math.
 */
public final class PartFlight {
   /** Arc height in blocks at the middle of the flight. */
   public static final double ARC = 1.2;

   private PartFlight() {
   }

   public static Vec3 position(Vec3 launch, Vec3 anchor, float progress) {
      float p = clamp(progress);
      float e = ease(p);
      return launch.lerp(anchor, e).add(0.0, ARC * Math.sin(Math.PI * p), 0.0);
   }

   /** 1 while the part still tumbles, 0 once it is aligned with the body. */
   public static float tumble(float progress) {
      return 1.0F - ease(clamp(progress));
   }

   static float ease(float p) {
      return p * p * (3.0F - 2.0F * p);
   }

   private static float clamp(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }
}
