package dev.baranhan.viltrumiteflight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraftforge.fml.loading.FMLPaths;

public class ViltrumiteFlightCameraConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final File FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "viltrumiteflight-camera.json");
   public boolean cameraRoll = true;
   public float maxCameraRoll = 80.0F;
   public float cameraRollMultiplier = 0.11F;
   public float cameraRollRoughness = 7.0F;
   public static ViltrumiteFlightCameraConfig INSTANCE = new ViltrumiteFlightCameraConfig();

   public static void load() {
      if (FILE.exists()) {
         try (FileReader reader = new FileReader(FILE)) {
            INSTANCE = (ViltrumiteFlightCameraConfig)GSON.fromJson(reader, ViltrumiteFlightCameraConfig.class);
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      } else {
         save();
      }
   }

   public static void save() {
      try (FileWriter writer = new FileWriter(FILE)) {
         GSON.toJson(INSTANCE, writer);
      } catch (IOException var5) {
         var5.printStackTrace();
      }
   }
}
