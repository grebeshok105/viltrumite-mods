package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;

/**
 * Tony's arc reactor: an emissive glow on the chest of the model (follows
 * every pose). Gentle pulse while the suit is off; a bright flash when the
 * nano wave leaves the reactor (deploy start) or returns into it (retract
 * end). While the suit is worn the Mark 50 glow map draws the reactor.
 */
public class ReactorGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   /** Reactor core on the body front, model pixels (y down from the neck): skin texels 23-24 x 23-24. */
   private static final float CORE_X0 = -1.0F;
   private static final float CORE_X1 = 1.0F;
   private static final float CORE_Y0 = 3.0F;
   private static final float CORE_Y1 = 5.0F;
   /** Just in front of the jacket layer (body front z = -2, jacket +0.25). */
   private static final float FRONT_Z = -2.3F;
   /** Wave ticks around the reactor flash. */
   private static final float FLASH_TICKS = 6.0F;

   public ReactorGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   /** Glow strength 0..1.6 for a snapshot; 0 = nothing drawn. Pure for tests. */
   public static float intensity(boolean worn, boolean deploying, boolean retracting, float waveTicks, float waveLength, float ageInTicks) {
      float pulse = 0.8F + 0.2F * (float)Math.sin(ageInTicks * 0.12F);
      if (deploying) {
         float flash = 1.0F - waveTicks / FLASH_TICKS;
         return flash > 0.0F ? pulse + 0.8F * flash : 0.0F;
      }

      if (retracting) {
         float flash = 1.0F - (waveLength - waveTicks) / FLASH_TICKS;
         return flash > 0.0F ? pulse + 0.8F * flash : 0.0F;
      }

      return worn ? 0.0F : pulse;
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
      float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null || player.isInvisible() || ShaderCompat.isShadowPass() || !this.getParentModel().body.visible) {
         return;
      }

      float k = intensity(IronManView.worn(snapshot), IronManView.deploying(snapshot), IronManView.retracting(snapshot),
         snapshot.actionElapsed() + partialTick, snapshot.actionLength(), ageInTicks);
      if (k <= 0.0F) {
         return;
      }

      poseStack.pushPose();
      this.getParentModel().body.translateAndRotate(poseStack);
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
      int core = Math.min(255, (int)(200 * k));
      quad(buffer, pose, CORE_X0, CORE_Y0, CORE_X1, CORE_Y1, FRONT_Z, 170, 240, 255, core);
      // Halo: one pixel around the ring, wider during the wave flash.
      float halo = 1.0F + Math.max(0.0F, k - 1.0F) * 1.5F;
      quad(buffer, pose, CORE_X0 - halo, CORE_Y0 - halo, CORE_X1 + halo, CORE_Y1 + halo, FRONT_Z - 0.02F, 80, 200, 255, Math.min(255, (int)(70 * k)));
      poseStack.popPose();
   }

   private static void quad(VertexConsumer buffer, Matrix4f pose, float x0, float y0, float x1, float y1, float z, int r, int g, int b, int a) {
      float s = 1.0F / 16.0F;
      buffer.vertex(pose, x0 * s, y0 * s, z * s).color(r, g, b, a).endVertex();
      buffer.vertex(pose, x0 * s, y1 * s, z * s).color(r, g, b, a).endVertex();
      buffer.vertex(pose, x1 * s, y1 * s, z * s).color(r, g, b, a).endVertex();
      buffer.vertex(pose, x1 * s, y0 * s, z * s).color(r, g, b, a).endVertex();
   }
}
