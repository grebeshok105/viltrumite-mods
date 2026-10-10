package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;

/**
 * First-person shield guard (animation-system §5, weight like the block pose):
 * the left arm turns so the forearm lies across the lower view and the plate
 * on its outer side faces forward. Target solved against the vanilla
 * first-person arm transform: plate centre at (-0.42, -0.30, -0.85) in view space.
 */
public final class IronManFirstPerson {
   private static final Quaternionf GUARD = new Quaternionf(0.31364F, 0.74966F, -0.32919F, -0.48092F);
   private static final float GUARD_X = -1.3666F;
   private static final float GUARD_Y = 0.3784F;
   private static final float GUARD_Z = -1.1241F;
   /** Blend speed (1/s): about 0.12 s to raise. */
   private static final float RATE = 18.0F;
   private static final WeakHashMap<AbstractClientPlayer, float[]> WEIGHT = new WeakHashMap<>();

   private IronManFirstPerson() {
   }

   static boolean shieldUp(AbstractClientPlayer player) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      return snapshot != null && IronManView.worn(snapshot) && IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.SHIELD_UP);
   }

   /** Weight 0..1, eased by real time (frame-rate independent). Render thread only. */
   public static float guardWeight(AbstractClientPlayer player) {
      float[] state = WEIGHT.computeIfAbsent(player, p -> new float[]{0.0F, Float.NaN});
      long now = System.nanoTime();
      float last = state[1];
      float dt = Float.isNaN(last) ? 0.0F : Math.min(0.1F, (now / 1.0E9F) - last);
      state[1] = now / 1.0E9F;
      float a = 1.0F - (float)Math.exp(-RATE * dt);
      state[0] = Mth.lerp(a, state[0], shieldUp(player) ? 1.0F : 0.0F);
      return state[0];
   }

   /** The shield arm stays drawn in first person while the guard shows. */
   public static boolean wantsOffhand(AbstractClientPlayer player) {
      float[] state = WEIGHT.get(player);
      return shieldUp(player) || state != null && state[0] > 0.01F;
   }

   /** Applied right after the arm's pushPose, before the vanilla arm transform. */
   public static void applyGuard(AbstractClientPlayer player, HumanoidArm arm, PoseStack poseStack) {
      if (arm != HumanoidArm.LEFT) {
         return;
      }

      float w = guardWeight(player);
      if (w < 0.001F) {
         return;
      }

      poseStack.translate(GUARD_X * w, GUARD_Y * w, GUARD_Z * w);
      poseStack.mulPose(new Quaternionf().slerp(GUARD, w));
   }
}
