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
   priority = 1520
)
public class ChopRendererCoreMixin2 {
   @Inject(
      method = {"setupRotations"},
      at = {@At("TAIL")}
   )
   private void onSetupChopTransforms2(
      AbstractClientPlayer player, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         int chopTicks = corePlayer.getChopTicks();
         if (chopTicks > 0) {
            if (corePlayer.getChopType() == 1) {
               boolean isFlying = false;
               if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
                  isFlying = flightPlayer.getFlightState() != FlightState.NONE;
               }

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
               float lookPitch = player.getViewXRot(partialTicks);
               float pivotY = 0.8F;
               poseStack.translate(0.0F, pivotY, 0.0F);
               if (isFlying) {
                  float aimPitch = -lookPitch * 0.4F * Math.abs(lungeMultiplier);
                  poseStack.mulPose(Axis.XP.rotationDegrees(aimPitch));
               }

               float spinYaw = 0.0F;
               if (time < 0.25F) {
                  float localT = time / 0.25F;
                  spinYaw = Mth.lerp(localT, 0.0F, 45.0F);
               } else if (time < 0.44F) {
                  float localT = (time - 0.25F) / 0.19F;
                  spinYaw = Mth.lerp(localT, 45.0F, -360.0F);
               } else {
                  spinYaw = -360.0F;
               }

               poseStack.mulPose(Axis.YP.rotationDegrees(spinYaw * mirror));
               float chopRoll = -5.0F * lungeMultiplier * mirror;
               poseStack.mulPose(Axis.ZP.rotationDegrees(chopRoll));
               poseStack.translate(0.0F, -pivotY, 0.0F);
               if (isFlying) {
                  float pitchRad = lookPitch * (float) (Math.PI / 180.0);
                  float pitchOffset = Mth.sin(pitchRad);
                  float baseZ = -0.1F;
                  float moveZ = (baseZ + pitchOffset * 0.1F) * lungeMultiplier;
                  float moveY = -pitchOffset * 0.03F * lungeMultiplier;
                  poseStack.translate(0.0F, moveY, moveZ);
               }
            }
         }
      }
   }
}
