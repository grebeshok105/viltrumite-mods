package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Veronica card preview of the Mark 48 in its rest pose (feet at the origin, geo y up, the GUI y down). */
public final class HulkbusterPreview {
   /** Screen pixels per block; the body is 2 blocks tall. */
   private static final float SCALE = 17.0F;

   private HulkbusterPreview() {
   }

   public static void draw(PoseStack pose, MultiBufferSource buffers, float yawDegrees, float shade) {
      BakedGeoModel body = AnimCache.model(HulkbusterAssets.BODY);
      Animation idle = AnimCache.animation(HulkbusterAssets.ANIMATIONS, HulkbusterPoses.Clip.IDLE.animation());
      if (body == null || idle == null) {
         return;
      }

      AnimationController.seek(idle, body, 0.0);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(yawDegrees));
      pose.scale(-SCALE, -SCALE, SCALE);
      AnimRenderer.render(body, pose, null, buffers.getBuffer(RenderType.entityCutoutNoCull(HulkbusterAssets.TEXTURE)), LightTexture.FULL_BRIGHT,
         OverlayTexture.NO_OVERLAY, shade, shade, shade, 1.0F, null);
      AnimRenderer.render(body, pose, null, buffers.getBuffer(RenderType.eyes(HulkbusterAssets.GLOW)), LightTexture.FULL_BRIGHT,
         OverlayTexture.NO_OVERLAY, shade, shade, shade, 1.0F, null);
      pose.popPose();
   }
}
