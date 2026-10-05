package dev.baranhan.viltrumitecore.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ViltrumiteShaders {
   private static final Logger LOGGER = LoggerFactory.getLogger("viltrumitecore/shaders");
   private static final ResourceLocation SHADER = new ResourceLocation("viltrumitecore", "shaders/post/dash_impact.json");
   private static PostChain chain;
   private static boolean failed;
   private static int lastWidth = -1;
   private static int lastHeight = -1;

   public static PostChain get() {
      if (failed) {
         return null;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         RenderTarget main = minecraft.getMainRenderTarget();
         if (chain == null) {
            try {
               chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), main, SHADER);
               chain.resize(main.width, main.height);
               lastWidth = main.width;
               lastHeight = main.height;
            } catch (Exception var3) {
               LOGGER.error("Post shader yuklenemedi: {}", SHADER, var3);
               failed = true;
               chain = null;
               return null;
            }
         }

         if (main.width != lastWidth || main.height != lastHeight) {
            chain.resize(main.width, main.height);
            lastWidth = main.width;
            lastHeight = main.height;
         }

         return chain;
      }
   }

   public static void process(float partialTick) {
      PostChain active = get();
      if (active != null) {
         RenderSystem.disableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.resetTextureMatrix();
         active.process(partialTick);
         Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
      }
   }

   public static void invalidate() {
      if (chain != null) {
         chain.close();
         chain = null;
      }

      failed = false;
      lastWidth = -1;
      lastHeight = -1;
   }

   private ViltrumiteShaders() {
   }

   @EventBusSubscriber(
      modid = "viltrumitecore",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static final class Registrar {
      @SubscribeEvent
      public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
         event.registerReloadListener((ResourceManagerReloadListener)manager -> ViltrumiteShaders.invalidate());
      }

      private Registrar() {
      }
   }
}
