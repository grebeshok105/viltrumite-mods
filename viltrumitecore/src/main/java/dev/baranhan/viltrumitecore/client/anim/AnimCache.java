package dev.baranhan.viltrumitecore.client.anim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import java.io.BufferedReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnimCache implements ResourceManagerReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("viltrumitecore/anim");
   public static final String MOD_ID = "viltrumitecore";
   private static Map<ResourceLocation, BakedGeoModel> models = Collections.emptyMap();
   private static Map<ResourceLocation, Map<String, Animation>> animationFiles = Collections.emptyMap();

   public void onResourceManagerReload(ResourceManager manager) {
      Map<ResourceLocation, BakedGeoModel> newModels = new HashMap<>();
      Map<ResourceLocation, Map<String, Animation>> newAnimFiles = new HashMap<>();

      for (Entry<ResourceLocation, Resource> entry : manager.listResources("geo", id -> id.getPath().endsWith(".geo.json")).entrySet()) {
         ResourceLocation geoPath = entry.getKey();

         try (BufferedReader reader = entry.getValue().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            newModels.put(geoPath, BakedGeoModel.parse(json));
         } catch (Exception var15) {
            LOGGER.error("Geo modeli yuklenemedi: {}", geoPath, var15);
         }
      }

      for (Entry<ResourceLocation, Resource> entry : manager.listResources("animations", id -> id.getPath().endsWith(".animation.json")).entrySet()) {
         try (BufferedReader reader = entry.getValue().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            newAnimFiles.put(entry.getKey(), AnimationParser.parse(json));
            if (!AnimationParser.lastWarnings.isEmpty()) {
               LOGGER.warn("{} icinde desteklenmeyen molang ifadeleri atlandi: {}", entry.getKey(), AnimationParser.lastWarnings);
            }
         } catch (Exception var13) {
            LOGGER.error("Animasyon yuklenemedi: {}", entry.getKey(), var13);
         }
      }

      models = newModels;
      animationFiles = newAnimFiles;
      int animCount = animationFiles.values().stream().mapToInt(Map::size).sum();
      LOGGER.info("{} geo modeli, {} animasyon yuklendi.", models.size(), animCount);
   }

   public static ResourceLocation itemGeo(String name) {
      return new ResourceLocation("viltrumitecore", "geo/item/" + name + ".geo.json");
   }

   public static ResourceLocation itemAnimations(String name) {
      return new ResourceLocation("viltrumitecore", "animations/item/" + name + ".animation.json");
   }

   public static ResourceLocation itemTexture(String name) {
      return new ResourceLocation("viltrumitecore", "textures/item/" + name + ".png");
   }

   public static BakedGeoModel model(ResourceLocation geoPath) {
      return models.get(geoPath);
   }

   public static Map<String, Animation> animations(ResourceLocation animationPath) {
      return animationFiles.getOrDefault(animationPath, Collections.emptyMap());
   }

   public static Animation animation(ResourceLocation animationPath, String animationName) {
      return animations(animationPath).get(animationName);
   }

   @EventBusSubscriber(
      modid = "viltrumitecore",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static final class Registrar {
      @SubscribeEvent
      public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
         event.registerReloadListener(new AnimCache());
      }

      private Registrar() {
      }
   }
}
