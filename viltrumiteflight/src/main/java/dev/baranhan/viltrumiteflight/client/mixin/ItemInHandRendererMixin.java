package dev.baranhan.viltrumiteflight.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumiteflight.client.PoseDataManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public class ItemInHandRendererMixin {
   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("HEAD")}
   )
   private void onRenderFirstPersonItem(
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
      poseStack.translate(PoseDataManager.FP.translateX, PoseDataManager.FP.translateY, PoseDataManager.FP.translateZ);
      poseStack.mulPose(Axis.XP.rotationDegrees(PoseDataManager.FP.rotateX));
      poseStack.mulPose(Axis.YP.rotationDegrees(PoseDataManager.FP.rotateY));
      poseStack.mulPose(Axis.ZP.rotationDegrees(PoseDataManager.FP.rotateZ));
   }
}
