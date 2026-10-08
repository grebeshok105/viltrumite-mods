package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.homelander.FocusClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The glow outline colour of a focus target is its palette colour (client entities only). */
@Mixin({Entity.class})
public abstract class HomelanderTeamColorMixin {
   @Inject(
      method = {"getTeamColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void homelanderFocusColor(CallbackInfoReturnable<Integer> cir) {
      Entity self = (Entity)(Object)this;
      if (self.level().isClientSide) {
         int color = FocusClient.colorOf(self);
         if (color >= 0) {
            cir.setReturnValue(color);
         }
      }
   }
}
