package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerBoneMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Satsu combat parts (convert_ironman_stage2_parts.py) parse with the bones the client poses. */
class CombatPartsParseTest {
   private static final Path ROOT = Path.of("src/main/resources/assets/viltrumitecore");
   private static final Path BIN = Path.of("src/main/binassets/assets/viltrumitecore");

   private static BakedGeoModel load(String relative) throws IOException {
      JsonObject root = JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();
      return BakedGeoModel.parse(root);
   }

   private static void assertTop(BakedGeoModel geo, PlayerBoneMap.Part part) {
      assertEquals(1, geo.topLevelBones().size());
      assertEquals(part, PlayerBoneMap.of(geo.topLevelBones().get(0).name));
   }

   @Test
   void weaponsGrowFromAChildBoneOnTheRightArm() throws IOException {
      for (String[] weapon : List.of(new String[]{"nano_blade", "blade"}, new String[]{"nano_hammer", "hammer"},
         new String[]{"rocket_launcher", "rocket_launcher"}, new String[]{"rocket_launcher_first_person", "rocket_launcher"})) {
         BakedGeoModel geo = load("geo/ironman/nano/" + weapon[0] + ".geo.json");
         assertTop(geo, PlayerBoneMap.Part.RIGHT_ARM);
         assertNotNull(geo.getBone(weapon[1]), weapon[0]);
         assertFalse(geo.getBone(weapon[1]).cubes.isEmpty(), weapon[0]);
      }
   }

   @Test
   void shieldsAndBodyPartsSitOnTheirPlayerParts() throws IOException {
      assertTop(load("geo/ironman/nano/nano_shield.geo.json"), PlayerBoneMap.Part.LEFT_ARM);
      assertTop(load("geo/ironman/marks/energy_shield.geo.json"), PlayerBoneMap.Part.BODY);
      assertTop(load("geo/ironman/marks/energy_shield_first_person.geo.json"), PlayerBoneMap.Part.RIGHT_ARM);
      BakedGeoModel rockets = load("geo/ironman/nano/shoulder_rockets.geo.json");
      assertTop(rockets, PlayerBoneMap.Part.BODY);
      assertNotNull(rockets.getBone("shoulder_rockets"));
      BakedGeoModel rods = load("geo/ironman/nano/stabilizer.geo.json");
      assertTop(rods, PlayerBoneMap.Part.BODY);
      assertEquals(4, rods.topLevelBones().get(0).children.size());
   }

   @Test
   void fourStabilizerFlamesOnTheRodTips() throws IOException {
      BakedGeoModel flames = load("geo/ironman/flames/stabilizer.geo.json");
      for (String name : List.of("stabRight", "stabLeft", "stabRightLow", "stabLeftLow")) {
         assertNotNull(flames.getBone(name), name);
      }
   }

   @Test
   void everyPartTextureIsCommitted() {
      for (String name : List.of("nano_blade", "nano_blade_glow", "nano_hammer", "nano_hammer_glow", "nano_shield", "rocket_launcher", "stabilizer",
         "stabilizer_glow")) {
         assertTrue(Files.isRegularFile(BIN.resolve("textures/entity/ironman/nano/" + name + ".png.b64")), name);
      }

      assertTrue(Files.isRegularFile(BIN.resolve("textures/entity/ironman/marks/energy_shield.png.b64")));
   }
}
