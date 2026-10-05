package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerFreezeMixin {
   @Inject(
      method = {"canFreeze"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumiteCannotFreeze(CallbackInfoReturnable<Boolean> cir) {
      if (((ViltrumiteCorePlayer)(Object)this).isViltrumite()) {
         cir.setReturnValue(false);
      }
   }
}
