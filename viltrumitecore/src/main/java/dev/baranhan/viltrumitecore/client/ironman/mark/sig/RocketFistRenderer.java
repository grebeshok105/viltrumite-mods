package dev.baranhan.viltrumitecore.client.ironman.mark.sig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.entity.RocketFistEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Mark 42 glove in flight: the fist model, yawed along its flight direction. */
public final class RocketFistRenderer extends EntityRenderer<RocketFistEntity> {
   private static final ResourceLocation MODEL = new ResourceLocation("viltrumitecore", "geo/ironman/marks/fist.geo.json");
   private static final ResourceLocation TEXTURE = new ResourceLocation("viltrumitecore", "textures/entity/ironman/marks/glove.png");

   public RocketFistRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public ResourceLocation getTextureLocation(RocketFistEntity entity) {
      return TEXTURE;
   }

   @Override
   public void render(RocketFistEntity fist, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
      BakedGeoModel model = AnimCache.model(MODEL);
      if (model == null) {
         return;
      }

      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(fist.getYRot() + 180.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(fist.getXRot()));
      VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
      AnimRenderer.render(model, poseStack, vertices, light, OverlayTexture.NO_OVERLAY);
      poseStack.popPose();
   }
}
