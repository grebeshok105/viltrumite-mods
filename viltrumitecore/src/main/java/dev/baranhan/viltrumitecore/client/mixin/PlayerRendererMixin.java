package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public abstract class PlayerRendererMixin {
   @Inject(
      method = {"scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V"},
      at = {@At("TAIL")}
   )
   protected void onScale(AbstractClientPlayer player, PoseStack poseStack, float partialTickTime, CallbackInfo ci) {
      float cloneScale = ((ViltrumiteCorePlayer)player).getCloneScale();
      if (cloneScale != 1.0F) {
         poseStack.scale(cloneScale, cloneScale, cloneScale);
      }
   }
}
