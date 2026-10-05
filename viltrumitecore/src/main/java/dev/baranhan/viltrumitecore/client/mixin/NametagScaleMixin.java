package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public abstract class NametagScaleMixin<T extends Entity> {
   @Inject(
      method = {"renderNameTag"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
         shift = Shift.AFTER
      )}
   )
   private void onRenderNameTagScale(T entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
      if (entity instanceof ViltrumiteCorePlayer corePlayer) {
         float scale = corePlayer.getCloneScale();
         if (scale != 1.0F) {
            float yOffset = 0.0F;
            if (scale > 1.0F) {
               yOffset = -1.5F + 1.5F * scale;
            } else if (scale < 1.0F) {
               yOffset = -2.5F + 2.5F * scale;
            }

            poseStack.translate(0.0, (double)yOffset, 0.0);
         }
      }
   }
}
