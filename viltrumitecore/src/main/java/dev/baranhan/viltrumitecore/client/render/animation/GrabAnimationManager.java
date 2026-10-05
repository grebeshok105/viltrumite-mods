package dev.baranhan.viltrumitecore.client.render.animation;

import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class GrabAnimationManager {
   private static final WeakHashMap<LivingEntity, GrabAnimationManager.GrabAnimState> GRAB_STATES = new WeakHashMap<>();

   public static float calculateWeight(LivingEntity entity, boolean isGrabbing) {
      GrabAnimationManager.GrabAnimState state = GRAB_STATES.computeIfAbsent(entity, k -> new GrabAnimationManager.GrabAnimState());
      long now = System.currentTimeMillis();
      float delta = (float)(now - state.lastTime) / 1000.0F;
      if (delta > 0.0F) {
         if (delta > 0.1F) {
            delta = 0.1F;
         }

         state.lastTime = now;
         float targetWeight = isGrabbing ? 1.0F : 0.0F;
         float smoothFactor = 1.0F - (float)Math.exp((double)(-12.0F * delta));
         state.weight = Mth.lerp(smoothFactor, state.weight, targetWeight);
      }

      return state.weight;
   }

   public static class GrabAnimState {
      public float weight = 0.0F;
      public long lastTime = System.currentTimeMillis();
   }
}
