package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityFreezeMixin {
   @Inject(
      method = {"getTicksFrozen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumiteNeverFreezesVisually(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this instanceof Player player && ((ViltrumiteCorePlayer)player).isViltrumite()) {
         cir.setReturnValue(0);
      }
   }

   @Inject(
      method = {"getAirSupply"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumiteInfiniteAir(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this instanceof Player player && ((ViltrumiteCorePlayer)player).isViltrumite()) {
         cir.setReturnValue(player.getMaxAirSupply());
      }
   }
}
