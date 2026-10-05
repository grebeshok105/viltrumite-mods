package dev.baranhan.viltrumitecore.client.render.animation;

import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SilhouetteManager {
   private static final WeakHashMap<LivingEntity, SilhouetteManager.State> STATES = new WeakHashMap<>();

   public static SilhouetteManager.State getState(LivingEntity entity, boolean shouldDraw) {
      SilhouetteManager.State state = STATES.computeIfAbsent(entity, k -> new SilhouetteManager.State());
      long now = System.currentTimeMillis();
      float delta = (float)(now - state.lastTime) / 1000.0F;
      if (delta > 0.0F) {
         if (delta > 0.1F) {
            delta = 0.1F;
         }

         state.lastTime = now;
         float targetAlpha = shouldDraw ? 1.0F : 0.0F;
         state.alphaWeight = Mth.lerp(1.0F - (float)Math.exp((double)(-10.0F * delta)), state.alphaWeight, targetAlpha);
         double dx = entity.getX() - entity.xo;
         double dy = entity.getY() - entity.yo;
         double dz = entity.getZ() - entity.zo;
         double speed3D = Math.sqrt(dx * dx + dy * dy + dz * dz);
         Vec3 targetOffset = Vec3.ZERO;
         if (shouldDraw && speed3D > 0.05) {
            double targetSpread = Math.min(0.5, speed3D * 0.4);
            Vec3 moveDir = new Vec3(dx, dy, dz).normalize();
            targetOffset = moveDir.scale(targetSpread);
         }

         double lerpSpeed = 1.0 - Math.exp(-22.0 * (double)delta);
         state.currentTrailOffset = state.currentTrailOffset.lerp(targetOffset, lerpSpeed);
      }

      return state;
   }

   public static class State {
      public float alphaWeight = 0.0F;
      public long lastTime = System.currentTimeMillis();
      public Vec3 currentTrailOffset = Vec3.ZERO;
   }
}
