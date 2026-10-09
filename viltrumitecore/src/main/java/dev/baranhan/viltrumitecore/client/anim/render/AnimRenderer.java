package dev.baranhan.viltrumitecore.client.anim.render;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class AnimRenderer {
   public static void render(BakedGeoModel model, PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
      render(model, poseStack, null, buffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F, null);
   }

   public static void render(
      BakedGeoModel model,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      int light,
      int overlay,
      float r,
      float g,
      float b,
      float a,
      AnimRenderer.BoneHook hook
   ) {
      if (model != null) {
         for (GeoBone bone : model.topLevelBones()) {
            renderBone(bone, poseStack, bufferSource, buffer, light, overlay, r, g, b, a, hook);
         }
      }
   }

   /**
    * Renders one bone with its children (no hook). Used by {@link PlayerGeoLayer}
    * to attach top-level bones to player model parts.
    */
   public static void renderBone(GeoBone bone, PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
      renderBone(bone, poseStack, null, buffer, light, overlay, r, g, b, a, null);
   }

   private static void renderBone(
      GeoBone bone,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      int light,
      int overlay,
      float r,
      float g,
      float bl,
      float a,
      AnimRenderer.BoneHook hook
   ) {
      poseStack.pushPose();
      if (bone.posX != 0.0F || bone.posY != 0.0F || bone.posZ != 0.0F) {
         poseStack.translate(-bone.posX / 16.0F, bone.posY / 16.0F, bone.posZ / 16.0F);
      }

      poseStack.translate(bone.pivotX / 16.0F, bone.pivotY / 16.0F, bone.pivotZ / 16.0F);
      if (bone.rotZ != 0.0F) {
         poseStack.mulPose(Axis.ZP.rotation(bone.rotZ));
      }

      if (bone.rotY != 0.0F) {
         poseStack.mulPose(Axis.YP.rotation(bone.rotY));
      }

      if (bone.rotX != 0.0F) {
         poseStack.mulPose(Axis.XP.rotation(bone.rotX));
      }

      if (bone.scaleX != 1.0F || bone.scaleY != 1.0F || bone.scaleZ != 1.0F) {
         poseStack.scale(bone.scaleX, bone.scaleY, bone.scaleZ);
      }

      poseStack.translate(-bone.pivotX / 16.0F, -bone.pivotY / 16.0F, -bone.pivotZ / 16.0F);
      if (hook != null) {
         hook.onBone(bone, poseStack, bufferSource, light, overlay);
      }

      if (!bone.hidden) {
         for (GeoBone.Cube cube : bone.cubes) {
            renderCube(cube, poseStack, buffer, light, overlay, r, g, bl, a);
         }
      }

      for (GeoBone child : bone.children) {
         renderBone(child, poseStack, bufferSource, buffer, light, overlay, r, g, bl, a, hook);
      }

      poseStack.popPose();
   }

   private static void renderCube(GeoBone.Cube cube, PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
      boolean rotated = cube.hasRotation();
      if (rotated) {
         poseStack.pushPose();
         poseStack.translate(cube.pivot().x, cube.pivot().y, cube.pivot().z);
         if (cube.rotation().z != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(cube.rotation().z));
         }

         if (cube.rotation().y != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(cube.rotation().y));
         }

         if (cube.rotation().x != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(cube.rotation().x));
         }

         poseStack.translate(-cube.pivot().x, -cube.pivot().y, -cube.pivot().z);
      }

      Pose pose = poseStack.last();
      Matrix4f position = pose.pose();
      Matrix3f normalMatrix = pose.normal();
      Vector3f normal = new Vector3f();

      for (GeoBone.Quad quad : cube.quads()) {
         normal.set(quad.normal()).mul(normalMatrix);

         for (GeoBone.Vertex vertex : quad.vertices()) {
            buffer.vertex(position, vertex.x(), vertex.y(), vertex.z())
               .color(r, g, b, a)
               .uv(vertex.u(), vertex.v())
               .overlayCoords(overlay)
               .uv2(light)
               .normal(normal.x, normal.y, normal.z)
               .endVertex();
         }
      }

      if (rotated) {
         poseStack.popPose();
      }
   }

   public static double time(float partialTick) {
      return Blaze3D.getTime() * 20.0;
   }

   private AnimRenderer() {
   }

   @FunctionalInterface
   public interface BoneHook {
      void onBone(GeoBone var1, PoseStack var2, MultiBufferSource var3, int var4, int var5);
   }
}
