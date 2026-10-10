package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.entity.MicroMissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Micro-missile body (Sind arm rocket, 2x) nose along the flight; the smoke trail and flame stay in IronManCombatVfx. */
public final class MissileRenderer extends EntityRenderer<MicroMissileEntity> {
   private static final ResourceLocation MODEL = new ResourceLocation("viltrumitecore", "geo/ironman/missiles/missile.geo.json");
   private static final ResourceLocation TEXTURE = new ResourceLocation("viltrumitecore", "textures/entity/ironman/missiles/rocket.png");

   public MissileRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(MicroMissileEntity missile, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
      BakedGeoModel model = AnimCache.model(MODEL);
      Vec3 v = missile.getDeltaMovement();
      if (model == null || v.lengthSqr() < 1.0E-8) {
         return;
      }

      Vec3 d = v.normalize();
      // Model nose is -z: pitch it up by asin(dy), then yaw it onto the horizontal direction.
      stack.pushPose();
      stack.mulPose(Axis.YP.rotation((float)Mth.atan2(-d.x, -d.z)));
      stack.mulPose(Axis.XP.rotation((float)Math.asin(Mth.clamp(d.y, -1.0, 1.0))));
      model.resetBones();
      AnimRenderer.render(model, stack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
      stack.popPose();
      super.render(missile, yaw, partialTick, stack, buffers, light);
   }

   @Override
   public ResourceLocation getTextureLocation(MicroMissileEntity missile) {
      return TEXTURE;
   }
}
