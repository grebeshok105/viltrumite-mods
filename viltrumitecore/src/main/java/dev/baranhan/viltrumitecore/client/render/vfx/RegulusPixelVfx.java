package dev.baranhan.viltrumitecore.client.render.vfx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Pixel-style shape helpers shared by the Regulus VFX managers. Everything is
 * POSITION_COLOR quads in camera-relative space — the same "paint pixel"
 * vocabulary the existing managers use (skill §8).
 */
public final class RegulusPixelVfx {
   private RegulusPixelVfx() {
   }

   /** Billboard pixel at a world position, facing the camera via its basis. */
   public static void billboardPixel(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Vec3 pos, float size, int r, int g, int b, int alpha) {
      Vector3f left = camera.getLeftVector();
      Vector3f up = camera.getUpVector();
      float cx = (float)(pos.x - cameraPos.x);
      float cy = (float)(pos.y - cameraPos.y);
      float cz = (float)(pos.z - cameraPos.z);
      float lx = left.x * size;
      float ly = left.y * size;
      float lz = left.z * size;
      float ux = up.x * size;
      float uy = up.y * size;
      float uz = up.z * size;
      buffer.vertex(cx - lx - ux, cy - ly - uy, cz - lz - uz).color(r, g, b, alpha).endVertex();
      buffer.vertex(cx + lx - ux, cy + ly - uy, cz + lz - uz).color(r, g, b, alpha).endVertex();
      buffer.vertex(cx + lx + ux, cy + ly + uy, cz + lz + uz).color(r, g, b, alpha).endVertex();
      buffer.vertex(cx - lx + ux, cy - ly + uy, cz - lz + uz).color(r, g, b, alpha).endVertex();
   }

   /** Flat XZ pixel — lies on the ground or a horizontal dome band. */
   public static void groundPixel(BufferBuilder buffer, Vec3 cameraPos, double x, double y, double z, float size, int r, int g, int b, int alpha) {
      float cx = (float)(x - cameraPos.x);
      float cy = (float)(y - cameraPos.y);
      float cz = (float)(z - cameraPos.z);
      buffer.vertex(cx - size, cy, cz - size).color(r, g, b, alpha).endVertex();
      buffer.vertex(cx + size, cy, cz - size).color(r, g, b, alpha).endVertex();
      buffer.vertex(cx + size, cy, cz + size).color(r, g, b, alpha).endVertex();
      buffer.vertex(cx - size, cy, cz + size).color(r, g, b, alpha).endVertex();
   }

   /** Dotted ground ring in the XZ plane. */
   public static void groundRing(BufferBuilder buffer, Vec3 cameraPos, Vec3 center, float radius, float dotSize, int r, int g, int b, int alpha) {
      int steps = Math.max(24, (int)(radius * 14.0F));
      for (int i = 0; i < steps; i++) {
         double angle = (Math.PI * 2.0) * (double)i / (double)steps;
         groundPixel(buffer, cameraPos, center.x + Math.cos(angle) * radius, center.y + 0.03, center.z + Math.sin(angle) * radius, dotSize, r, g, b, alpha);
      }
   }

   /** Dotted beam between two world points. */
   public static void beamDots(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Vec3 from, Vec3 to, float spacing, float dotSize, int r, int g, int b, int alpha) {
      double length = from.distanceTo(to);
      int steps = Math.max(2, (int)(length / (double)spacing));
      for (int i = 0; i <= steps; i++) {
         Vec3 point = from.lerp(to, (double)i / (double)steps);
         billboardPixel(buffer, cameraPos, camera, point, dotSize, r, g, b, alpha);
      }
   }

   /** Dotted wireframe box around an entity bounds. */
   public static void boxOutline(BufferBuilder buffer, Vec3 cameraPos, Camera camera, AABB box, float dotSize, int r, int g, int b, int alpha) {
      Vec3[] corners = {
         new Vec3(box.minX, box.minY, box.minZ),
         new Vec3(box.maxX, box.minY, box.minZ),
         new Vec3(box.maxX, box.minY, box.maxZ),
         new Vec3(box.minX, box.minY, box.maxZ),
         new Vec3(box.minX, box.maxY, box.minZ),
         new Vec3(box.maxX, box.maxY, box.minZ),
         new Vec3(box.maxX, box.maxY, box.maxZ),
         new Vec3(box.minX, box.maxY, box.maxZ)
      };
      int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
      for (int[] edge : edges) {
         Vec3 a = corners[edge[0]];
         Vec3 c = corners[edge[1]];
         int steps = Math.max(2, (int)(a.distanceTo(c) / 0.25));
         for (int i = 0; i <= steps; i++) {
            billboardPixel(buffer, cameraPos, camera, a.lerp(c, (double)i / (double)steps), dotSize, r, g, b, alpha);
         }
      }
   }

   /**
    * Hemisphere shell of pixel dots: latitude bands around the center with a
    * slow radial shimmer, the distortion-dome look of Lion's Heart.
    */
   public static void domeShell(BufferBuilder buffer, Vec3 cameraPos, Vec3 center, float radius, float timeSeconds, int r, int g, int b, int alpha) {
      for (int band = 1; band <= 6; band++) {
         double theta = (Math.PI / 2.0) * (double)band / 7.0;
         double bandY = Math.sin(theta) * radius;
         double bandRadius = Math.cos(theta) * radius;
         int steps = Math.max(10, (int)(bandRadius * 8.0));
         for (int i = 0; i < steps; i++) {
            double az = (Math.PI * 2.0) * (double)i / (double)steps + timeSeconds * 0.15;
            double wobble = 1.0 + 0.05 * Math.sin(timeSeconds * 3.0 + band * 1.7 + i * 0.9);
            double x = center.x + Math.cos(az) * bandRadius * wobble;
            double y = center.y + bandY * wobble;
            double z = center.z + Math.sin(az) * bandRadius * wobble;
            int fade = (int)(alpha * (1.0 - (double)band / 9.0));
            groundPixel(buffer, cameraPos, x, y, z, 0.06F, r, g, b, fade);
         }
      }
   }

   /** Soft 3x3 cross glow — the white highlight on a frozen projectile. */
   public static void crossGlow(BufferBuilder buffer, Vec3 cameraPos, Camera camera, Vec3 pos, float size, int r, int g, int b, int alpha) {
      billboardPixel(buffer, cameraPos, camera, pos, size, r, g, b, alpha);
      billboardPixel(buffer, cameraPos, camera, pos.add(size, 0.0, 0.0), size * 0.5F, r, g, b, alpha / 2);
      billboardPixel(buffer, cameraPos, camera, pos.subtract(size, 0.0, 0.0), size * 0.5F, r, g, b, alpha / 2);
      billboardPixel(buffer, cameraPos, camera, pos.add(0.0, size, 0.0), size * 0.5F, r, g, b, alpha / 2);
      billboardPixel(buffer, cameraPos, camera, pos.subtract(0.0, size, 0.0), size * 0.5F, r, g, b, alpha / 2);
   }

   /** Expanding ground ring with square-root ease-out, alpha fading to zero. */
   public static void expandingRing(BufferBuilder buffer, Vec3 cameraPos, Vec3 center, float progress, float maxRadius, int r, int g, int b, int alpha) {
      float eased = Mth.sqrt(progress);
      groundRing(buffer, cameraPos, center, eased * maxRadius, 0.09F, r, g, b, alpha);
      if (progress > 0.25F) {
         groundRing(buffer, cameraPos, center, eased * maxRadius * 0.6F, 0.07F, r, g, b, alpha / 2);
      }
   }
}
