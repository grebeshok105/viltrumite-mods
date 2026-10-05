package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumitecore.client.render.animation.SilhouetteManager;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class SilhouetteRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @Shadow
   protected M model;

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
      )}
   )
   private void renderSilhouettes(
      T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         boolean isFlying = false;
         if (livingEntity instanceof ViltrumiteFlightPlayer flightPlayer) {
            isFlying = flightPlayer.getFlightState() != FlightState.NONE;
         }

         boolean shouldDraw = corePlayer.isSuperSpeed() && !isFlying;
         SilhouetteManager.State state = SilhouetteManager.getState(livingEntity, shouldDraw);
         if (state.alphaWeight < 0.01F) {
            return;
         }

         EntityRenderer<T> renderer = (EntityRenderer<T>)this;
         ResourceLocation texture = renderer.getTextureLocation(livingEntity);
         RenderType translucentLayer = RenderType.entityTranslucent(texture);
         VertexConsumer consumer = buffer.getBuffer(translucentLayer);
         float lerpedBodyYaw = Mth.lerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);

         for (int j = 3; j >= 1; j--) {
            poseStack.pushPose();
            Vec3 worldOffset = state.currentTrailOffset.scale((double)(-j));
            float yawRad = (lerpedBodyYaw - 180.0F) * (float) (Math.PI / 180.0);
            double cos = Math.cos((double)yawRad);
            double sin = Math.sin((double)yawRad);
            double rx = worldOffset.x * cos + worldOffset.z * sin;
            double rz = -worldOffset.x * sin + worldOffset.z * cos;
            double localX = -rx;
            double localY = -worldOffset.y;
            poseStack.translate(localX, localY, rz);
            poseStack.scale(0.97F, 0.97F, 0.97F);
            float gray = 1.0F;
            float alpha = 0.6F / (float)j * state.alphaWeight;
            this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, gray, gray, gray, alpha);
            poseStack.popPose();
         }
      }
   }
}
