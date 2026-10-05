package dev.baranhan.viltrumiteflight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraftforge.fml.loading.FMLPaths;

public class ViltrumiteConfigClient {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final File FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "viltrumiteflight-client.json");
   public boolean enableFovEffect = true;
   public float fovMultiplier = 0.5F;
   public float smoothFov = 200.0F;
   public boolean enableWindLoopSound = true;
   public float windVolumeMultiplier = 1.0F;
   public float flightRotY = 0.85F;
   public boolean enableSonicBoomSound = true;
   public float sonicBoomVolume = 5.0F;
   public float maxBankAngle = 120.0F;
   public static ViltrumiteConfigClient INSTANCE = new ViltrumiteConfigClient();

   public static void load() {
      if (FILE.exists()) {
         try (FileReader reader = new FileReader(FILE)) {
            INSTANCE = (ViltrumiteConfigClient)GSON.fromJson(reader, ViltrumiteConfigClient.class);
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
