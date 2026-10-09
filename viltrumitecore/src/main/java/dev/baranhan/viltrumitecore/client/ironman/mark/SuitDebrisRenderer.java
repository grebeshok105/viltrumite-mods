package dev.baranhan.viltrumitecore.client.ironman.mark;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.entity.SuitDebrisEntity;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A broken plate of a mark (spec §4.3): the part's slice in the mark skin,
 * tumbling. In the last 20 of {@link SuitDebrisEntity#LIFE} ticks it shrinks
 * and fades out.
 */
public final class SuitDebrisRenderer extends EntityRenderer<SuitDebrisEntity> {
   private static final int FADE_TICKS = 20;

   public SuitDebrisRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(SuitDebrisEntity debris, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
      MarkId mark = debris.mark() == null ? MarkId.MARK_7 : debris.mark();
      List<SuitPart> parts = SuitPart.of(mark);
      int index = Math.max(0, Math.min(parts.size() - 1, debris.part()));
      BakedGeoModel geo = AnimCache.model(PlateParts.geo(parts.get(index)));
      if (geo == null) {
         return;
      }

      float age = debris.tickCount + partialTick;
      float remaining = Mth.clamp((SuitDebrisEntity.LIFE - age) / FADE_TICKS, 0.0F, 1.0F);
      float scale = Math.max(0.02F, remaining);
      geo.resetBones();
      Vec3 centre = PlateParts.centre(geo);
      stack.pushPose();
      stack.mulPose(Axis.YP.rotationDegrees(age * 9.0F + index * 40.0F));
      stack.mulPose(Axis.XP.rotationDegrees(age * 5.0F));
      stack.scale(scale, scale, scale);
      stack.translate(-centre.x, -centre.y, -centre.z);
      AnimRenderer.render(geo, stack, null, buffers.getBuffer(RenderType.entityTranslucentCull(MarkTextures.skin(mark))), LightTexture.FULL_BRIGHT,
         OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, remaining, null);
      stack.popPose();
      super.render(debris, entityYaw, partialTick, stack, buffers, light);
   }

   @Override
   public ResourceLocation getTextureLocation(SuitDebrisEntity debris) {
      return MarkTextures.skin(debris.mark() == null ? MarkId.MARK_7 : debris.mark());
   }
}
