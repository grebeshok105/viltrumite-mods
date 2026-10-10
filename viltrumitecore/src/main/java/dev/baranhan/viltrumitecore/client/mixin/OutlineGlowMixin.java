package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.render.vfx.OutlineTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Outline targets (Homelander focus, Iron Man scan) glow through walls on the owner's client only. */
@Mixin({Minecraft.class})
public abstract class OutlineGlowMixin {
   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumitecore$outlineGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (OutlineTargets.colorOf(entity) >= 0) {
         cir.setReturnValue(true);
      }
   }
}
