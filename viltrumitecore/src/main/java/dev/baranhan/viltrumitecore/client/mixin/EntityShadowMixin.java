package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class EntityShadowMixin {
   @Inject(
      method = {"renderShadow"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void disableGrabbedShadow(
      PoseStack poseStack, MultiBufferSource buffer, Entity entity, float opacity, float partialTicks, LevelReader levelReader, float radius, CallbackInfo ci
   ) {
      if (entity.level() != null && entity.level().isClientSide()) {
         if (entity instanceof LivingEntity living) {
            for (Player player : entity.level().players()) {
               if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.getGrabbedTarget() == living) {
                  ci.cancel();
                  return;
               }
            }
         }
      }
   }
}
