package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.platform.NativeImage;
import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nano reveal frames (reveal technique "pre-baked frames", see
 * docs/spikes/2026-10-ironman-reveal.md). For each frame 1..16 four textures
 * are baked from Tony's skin, the Mark 50 skin and its glow map with
 * {@link RevealMask}:
 * <ul>
 *    <li>skin: suit where revealed, Tony elsewhere; the head always stays Tony
 *    (the helmet is a separate 3D part);</li>
 *    <li>cut: suit where revealed, transparent elsewhere (helmet part);</li>
 *    <li>glow: emissive pixels where revealed (additive, black = nothing);</li>
 *    <li>rim: the cyan wave front with a scale pattern.</li>
 * </ul>
 * All are plain vanilla render types, so shader packs draw them like any
 * entity texture. Built lazily on the render thread, rebuilt after a reload.
 */
public final class IronManSuitTextures implements ResourceManagerReloadListener {
   public static final ResourceLocation SUIT = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_mark_50.png");
   public static final ResourceLocation SUIT_GLOW = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_mark_50_glow.png");
   private static final Logger LOGGER = LoggerFactory.getLogger("viltrumitecore/ironman");
   private static final String[] KINDS = {"skin", "cut", "glow", "rim"};
   private static final int SKIN = 0;
   private static final int CUT = 1;
   private static final int GLOW = 2;
   private static final int RIM = 3;
   /** Declared after the constants above: the constructor reads KINDS. */
   public static final IronManSuitTextures INSTANCE = new IronManSuitTextures();
   private final ResourceLocation[][] frames = new ResourceLocation[KINDS.length][RevealMask.FRAMES + 1];
   private boolean built;
   private boolean failed;

   private IronManSuitTextures() {
   }

   @Override
   public void onResourceManagerReload(ResourceManager manager) {
      // Rebuilt on the next render from the new resources.
      this.built = false;
      this.failed = false;
   }

   /** Skin for a frame (1..16); frame 0 = Tony's own skin. */
   public ResourceLocation skin(int frame) {
      return frame <= 0 ? IronManClient.SKIN : this.get(SKIN, frame, IronManClient.SKIN);
   }

   @Nullable
   public ResourceLocation cut(int frame) {
      return this.get(CUT, frame, null);
   }

   @Nullable
   public ResourceLocation glow(int frame) {
      return this.get(GLOW, frame, null);
   }

   @Nullable
   public ResourceLocation rim(int frame) {
      return frame >= RevealMask.FRAMES ? null : this.get(RIM, frame, null);
   }

   private ResourceLocation get(int kind, int frame, @Nullable ResourceLocation fallback) {
      if (frame <= 0 || !this.ensure()) {
         return fallback;
      }

      ResourceLocation location = this.frames[kind][Math.min(RevealMask.FRAMES, frame)];
      return location == null ? fallback : location;
   }

   private boolean ensure() {
      if (this.built) {
         return true;
      }

      if (this.failed) {
         return false;
      }

      Minecraft minecraft = Minecraft.getInstance();
      ResourceManager resources = minecraft.getResourceManager();
      try (NativeImage tony = read(resources, IronManClient.SKIN);
         NativeImage suit = read(resources, SUIT);
         NativeImage glow = read(resources, SUIT_GLOW)) {
         this.bake(minecraft.getTextureManager(), tony, suit, glow);
         this.built = true;
         return true;
      } catch (IOException | RuntimeException exception) {
         LOGGER.error("Iron Man reveal frames could not be built", exception);
         this.failed = true;
         return false;
      }
   }

   private void bake(TextureManager textures, NativeImage tony, NativeImage suit, NativeImage glow) {
      RevealMask.Field field = RevealMask.field();
      int size = RevealMask.SIZE;
      for (int frame = 1; frame <= RevealMask.FRAMES; frame++) {
         float progress = RevealMask.progressOf(frame);
         NativeImage[] out = new NativeImage[KINDS.length];
         for (int kind = 0; kind < KINDS.length; kind++) {
            out[kind] = new NativeImage(size, size, true);
         }

         for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
               float distance = field.at(x, y);
               boolean head = field.isHead(x, y);
               boolean shown = RevealMask.visible(distance, progress);
               int tonyPixel = pixel(tony, x, y);
               int suitPixel = pixel(suit, x, y);
               out[SKIN].setPixelRGBA(x, y, shown && !head ? suitPixel : tonyPixel);
               out[CUT].setPixelRGBA(x, y, shown ? suitPixel : 0);
               out[GLOW].setPixelRGBA(x, y, shown ? additive(pixel(glow, x, y)) : 0);
               boolean rim = RevealMask.rim(distance, progress) && alpha(suitPixel) > 0;
               out[RIM].setPixelRGBA(x, y, rim ? rimColor(x, y, (progress - distance) / RevealMask.RIM_BAND) : 0);
            }
         }

         for (int kind = 0; kind < KINDS.length; kind++) {
            ResourceLocation location = new ResourceLocation("viltrumitecore", "dynamic/ironman/" + KINDS[kind] + "_" + frame);
            textures.release(location);
            textures.register(location, new DynamicTexture(out[kind]));
            this.frames[kind][frame] = location;
         }
      }
   }

   private static NativeImage read(ResourceManager resources, ResourceLocation location) throws IOException {
      try (InputStream stream = resources.open(location)) {
         return NativeImage.read(stream);
      }
   }

   private static int pixel(NativeImage image, int x, int y) {
      return x < image.getWidth() && y < image.getHeight() ? image.getPixelRGBA(x, y) : 0;
   }

   private static int alpha(int abgr) {
      return abgr >>> 24 & 0xFF;
   }

   /** Premultiply for additive blending: transparent pixels add nothing. */
   private static int additive(int abgr) {
      int a = alpha(abgr);
      if (a == 0) {
         return 0;
      }

      int r = (abgr & 0xFF) * a / 255;
      int g = (abgr >>> 8 & 0xFF) * a / 255;
      int b = (abgr >>> 16 & 0xFF) * a / 255;
      return 0xFF000000 | b << 16 | g << 8 | r;
   }

   /** Cyan front, brightest at the edge; a hex-like pattern of nano scales. */
   private static int rimColor(int x, int y, float depth) {
      float edge = 1.0F - Math.max(0.0F, Math.min(1.0F, depth));
      boolean scale = ((x + (y / 2 % 2) * 2) % 4 == 0) || (y % 2 == 0 && x % 4 == 2);
      float k = (0.35F + 0.65F * edge) * (scale ? 1.0F : 0.6F);
      int r = (int)(90 * k);
      int g = (int)(225 * k);
      int b = (int)(255 * k);
      return 0xFF000000 | b << 16 | g << 8 | r;
   }
}
