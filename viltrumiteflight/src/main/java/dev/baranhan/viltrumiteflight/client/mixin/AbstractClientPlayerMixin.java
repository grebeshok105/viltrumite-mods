package dev.baranhan.viltrumiteflight.client.mixin;

import dev.baranhan.viltrumiteflight.config.ViltrumiteConfigClient;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class AbstractClientPlayerMixin {
   @Unique
   private static final ResourceLocation CUSTOM_SKIN_ID = new ResourceLocation("viltrumiteflight", "textures/entity/skin_viltrumite_1.png");

   @Inject(
      method = {"getFieldOfViewModifier"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetFovMultiplier(CallbackInfoReturnable<Float> cir) {
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
      if (ViltrumiteConfigClient.INSTANCE.enableFovEffect && player instanceof ViltrumiteFlightPlayer flightPlayer) {
         if (flightPlayer.getFlightState() == FlightState.NONE) {
            return;
         }

         float baseFov = (Float)cir.getReturnValue();
         float tickDelta = Minecraft.getInstance().getPartialTick();
         float throttle = flightPlayer.getLerpedFlightThrottle(tickDelta);
         float rawCustomFov = baseFov + throttle * ViltrumiteConfigClient.INSTANCE.fovMultiplier;
         float smoothedFov = (float)Math.round(rawCustomFov * ViltrumiteConfigClient.INSTANCE.smoothFov) / ViltrumiteConfigClient.INSTANCE.smoothFov;
         cir.setReturnValue(smoothedFov);
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void stabilizeHighSpeedCape(CallbackInfo ci) {
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
      if (player.level().isClientSide() && player instanceof ViltrumiteFlightPlayer flightPlayer) {
         FlightState state = flightPlayer.getFlightState();
         float throttle = flightPlayer.getFlightThrottle();
         if (state != FlightState.NONE && (state != FlightState.HOVER || !(throttle < 0.8F))) {
            double moveX = player.getX() - player.xo;
            double moveY = player.getY() - player.yo;
            double moveZ = player.getZ() - player.zo;
            double expectedCapeX = player.xCloakO + moveX;
            double expectedCapeY = player.yCloakO + moveY;
            double expectedCapeZ = player.zCloakO + moveZ;
            double targetX = player.getX();
            double targetZ = player.getZ();
            float flutter = (float)Math.sin((double)((float)player.tickCount * 0.5F)) * 0.05F;
            double targetY = player.getY() + (double)flutter;
            player.xCloak = Mth.lerp(0.3, expectedCapeX, targetX);
            player.yCloak = Mth.lerp(0.3, expectedCapeY, targetY);
            player.zCloak = Mth.lerp(0.3, expectedCapeZ, targetZ);
         }
      }
   }
}
