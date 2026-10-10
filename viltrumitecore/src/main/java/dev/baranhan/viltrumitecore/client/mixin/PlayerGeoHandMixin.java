package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person arm: geo parts on that arm (PlayerGeoLayer) after the vanilla arm is drawn. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerGeoHandMixin {
   @Inject(
      method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
      at = @At("TAIL")
   )
   private void viltrumitecore$geoArm(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
      CallbackInfo ci) {
      PlayerRenderer renderer = (PlayerRenderer)(Object)this;
      PlayerGeoLayer.renderArm(poseStack, buffers, light, player, renderer.getModel(), arm, Minecraft.getInstance().getFrameTime());
   }
}
