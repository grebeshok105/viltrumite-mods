package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1500
)
public abstract class FirstPersonPunchMixin {
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
   private void onRenderFirstPersonPunch(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         int punchTicks = corePlayer.getPunchTicks();
         if (punchTicks > 0) {
            boolean isLeftPunch = corePlayer.isLeftArmPunch();
            HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRenderingLeftArm = currentArm == HumanoidArm.LEFT;
            if (isLeftPunch == isRenderingLeftArm) {
               float time = (20.0F - ((float)punchTicks - partialTicks)) / 20.0F;
               time = Mth.clamp(time, 0.0F, 1.0F);
               float m = isLeftPunch ? -1.0F : 1.0F;
               float targetX = 0.0F;
               float targetY = 0.0F;
               float targetZ = 0.0F;
               float targetRX = 0.0F;
               float targetRY = 0.0F;
               float targetRZ = 0.0F;
               float overMultiplier = 1.15F;
               if (time < 0.2F) {
                  float localT = time / 0.2F;
                  localT *= localT;
                  targetX = Mth.lerp(localT, 0.0F, 0.43F);
                  targetY = Mth.lerp(localT, 0.0F, 0.3F);
                  targetZ = Mth.lerp(localT, 0.0F, 0.24F);
                  targetRX = Mth.lerp(localT, 0.0F, -10.52F);
                  targetRY = Mth.lerp(localT, 0.0F, 14.02F);
                  targetRZ = Mth.lerp(localT, 0.0F, -2.98F);
               } else if (time < 0.25F) {
                  float localT = (time - 0.2F) / 0.05F;
                  targetX = Mth.lerp(localT, 0.43F, -0.31F * overMultiplier);
                  targetY = Mth.lerp(localT, 0.3F, 0.27F * overMultiplier);
                  targetZ = Mth.lerp(localT, 0.24F, -0.4F * overMultiplier);
                  targetRX = Mth.lerp(localT, -10.52F, -38.16F * overMultiplier);
                  targetRY = Mth.lerp(localT, 14.02F, 13.25F * overMultiplier);
                  targetRZ = Mth.lerp(localT, -2.98F, 31.7F * overMultiplier);
               } else if (time < 0.32F) {
                  float localT = (time - 0.25F) / 0.07F;
                  targetX = Mth.lerp(localT, -0.31F * overMultiplier, -0.31F);
                  targetY = Mth.lerp(localT, 0.27F * overMultiplier, 0.27F);
                  targetZ = Mth.lerp(localT, -0.4F * overMultiplier, -0.4F);
                  targetRX = Mth.lerp(localT, -38.16F * overMultiplier, -38.16F);
                  targetRY = Mth.lerp(localT, 13.25F * overMultiplier, 13.25F);
                  targetRZ = Mth.lerp(localT, 31.7F * overMultiplier, 31.7F);
               } else if (time < 0.65F) {
                  targetX = -0.31F;
                  targetY = 0.27F;
                  targetZ = -0.4F;
                  targetRX = -38.16F;
                  targetRY = 13.25F;
                  targetRZ = 31.7F;
               } else if (time < 0.9F) {
                  float localT = (time - 0.65F) / 0.25F;
                  targetX = Mth.lerp(localT, -0.31F, 0.0F);
                  targetY = Mth.lerp(localT, 0.27F, 0.0F);
                  targetZ = Mth.lerp(localT, -0.4F, 0.0F);
                  targetRX = Mth.lerp(localT, -38.16F, 0.0F);
                  targetRY = Mth.lerp(localT, 13.25F, 0.0F);
                  targetRZ = Mth.lerp(localT, 31.7F, 0.0F);
               }

               poseStack.translate(targetX * m, targetY, targetZ);
               poseStack.mulPose(new Quaternionf().rotateX((float)Math.toRadians((double)targetRX)));
               poseStack.mulPose(new Quaternionf().rotateY((float)Math.toRadians((double)(targetRY * m))));
               poseStack.mulPose(new Quaternionf().rotateZ((float)Math.toRadians((double)(targetRZ * m))));
            }
         }
      }
   }
}
