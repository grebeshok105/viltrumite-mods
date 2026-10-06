package dev.baranhan.viltrumitecore.client.render.animation;

import java.util.EnumMap;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Smooth on/off blend for continuous Regulus poses (king stance, channel pull,
 * ritual hold). Same wall-clock lerp as GrabAnimationManager, one weight per
 * pose kind so Lion's Heart and the Evangelium ritual can overlap.
 */
public class RegulusAnimationManager {
   public enum Pose {
      LION,
      CHANNEL,
      RITUAL
   }

   private static final WeakHashMap<LivingEntity, EnumMap<Pose, RegulusAnimationManager.RegulusAnimState>> POSE_STATES = new WeakHashMap<>();

   public static float calculateWeight(LivingEntity entity, RegulusAnimationManager.Pose pose, boolean active) {
      EnumMap<Pose, RegulusAnimationManager.RegulusAnimState> states = POSE_STATES.computeIfAbsent(entity, k -> new EnumMap<>(Pose.class));
      RegulusAnimationManager.RegulusAnimState state = states.computeIfAbsent(pose, k -> new RegulusAnimationManager.RegulusAnimState());
      long now = System.currentTimeMillis();
      float delta = (float)(now - state.lastTime) / 1000.0F;
      if (delta > 0.0F) {
         if (delta > 0.1F) {
            delta = 0.1F;
         }

         state.lastTime = now;
         float targetWeight = active ? 1.0F : 0.0F;
         float smoothFactor = 1.0F - (float)Math.exp((double)(-13.0F * delta));
         state.weight = Mth.lerp(smoothFactor, state.weight, targetWeight);
      }

      return state.weight;
   }

   public static class RegulusAnimState {
      public float weight = 0.0F;
      public long lastTime = System.currentTimeMillis();
   }
}
