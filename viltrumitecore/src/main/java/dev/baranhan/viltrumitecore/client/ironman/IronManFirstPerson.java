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
 * Iron Man first-person arm layer (animation-system §5, pattern of
 * FirstPersonHomelanderMixin / FirstPersonBlockMixin): every combat pose of
 * IronManPoser has its first-person view. Repulsor palm raised to the
 * crosshair with a recoil kick (both palms for a volley), Unibeam arms pulled
 * down, missile stance, blade slash sweep, hammer wind-up and overhead slam,
 * blade-dash thrust, and the shield guard: the left arm turns so the forearm
 * lies across the lower view and the plate faces forward (target solved
 * against the vanilla arm transform: plate centre at (-0.42, -0.30, -0.85)).
 * Weights ease by real time, so both arms and all passes share one step.
 */
public final class IronManFirstPerson {
   private static final Quaternionf GUARD = new Quaternionf(0.31364F, 0.74966F, -0.32919F, -0.48092F);
   private static final float GUARD_X = -1.3666F;
   private static final float GUARD_Y = 0.3784F;
   private static final float GUARD_Z = -1.1241F;
   /** Blend speed (1/s): about 0.12 s to raise. */
   private static final float RATE = 18.0F;
   private static final WeakHashMap<AbstractClientPlayer, float[]> WEIGHT = new WeakHashMap<>();
   private static final WeakHashMap<AbstractClientPlayer, Weights> COMBAT = new WeakHashMap<>();

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

   /** The left arm stays drawn in first person while a two-hand or left-hand pose shows. */
   public static boolean wantsOffhand(AbstractClientPlayer player) {
      float[] state = WEIGHT.get(player);
      Weights w = COMBAT.get(player);
      return shieldUp(player) || state != null && state[0] > 0.01F
         || w != null && (w.aimLeft > 0.01F || w.unibeam > 0.01F || w.missiles > 0.01F);
   }

   /** Combat poses on one first-person arm; applied after the arm's pushPose (view space). */
   public static void applyCombat(AbstractClientPlayer player, HumanoidArm arm, PoseStack poseStack, float partialTick) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         COMBAT.remove(player);
         return;
      }

      IronManPoser.Combat c = IronManPoser.Combat.of(snapshot, partialTick);
      Weights w = COMBAT.computeIfAbsent(player, p -> new Weights());
      w.advance(c);
      boolean right = arm == HumanoidArm.RIGHT;
      float side = right ? 1.0F : -1.0F;
      float aim = right ? w.aimRight : w.aimLeft;
      float tx = 0.0F;
      float ty = 0.0F;
      float tz = 0.0F;
      float rx = 0.0F;
      float ry = 0.0F;
      float rz = 0.0F;
      if (aim > 0.001F) {
         // Palm up to the crosshair, then a short kick back on the shot.
         tx -= side * 0.2F * aim;
         ty += 0.14F * aim + 0.03F * w.recoil * aim;
         tz += -0.05F * aim + 0.12F * w.recoil * aim;
         rx += -28.0F * aim + 14.0F * w.recoil * aim;
         ry += side * -10.0F * aim;
      }

      if (w.unibeam > 0.001F) {
         // Arms pulled down and out of the way of the chest beam, a fine tremble.
         float tremble = 0.01F * Mth.sin((player.tickCount + partialTick) * 2.9F);
         tx += side * 0.1F * w.unibeam;
         ty += -0.32F * w.unibeam + tremble;
         tz += 0.08F * w.unibeam;
         rz += side * 14.0F * w.unibeam;
      }

      if (w.missiles > 0.001F) {
         tx += side * 0.05F * w.missiles;
         ty -= 0.1F * w.missiles;
         rx += 8.0F * w.missiles;
      }

      if (right) {
         if (c.slash() >= 0.0F) {
            // Sweep right to left across the view.
            float t = c.slash();
            float arc = Mth.sin(t * Mth.PI);
            float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            tx += -0.32F * arc;
            ty += 0.06F * arc;
            tz += -0.12F * arc;
            ry += Mth.lerp(e, 30.0F, -50.0F);
            rz += -22.0F * arc;
         }

         if (c.swing() >= 0.0F) {
            // Hammer: up over the head, then down hard.
            float t = c.swing();
            float up = t < 0.35F ? t / 0.35F : 1.0F - Math.min(1.0F, (t - 0.35F) / 0.2F);
            float down = t < 0.35F ? 0.0F : t < 0.55F ? (t - 0.35F) / 0.2F : 1.0F - (t - 0.55F) / 0.45F;
            tx += -0.12F * (up + down);
            ty += 0.3F * up - 0.08F * down;
            tz += 0.1F * up - 0.15F * down;
            rx += -60.0F * up + 30.0F * down;
         }

         if (w.windup > 0.001F) {
            ty += 0.24F * w.windup;
            tz += 0.08F * w.windup;
            rx += -48.0F * w.windup;
         }

         if (w.lunge > 0.001F) {
            tx += -0.1F * w.lunge;
            ty += 0.04F * w.lunge;
            tz += -0.24F * w.lunge;
            rx += -14.0F * w.lunge;
         }
      }

      if (tx == 0.0F && ty == 0.0F && tz == 0.0F && rx == 0.0F && ry == 0.0F && rz == 0.0F) {
         return;
      }

      poseStack.translate(tx, ty, tz);
      poseStack.mulPose(new Quaternionf().rotateX((float)Math.toRadians(rx)).rotateY((float)Math.toRadians(ry)).rotateZ((float)Math.toRadians(rz)));
   }

   /** First-person combat weights (separate from the third-person ones, animation-system §5). */
   private static final class Weights {
      float aimRight;
      float aimLeft;
      float recoil;
      float unibeam;
      float missiles;
      float windup;
      float lunge;
      long lastNanos = System.nanoTime();

      void advance(IronManPoser.Combat c) {
         long now = System.nanoTime();
         float dt = Math.min(0.1F, (now - this.lastNanos) / 1.0E9F);
         this.lastNanos = now;
         float a = 1.0F - (float)Math.exp(-RATE * dt);
         float fast = 1.0F - (float)Math.exp(-RATE * 1.8F * dt);
         this.aimRight = Mth.lerp(a, this.aimRight, c.aimRight() ? 1.0F : 0.0F);
         this.aimLeft = Mth.lerp(a, this.aimLeft, c.aimLeft() ? 1.0F : 0.0F);
         this.recoil = Mth.lerp(fast, this.recoil, c.recoil());
         this.unibeam = Mth.lerp(a, this.unibeam, c.unibeam() ? 1.0F : 0.0F);
         this.missiles = Mth.lerp(a, this.missiles, c.missiles() ? 1.0F : 0.0F);
         this.windup = Mth.lerp(a, this.windup, c.windup() >= 0.0F ? 1.0F : 0.0F);
         this.lunge = Mth.lerp(a, this.lunge, c.lunge() ? 1.0F : 0.0F);
      }
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
