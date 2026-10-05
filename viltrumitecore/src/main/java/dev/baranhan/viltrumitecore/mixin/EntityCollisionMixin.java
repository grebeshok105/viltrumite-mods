package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityCollisionMixin {
   @Inject(
      method = {"push(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cancelGrabbedCollision(Entity other, CallbackInfo ci) {
      Entity me = (Entity)(Object)this;
      if (me instanceof Player && me instanceof ViltrumiteCorePlayer corePlayer) {
         LivingEntity grabbed = corePlayer.getGrabbedTarget();
         if (grabbed != null && (other == grabbed || this.isPart(other, grabbed))) {
            ci.cancel();
            return;
         }
      }

      if (other instanceof Player && other instanceof ViltrumiteCorePlayer corePlayerx) {
         LivingEntity grabbed = corePlayerx.getGrabbedTarget();
         if (grabbed != null && (me == grabbed || this.isPart(me, grabbed))) {
            ci.cancel();
            return;
         }
      }
   }

   private boolean isPart(Entity entity, Entity parent) {
      if (entity instanceof PartEntity<?> part) {
         return part.getParent() == parent;
      } else {
         return entity instanceof EnderDragonPart part ? part.parentMob == parent : false;
      }
   }
}
