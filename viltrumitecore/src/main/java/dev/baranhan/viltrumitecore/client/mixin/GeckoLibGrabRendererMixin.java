package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin({GeoEntityRenderer.class})
public abstract class GeckoLibGrabRendererMixin {
   @Inject(
      method = {"render", "render"},
      at = {@At("HEAD")},
      remap = false,
      require = 1
   )
   private void onGeckoRenderFix(
      Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (entity instanceof LivingEntity livingEntity) {
         if (livingEntity.level() != null && livingEntity.level().isClientSide) {
            Player holdingPlayer = this.getHoldingPlayer(livingEntity);
            if (holdingPlayer != null) {
               float playerLerpedYaw = Mth.lerp(partialTicks, holdingPlayer.yRotO, holdingPlayer.getYRot());
               float targetYaw = playerLerpedYaw + 180.0F;
               livingEntity.setYRot(targetYaw);
               livingEntity.yRotO = targetYaw;
               livingEntity.yBodyRot = targetYaw;
               livingEntity.yBodyRotO = targetYaw;
               livingEntity.yHeadRot = targetYaw;
               livingEntity.yHeadRotO = targetYaw;
               Vec3 handPos = null;
               Minecraft minecraft = Minecraft.getInstance();
               boolean isFirstPerson = holdingPlayer == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
               if (isFirstPerson) {
                  Vec3 localHandPos = ((ViltrumiteCorePlayer)holdingPlayer).getFirstPersonLocalHandPos();
                  if (localHandPos != null) {
                     Camera camera = minecraft.gameRenderer.getMainCamera();
                     Vector3f localRotator = new Vector3f((float)localHandPos.x, (float)localHandPos.y, (float)localHandPos.z);
                     localRotator.rotateX((float)Math.toRadians((double)(-camera.getXRot())));
                     localRotator.rotateY((float)Math.toRadians((double)(-(camera.getYRot() + 180.0F))));
                     handPos = camera.getPosition().add((double)localRotator.x(), (double)localRotator.y(), (double)localRotator.z());
                  }
               } else {
                  Vec3 handOffset = ((ViltrumiteCorePlayer)holdingPlayer).getCalculatedHandOffset();
                  if (handOffset != null) {
                     double pLerpX = Mth.lerp((double)partialTicks, holdingPlayer.xo, holdingPlayer.getX());
                     double pLerpY = Mth.lerp((double)partialTicks, holdingPlayer.yo, holdingPlayer.getY());
                     double pLerpZ = Mth.lerp((double)partialTicks, holdingPlayer.zo, holdingPlayer.getZ());
                     Vec3 currentBodyPos = new Vec3(pLerpX, pLerpY, pLerpZ);
                     handPos = currentBodyPos.add(handOffset);
                  } else {
                     handPos = ((ViltrumiteCorePlayer)holdingPlayer).getCalculatedHandPos();
                  }
               }

               if (handPos != null) {
                  Vec3 lookDir = holdingPlayer.getViewVector(partialTicks).normalize();
                  float pushForce = isFirstPerson ? 0.0F : livingEntity.getBbWidth() * 0.6F;
                  Vec3 adjustedHandPos = handPos.add(lookDir.scale((double)pushForce));
                  double renderX = Mth.lerp((double)partialTicks, livingEntity.xo, livingEntity.getX());
                  double renderY = Mth.lerp((double)partialTicks, livingEntity.yo, livingEntity.getY());
                  double renderZ = Mth.lerp((double)partialTicks, livingEntity.zo, livingEntity.getZ());
                  double offsetX = adjustedHandPos.x - renderX;
                  double offsetY = adjustedHandPos.y - renderY;
                  double offsetZ = adjustedHandPos.z - renderZ;
                  poseStack.translate(offsetX, offsetY, offsetZ);
                  float playerPitch = Mth.lerp(partialTicks, holdingPlayer.xRotO, holdingPlayer.getXRot());
                  if (holdingPlayer instanceof ViltrumiteFlightPlayer flightPlayer && !isFirstPerson) {
                     float throttle = flightPlayer.getLerpedFlightThrottle(partialTicks);
                     poseStack.mulPose(Axis.XP.rotationDegrees(throttle * 40.0F));
                  }

                  poseStack.mulPose(Axis.XP.rotationDegrees(playerPitch));
                  float grabHeight = livingEntity.getBbHeight() * 0.85F;
                  if (isFirstPerson) {
                     poseStack.scale(1.4F, 1.4F, 1.4F);
                  }

                  poseStack.translate(0.0, (double)(-grabHeight), 0.0);
               }
            }
         }
      }
   }

   private Player getHoldingPlayer(LivingEntity entity) {
      for (Player player : entity.level().players()) {
         if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.getGrabbedTarget() == entity) {
            return player;
         }
      }

      return null;
   }
}
