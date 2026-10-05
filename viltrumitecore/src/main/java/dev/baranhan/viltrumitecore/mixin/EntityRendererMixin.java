package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin<T extends Entity> {
   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void forceRenderLasers(T entity, Frustum camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof Player player && player.getMainHandItem().getItem() instanceof InfinityGunItem) {
         cir.setReturnValue(true);
      }
   }
}
