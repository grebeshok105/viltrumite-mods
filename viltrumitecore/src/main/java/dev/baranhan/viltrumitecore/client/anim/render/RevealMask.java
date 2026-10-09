package dev.baranhan.viltrumitecore.client.anim.render;

/**
 * Pixel reveal mask for a 64x64 player-format skin (classic 4 px arms).
 * Every texel of the standard box layout (inner and outer layer) gets a
 * normalized distance 0..1 from an origin on the body; a texel is shown when
 * its distance is at most the reveal progress. Head texels come after every
 * body texel (they grow from the neck), so a helmet closes last.
 *
 * Pure logic, no client classes: the frame textures are baked from this field
 * (see animation-system skill, "Suit skin reveal").
 */
public final class RevealMask {
   public static final int SIZE = 64;
   /** Frame count of a full reveal; frame 0 = nothing, frame FRAMES = all. */
   public static final int FRAMES = 16;
   /** Width of the bright wave front, in normalized distance. */
   public static final float RIM_BAND = 0.09F;
   /** Arc reactor, model pixels (y up, feet at 0, -z = front). */
   public static final float[] REACTOR = {0.0F, 20.0F, -2.0F};
   private static final float[] NECK = {0.0F, 24.0F, 0.0F};
   /** Head distances are compressed so the helmet takes about a fifth of the wave. */
   private static final float HEAD_WEIGHT = 0.5F;
   private static final float JITTER = 0.02F;

   /**
    * Box layout: uv origin, size (w, h, d), min corner (y up), overlay uv origin,
    * head flag. Same layout as PlayerModel.createMesh with classic arms.
    */
   private static final Box[] BOXES = {
      new Box(0, 0, 8, 8, 8, -4, 24, -4, 32, 0, true),
      new Box(16, 16, 8, 12, 4, -4, 12, -2, 16, 32, false),
      new Box(40, 16, 4, 12, 4, -8, 12, -2, 40, 32, false),
      new Box(32, 48, 4, 12, 4, 4, 12, -2, 48, 48, false),
      new Box(0, 16, 4, 12, 4, -4, 0, -2, 0, 32, false),
      new Box(16, 48, 4, 12, 4, 0, 0, -2, 0, 48, false)
   };

   private static Field cached;

   private RevealMask() {
   }

   /** Normalized distance per texel (index y * 64 + x); NaN = texel not used by the model. */
   public static Field field() {
      Field field = cached;
      if (field == null) {
         field = build();
         cached = field;
      }

      return field;
   }

   /** Texel shown at this reveal progress (0..1). */
   public static boolean visible(float distance, float progress) {
      return !Float.isNaN(distance) && progress > 0.0F && distance <= progress;
   }

   /** Texel on the bright wave front at this progress. */
   public static boolean rim(float distance, float progress) {
      return visible(distance, progress) && progress < 1.0F && distance > progress - RIM_BAND;
   }

   /** Frame index 0..FRAMES for a reveal progress. */
   public static int frame(float progress) {
      if (progress <= 0.0F) {
         return 0;
      }

      return Math.max(1, Math.min(FRAMES, (int)Math.ceil(progress * FRAMES - 1.0E-4F)));
   }

   /** Progress shown by a frame index. */
   public static float progressOf(int frame) {
      return Math.max(0, Math.min(FRAMES, frame)) / (float)FRAMES;
   }

   private static Field build() {
      float[] raw = new float[SIZE * SIZE];
      boolean[] head = new boolean[SIZE * SIZE];
      float[] points = new float[SIZE * SIZE * 3];
      java.util.Arrays.fill(raw, Float.NaN);
      float bodyMax = 0.0F;
      for (Box box : BOXES) {
         if (!box.head) {
            bodyMax = Math.max(bodyMax, box.maxDistance(REACTOR));
         }
      }

      for (Box box : BOXES) {
         for (int layer = 0; layer < 2; layer++) {
            int u0 = layer == 0 ? box.u : box.ou;
            int v0 = layer == 0 ? box.v : box.ov;
            box.forEachTexel(u0, v0, (x, y, p) -> {
               float d = box.head ? bodyMaxPlus(p) : distance(p, REACTOR);
               raw[y * SIZE + x] = d;
               System.arraycopy(p, 0, points, (y * SIZE + x) * 3, 3);
               head[y * SIZE + x] = box.head;
            });
         }
      }

      float headBase = bodyMax;
      float max = 0.0F;
      for (int i = 0; i < raw.length; i++) {
         if (!Float.isNaN(raw[i])) {
            if (head[i]) {
               raw[i] += headBase;
            }

            max = Math.max(max, raw[i]);
         }
      }

      float[] normalized = new float[raw.length];
      float headMin = Float.MAX_VALUE;
      float bodyMaxN = 0.0F;
      for (int i = 0; i < raw.length; i++) {
         if (Float.isNaN(raw[i])) {
            normalized[i] = Float.NaN;
            continue;
         }

         float n = raw[i] / max;
         // Small stable jitter: the front is ragged like scales, not a clean sphere.
         float jitter = (hash(i) - 0.5F) * JITTER;
         n = head[i] ? Math.max(n + jitter, 0.0F) : Math.max(0.001F, n + jitter);
         n = Math.min(1.0F, n);
         normalized[i] = n;
         if (head[i]) {
            headMin = Math.min(headMin, n);
         } else {
            bodyMaxN = Math.max(bodyMaxN, n);
         }
      }

      // Keep the strict order body < head after the jitter.
      if (headMin <= bodyMaxN) {
         float shift = bodyMaxN - headMin + 0.001F;
         headMin = Float.MAX_VALUE;
         for (int i = 0; i < normalized.length; i++) {
            if (head[i] && !Float.isNaN(normalized[i])) {
               normalized[i] = Math.min(1.0F, normalized[i] + shift);
               headMin = Math.min(headMin, normalized[i]);
            }
         }
      }

      return new Field(normalized, head, headMin, points);
   }

   private static float bodyMaxPlus(float[] p) {
      return HEAD_WEIGHT * distance(p, NECK);
   }

   private static float distance(float[] a, float[] b) {
      float dx = a[0] - b[0];
      float dy = a[1] - b[1];
      float dz = a[2] - b[2];
      return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static float hash(int i) {
      int h = i * 0x9E3779B1;
      h ^= h >>> 15;
      h *= 0x85EBCA77;
      h ^= h >>> 13;
      return (h & 0xFFFF) / 65535.0F;
   }

   /** Result of the distance build. */
   public static final class Field {
      private final float[] distance;
      private final boolean[] head;
      private final float headStart;
      private final float[] points;

      Field(float[] distance, boolean[] head, float headStart, float[] points) {
         this.distance = distance;
         this.head = head;
         this.headStart = headStart;
         this.points = points;
      }

      /**
       * Model-space surface point of texel (x, y) in pixels (y up, feet at 0,
       * -z front), written into out[0..2]; used by the wave VFX.
       */
      public void point(int x, int y, float[] out) {
         System.arraycopy(this.points, (y * SIZE + x) * 3, out, 0, 3);
      }

      /** Normalized distance of texel (x, y), NaN when unused. */
      public float at(int x, int y) {
         return this.distance[y * SIZE + x];
      }

      public boolean isHead(int x, int y) {
         return this.head[y * SIZE + x];
      }

      /** Smallest head distance: the helmet starts to close at this progress. */
      public float headStart() {
         return this.headStart;
      }

      /** First frame that shows any head texel. */
      public int headStartFrame() {
         return frame(this.headStart);
      }
   }

   @FunctionalInterface
   private interface TexelSink {
      void accept(int x, int y, float[] point);
   }

   private record Box(int u, int v, int w, int h, int d, float x0, float y0, float z0, int ou, int ov, boolean head) {
      float maxDistance(float[] origin) {
         float best = 0.0F;
         for (int i = 0; i < 8; i++) {
            float[] c = {
               (i & 1) == 0 ? this.x0 : this.x0 + this.w,
               (i & 2) == 0 ? this.y0 : this.y0 + this.h,
               (i & 4) == 0 ? this.z0 : this.z0 + this.d
            };
            best = Math.max(best, distance(c, origin));
         }

         return best;
      }

      /** Texel centre to surface point for the six faces of the standard box UV layout. */
      void forEachTexel(int u0, int v0, TexelSink sink) {
         float x1 = this.x0 + this.w;
         float y1 = this.y0 + this.h;
         float z1 = this.z0 + this.d;
         // top: row nearest the front touches the front face
         for (int j = 0; j < this.d; j++) {
            for (int i = 0; i < this.w; i++) {
               sink.accept(u0 + this.d + i, v0 + j, new float[]{this.x0 + i + 0.5F, y1, this.z0 + (this.d - 1 - j) + 0.5F});
               sink.accept(u0 + this.d + this.w + i, v0 + j, new float[]{this.x0 + i + 0.5F, this.y0, this.z0 + (this.d - 1 - j) + 0.5F});
            }
         }

         for (int j = 0; j < this.h; j++) {
            float y = y1 - j - 0.5F;
            for (int i = 0; i < this.d; i++) {
               // right side (entity right, -x): column next to the front is i = d - 1
               sink.accept(u0 + i, v0 + this.d + j, new float[]{this.x0, y, z1 - i - 0.5F});
               // left side (+x): column next to the front is i = 0
               sink.accept(u0 + this.d + this.w + i, v0 + this.d + j, new float[]{x1, y, this.z0 + i + 0.5F});
            }

            for (int i = 0; i < this.w; i++) {
               sink.accept(u0 + this.d + i, v0 + this.d + j, new float[]{this.x0 + i + 0.5F, y, this.z0});
               sink.accept(u0 + 2 * this.d + this.w + i, v0 + this.d + j, new float[]{x1 - i - 0.5F, y, z1});
            }
         }
      }
   }
}
