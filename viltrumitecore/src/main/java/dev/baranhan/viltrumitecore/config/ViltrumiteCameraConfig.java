package dev.baranhan.viltrumitecore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraftforge.fml.loading.FMLPaths;

public class ViltrumiteCameraConfig {
   public boolean enableCustomCamera = false;
   public float cameraOffsetX = -0.755F;
   public float cameraOffsetY = 0.135F;
   public float cameraOffsetZ = 1.025F;
   public boolean enableThirdPersonCrosshair = false;
   public static ViltrumiteCameraConfig INSTANCE = new ViltrumiteCameraConfig();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "viltrumitecore_camera.json");

   public static void load() {
      if (CONFIG_FILE.exists()) {
         try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ViltrumiteCameraConfig loaded = (ViltrumiteCameraConfig)GSON.fromJson(reader, ViltrumiteCameraConfig.class);
            if (loaded != null) {
               INSTANCE = loaded;
            }
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      } else {
         save();
      }
   }

   public static void save() {
      try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
         GSON.toJson(INSTANCE, writer);
      } catch (IOException var5) {
         var5.printStackTrace();
      }
   }
}
