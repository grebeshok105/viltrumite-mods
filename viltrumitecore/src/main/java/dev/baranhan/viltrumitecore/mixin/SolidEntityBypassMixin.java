package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class SolidEntityBypassMixin {
   @Inject(
      method = {"canBeCollidedWith"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void disableSolidCollision(CallbackInfoReturnable<Boolean> cir) {
      Entity me = (Entity)(Object)this;
      if (me instanceof LivingEntity living && this.isGrabbed(living)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"isPushable"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void disablePushable(CallbackInfoReturnable<Boolean> cir) {
      Entity me = (Entity)(Object)this;
      if (me instanceof LivingEntity living && this.isGrabbed(living)) {
         cir.setReturnValue(false);
      }
   }

   private boolean isGrabbed(LivingEntity entity) {
      if (entity.level() == null) {
         return false;
      } else {
         for (Player player : entity.level().players()) {
            if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.getGrabbedTarget() == entity) {
               return true;
            }
         }

         return false;
      }
   }
}
