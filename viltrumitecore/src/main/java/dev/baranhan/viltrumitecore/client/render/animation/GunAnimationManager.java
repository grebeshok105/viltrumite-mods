package dev.baranhan.viltrumitecore.client.render.animation;

import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class GunAnimationManager {
   private static final WeakHashMap<LivingEntity, GunAnimationManager.GunAnimState> GUN_STATES = new WeakHashMap<>();

   public static float calculateWeight(LivingEntity entity, boolean isActive) {
      GunAnimationManager.GunAnimState state = GUN_STATES.computeIfAbsent(entity, k -> new GunAnimationManager.GunAnimState());
      long now = System.currentTimeMillis();
      float delta = (float)(now - state.lastTime) / 1000.0F;
      if (delta > 0.0F) {
         if (delta > 0.1F) {
            delta = 0.1F;
         }

         state.lastTime = now;
         float targetWeight = isActive ? 1.0F : 0.0F;
         float smoothFactor = 1.0F - (float)Math.exp((double)(-25.0F * delta));
         state.weight = Mth.lerp(smoothFactor, state.weight, targetWeight);
      }

      return state.weight;
   }

   public static class GunAnimState {
      public float weight = 0.0F;
      public long lastTime = System.currentTimeMillis();
   }
}
