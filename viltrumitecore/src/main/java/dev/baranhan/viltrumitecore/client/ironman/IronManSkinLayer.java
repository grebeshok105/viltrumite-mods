package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Mark 50 glow (eyes, reactor, seams) for the current reveal frame. The suit
 * skin itself is the player's skin texture (IronManClient provider), so it
 * follows every pose and first person with no overlay.
 */
public class IronManSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public IronManSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
      float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      if (player.isInvisible()) {
         return;
      }

      ResourceLocation glow = IronManSkinFrames.glow(IronManSkinView.frame(player, partialTick));
      if (glow != null) {
         this.getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(glow)), 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      }
   }
}
