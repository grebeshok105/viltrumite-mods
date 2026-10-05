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
   priority = 1515
)
public class BarrageRendererCoreMixin {
   @Inject(
      method = {"setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"},
      at = {@At("TAIL")}
   )
   private void onSetupBarrageTransforms(
      AbstractClientPlayer player, PoseStack matrices, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         int barrageTicks = corePlayer.getBarrageTicks();
         if (barrageTicks != 0) {
            boolean isFlying = false;
            if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
               isFlying = flightPlayer.getFlightState() != FlightState.NONE;
            }

            if (isFlying) {
               float lungeMultiplier = 0.0F;
               if (barrageTicks > 0) {
                  float exactTime = (float)(barrageTicks - 1) + partialTicks;
                  if (exactTime < 0.0F) {
                     exactTime = 0.0F;
                  }

                  if (exactTime < 5.0F) {
                     lungeMultiplier = exactTime / 5.0F;
                  } else {
                     lungeMultiplier = 1.0F;
                  }
               } else {
                  float exactTimex = (float)barrageTicks + partialTicks;
                  float timeSinceRelease = 9.0F + exactTimex;
                  if (timeSinceRelease < 4.0F) {
                     lungeMultiplier = 1.0F;
                  } else {
                     lungeMultiplier = 1.0F - (timeSinceRelease - 4.0F) / 5.0F;
                  }
               }

               lungeMultiplier = Mth.clamp(lungeMultiplier, 0.0F, 1.0F);
               float pivotY = 0.7F;
               matrices.translate(0.0F, pivotY, 0.0F);
               float lookPitch = player.getViewXRot(partialTicks);
               float aimPitch = -lookPitch * 0.35F * lungeMultiplier;
               matrices.mulPose(Axis.XP.rotationDegrees(aimPitch));
               matrices.translate(0.0F, -pivotY, 0.0F);
               float pitchRad = lookPitch * (float) (Math.PI / 180.0);
               float pitchOffset = Mth.sin(pitchRad);
               float moveZ = (-0.2F + pitchOffset * 0.2F) * lungeMultiplier;
               float moveY = -pitchOffset * 0.1F * lungeMultiplier;
               matrices.translate(0.0F, moveY, moveZ);
            }
         }
      }
   }
}
