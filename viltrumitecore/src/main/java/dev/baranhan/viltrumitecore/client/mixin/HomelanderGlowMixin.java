package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.homelander.FocusClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Focus targets glow (vanilla outline, through walls) for the focusing player only. */
@Mixin({Minecraft.class})
public abstract class HomelanderGlowMixin {
   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void homelanderFocusGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (FocusClient.colorOf(entity) >= 0) {
         cir.setReturnValue(true);
      }
   }
}
