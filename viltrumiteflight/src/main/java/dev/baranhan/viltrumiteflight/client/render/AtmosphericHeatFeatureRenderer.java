package dev.baranhan.viltrumiteflight.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AtmosphericHeatFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");
   private static final ResourceLocation GLOWING_CAPE_TEXTURE = new ResourceLocation("viltrumiteflight", "textures/entity/slow_flying_cape.png");

   public AtmosphericHeatFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
      super(context);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
         int flightTicks = flightPlayer.getFlightTicks();
         if (ViltrumiteConfig.INSTANCE.isHeatEnabled) {
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
               poseStack.pushPose();
               poseStack.translate(0.0, 0.0225F, 0.0);

               for (int i = 0; i < 3; i++) {
                  poseStack.pushPose();
                  float scale = 1.0625F;
                  float jitter = (float)Math.sin((double)(time * 5.0F + (float)i)) * 0.005F * progress;
                  scale += jitter;
                  poseStack.translate(0.0, 1.0, 0.0);
                  poseStack.scale(scale, scale, scale);
                  poseStack.translate(0.0, -1.0, 0.0);
                  float intensity = baseIntensity * (1.0F / (float)(i + 1));
                  float r = maxR * intensity;
                  float g = maxG * intensity;
                  float b = maxB * intensity;
                  ((PlayerModel)this.getParentModel()).renderToBuffer(poseStack, vertexConsumer, glowingLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
                  poseStack.popPose();
               }

               poseStack.popPose();
            }
         }
      }
   }
}
