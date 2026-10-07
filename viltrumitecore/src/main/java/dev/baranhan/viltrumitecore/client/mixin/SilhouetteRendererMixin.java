package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.client.render.animation.SilhouetteManager;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
      // Carriers are marked by a small beating heart inside the body (RegulusActionVFXManager), not a silhouette.
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

         EntityRenderer<T> renderer = (EntityRenderer<T>)(Object)this;
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

   /**
    * Regulus heart carriers get a red body silhouette the owning player sees
    * through walls (spec 5.4): the vanilla glowing path — OutlineBufferSource
    * draws the model solid into the depth-test-off outline target. The
    * carrier id list is owner-private, so the shell only exists for the
    * Regulus who bound the heart; SilhouetteManager fades it in and out.
    */
   @Unique
   private void renderCarrierSilhouette(T livingEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      // Carriers are mobs (isValidCarrier excludes players); skipping players
      // here also keeps the single-frame getState delta for the speed trail.
      if (livingEntity instanceof Player) {
         return;
      }
      boolean isCarrier = this.regulus$isCarrier(livingEntity);
      SilhouetteManager.State state = SilhouetteManager.getState(livingEntity, isCarrier);
      if (!isCarrier || state.alphaWeight < 0.01F) {
         return;
      }

      EntityRenderer<T> renderer = (EntityRenderer<T>)(Object)this;
      OutlineBufferSource outline = Minecraft.getInstance().renderBuffers().outlineBufferSource();
      outline.setColor(191, 34, 26, (int)(210.0F * Math.max(0.35F, state.alphaWeight)));
      VertexConsumer consumer = outline.getBuffer(RenderType.outline(renderer.getTextureLocation(livingEntity)));
      poseStack.pushPose();
      poseStack.scale(1.06F, 1.06F, 1.06F);
      this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   @Unique
   private boolean regulus$isCarrier(T livingEntity) {
      Minecraft client = Minecraft.getInstance();
      if (!(client.player instanceof HeroPlayer heroPlayer)) {
         return false;
      }
      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
         return false;
      }

      int id = livingEntity.getId();
      for (int carrierId : ClientHeroData.carriers()) {
         if (carrierId == id) {
            return true;
         }
      }
      return false;
   }

}
