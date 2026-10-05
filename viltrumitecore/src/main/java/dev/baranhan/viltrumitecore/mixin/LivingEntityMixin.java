package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public class LivingEntityMixin {
   @Inject(
      method = {"swing(Lnet/minecraft/world/InteractionHand;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preventSwingAnimation(InteractionHand hand, CallbackInfo ci) {
      if (this instanceof Player player
         && player instanceof ViltrumiteCorePlayer corePlayer
         && (corePlayer.getPunchTicks() > 0 || corePlayer.getChopTicks() > 0 || corePlayer.isDashing() || corePlayer.isBlocking())) {
         ci.cancel();
      }
   }
}
