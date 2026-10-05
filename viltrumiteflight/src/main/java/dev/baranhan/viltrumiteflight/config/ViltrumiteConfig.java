package dev.baranhan.viltrumiteflight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraftforge.fml.loading.FMLPaths;

public class ViltrumiteConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final File FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "viltrumiteflight.json");
   public float maxFlightSpeed = 9.0F;
   public float throttleSpeed = 0.017F;
   public boolean isHeatEnabled = true;
   public boolean breakBlocksOnTakeoff = true;
   public static ViltrumiteConfig INSTANCE = new ViltrumiteConfig();

   public static void load() {
      if (FILE.exists()) {
         try (FileReader reader = new FileReader(FILE)) {
            INSTANCE = (ViltrumiteConfig)GSON.fromJson(reader, ViltrumiteConfig.class);
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
