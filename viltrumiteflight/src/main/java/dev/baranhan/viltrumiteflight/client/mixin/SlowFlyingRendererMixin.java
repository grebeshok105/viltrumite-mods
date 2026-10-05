package dev.baranhan.viltrumiteflight.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumiteflight.client.render.FlightAnimManager;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfigClient;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public class SlowFlyingRendererMixin {
   @Inject(
      method = {"setupRotations"},
      at = {@At("TAIL")}
   )
   private void onSetupTransforms(AbstractClientPlayer player, PoseStack poseStack, float animationProgress, float bodyYaw, float partialTicks, CallbackInfo ci) {
      if (player instanceof ViltrumiteFlightPlayer omniPlayer) {
         boolean isGui = RenderSystem.getProjectionMatrix().m33() > 0.5F && !ShaderCompat.isShadowPass();
         FlightAnimManager.AnimState state = FlightAnimManager.getState(player.getUUID());
         boolean isFlying = omniPlayer.getFlightState() != FlightState.NONE;
         float currentAge = (float)player.tickCount + partialTicks;
         boolean frameGapDetected = false;
         float rawDelta;
         if (!(state.lastAge < 0.0F) && !(currentAge < state.lastAge) && !(currentAge > state.lastAge + 20.0F)) {
            rawDelta = (currentAge - state.lastAge) / 20.0F;
         } else {
            frameGapDetected = true;
            rawDelta = 0.05F;
         }

         state.deltaSeconds = isGui ? 0.0F : (rawDelta > 0.1F ? 0.1F : rawDelta);
         state.lastAge = isGui ? state.lastAge : currentAge;
         float throttleForRipple = omniPlayer.getLerpedFlightThrottle(partialTicks);
         boolean isAboveSonic = throttleForRipple >= 0.6F;
         if (frameGapDetected && !isGui) {
            state.wasAboveSonic = isAboveSonic;
         }

         if (isAboveSonic && !state.wasAboveSonic && !isGui) {
            state.rippleTime = 0.0F;
         }

         if (state.rippleTime >= 0.0F) {
            state.rippleTime = state.rippleTime + state.deltaSeconds;
            if (state.rippleTime > 2.0F) {
               state.rippleTime = -1.0F;
            }
         }

         state.wasAboveSonic = isGui ? state.wasAboveSonic : isAboveSonic;
         if (isFlying && !state.wasFlying && !isGui) {
            state.currentPitch = 0.0F;
            state.currentRoll = 0.0F;
            state.smoothedTurnSpeed = 0.0F;
            state.lastRenderYaw = Mth.lerp(partialTicks, player.yRotO, player.getYRot());
         }

         state.wasFlying = isGui ? state.wasFlying : isFlying;
         float targetPitch = 0.0F;
         float targetRoll = 0.0F;
         if (isFlying) {
            float currentYaw = Mth.lerp(partialTicks, player.yRotO, player.getYRot());
            if (frameGapDetected && !isGui) {
               state.lastRenderYaw = currentYaw;
               state.smoothedTurnSpeed = 0.0F;
            }

            float yawDelta = Mth.wrapDegrees(currentYaw - state.lastRenderYaw);
            state.lastRenderYaw = isGui ? state.lastRenderYaw : currentYaw;
            float turnSpeed = state.deltaSeconds > 0.0F ? yawDelta / state.deltaSeconds : 0.0F;
            state.smoothedTurnSpeed = Mth.lerp(10.0F * state.deltaSeconds, state.smoothedTurnSpeed, turnSpeed);
            float throttle = omniPlayer.getLerpedFlightThrottle(partialTicks);
            float lerpedPitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
            float cruiseFactor = Math.max(0.0F, Math.min(1.0F, throttle / 0.35F));
            state.smoothedHoverForward = Mth.lerp(10.0F * state.deltaSeconds, state.smoothedHoverForward, omniPlayer.getHoverForward());
            state.smoothedHoverSideways = Mth.lerp(10.0F * state.deltaSeconds, state.smoothedHoverSideways, omniPlayer.getHoverSideways());
            float hoverPitch = state.smoothedHoverForward * -11.5F;
            float hoverRoll = state.smoothedHoverSideways * 11.5F;
            float cruisePitch = -lerpedPitch - 90.0F;
            float cruiseRoll = Mth.clamp(
               state.smoothedTurnSpeed * -0.3F, -ViltrumiteConfigClient.INSTANCE.maxBankAngle, ViltrumiteConfigClient.INSTANCE.maxBankAngle
            );
            targetPitch = Mth.lerp(cruiseFactor, hoverPitch, cruisePitch);
            targetRoll = Mth.lerp(cruiseFactor, hoverRoll, cruiseRoll);
         }

         float smoothFactor = 1.0F - (float)Math.exp((double)(-4.5F * state.deltaSeconds));
         if (frameGapDetected && !isGui) {
            state.currentPitch = targetPitch;
            state.currentRoll = targetRoll;
         } else {
            state.currentPitch = Mth.lerp(smoothFactor, state.currentPitch, targetPitch);
            state.currentRoll = Mth.lerp(smoothFactor, state.currentRoll, targetRoll);
         }

         if (Math.abs(state.currentPitch) > 0.1F || Math.abs(state.currentRoll) > 0.1F) {
            float pivotY = ViltrumiteConfigClient.INSTANCE.flightRotY;
            poseStack.translate(0.0F, pivotY, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.currentRoll));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.currentPitch));
            float shiftX = state.currentRoll * -0.002F;
            float shiftZ = state.currentPitch * 0.002F;
            poseStack.translate(shiftX, -pivotY, shiftZ);
         }

         if (state.transition > 0.0F) {
            state.hoverTime = state.hoverTime + state.deltaSeconds;
         } else {
            state.hoverTime = 0.0F;
         }

         float throttle = omniPlayer.getLerpedFlightThrottle(partialTicks);
         float cruiseFade = 1.0F - Mth.clamp(throttle / 0.35F, 0.0F, 1.0F);
         float finalHoverWeight = cruiseFade * state.transition;
         if (finalHoverWeight > 0.0F) {
            float time = state.hoverTime * 20.0F;
            float sin = (float)Math.sin((double)(time * 0.08F));
            float k = 1.12F;
            float hoverY = (float)Math.tanh((double)(k * sin)) * 0.065F * finalHoverWeight;
            poseStack.translate(0.0, (double)hoverY, 0.0);
         }
      }
   }
}
