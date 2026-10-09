package dev.baranhan.viltrumitecore.client.ironman.mark;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.AnimCache;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationController;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.AnimRenderer;
import dev.baranhan.viltrumitecore.entity.EmptySuitEntity;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The empty suit (spec §12.6): a player-shaped shell in the mark skin with the
 * interior texture inside. The "open" animation (30 ticks) opens the plates and
 * tilts the helmet back while Tony steps out or walks in. Standing, the eye glow
 * shimmers. Geo in the body frame, drawn at player scale.
 */
public final class EmptySuitRenderer extends EntityRenderer<EmptySuitEntity> {
   private static final ResourceLocation SHELL = new ResourceLocation("viltrumitecore", "geo/ironman/marks/empty_suit.geo.json");
   private static final ResourceLocation INNER = new ResourceLocation("viltrumitecore", "geo/ironman/marks/empty_suit_interior.geo.json");
   private static final ResourceLocation ANIMS = new ResourceLocation("viltrumitecore", "animations/ironman/empty_suit.animation.json");
   private static final float BODY_SCALE = 0.9375F;
   private static final WeakHashMap<EmptySuitEntity, Drive> DRIVES = new WeakHashMap<>();

   public EmptySuitRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(EmptySuitEntity suit, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
      BakedGeoModel shell = AnimCache.model(SHELL);
      BakedGeoModel inner = AnimCache.model(INNER);
      if (shell == null || inner == null) {
         return;
      }

      MarkId mark = suit.mark() == null ? MarkId.MARK_7 : suit.mark();
      Drive drive = DRIVES.computeIfAbsent(suit, s -> new Drive());
      drive.update(suit.phase());
      drive.apply(shell, inner, partialTick);
      float glowPulse = 0.85F + 0.15F * Mth.sin((suit.tickCount + partialTick) * 0.15F);
      stack.pushPose();
      stack.mulPose(Axis.YP.rotationDegrees(180.0F - suit.getYRot()));
      stack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
      AnimRenderer.render(shell, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(MarkTextures.skin(mark))), light, OverlayTexture.NO_OVERLAY,
         1.0F, 1.0F, 1.0F, 1.0F, null);
      AnimRenderer.render(inner, stack, null, buffers.getBuffer(RenderType.entityCutoutNoCull(MarkTextures.INTERIOR)), light, OverlayTexture.NO_OVERLAY,
         1.0F, 1.0F, 1.0F, 1.0F, null);
      AnimRenderer.render(shell, stack, null, buffers.getBuffer(RenderType.eyes(MarkTextures.glow(mark))), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
         glowPulse, glowPulse, glowPulse, 1.0F, null);
      stack.popPose();
      super.render(suit, entityYaw, partialTick, stack, buffers, light);
   }

   @Override
   public ResourceLocation getTextureLocation(EmptySuitEntity suit) {
      return MarkTextures.skin(suit.mark() == null ? MarkId.MARK_7 : suit.mark());
   }

   /** Plays the open clip on both models from the entity's phase start. */
   private static final class Drive {
      private final AnimationController shell = new AnimationController("empty_suit_shell");
      private final AnimationController inner = new AnimationController("empty_suit_inner");
      private EmptySuitEntity.Phase last;

      void update(EmptySuitEntity.Phase phase) {
         if (phase == this.last) {
            return;
         }

         this.last = phase;
         if (phase == EmptySuitEntity.Phase.OPENING || phase == EmptySuitEntity.Phase.ENTERING) {
            Animation open = AnimCache.animation(ANIMS, "open");
            this.shell.restart(open);
            this.inner.restart(open);
         } else {
            this.shell.stop();
            this.inner.stop();
         }
      }

      void apply(BakedGeoModel shell, BakedGeoModel inner, float partialTick) {
         double time = AnimRenderer.time(partialTick);
         this.shell.apply(shell, time);
         this.inner.apply(inner, time);
      }
   }
}
