package dev.baranhan.viltrumitecore.client.ironman.mark;

import com.mojang.blaze3d.platform.NativeImage;
import dev.baranhan.viltrumitecore.client.ironman.IronManClient;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mark skins with the faceplate open: the mark's skin and glow map, with Tony's
 * face (head front and hat front of the skin layout) copied from his own skin;
 * the rest of the helmet stays. The plate itself is drawn by {@link MarkFaceplate}. Baked once
 * per resource reload into DynamicTextures. A mark whose texture is missing
 * keeps the closed skin.
 */
public final class MarkSkins implements ResourceManagerReloadListener {
   public static final MarkSkins INSTANCE = new MarkSkins();
   private static final Logger LOGGER = LoggerFactory.getLogger("viltrumitecore/ironman");
   private static final int HEAD_ROWS = 16;
   private final ResourceLocation[] openSkins = new ResourceLocation[MarkId.count()];
   private final ResourceLocation[] openGlows = new ResourceLocation[MarkId.count()];
   private boolean built;

   private MarkSkins() {
   }

   public ResourceLocation skin(MarkId mark, boolean helmetOpen) {
      if (!helmetOpen) {
         return MarkTextures.skin(mark);
      }

      ensure();
      ResourceLocation open = this.openSkins[mark.ordinal()];
      return open != null ? open : MarkTextures.skin(mark);
   }

   public ResourceLocation glow(MarkId mark, boolean helmetOpen) {
      if (!helmetOpen) {
         return MarkTextures.glow(mark);
      }

      ensure();
      ResourceLocation open = this.openGlows[mark.ordinal()];
      return open != null ? open : MarkTextures.glow(mark);
   }

   @Override
   public void onResourceManagerReload(ResourceManager manager) {
      this.built = false;
      Arrays.fill(this.openSkins, null);
      Arrays.fill(this.openGlows, null);
   }

   private void ensure() {
      if (this.built) {
         return;
      }

      this.built = true;
      Minecraft minecraft = Minecraft.getInstance();
      ResourceManager resources = minecraft.getResourceManager();
      TextureManager textures = minecraft.getTextureManager();
      NativeImage tony;
      try {
         tony = read(resources, IronManClient.SKIN);
      } catch (IOException | RuntimeException exception) {
         LOGGER.warn("Tony skin missing, mark helmet-open skins skipped", exception);
         return;
      }

      try {
         for (MarkId mark : MarkId.values()) {
            this.openSkins[mark.ordinal()] = bakeHead(resources, textures, tony, MarkTextures.skin(mark), "dynamic/ironman/mark_open_" + mark.key());
            this.openGlows[mark.ordinal()] = bakeHead(resources, textures, null, MarkTextures.glow(mark), "dynamic/ironman/mark_open_glow_" + mark.key());
         }
      } finally {
         tony.close();
      }
   }

   private static ResourceLocation bakeHead(ResourceManager resources, TextureManager textures, NativeImage tony, ResourceLocation source, String name) {
      try (NativeImage image = read(resources, source)) {
         // Only the faceplate opens (head front + hat front): the rest of the helmet stays on.
         for (int y = 0; y < HEAD_ROWS && y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
               if (dev.baranhan.viltrumitecore.client.ironman.IronManSuitTextures.facePixel(x, y)) {
                  image.setPixelRGBA(x, y, tony == null ? 0 : pixel(tony, x, y));
               }
            }
         }

         ResourceLocation location = new ResourceLocation("viltrumitecore", name);
         textures.release(location);
         textures.register(location, new DynamicTexture(copy(image)));
         return location;
      } catch (IOException | RuntimeException exception) {
         return null;
      }
   }

   private static NativeImage copy(NativeImage source) {
      NativeImage out = new NativeImage(source.getWidth(), source.getHeight(), true);
      out.copyFrom(source);
      return out;
   }

   private static int pixel(NativeImage image, int x, int y) {
      return x < image.getWidth() && y < image.getHeight() ? image.getPixelRGBA(x, y) : 0;
   }

   private static NativeImage read(ResourceManager resources, ResourceLocation location) throws IOException {
      try (InputStream stream = resources.open(location)) {
         return NativeImage.read(stream);
      }
   }
}
