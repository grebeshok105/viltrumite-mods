package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.platform.NativeImage;
import dev.baranhan.viltrumitecore.client.anim.render.RevealMask;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The 16 reveal frames of the Mark 50 skin (spike: frames composited into the
 * player skin, vanilla render path). Frame k shows the suit where
 * RevealMask ≤ k/16, Tony elsewhere, and a cyan nanite rim on the front; the
 * glow frames mask the suit's light texture the same way. Baked lazily on the
 * render thread, dropped on resource reload.
 */
public final class IronManSkinFrames implements ResourceManagerReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("viltrumitecore/ironman");
   public static final ResourceLocation SUIT = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_mark_50.png");
   public static final ResourceLocation SUIT_GLOW = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_mark_50_glow.png");
   /** Reactor in body space (pixels, y up from the feet, front face). */
   static final float[] FIELD = RevealMask.playerSkinField(0.0F, 20.5F, -2.0F, 0.2F);
   private static final int RIM_BRIGHT = 0xFFFFE866;
   private static final int RIM_DARK = 0xFFD09F2A;
   private static ResourceLocation[] skins;
   private static ResourceLocation[] glows;
   private static boolean failed;

   /** Skin for a frame 0..16 (0 = Tony, 16 = full suit). */
   public static ResourceLocation skin(int frame) {
      if (frame <= 0) {
         return IronManClient.SKIN;
      }

      if (frame >= RevealMask.FRAMES || !ensure()) {
         return SUIT;
      }

      return skins[frame];
   }

   /** Glow for a frame, or null for frame 0. */
   @Nullable
   public static ResourceLocation glow(int frame) {
      if (frame <= 0) {
         return null;
      }

      if (frame >= RevealMask.FRAMES || !ensure()) {
         return SUIT_GLOW;
      }

      return glows[frame];
   }

   private static boolean ensure() {
      if (skins != null) {
         return true;
      }

      if (failed) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      try (NativeImage tony = read(mc, IronManClient.SKIN); NativeImage suit = read(mc, SUIT); NativeImage light = read(mc, SUIT_GLOW)) {
         TextureManager textures = mc.getTextureManager();
         ResourceLocation[] newSkins = new ResourceLocation[RevealMask.FRAMES];
         ResourceLocation[] newGlows = new ResourceLocation[RevealMask.FRAMES];
         for (int k = 1; k < RevealMask.FRAMES; k++) {
            NativeImage skin = new NativeImage(64, 64, true);
            NativeImage glow = new NativeImage(64, 64, true);
            bake(k, tony, suit, light, skin, glow);
            newSkins[k] = textures.register("viltrumitecore_ironman_reveal_" + k, new DynamicTexture(skin));
            newGlows[k] = textures.register("viltrumitecore_ironman_reveal_glow_" + k, new DynamicTexture(glow));
         }

         skins = newSkins;
         glows = newGlows;
         return true;
      } catch (Exception e) {
         failed = true;
         LOGGER.error("Iron Man reveal frames could not be baked; the suit shows without the wave", e);
         return false;
      }
   }

   /** Pixel colours are ABGR in NativeImage. */
   static void bake(int frame, NativeImage tony, NativeImage suit, NativeImage light, NativeImage outSkin, NativeImage outGlow) {
      float progress = frame / (float)RevealMask.FRAMES;
      for (int v = 0; v < 64; v++) {
         for (int u = 0; u < 64; u++) {
            float t = FIELD[v * 64 + u];
            boolean shown = RevealMask.visible(t, progress);
            int skin = shown ? suit.getPixelRGBA(u, v) : tony.getPixelRGBA(u, v);
            int glow = shown ? light.getPixelRGBA(u, v) : 0;
            if (RevealMask.rim(t, frame) && (suit.getPixelRGBA(u, v) >>> 24) != 0) {
               // Hex-ish nanite scales: alternating bright/dark cyan on the wave front.
               int rim = (u + 2 * v) % 3 == 0 ? RIM_DARK : RIM_BRIGHT;
               skin = rim;
               glow = rim;
            }

            outSkin.setPixelRGBA(u, v, skin);
            outGlow.setPixelRGBA(u, v, glow);
         }
      }
   }

   private static NativeImage read(Minecraft mc, ResourceLocation location) throws java.io.IOException {
      try (InputStream in = mc.getResourceManager().getResourceOrThrow(location).open()) {
         return NativeImage.read(in);
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager manager) {
      Minecraft mc = Minecraft.getInstance();
      if (skins != null) {
         for (int k = 1; k < RevealMask.FRAMES; k++) {
            mc.getTextureManager().release(skins[k]);
            mc.getTextureManager().release(glows[k]);
         }
      }

      skins = null;
      glows = null;
      failed = false;
   }

   @EventBusSubscriber(modid = "viltrumitecore", bus = Bus.MOD, value = {Dist.CLIENT})
   public static final class Registration {
      private Registration() {
      }

      @SubscribeEvent
      public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
         event.registerReloadListener(new IronManSkinFrames());
      }
   }
}
