package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.render.vfx.OutlineTargets;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The glow outline colour of an outline target is its source colour (client entities only). */
@Mixin({Entity.class})
public abstract class OutlineTeamColorMixin {
   @Inject(
      method = {"getTeamColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumitecore$outlineColor(CallbackInfoReturnable<Integer> cir) {
      Entity self = (Entity)(Object)this;
      if (self.level().isClientSide) {
         int color = OutlineTargets.colorOf(self);
         if (color >= 0) {
            cir.setReturnValue(color);
         }
      }
   }
}
