package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumiteflight.util.FlightPermissions;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Player.class},
   priority = 1500
)
public abstract class CoreFlightHookMixin {
   @Inject(
      method = {"handleFlightCollision"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   @Dynamic("ViltrumiteFlight mod\u00fcl\u00fc taraf\u0131ndan runtime'da eklenecek")
   private void overrideFlightCollision(CallbackInfo ci) {
      Player player = (Player)(Object)this;
      if (FlightPermissions.allowsModFlight(player)
         && player instanceof ViltrumiteFlightPlayer flightPlayer && flightPlayer.getFlightThrottle() >= 0.6F) {
         ci.cancel();
      }
   }
}
