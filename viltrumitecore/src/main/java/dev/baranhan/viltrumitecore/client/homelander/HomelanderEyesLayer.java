package dev.baranhan.viltrumitecore.client.homelander;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Homelander's glowing eyes drawn on the model's face (so they follow every
 * pose, flight included), and the world position of both eyes for the laser
 * beams. Positions are valid for the current frame only.
 */
public class HomelanderEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   /** Eye pixels on the 8x8 face (model pixels): x [-3,-1] and [1,3], y [-4,-3], front face z = -4. */
   private static final float EYE_Y0 = -4.0F;
   private static final float EYE_Y1 = -3.0F;
   private static final float FACE_Z = -4.6F;
   private static final Map<UUID, Vec3[]> EYES = new HashMap<>();

   public HomelanderEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   /** Eyes of this player rendered this frame (left, right), or null. */
   @Nullable
   public static Vec3[] eyes(UUID player) {
      return EYES.get(player);
   }

   /** Called once per frame after the beams are drawn. */
   public static void endFrame() {
      EYES.clear();
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
      float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      HeroPublicSnapshot snapshot = HomelanderPoser.homelander(player);
      if (snapshot == null || player.isInvisible()) {
         return;
      }

      poseStack.pushPose();
      this.getParentModel().head.translateAndRotate(poseStack);
      Matrix4f pose = poseStack.last().pose();
      if (!ShaderCompat.isShadowPass()) {
         EYES.put(player.getUUID(), new Vec3[]{world(pose, 2.0F), world(pose, -2.0F)});
      }

      boolean laser = HomelanderPoser.laserOn(snapshot);
      boolean focus = HomelanderPoser.focusOn(snapshot);
      if (laser || focus) {
         float flicker = 0.85F + 0.15F * (float)Math.sin(ageInTicks * 1.7F);
         VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
         int core = (int)((laser ? 255 : 170) * flicker);
         for (float side : new float[]{-1.0F, 1.0F}) {
            float x0 = side < 0 ? -3.0F : 1.0F;
            quad(buffer, pose, x0, EYE_Y0, x0 + 2.0F, EYE_Y1, FACE_Z, 255, 70, 40, core);
            if (laser) {
               // Soft halo one pixel around the eye, still on the face.
               quad(buffer, pose, x0 - 0.5F, EYE_Y0 - 0.5F, x0 + 2.5F, EYE_Y1 + 0.5F, FACE_Z - 0.02F, 255, 40, 20, (int)(90 * flicker));
            }
         }
      }

      poseStack.popPose();
   }

   /** Eye centre in world space: undo the view rotation of the level pose stack, add the camera. */
   private static Vec3 world(Matrix4f pose, float xPixels) {
      Vector4f p = pose.transform(new Vector4f(xPixels / 16.0F, (EYE_Y0 + EYE_Y1) * 0.5F / 16.0F, FACE_Z / 16.0F, 1.0F));
      Matrix3f inverseView = RenderSystem.getInverseViewRotationMatrix();
      Vector3f v = inverseView.transform(new Vector3f(p.x(), p.y(), p.z()));
      Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      return camera.add(v.x(), v.y(), v.z());
   }

   private static void quad(VertexConsumer buffer, Matrix4f pose, float x0, float y0, float x1, float y1, float z, int r, int g, int b, int a) {
      float s = 1.0F / 16.0F;
      buffer.vertex(pose, x0 * s, y0 * s, z * s).color(r, g, b, a).endVertex();
      buffer.vertex(pose, x0 * s, y1 * s, z * s).color(r, g, b, a).endVertex();
      buffer.vertex(pose, x1 * s, y1 * s, z * s).color(r, g, b, a).endVertex();
      buffer.vertex(pose, x1 * s, y0 * s, z * s).color(r, g, b, a).endVertex();
   }
}
