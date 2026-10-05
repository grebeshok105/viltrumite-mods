package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class GrabbedEntityRendererMixin2<T extends Entity> {
   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void forceRenderWhenGrabbed(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (entity.level() != null && entity.level().isClientSide) {
         if (entity instanceof LivingEntity) {
            Player holdingPlayer = this.getHoldingPlayer(entity);
            if (holdingPlayer != null) {
               if (holdingPlayer != Minecraft.getInstance().player && !frustum.isVisible(holdingPlayer.getBoundingBox())) {
                  cir.setReturnValue(false);
               } else {
                  cir.setReturnValue(true);
               }
            }
         }
      }
   }

   private Player getHoldingPlayer(T entity) {
      for (Player player : entity.level().players()) {
         if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.getGrabbedTarget() == entity) {
            return player;
         }
      }

      return null;
   }

   @Inject(
      method = {"renderNameTag"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void fixGrabbedNametagSpace(T entity, Component component, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
      if (entity.level() != null && entity.level().isClientSide) {
         Player holdingPlayer = this.getHoldingPlayer(entity);
         if (holdingPlayer != null) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean isFirstPerson = holdingPlayer == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
            if (isFirstPerson) {
               ci.cancel();
               return;
            }

            float partialTicks = minecraft.getFrameTime();
            float grabHeight = entity.getEyeHeight() * 0.9F;
            poseStack.translate(0.0, (double)grabHeight, 0.0);
            if (holdingPlayer instanceof ViltrumiteFlightPlayer flightPlayer) {
               float throttle = flightPlayer.getLerpedFlightThrottle(partialTicks);
               poseStack.mulPose(Axis.XN.rotationDegrees(throttle * 40.0F));
            }

            float playerPitch = Mth.lerp(partialTicks, holdingPlayer.xRotO, holdingPlayer.getXRot());
            poseStack.mulPose(Axis.XN.rotationDegrees(playerPitch));
            float playerYaw = Mth.lerp(partialTicks, holdingPlayer.yRotO, holdingPlayer.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(playerYaw));
            poseStack.translate(0.0, (double)(-entity.getBbHeight()) + 0.2, 0.0);
         }
      }
   }
}
