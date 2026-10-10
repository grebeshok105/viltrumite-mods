package dev.baranhan.viltrumitecore.client.ironman;

import static dev.baranhan.viltrumitecore.client.anim.pose.PoseKeys.key;
import static dev.baranhan.viltrumitecore.client.anim.pose.PoseKeys.under;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.anim.pose.FirstPersonArm;
import dev.baranhan.viltrumitecore.client.ironman.anim.IronManAnimation;
import dev.baranhan.viltrumitecore.client.ironman.anim.IronManAnimation.Layer;
import dev.baranhan.viltrumitecore.client.ironman.anim.IronManAnimation.Signature;
import dev.baranhan.viltrumitecore.client.ironman.anim.IronManAnimation.View;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkState;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;

/**
 * Iron Man first-person arms (animation-system §5, PR 19 iteration 2): every
 * third-person combat pose has its first-person view, posed with
 * {@link FirstPersonArm} (turns about the vanilla shoulder, keys blend on the
 * shortest arc, so a half-blended pose is the same arm halfway there). Keys
 * are {pitch, yaw, roll, x, y, z} for the right arm, solved with
 * {@code tools/preview_fp_arm.py --solve PALM DIRECTION} (palm point and arm
 * direction in view space, noted at each key), mirrored for the left arm.
 * Timing and per-hand weights come from {@link IronManAnimation}; the shield
 * guard stays the solved quaternion of iteration 1, applied last.
 */
public final class IronManFirstPerson {
   private static final Quaternionf GUARD = new Quaternionf(0.31364F, 0.74966F, -0.32919F, -0.48092F);
   private static final float GUARD_X = -1.3666F;
   private static final float GUARD_Y = 0.3784F;
   private static final float GUARD_Z = -1.1241F;
   /** Blend speed (1/s) of the guard. */
   private static final float RATE = 18.0F;
   private static final WeakHashMap<AbstractClientPlayer, float[]> WEIGHT = new WeakHashMap<>();
   private static final FirstPersonArm ARM = new FirstPersonArm();

   /** Palm (0.30, -0.24, -0.92) on the crosshair line. */
   private static final float[] AIM = {-34.97F, 22.4F, 8.5F, -0.16F, 0.589F, 0.358F};
   /** Recoil: palm (0.33, -0.15, -0.80), tipped up (-0.2, 0.45, -1). */
   private static final float[] KICK = {-15.68F, 26.25F, 18.98F, -0.014F, 0.391F, 0.401F};
   /** Forearm launcher: palm (0.40, -0.32, -1.0), along the look, pod in view. */
   private static final float[] MISSILE = {-34.18F, 22.82F, 9.46F, -0.049F, 0.494F, 0.277F};
   /** Unibeam: arms down and out of the beam, palm (0.62, -0.78, -0.75). */
   private static final float[] UNIBEAM = {-65.47F, 13.92F, -34.16F, -0.165F, 0.589F, 0.216F};
   /** Blade lunge: palm (0.20, -0.20, -1.15) thrust forward. */
   private static final float[] LUNGE = {-32.48F, 25.32F, 14.04F, -0.19F, 0.569F, 0.119F};
   /** Hammer charge: drawn back over the shoulder, palm (0.52, 0.05, -0.55). */
   private static final float[] WINDUP = {57.63F, -11.57F, 19.78F, -0.193F, 0.191F, -0.139F};
   /** War Machine brace: palm (0.42, -0.46, -0.92). */
   private static final float[] GUN = {-33.83F, 23.11F, 10.06F, -0.021F, 0.346F, 0.356F};
   /** Slam jump: arms up, palm (0.45, 0.25, -0.62). */
   private static final float[] SLAM_UP = {38.8F, 0.46F, 15.77F, -0.18F, 0.375F, 0.059F};
   /** Arms a little out and down while the plates wrap, palm (0.78, -0.62, -0.92). */
   private static final float[] EQUIP = {-35.18F, 8.34F, -12.28F, 0.031F, 0.301F, 0.303F};
   /** Hand to the faceplate (helmet gesture, left arm), palm (0.20, -0.08, -0.42). */
   private static final float[] FACE = {10.57F, 45.32F, 38.95F, 0.153F, 0.215F, 0.383F};

   /** Forehand: cocked out right (0.58,-0.18,-0.78), across (0.02,-0.22,-1.0), through to the left (-0.45,-0.35,-0.85). */
   private static final float[][] SLASH = {
      under(0), key(2, -5.5F, -14.25F, -13.2F, -0.389F, 0.377F, 0.238F), key(4, -29.4F, 19.05F, 5.65F, -0.496F, 0.549F, 0.273F),
      key(6, -77.76F, 41.9F, 77.34F, -0.301F, 0.611F, 0.071F), under(13)
   };
   /** Backhand: cocked left (-0.30,-0.12,-0.80), across (0.15,-0.25,-1.0), out right (0.62,-0.38,-0.85). */
   private static final float[][] BACKHAND = {
      under(0), key(2, -34.81F, 47.47F, 56.15F, -0.237F, 0.491F, 0.192F), key(4, -29.77F, 13.68F, -1.82F, -0.476F, 0.557F, 0.263F),
      key(6, -31.85F, -19.52F, -38.96F, -0.491F, 0.543F, 0.073F), under(13)
   };
   /** Hammer: up over the head (0.35,0.12,-0.72), hold, smash (0.16,-0.46,-1.2), settle (0.2,-0.40,-1.15). */
   private static final float[][] HAMMER = {
      under(0), key(4, 27.41F, 11.02F, 16.59F, -0.167F, 0.279F, 0.112F), key(6, 30.0F, 11.0F, 16.6F, -0.167F, 0.29F, 0.1F),
      key(8, -55.48F, 25.73F, 6.04F, -0.282F, 0.642F, 0.031F), key(12, -46.94F, 24.07F, 6.52F, -0.256F, 0.593F, 0.113F), under(19)
   };
   /** Forming: fist raised before the eyes (0.24,-0.30,-0.82) while the weapon grows. */
   private static final float[][] FORM = {
      under(0), key(4, -18.72F, 33.1F, 27.6F, 0.023F, 0.244F, 0.344F), key(8, -20.0F, 31.0F, 26.0F, 0.02F, 0.25F, 0.34F), under(14)
   };
   /** Dissolving: raised, then flicked down (0.52,-0.62,-0.82). */
   private static final float[][] DISSOLVE = {
      under(0), key(3, -18.72F, 33.1F, 27.6F, 0.023F, 0.244F, 0.344F), key(6, -51.86F, 13.85F, -17.47F, -0.197F, 0.541F, 0.344F), under(11)
   };

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
      float[] guard = WEIGHT.get(player);
      if (shieldUp(player) || guard != null && guard[0] > 0.01F) {
         return true;
      }

      IronManAnimation.State s = IronManAnimation.of(player);
      if (s == null || !s.worn()) {
         return HelmetAnim.gesturing(player);
      }

      Signature sig = s.signature();
      return s.stance(false) || s.missiles() || s.unibeamPhase() == 1 || s.unibeamPhase() == 2 || s.windup() || s.striking()
         || sig == Signature.GUN || sig == Signature.SLAM_JUMP || sig == Signature.SLAM_DIVE || HelmetAnim.gesturing(player);
   }

   /** Combat poses on one first-person arm; applied after the arm's pushPose (view space). */
   public static void applyCombat(AbstractClientPlayer player, HumanoidArm arm, PoseStack poseStack, float partialTick) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      IronManAnimation.State s = IronManAnimation.of(player);
      if (snapshot == null || s == null) {
         return;
      }

      boolean right = arm == HumanoidArm.RIGHT;
      float side = right ? 1.0F : -1.0F;
      View view = right ? View.FIRST_PERSON_RIGHT : View.FIRST_PERSON_LEFT;
      float time = player.tickCount + partialTick;
      FirstPersonArm limb = ARM.reset();

      MarkState mark = MarkState.of(snapshot);
      float equip = s.weight(view, Layer.EQUIP, mark.markOn() && (mark.equipping() || mark.equipPhase() == IronManFlags.EQUIP_PARTIAL));
      limb.blend(EQUIP, equip);
      int phase = s.unibeamPhase();
      float unibeam = s.weight(view, Layer.UNIBEAM, phase == 1 || phase == 2);
      if (unibeam > 0.001F) {
         limb.blend(UNIBEAM, unibeam);
         limb.add(0.0F, 0.0F, 0.0F, 0.0F, 0.01F * Mth.sin(time * 2.9F) * unibeam, 0.0F);
      }

      float stance = s.weight(view, right ? Layer.STANCE_RIGHT : Layer.STANCE_LEFT, s.stance(right));
      float missiles = s.weight(view, Layer.MISSILES, s.missiles());
      if (stance > 0.001F) {
         limb.blend(AIM, stance);
         float kick = IronManAnimation.kick(s.sinceShot(right, partialTick));
         limb.blend(KICK, kick * stance);
         float charge = s.charge(right);
         if (charge > 0.0F) {
            limb.add(0.6F * charge * Mth.sin(time * 2.7F), 0.0F, 0.0F, 0.0F, 0.004F * charge * Mth.sin(time * 3.3F), 0.0F);
         }
      }

      if (missiles > 0.001F) {
         limb.blend(MISSILE, missiles);
         float kick = IronManAnimation.kick(s.sinceMissiles(partialTick) * 1.5F);
         limb.add(8.0F * kick, 0.0F, 0.0F, 0.0F, 0.0F, 0.05F * kick);
      }

      if (right) {
         float lunge = s.weight(view, Layer.LUNGE, s.lunge());
         limb.blend(LUNGE, lunge);
         float windup = s.weight(view, Layer.WINDUP, s.windup());
         if (windup > 0.001F) {
            limb.blend(WINDUP, windup);
            if (s.windupCharge() >= 0.999F) {
               limb.add(1.5F * Mth.sin(time * 4.0F) * windup, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            }
         }

         float form = s.weight(view, Layer.FORM, s.forming());
         limb.blend(s.dissolve() ? DISSOLVE : FORM, s.formTime(partialTick), form);
         float strike = s.weight(view, Layer.STRIKE, s.striking());
         limb.blend(s.hammer() ? HAMMER : s.backhand() ? BACKHAND : SLASH, s.strikeTime(partialTick), strike);
      }

      Signature sig = s.signature() != Signature.NONE ? s.signature() : s.lastSignature();
      float signature = s.weight(view, Layer.SIGNATURE, s.signature() != Signature.NONE);
      if (signature > 0.001F) {
         switch (sig) {
            case LASER, FIST -> {
               if (right) {
                  limb.blend(AIM, signature);
               }
            }
            case GUN -> limb.blend(GUN, signature);
            case SLAM_JUMP -> limb.blend(SLAM_UP, signature);
            case SLAM_DIVE -> limb.blend(AIM, signature);
            default -> {
            }
         }
      }

      if (!right) {
         // Smooth envelope of its own (rise, hold, lower); none when the suit comes off.
         limb.blend(FACE, HelmetAnim.gesture(player, partialTick));
      }

      limb.apply(poseStack, side);
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
