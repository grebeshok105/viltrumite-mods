package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.PlayerGrabStateSyncS2CPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class LivingEntityGrabFailsafeMixin {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void onTickFailsafe(CallbackInfo ci) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (!entity.level().isClientSide() && entity.getTags().contains("ViltrumiteGrabbed")) {
         boolean isBeingHeld = false;

         for (Player player : entity.level().players()) {
            if (player instanceof ViltrumiteCorePlayer corePlayer) {
               LivingEntity grabbedTarget = corePlayer.getGrabbedTarget();
               if (grabbedTarget != null && grabbedTarget.equals(entity)) {
                  isBeingHeld = true;
                  break;
               }
            }
         }

         if (!isBeingHeld) {
            entity.removeTag("ViltrumiteGrabbed");
            if (entity instanceof Mob mob) {
               mob.setNoAi(false);
            }

            if (entity instanceof ServerPlayer grabbedPlayer) {
               CoreMessages.sendToPlayer(new PlayerGrabStateSyncS2CPacket(false, null), grabbedPlayer);
            }

            entity.hasImpulse = true;
         }
      }
   }
}
