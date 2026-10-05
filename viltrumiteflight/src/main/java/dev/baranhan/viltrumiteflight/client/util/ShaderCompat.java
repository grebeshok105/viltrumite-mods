package dev.baranhan.viltrumiteflight.client.util;

import java.lang.reflect.Field;
import net.minecraftforge.fml.ModList;

public class ShaderCompat {
   private static boolean initialized = false;
   private static Field activeShadowField = null;

   public static boolean isShadowPass() {
      if (!initialized) {
         boolean isShaderModLoaded = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
         if (isShaderModLoaded) {
            String[] possibleClassNames = new String[]{
               "net.irisshaders.iris.shadows.ShadowRenderer",
               "net.coderbot.iris.pipeline.ShadowRenderer",
               "net.coderbot.iris.shadows.ShadowRenderer",
               "net.irisshaders.iris.pipeline.ShadowRenderer"
            };

            for (String className : possibleClassNames) {
               try {
                  Class<?> shadowRendererClass = Class.forName(className);
                  activeShadowField = shadowRendererClass.getDeclaredField("ACTIVE");
                  activeShadowField.setAccessible(true);
                  System.out.println("[ViltrumiteCore] BINGO! G\u00f6lge motoru bulundu: " + className);
                  break;
               } catch (Exception var8) {
               }
            }

            if (activeShadowField == null) {
               System.out
                  .println("[ViltrumiteCore] UYARI: Iris/Oculus y\u00fckl\u00fc ama ShadowRenderer bulunamad\u0131! G\u00f6lge lag\u0131 ya\u015fanabilir.");
            }
         }

         initialized = true;
      }

      if (activeShadowField != null) {
         try {
            return activeShadowField.getBoolean(null);
         } catch (Exception var7) {
            return false;
         }
      } else {
         return false;
      }
   }
}
