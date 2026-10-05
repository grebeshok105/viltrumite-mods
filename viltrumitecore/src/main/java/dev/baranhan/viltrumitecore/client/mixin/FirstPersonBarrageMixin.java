package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class FirstPersonBarrageMixin {
   @Unique
   private static final float S1_X = 0.43F;
   @Unique
   private static final float S1_Y = 0.3F;
   @Unique
   private static final float S1_Z = 0.24F;
   @Unique
   private static final float S1_RX = -10.52F;
   @Unique
   private static final float S1_RY = 14.02F;
   @Unique
   private static final float S1_RZ = -2.98F;
   @Unique
   private static final float S2_X = -0.31F;
   @Unique
   private static final float S2_Y = 0.27F;
   @Unique
   private static final float S2_Z = -0.4F;
   @Unique
   private static final float S2_RX = -38.16F;
   @Unique
   private static final float S2_RY = 13.25F;
   @Unique
   private static final float S2_RZ = 31.7F;

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void onRenderFirstPersonBarrage(
      AbstractClientPlayer pPlayer,
      float pPartialTicks,
      float pPitch,
      InteractionHand pHand,
      float pSwingProgress,
      ItemStack pStack,
      float pEquipProgress,
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pCombinedLight,
      CallbackInfo ci
   ) {
      if (pPlayer instanceof ViltrumiteCorePlayer corePlayer) {
         int barrageTicks = corePlayer.getBarrageTicks();
         if (barrageTicks != 0) {
            HumanoidArm currentArm = pHand == InteractionHand.MAIN_HAND ? pPlayer.getMainArm() : pPlayer.getMainArm().getOpposite();
            boolean isRenderingLeftArm = currentArm == HumanoidArm.LEFT;
            float m = isRenderingLeftArm ? -1.0F : 1.0F;
            float exactTime = barrageTicks > 0 ? (float)(barrageTicks - 1) + pPartialTicks : (float)barrageTicks + pPartialTicks;
            if (barrageTicks > 0 && exactTime < 0.0F) {
               exactTime = 0.0F;
            }

            float targetX = 0.0F;
            float targetY = 0.0F;
            float targetZ = 0.0F;
            float targetRX = 0.0F;
            float targetRY = 0.0F;
            float targetRZ = 0.0F;
            if (barrageTicks > 0) {
               if (exactTime < 5.0F) {
                  float progress = exactTime / 5.0F;
                  progress *= progress;
                  targetX = Mth.lerp(progress, 0.0F, 0.43F);
                  targetY = Mth.lerp(progress, 0.0F, 0.3F);
                  targetZ = Mth.lerp(progress, 0.0F, 0.24F);
                  targetRX = Mth.lerp(progress, 0.0F, -10.52F);
                  targetRY = Mth.lerp(progress, 0.0F, 14.02F);
                  targetRZ = Mth.lerp(progress, 0.0F, -2.98F);
               } else {
                  float punchTime = exactTime - 5.0F;
                  int punchIndex = (int)(punchTime / 3.0F);
                  float p = punchTime % 3.0F / 3.0F;
                  boolean isRightPunching = punchIndex % 2 == 0;
                  boolean isThisArmPunching = isRenderingLeftArm != isRightPunching;
                  if (isThisArmPunching) {
                     float overMultiplier = 1.15F;
                     if (p < 0.125F) {
                        targetX = 0.43F;
                        targetY = 0.3F;
                        targetZ = 0.24F;
                        targetRX = -10.52F;
                        targetRY = 14.02F;
                        targetRZ = -2.98F;
                     } else if (p < 0.25F) {
                        float t = (p - 0.125F) / 0.125F;
                        targetX = Mth.lerp(t, 0.43F, -0.31F * overMultiplier);
                        targetY = Mth.lerp(t, 0.3F, 0.27F * overMultiplier);
                        targetZ = Mth.lerp(t, 0.24F, -0.4F * overMultiplier);
                        targetRX = Mth.lerp(t, -10.52F, -38.16F * overMultiplier);
                        targetRY = Mth.lerp(t, 14.02F, 13.25F * overMultiplier);
                        targetRZ = Mth.lerp(t, -2.98F, 31.7F * overMultiplier);
                     } else if (p < 0.55F) {
                        float t = (p - 0.25F) / 0.3F;
                        targetX = Mth.lerp(t, -0.31F * overMultiplier, -0.31F);
                        targetY = Mth.lerp(t, 0.27F * overMultiplier, 0.27F);
                        targetZ = Mth.lerp(t, -0.4F * overMultiplier, -0.4F);
                        targetRX = Mth.lerp(t, -38.16F * overMultiplier, -38.16F);
                        targetRY = Mth.lerp(t, 13.25F * overMultiplier, 13.25F);
                        targetRZ = Mth.lerp(t, 31.7F * overMultiplier, 31.7F);
                     } else if (p < 0.75F) {
                        targetX = -0.31F;
                        targetY = 0.27F;
                        targetZ = -0.4F;
                        targetRX = -38.16F;
                        targetRY = 13.25F;
                        targetRZ = 31.7F;
                     } else {
                        float t = (p - 0.75F) / 0.25F;
                        targetX = Mth.lerp(t, -0.31F, 0.43F);
                        targetY = Mth.lerp(t, 0.27F, 0.3F);
                        targetZ = Mth.lerp(t, -0.4F, 0.24F);
                        targetRX = Mth.lerp(t, -38.16F, -10.52F);
                        targetRY = Mth.lerp(t, 13.25F, 14.02F);
                        targetRZ = Mth.lerp(t, 31.7F, -2.98F);
                     }
                  } else {
                     targetX = 0.43F;
                     targetY = 0.3F;
                     targetZ = 0.24F;
                     targetRX = -10.52F;
                     targetRY = 14.02F;
                     targetRZ = -2.98F;
                  }
               }
            } else {
               float timeSinceRelease = 9.0F + exactTime;
               float progress = 0.0F;
               if (timeSinceRelease >= 4.0F) {
                  progress = (timeSinceRelease - 4.0F) / 5.0F;
               }

               boolean wasThisArmPunching = corePlayer.isLeftBarrageArm() == isRenderingLeftArm;
               float baseX = wasThisArmPunching ? -0.31F : 0.43F;
               float baseY = wasThisArmPunching ? 0.27F : 0.3F;
               float baseZ = wasThisArmPunching ? -0.4F : 0.24F;
               float baseRX = wasThisArmPunching ? -38.16F : -10.52F;
               float baseRY = wasThisArmPunching ? 13.25F : 14.02F;
               float baseRZ = wasThisArmPunching ? 31.7F : -2.98F;
               targetX = Mth.lerp(progress, baseX, 0.0F);
               targetY = Mth.lerp(progress, baseY, 0.0F);
               targetZ = Mth.lerp(progress, baseZ, 0.0F);
               targetRX = Mth.lerp(progress, baseRX, 0.0F);
               targetRY = Mth.lerp(progress, baseRY, 0.0F);
               targetRZ = Mth.lerp(progress, baseRZ, 0.0F);
            }

            pPoseStack.translate(targetX * m, targetY, targetZ);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(targetRX));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(targetRY * m));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(targetRZ * m));
         }
      }
   }
}
