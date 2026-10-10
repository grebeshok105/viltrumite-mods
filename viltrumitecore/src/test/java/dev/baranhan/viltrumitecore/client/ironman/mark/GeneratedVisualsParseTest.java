package dev.baranhan.viltrumitecore.client.ironman.mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
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
