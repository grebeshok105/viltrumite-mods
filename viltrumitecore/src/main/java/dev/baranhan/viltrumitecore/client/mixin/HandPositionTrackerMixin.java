package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.HandPosSyncC2SPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.ViltrumiteFlightClient;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class HandPositionTrackerMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
      )}
   )
   private void extractHandPosition(
      T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (ViltrumiteCoreClient.isWorldRendering) {
         if (!ShaderCompat.isShadowPass()) {
            if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
               boolean isGrabbing = corePlayer.isTryingToGrab() || corePlayer.getGrabbedTarget() != null;
               boolean isBarraging = corePlayer.getBarrageTicks() > 0;
               ItemStack mainHand = livingEntity.getMainHandItem();
               boolean isGunActive = mainHand.getItem() instanceof InfinityGunItem && InfinityGunItem.isFiring(mainHand);
               if (isGrabbing || isBarraging || isGunActive) {
                  if (!(((LivingEntityRenderer)this).getModel() instanceof HumanoidModel<?> model)) {
                     return;
                  }

                  poseStack.pushPose();
                  if (isGunActive) {
                     model.rightArm.translateAndRotate(poseStack);
                     poseStack.translate(0.0F, 1.875F, -0.25F);
                  } else if (isBarraging) {
                     if (corePlayer.isLeftBarrageArm()) {
                        model.leftArm.translateAndRotate(poseStack);
                     } else {
                        model.rightArm.translateAndRotate(poseStack);
                     }

                     poseStack.translate(0.0F, 0.625F, 0.0F);
                  } else {
                     model.leftArm.translateAndRotate(poseStack);
                     poseStack.translate(0.0F, 0.46875F, 0.0F);
                  }

                  Vector3f localPos = poseStack.last().pose().getTranslation(new Vector3f());
                  Vector3f localDir = new Vector3f(0.0F, -1.0F, 0.0F);
                  poseStack.last().normal().transform(localDir);
                  poseStack.popPose();
                  Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                  float invRoll = (float)Math.toRadians((double)(-ViltrumiteFlightClient.currentCameraRoll));
                  float invPitch = (float)Math.toRadians((double)(-camera.getXRot()));
                  float invYaw = (float)Math.toRadians((double)(-(camera.getYRot() + 180.0F)));
                  localPos.rotateZ(invRoll);
                  localPos.rotateX(invPitch);
                  localPos.rotateY(invYaw);
                  localDir.rotateZ(invRoll);
                  localDir.rotateX(invPitch);
                  localDir.rotateY(invYaw);
                  Vec3 exactWorldPos = camera.getPosition().add((double)localPos.x(), (double)localPos.y(), (double)localPos.z());
                  double lerpX = Mth.lerp((double)partialTicks, livingEntity.xOld, livingEntity.getX());
                  double lerpY = Mth.lerp((double)partialTicks, livingEntity.yOld, livingEntity.getY());
                  double lerpZ = Mth.lerp((double)partialTicks, livingEntity.zOld, livingEntity.getZ());
                  Vec3 bodyPos = new Vec3(lerpX, lerpY, lerpZ);
                  Vec3 handOffset = exactWorldPos.subtract(bodyPos);
                  corePlayer.setCalculatedHandPos(exactWorldPos);
                  corePlayer.setCalculatedHandOffset(handOffset);
                  double horizontalLength = Math.sqrt((double)(localDir.x() * localDir.x() + localDir.z() * localDir.z()));
                  float handPitch = (float)Math.toDegrees(-Math.atan2((double)localDir.y(), horizontalLength));
                  float handYaw = (float)Math.toDegrees(Math.atan2((double)(-localDir.x()), (double)localDir.z()));
                  corePlayer.setCalculatedHandPitch(handPitch);
                  corePlayer.setCalculatedHandYaw(handYaw);
                  if (isGrabbing) {
                     CoreMessages.sendToServer(new HandPosSyncC2SPacket(exactWorldPos.x, exactWorldPos.y, exactWorldPos.z));
                  }
               }
            }
         }
      }
   }
}
