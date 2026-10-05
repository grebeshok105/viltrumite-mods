package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.render.animation.BlockAnimationManager;
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

@Mixin({ItemInHandRenderer.class})
public abstract class FirstPersonBlockMixin {
   @Unique
   private static final float R_X = 1.34F;
   @Unique
   private static final float R_Y = -0.24F;
   @Unique
   private static final float R_Z = -0.85F;
   @Unique
   private static final float L_X = 0.98F;
   @Unique
   private static final float L_Y = 0.3F;
   @Unique
   private static final float L_Z = -0.39F;
   @Unique
   private static final Quaternionf RIGHT_TARGET_ROT = new Quaternionf()
      .rotateX((float)Math.toRadians(130.24F))
      .rotateY((float)Math.toRadians(41.75))
      .rotateZ((float)Math.toRadians(-147.78F));
   @Unique
   private static final Quaternionf LEFT_TARGET_ROT = new Quaternionf()
      .rotateX((float)Math.toRadians(8.78F))
      .rotateY((float)Math.toRadians(-45.49F))
      .rotateZ((float)Math.toRadians(60.04F));

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void onRenderFirstPersonBlock(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float attackAnim,
      ItemStack stack,
      float equipAnim,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         boolean isBlocking = corePlayer.isBlocking();
         float weight = BlockAnimationManager.calculateWeight(player, isBlocking);
         if (!(weight < 0.001F) || isBlocking) {
            HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isLeftArm = currentArm == HumanoidArm.LEFT;
            float m = isLeftArm ? -1.0F : 1.0F;
            float targetX = isLeftArm ? 0.98F : 1.34F;
            float targetY = isLeftArm ? 0.3F : -0.24F;
            float targetZ = isLeftArm ? -0.39F : -0.85F;
            float x = Mth.lerp(weight, 0.0F, targetX * m);
            float y = Mth.lerp(weight, 0.0F, targetY);
            float z = Mth.lerp(weight, 0.0F, targetZ);
            poseStack.translate(x, y, z);
            Quaternionf startRot = new Quaternionf().identity();
            Quaternionf targetRot = isLeftArm ? LEFT_TARGET_ROT : RIGHT_TARGET_ROT;
            Quaternionf currentRot = new Quaternionf(startRot).slerp(targetRot, weight);
            poseStack.mulPose(currentRot);
         }
      }
   }
}
