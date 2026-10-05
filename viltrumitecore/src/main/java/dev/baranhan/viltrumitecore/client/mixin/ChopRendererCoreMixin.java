package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerRenderer.class},
   priority = 1510
)
public class ChopRendererCoreMixin {
   @Inject(
      method = {"setupRotations"},
      at = {@At("TAIL")}
   )
   private void onSetupChopTransforms(
      AbstractClientPlayer player, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         int chopTicks = corePlayer.getChopTicks();
         if (chopTicks > 0) {
            boolean isFlying = false;
            if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
               isFlying = flightPlayer.getFlightState() != FlightState.NONE;
            }

            if (isFlying) {
               float exactTicks = (float)chopTicks - partialTicks;
               float time = (20.0F - exactTicks) / 20.0F;
               time = Mth.clamp(time, 0.0F, 1.0F);
               float lungeMultiplier = 0.0F;
               if (time < 0.25F) {
                  float localT = time / 0.25F;
                  lungeMultiplier = Mth.lerp(localT, 0.0F, -0.3F);
               } else if (time < 0.35F) {
                  float localT = (time - 0.25F) / 0.1F;
                  lungeMultiplier = Mth.lerp(localT, -0.3F, 1.0F);
               } else if (time < 0.65F) {
                  lungeMultiplier = 1.0F;
               } else if (time < 0.85F) {
                  float localT = (time - 0.65F) / 0.2F;
                  lungeMultiplier = Mth.lerp(localT, 1.0F, 0.0F);
               } else {
                  lungeMultiplier = 0.0F;
               }

               float mirror = corePlayer.isLeftChop() ? -1.0F : 1.0F;
               float pivotY = 0.7F;
               poseStack.translate(0.0F, pivotY, 0.0F);
               float lookPitch = player.getViewXRot(partialTicks);
               float aimPitch = -lookPitch * 0.45F * Math.abs(lungeMultiplier);
               poseStack.mulPose(Axis.XP.rotationDegrees(aimPitch));
               float chopYaw = 10.0F * lungeMultiplier * mirror;
               poseStack.mulPose(Axis.YP.rotationDegrees(chopYaw));
               float chopRoll = -8.0F * lungeMultiplier * mirror;
               poseStack.mulPose(Axis.ZP.rotationDegrees(chopRoll));
               poseStack.translate(0.0F, -pivotY, 0.0F);
               float pitchRad = lookPitch * (float) (Math.PI / 180.0);
               float pitchOffset = Mth.sin(pitchRad);
               float baseZ = -0.5F;
               float moveZ = (baseZ + pitchOffset * 0.7F) * lungeMultiplier;
               float moveY = -pitchOffset * 0.3F * lungeMultiplier;
               float moveX = -0.07F * lungeMultiplier * mirror;
               poseStack.translate(moveX, moveY, moveZ);
            }
         }
      }
   }
}
