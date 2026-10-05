package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.ViltrumiteFlightClient;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class BodyRotationTrackerMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
      )}
   )
   private void extractBodyRotation(
      T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (ViltrumiteCoreClient.isWorldRendering) {
         if (!ShaderCompat.isShadowPass()) {
            if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
               Vector3f localForward = new Vector3f(0.0F, 0.0F, 1.0F);
               poseStack.last().normal().transform(localForward);
               Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
               float invRoll = (float)Math.toRadians((double)(-ViltrumiteFlightClient.currentCameraRoll));
               float invPitch = (float)Math.toRadians((double)(-camera.getXRot()));
               float invYaw = (float)Math.toRadians((double)(-(camera.getYRot() + 180.0F)));
               localForward.rotateZ(invRoll);
               localForward.rotateX(invPitch);
               localForward.rotateY(invYaw);
               double horizontalLength = Math.sqrt((double)(localForward.x() * localForward.x() + localForward.z() * localForward.z()));
               float bodyPitch = (float)Math.toDegrees(-Math.atan2((double)localForward.y(), horizontalLength));
               float bodyYaw = (float)Math.toDegrees(Math.atan2((double)(-localForward.x()), (double)localForward.z()));
               corePlayer.setCalculatedBodyPitch(bodyPitch);
               corePlayer.setCalculatedBodyYaw(bodyYaw);
            }
         }
      }
   }
}
