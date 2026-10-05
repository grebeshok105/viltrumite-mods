package dev.baranhan.viltrumiteflight.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CapeLayer.class})
public class CapeHeatFeatureMixin {
   private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

   @Inject(
      method = {"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
      )}
   )
   private void renderGlowingCapeShell(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
         if (ViltrumiteConfig.INSTANCE.isHeatEnabled) {
            int flightTicks = flightPlayer.getFlightTicks();
            if (flightTicks > 200) {
               float progress = (float)(flightTicks - 200) / 200.0F;
               progress = Mth.clamp(progress, 0.0F, 1.0F);
               float time = (float)player.tickCount + partialTicks;
               float maxR = 1.0F;
               float maxG = Mth.lerp(progress, 0.0F, 0.8F);
               float maxB = Mth.lerp(progress, 0.0F, 0.2F);
               float baseIntensity = Mth.lerp(progress, 0.0F, 1.0F);
               int glowingLight = 15728880;
               RenderType renderType = RenderType.energySwirl(WHITE_TEXTURE, time * 0.015F, time * 0.015F);
               VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
               RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer = (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)this;
               ModelPart cloakPart = ((PlayerModelAccessor)renderer.getParentModel()).getCloak();
               if (cloakPart != null) {
                  for (int i = 0; i < 3; i++) {
                     poseStack.pushPose();
                     float scale = 1.0625F;
                     float jitter = (float)Math.sin((double)(time * 5.0F + (float)i)) * 0.005F * progress;
                     scale += jitter;
                     poseStack.scale(scale, scale, scale);
                     float intensity = baseIntensity * (1.0F / (float)(i + 1));
                     float r = maxR * intensity;
                     float g = maxG * intensity;
                     float b = maxB * intensity;
                     cloakPart.render(poseStack, vertexConsumer, glowingLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
                     poseStack.popPose();
                  }
               }
            }
         }
      }
   }
}
