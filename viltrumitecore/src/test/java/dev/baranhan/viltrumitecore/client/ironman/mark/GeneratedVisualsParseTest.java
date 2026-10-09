package dev.baranhan.viltrumitecore.client.ironman.mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GeneratedVisualsParseTest {
   private static final Path ROOT = Path.of("src/main/resources/assets/viltrumitecore");

   private static BakedGeoModel load(String relative) throws IOException {
      JsonObject root = JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();
      return BakedGeoModel.parse(root);
   }

   @Test
   void everyPlateParsesToOneArmorBoneWithCubes() throws IOException {
      for (MarkId mark : MarkId.values()) {
         for (SuitPart part : SuitPart.of(mark)) {
            BakedGeoModel geo = load("geo/ironman/marks/plates/" + PlateParts.key(part) + ".geo.json");
            assertEquals(1, geo.topLevelBones().size(), PlateParts.key(part));
            assertFalse(geo.topLevelBones().get(0).cubes.isEmpty(), PlateParts.key(part));
            assertTrue(PlateParts.centre(geo).length() > 0.0, PlateParts.key(part));
         }
      }
   }

   @Test
   void podParsesWithDoorsAsChildren() throws IOException {
      BakedGeoModel pod = load("geo/ironman/veronica/veronica_pod.geo.json");
      assertEquals(1, pod.topLevelBones().size());
      assertEquals(2, pod.topLevelBones().get(0).children.size());
      assertTrue(pod.getBone("door_left") != null && pod.getBone("door_right") != null);
   }

   @Test
   void emptySuitAndShieldParse() throws IOException {
      assertEquals(6, load("geo/ironman/marks/empty_suit.geo.json").topLevelBones().size());
      assertEquals(6, load("geo/ironman/marks/empty_suit_interior.geo.json").topLevelBones().size());
      assertEquals(1, load("geo/ironman/marks/energy_shield.geo.json").topLevelBones().size());
   }

   @Test
   void generatedAnimationsParseWithoutWarnings() throws IOException {
      AnimationParser.lastWarnings.clear();
      JsonObject pod = JsonParser.parseString(Files.readString(ROOT.resolve("animations/ironman/veronica_pod.animation.json"))).getAsJsonObject();
      assertTrue(AnimationParser.parse(pod).keySet().containsAll(java.util.List.of("open", "close")));
      JsonObject suit = JsonParser.parseString(Files.readString(ROOT.resolve("animations/ironman/empty_suit.animation.json"))).getAsJsonObject();
      assertTrue(AnimationParser.parse(suit).containsKey("open"));
      assertTrue(AnimationParser.lastWarnings.isEmpty(), AnimationParser.lastWarnings.toString());
   }
}
