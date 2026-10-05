package dev.baranhan.viltrumiteflight.client.mixin;

import dev.baranhan.viltrumiteflight.client.render.FlightAnimManager;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Shadow
   private PostChain postEffect;
   @Shadow
   private float fov;
   @Shadow
   private float oldFov;

   @Shadow
   abstract void loadEffect(ResourceLocation var1);

   @Shadow
   public abstract void shutdownEffect();

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void onRender(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.player instanceof ViltrumiteFlightPlayer omniPlayer) {
         float throttle = omniPlayer.getLerpedFlightThrottle(partialTicks);
         ResourceLocation shaderId = new ResourceLocation("viltrumiteflight", "shaders/post/sonic_boom.json");
         int takeoffTicks = omniPlayer.getTakeoffTicks();
         float takeoffShakeIntensity = 0.0F;
         if (takeoffTicks > 0) {
            takeoffShakeIntensity = Math.min(1.0F, (float)takeoffTicks / 3.0F);
         }

         boolean shouldBeActive = throttle > 0.0F || omniPlayer.getFlightState() != FlightState.NONE || takeoffShakeIntensity > 0.0F;
         if (shouldBeActive) {
            if (this.postEffect == null || !this.postEffect.getName().equals(shaderId.toString())) {
               this.loadEffect(shaderId);
            }

            if (this.postEffect != null) {
               FlightAnimManager.AnimState state = FlightAnimManager.getState(client.player.getUUID());
               List<PostPass> passes = ((PostChainAccessor)this.postEffect).getPasses();
               float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
               float rippleTime = Math.max(0.0F, state.rippleTime);

               for (PostPass pass : passes) {
                  pass.getEffect().safeGetUniform("Throttle").set(throttle);
                  pass.getEffect().safeGetUniform("RippleTime").set(rippleTime);
                  pass.getEffect().safeGetUniform("Time").set(time);
                  pass.getEffect().safeGetUniform("TakeoffShake").set(takeoffShakeIntensity);
               }
            }
         } else if (this.postEffect != null && this.postEffect.getName().equals(shaderId.toString())) {
            this.shutdownEffect();
         }
      }
   }

   @Inject(
      method = {"tickFov"},
      at = {@At("TAIL")}
   )
   private void shatterFovLimit(CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         float masterTargetFov = client.player.getFieldOfViewModifier();
         float rawFov = this.oldFov + (masterTargetFov - this.oldFov) * 0.5F;
         this.fov = Math.min(rawFov, 2.0F);
      }
   }
}
