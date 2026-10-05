package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"com.bobmowzie.mowziesmobs.server.entity.frostmaw.EntityFrostmaw", "com.bobmowzie.mowziesmobs.server.entity.wroughtnaut.EntityWroughtnaut", "com.bobmowzie.mowziesmobs.server.entity.MowzieEntity"},
   remap = false
)
public abstract class MowziesRepelBypassMixin {
   @Inject(
      method = {"repelEntities"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void stopMowzieRepel(float x, float y, float z, float radius, CallbackInfo ci) {
      LivingEntity me = (LivingEntity)this;
      if (me.level() != null) {
         for (Player player : me.level().players()) {
            if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.getGrabbedTarget() == me) {
               ci.cancel();
               return;
            }
         }
      }
   }
}
