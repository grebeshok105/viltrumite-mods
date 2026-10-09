package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Emissive passes of the Mark 50 suit skin on the player model. The lit body
 * itself is the player skin (HeroSkins variant = baked reveal frame), so it
 * follows every pose and the first-person arm. This layer adds the glow map
 * (reactor, joints) and the cyan wave front of the frame. The head is hidden
 * here: the helmet is a geo part with its own glow (IronManPartsProvider).
 * Stage 2: the nano damage pass (NanoDamageVisuals) is drawn here too.
 */
public class IronManSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public IronManSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
      float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null || player.isInvisible() || ShaderCompat.isShadowPass()) {
         return;
      }

      int frame = IronManView.frame(snapshot, partialTick);
      if (frame <= 0) {
         return;
      }

      PlayerModel<AbstractClientPlayer> model = this.getParentModel();
      boolean head = model.head.visible;
      boolean hat = model.hat.visible;
      model.head.visible = false;
      model.hat.visible = false;
      try {
         ResourceLocation glow = IronManSuitTextures.INSTANCE.glow(frame);
         if (glow != null) {
            float pulse = 0.85F + 0.15F * (float)Math.sin(ageInTicks * 0.12F);
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(glow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1.0F);
         }

         // Nano damage (spec §4.2): broken pixels show Tony's skin, then close up (visual only).
         ResourceLocation damage = NanoDamageVisuals.INSTANCE.texture(player, snapshot);
         if (damage != null) {
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(damage)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
         }

         ResourceLocation rim = IronManSuitTextures.INSTANCE.rim(frame);
         if (rim != null) {
            float flicker = 0.75F + 0.25F * (float)Math.sin(ageInTicks * 2.3F);
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(rim)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, flicker, flicker, flicker, 1.0F);
         }
      } finally {
         model.head.visible = head;
         model.hat.visible = hat;
      }
   }
}
