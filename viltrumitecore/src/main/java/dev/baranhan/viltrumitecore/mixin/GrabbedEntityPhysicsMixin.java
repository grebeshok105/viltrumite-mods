package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class GrabbedEntityPhysicsMixin {
   @Inject(
      method = {"push(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPushEntity(Entity entity, CallbackInfo ci) {
      Entity var4 = (Entity)(Object)this;
      if (var4 instanceof LivingEntity living && this.isGrabbed(living)) {
         ci.cancel();
      }

      if (entity instanceof LivingEntity living && this.isGrabbed(living)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"push(DDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPushVector(double x, double y, double z, CallbackInfo ci) {
      Entity var9 = (Entity)(Object)this;
      if (var9 instanceof LivingEntity living && this.isGrabbed(living)) {
         ci.cancel();
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
