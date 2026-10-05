package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class GrabbedEntityRendererMixin<T extends LivingEntity> {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")}
   )
   private void onRenderAnimationFix(
      T entity, float entityYaw, float partialTicks, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci
   ) {
      if (entity.level() != null && entity.level().isClientSide()) {
         Player holdingPlayer = this.getHoldingPlayer(entity);
         if (holdingPlayer != null) {
            float playerLerpedYaw = Mth.lerp(partialTicks, holdingPlayer.yRotO, holdingPlayer.getYRot());
            float targetYaw = playerLerpedYaw + 180.0F;
            entity.setYRot(targetYaw);
            entity.yRotO = targetYaw;
            entity.yBodyRot = targetYaw;
            entity.yBodyRotO = targetYaw;
            entity.yHeadRot = targetYaw;
            entity.yHeadRotO = targetYaw;
            if (entity.walkAnimation != null) {
               entity.walkAnimation.setSpeed(0.0F);
            }

            if (entity instanceof Warden warden) {
               warden.sonicBoomAnimationState.stop();
               warden.roarAnimationState.stop();
               warden.attackAnimationState.stop();
               warden.sniffAnimationState.stop();
               warden.emergeAnimationState.stop();
               warden.diggingAnimationState.stop();
            }
         }
      }
   }

   @Inject(
      method = {"setupRotations"},
      at = {@At("TAIL")}
   )
   private void onSetupTransforms(T entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
      if (entity.level() != null && entity.level().isClientSide()) {
         Player holdingPlayer = this.getHoldingPlayer(entity);
         if (holdingPlayer != null) {
            Minecraft client = Minecraft.getInstance();
            float grabHeight = entity.getEyeHeight() * 0.9F;
            float playerPitch = Mth.lerp(tickDelta, holdingPlayer.xRotO, holdingPlayer.getXRot());
            if (holdingPlayer instanceof ViltrumiteFlightPlayer flightPlayer && !client.options.getCameraType().isFirstPerson()) {
               float throttle = flightPlayer.getLerpedFlightThrottle(tickDelta);
               matrices.mulPose(Axis.XP.rotationDegrees(throttle * 40.0F));
            }

            matrices.mulPose(Axis.XP.rotationDegrees(playerPitch));
            if (holdingPlayer == client.player && client.options.getCameraType().isFirstPerson()) {
               matrices.scale(1.4F, 1.4F, 1.4F);
            }

            matrices.translate(0.0, (double)(-grabHeight), 0.0);
         }
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"
      )
   )
   private void stopLimbFlailing(EntityModel model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      if (!(entity instanceof LivingEntity living) || this.getHoldingPlayer((T)living) == null) {
         model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
         return;
      }

      if (entity instanceof Player) {
         model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      } else {
         model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
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
}
