package dev.baranhan.viltrumiteflight.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumiteflight.client.ViltrumiteFlightClient;
import dev.baranhan.viltrumiteflight.client.render.FlightAnimManager;
import dev.baranhan.viltrumiteflight.config.ViltrumiteFlightCameraConfig;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class FlightCameraMixin {
   @Unique
   private float smoothedCameraRoll = 0.0F;
   @Unique
   private float lastCameraYaw = 0.0F;
   @Unique
   private float independentTurnSpeed = 0.0F;
   @Unique
   private long lastRenderTimeMs = 0L;

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
         shift = Shift.AFTER
      )}
   )
   private void applyFlightCameraRoll(float partialTicks, long nanoTime, PoseStack poseStack, CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getInstance();
      if (ViltrumiteFlightCameraConfig.INSTANCE.cameraRoll && minecraft.player != null) {
         if (minecraft.player instanceof ViltrumiteFlightPlayer omniPlayer) {
            FlightAnimManager.AnimState state = FlightAnimManager.getState(minecraft.player.getUUID());
            long currentTime = Util.getMillis();
            float deltaSeconds = this.lastRenderTimeMs == 0L ? 0.05F : (float)(currentTime - this.lastRenderTimeMs) / 1000.0F;
            if (deltaSeconds > 0.1F) {
               deltaSeconds = 0.1F;
            }

            this.lastRenderTimeMs = currentTime;
            if (omniPlayer.getFlightState() != FlightState.NONE) {
               float currentYaw = Mth.lerp(partialTicks, minecraft.player.yRotO, minecraft.player.getYRot());
               float yawDelta = Mth.wrapDegrees(currentYaw - this.lastCameraYaw);
               this.lastCameraYaw = currentYaw;
               float turnSpeed = deltaSeconds > 0.0F ? yawDelta / deltaSeconds : 0.0F;
               this.independentTurnSpeed = Mth.lerp(10.0F * deltaSeconds, this.independentTurnSpeed, turnSpeed);
               float maxCameraRoll = ViltrumiteFlightCameraConfig.INSTANCE.maxCameraRoll;
               float targetRoll = Mth.clamp(
                  this.independentTurnSpeed * ViltrumiteFlightCameraConfig.INSTANCE.cameraRollMultiplier * omniPlayer.getFlightThrottle(),
                  -maxCameraRoll,
                  maxCameraRoll
               );
               float smoothFactor = 1.0F - (float)Math.exp((double)(-ViltrumiteFlightCameraConfig.INSTANCE.cameraRollRoughness * deltaSeconds));
               this.smoothedCameraRoll = Mth.lerp(smoothFactor, this.smoothedCameraRoll, targetRoll);
               ViltrumiteFlightClient.currentCameraRoll = this.smoothedCameraRoll;
               poseStack.mulPose(Axis.ZP.rotationDegrees(this.smoothedCameraRoll));
            } else {
               float smoothFactor = 1.0F - (float)Math.exp((double)(-15.0F * deltaSeconds));
               this.smoothedCameraRoll = Mth.lerp(smoothFactor, this.smoothedCameraRoll, 0.0F);
               this.lastCameraYaw = Mth.lerp(partialTicks, minecraft.player.yRotO, minecraft.player.getYRot());
               if (Math.abs(this.smoothedCameraRoll) > 0.1F) {
                  poseStack.mulPose(Axis.ZP.rotationDegrees(this.smoothedCameraRoll));
               }
            }
         }
      }
   }
}
