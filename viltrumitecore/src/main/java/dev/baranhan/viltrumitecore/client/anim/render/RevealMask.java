package dev.baranhan.viltrumitecore.client.anim.render;

/**
 * Pixel reveal over the vanilla 64×64 player skin layout (spike
 * docs/spikes/2026-10-ironman-reveal.md). Every used texel gets a normalised
 * reveal time 0..1: body-space distance from an origin (e.g. the reactor) for
 * the body, and the last {@code lastShare} for the head (helmet closes last,
 * from the neck up). Unused texels are +∞.
 */
public final class RevealMask {
   public static final int FRAMES = 16;
   private static final float MIN = 1.0E-3F;

   /** u, v, W, H, D, x0, y0, z0 (pixels, y up from the feet, front = -z), head flag. */
   private static final float[][] BOXES = {
      {0, 0, 8, 8, 8, -4, 24, -4, 1}, {32, 0, 8, 8, 8, -4, 24, -4, 1},
      {16, 16, 8, 12, 4, -4, 12, -2, 0}, {16, 32, 8, 12, 4, -4, 12, -2, 0},
      {40, 16, 4, 12, 4, -8, 12, -2, 0}, {40, 32, 4, 12, 4, -8, 12, -2, 0},
      {32, 48, 4, 12, 4, 4, 12, -2, 0}, {48, 48, 4, 12, 4, 4, 12, -2, 0},
      {0, 16, 4, 12, 4, -3.9F, 0, -2, 0}, {0, 32, 4, 12, 4, -3.9F, 0, -2, 0},
      {16, 48, 4, 12, 4, -0.1F, 0, -2, 0}, {0, 48, 4, 12, 4, -0.1F, 0, -2, 0},
   };

   private RevealMask() {
   }

   public static float[] playerSkinField(float ox, float oy, float oz, float lastShare) {
      float[] dist = new float[64 * 64];
      boolean[] head = new boolean[64 * 64];
      java.util.Arrays.fill(dist, Float.POSITIVE_INFINITY);
      for (float[] b : BOXES) {
         boolean isHead = b[8] > 0;
         int u = (int)b[0];
         int v = (int)b[1];
         int w = (int)b[2];
         int h = (int)b[3];
         int d = (int)b[4];
         float x0 = b[5];
         float y0 = b[6];
         float z0 = b[7];
         // Points are pixel centres on the box surface.
         for (int i = 0; i < w; i++) {
            for (int j = 0; j < d; j++) {
               put(dist, head, u + d + i, v + j, isHead, x0 + i + 0.5F, y0 + h, z0 + j + 0.5F, ox, oy, oz);
               put(dist, head, u + d + w + i, v + j, isHead, x0 + i + 0.5F, y0, z0 + j + 0.5F, ox, oy, oz);
            }
         }

         for (int j = 0; j < h; j++) {
            float y = y0 + h - j - 0.5F;
            for (int i = 0; i < d; i++) {
               put(dist, head, u + i, v + d + j, isHead, x0, y, z0 + i + 0.5F, ox, oy, oz);
               put(dist, head, u + d + w + i, v + d + j, isHead, x0 + w, y, z0 + d - i - 0.5F, ox, oy, oz);
            }

            for (int i = 0; i < w; i++) {
               put(dist, head, u + d + i, v + d + j, isHead, x0 + i + 0.5F, y, z0, ox, oy, oz);
               put(dist, head, u + 2 * d + w + i, v + d + j, isHead, x0 + w - i - 0.5F, y, z0 + d, ox, oy, oz);
            }
         }
      }

      float bodyMax = 0.0F;
      float headMax = 0.0F;
      for (int k = 0; k < dist.length; k++) {
         if (Float.isFinite(dist[k])) {
            if (head[k]) {
               headMax = Math.max(headMax, dist[k]);
            } else {
               bodyMax = Math.max(bodyMax, dist[k]);
            }
         }
      }

      float bodyShare = 1.0F - lastShare;
      for (int k = 0; k < dist.length; k++) {
         if (!Float.isFinite(dist[k])) {
            continue;
         }

         float t = head[k]
            ? bodyShare + lastShare * Math.max(MIN, dist[k] / Math.max(headMax, 1.0E-3F))
            : bodyShare * dist[k] / Math.max(bodyMax, 1.0E-3F);
         dist[k] = Math.min(1.0F, Math.max(MIN, t));
      }

      return dist;
   }

   /** Head texels measure from the neck (helmet assembles upward), body texels from the origin. */
   private static void put(float[] dist, boolean[] head, int u, int v, boolean isHead, float x, float y, float z, float ox, float oy, float oz) {
      if (u < 0 || u >= 64 || v < 0 || v >= 64) {
         return;
      }

      float dx = x - (isHead ? 0.0F : ox);
      float dy = y - (isHead ? 24.0F : oy);
      float dz = z - (isHead ? 0.0F : oz);
      float d = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      int k = v * 64 + u;
      dist[k] = Math.min(dist[k], d);
      head[k] = isHead;
   }

   public static boolean used(float t) {
      return Float.isFinite(t);
   }

   public static boolean visible(float t, float progress) {
      return progress > 0.0F && t <= progress;
   }

   /** Frame 0 (nothing) .. FRAMES (everything) for a wave progress 0..1. */
   public static int frame(float progress) {
      return Math.max(0, Math.min(FRAMES, (int)Math.ceil(progress * FRAMES - 1.0E-4F)));
   }

   /** True on the wave front of this frame (the texels revealed last). */
   public static boolean rim(float t, int frame) {
      return frame > 0 && frame < FRAMES && t > (frame - 1) / (float)FRAMES && t <= frame / (float)FRAMES;
   }
}
