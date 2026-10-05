package dev.baranhan.viltrumitecore.client.render.animation;

import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class FirstPersonBlockAnimationManager {
   private static final WeakHashMap<LivingEntity, FirstPersonBlockAnimationManager.BlockAnimState> BLOCK_STATES = new WeakHashMap<>();

   public static float calculateWeight(LivingEntity entity, boolean isBlocking) {
      FirstPersonBlockAnimationManager.BlockAnimState state = BLOCK_STATES.computeIfAbsent(entity, k -> new FirstPersonBlockAnimationManager.BlockAnimState());
      long now = System.currentTimeMillis();
      float delta = (float)(now - state.lastTime) / 1000.0F;
      if (delta > 0.0F) {
         if (delta > 0.1F) {
            delta = 0.1F;
         }

         state.lastTime = now;
         float targetWeight = isBlocking ? 1.0F : 0.0F;
         float smoothFactor = 1.0F - (float)Math.exp((double)(-12.0F * delta));
         state.weight = Mth.lerp(smoothFactor, state.weight, targetWeight);
      }

      return state.weight;
   }

   public static class BlockAnimState {
      public float weight = 0.0F;
      public long lastTime = System.currentTimeMillis();
   }
}
