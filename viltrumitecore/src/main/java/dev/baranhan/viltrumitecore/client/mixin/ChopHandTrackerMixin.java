package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class ChopHandTrackerMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
      )}
   )
   private void captureChopHandPosition(
      T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (ViltrumiteCoreClient.isWorldRendering) {
         if (livingEntity instanceof ViltrumiteCorePlayer corePlayer && corePlayer.getChopTicks() > 0) {
            if (!(((LivingEntityRenderer)this).getModel() instanceof HumanoidModel<?> model)) {
               return;
            }

            poseStack.pushPose();
            if (corePlayer.isLeftChop()) {
               model.leftArm.translateAndRotate(poseStack);
            } else {
               model.rightArm.translateAndRotate(poseStack);
            }

            poseStack.translate(0.0, 0.46875, 0.0);
            Vector3f localPos = poseStack.last().pose().getTranslation(new Vector3f());
            poseStack.popPose();
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            localPos.rotateX((float)Math.toRadians((double)(-camera.getXRot())));
            localPos.rotateY((float)Math.toRadians((double)(-(camera.getYRot() + 180.0F))));
            Vec3 exactWorldPos = camera.getPosition().add((double)localPos.x(), (double)localPos.y(), (double)localPos.z());
            corePlayer.setChopHandPos(exactWorldPos);
         }
      }
   }
}
