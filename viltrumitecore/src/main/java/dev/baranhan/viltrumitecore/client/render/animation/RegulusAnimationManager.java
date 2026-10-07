package dev.baranhan.viltrumitecore.client.render.animation;

import dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
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
   private static final WeakHashMap<LivingEntity, EnumMap<Pose, RegulusAnimationManager.RegulusAnimState>> FIRST_PERSON_STATES = new WeakHashMap<>();
   private static final WeakHashMap<LivingEntity, EnumMap<Pose, RegulusAnimationManager.RegulusAnimState>> FIRST_PERSON_OFFHAND_STATES = new WeakHashMap<>();
   private static final WeakHashMap<LivingEntity, ActionClock> ACTION_CLOCKS = new WeakHashMap<>();

   public static float calculateWeight(LivingEntity entity, RegulusAnimationManager.Pose pose, boolean active) {
      return calculateWeight(entity, pose, active, POSE_STATES);
   }

   public static float calculateFirstPersonWeight(LivingEntity entity, Pose pose, boolean active) {
      return calculateFirstPersonWeight(entity, pose, active, true);
   }

   public static float calculateFirstPersonWeight(LivingEntity entity, Pose pose, boolean active, boolean mainHand) {
      return calculateWeight(entity, pose, active, mainHand ? FIRST_PERSON_STATES : FIRST_PERSON_OFFHAND_STATES);
   }

   public static void reset(LivingEntity entity) {
      POSE_STATES.remove(entity);
      FIRST_PERSON_STATES.remove(entity);
      FIRST_PERSON_OFFHAND_STATES.remove(entity);
      ACTION_CLOCKS.remove(entity);
   }

   public static float actionTime(LivingEntity entity, HeroPublicSnapshot snapshot, float partialTick) {
      ActionClock clock = ACTION_CLOCKS.computeIfAbsent(entity, k -> new ActionClock());
      long now = entity.level().getGameTime();
      if (clock.actionId != snapshot.actionId() || clock.elapsed != snapshot.actionElapsed()) {
         clock.actionId = snapshot.actionId();
         clock.elapsed = snapshot.actionElapsed();
         clock.updatedAt = now;
      }
      HeroAction action = HeroAction.byId(snapshot.actionId());
      return action == null ? 0.0F : RegulusPoseTiming.actionElapsed(clock.elapsed, now - clock.updatedAt, partialTick,
         RegulusPoseTiming.timing(action).length());
   }

   private static float calculateWeight(LivingEntity entity, Pose pose, boolean active,
      WeakHashMap<LivingEntity, EnumMap<Pose, RegulusAnimState>> perView) {
      EnumMap<Pose, RegulusAnimationManager.RegulusAnimState> states = perView.computeIfAbsent(entity, k -> new EnumMap<>(Pose.class));
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

   private static class ActionClock {
      int actionId = -1;
      int elapsed = -1;
      long updatedAt;
   }
}
