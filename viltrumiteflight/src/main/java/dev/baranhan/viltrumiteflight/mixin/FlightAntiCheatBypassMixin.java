package dev.baranhan.viltrumiteflight.mixin;

import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerGamePacketListenerImpl.class})
public class FlightAntiCheatBypassMixin {
   @Shadow
   public ServerPlayer player;

   @Inject(
      method = {"isSingleplayerOwner"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bypassMovementChecks(CallbackInfoReturnable<Boolean> cir) {
      if (this.player != null) {
         if (this.player instanceof ViltrumiteFlightPlayer flightPlayer) {
            FlightState state = flightPlayer.getFlightState();
            if (state == FlightState.CRUISE || state == FlightState.SONIC) {
               cir.setReturnValue(true);
            }
         }
      }
   }
}
