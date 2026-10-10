package dev.baranhan.viltrumitecore.client.ironman.veronica;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.client.ironman.mark.MarkTextures;
import dev.baranhan.viltrumitecore.entity.VeronicaPodEntity;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Veronica module (spec §12.1–§12.2), after the Age of Ultron stills: twin hulls
 * swing open on hinges once it lands and close as it leaves; core and thrusters glow.
 * The fire trail and heat aura
 * are drawn by {@link dev.baranhan.viltrumitecore.client.ironman.mark.MarkVfx}.
 */
public final class VeronicaPodRenderer extends EntityRenderer<VeronicaPodEntity> {
   private static final ResourceLocation GEO = new ResourceLocation("viltrumitecore", "geo/ironman/veronica/veronica_pod.geo.json");
   private static final ResourceLocation ANIMS = new ResourceLocation("viltrumitecore", "animations/ironman/veronica_pod.animation.json");
   private static final WeakHashMap<VeronicaPodEntity, Drive> DRIVES = new WeakHashMap<>();

   public VeronicaPodRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(VeronicaPodEntity pod, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
      BakedGeoModel model = AnimCache.model(GEO);
      if (model == null) {
         return;
      }

      Drive drive = DRIVES.computeIfAbsent(pod, p -> new Drive());
      drive.update(pod.phase());
      drive.apply(model, partialTick);
      AnimRenderer.render(model, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(MarkTextures.POD)), light, OverlayTexture.NO_OVERLAY,
         1.0F, 1.0F, 1.0F, 1.0F, null);
      // Core, running lights and thrusters glow (own map, black elsewhere).
      AnimRenderer.render(model, stack, null, buffers.getBuffer(RenderType.eyes(MarkTextures.POD_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
         1.0F, 1.0F, 1.0F, 1.0F, null);
      super.render(pod, entityYaw, partialTick, stack, buffers, light);
   }

   @Override
   public ResourceLocation getTextureLocation(VeronicaPodEntity pod) {
      return MarkTextures.POD;
   }

   private static final class Drive {
      private final AnimationController doors = new AnimationController("pod_doors");
      private VeronicaPodEntity.Phase last;

      void update(VeronicaPodEntity.Phase phase) {
         if (phase == this.last) {
            return;
         }

         this.last = phase;
         if (phase == VeronicaPodEntity.Phase.LANDED) {
            this.doors.restart(AnimCache.animation(ANIMS, "open"));
         } else if (phase == VeronicaPodEntity.Phase.LEAVING) {
            this.doors.restart(AnimCache.animation(ANIMS, "close"));
         } else {
            this.doors.stop();
         }
      }

      void apply(BakedGeoModel model, float partialTick) {
         this.doors.apply(model, AnimRenderer.time(partialTick));
      }
   }
}
