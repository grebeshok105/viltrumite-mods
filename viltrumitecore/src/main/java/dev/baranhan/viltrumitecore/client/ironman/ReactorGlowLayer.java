package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Tony's arc reactor: emissive mask (RenderType.eyes, vanilla render type so
 * shader packs keep it) drawn on the whole model so it follows every pose.
 */
public class ReactorGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation GLOW = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_reactor_glow.png");

   public ReactorGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
      float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      if (player.isInvisible() || !(player instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot.heroId() != HeroId.IRON_MAN || !ReactorGlow.visible(snapshot.heroFlags())) {
         return;
      }

      float pulse = ReactorGlow.pulse(ageInTicks);
      this.getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GLOW)), 15728640, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1.0F);
   }
}
