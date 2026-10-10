package dev.baranhan.viltrumitecore.client.ironman.mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.geo.GeoBone;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerBoneMap;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The Sind Mark 42 pieces, rocket fist and Mark 7 laser (convert_ironman_sind.py) parse as the client draws them. */
class SindMark42AssetsTest {
   private static final Path ROOT = Path.of("src/main/resources/assets/viltrumitecore");

   private static BakedGeoModel load(String namespaced) throws IOException {
      String relative = namespaced.substring(namespaced.indexOf(':') + 1);
      JsonObject root = JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();
      return BakedGeoModel.parse(root);
   }

   private static boolean hasCubes(GeoBone bone) {
      return !bone.cubes.isEmpty() || bone.children.stream().anyMatch(SindMark42AssetsTest::hasCubes);
   }

   @Test
   void fourteenPiecesSitOnTheirLimbs() throws IOException {
      assertEquals(14, SuitPart.of(MarkId.MARK_42).size());
      for (SuitPart part : SuitPart.of(MarkId.MARK_42)) {
         BakedGeoModel geo = load(MarkParts.geo(MarkId.MARK_42, part).toString());
         assertEquals(1, geo.topLevelBones().size(), part.name());
         GeoBone top = geo.topLevelBones().get(0);
         assertEquals(part.bone().name(), PlayerBoneMap.of(top.name).name(), part.name());
         assertTrue(hasCubes(top), part.name());
         BakedGeoModel fire = load(Mark42Parts.fire(part).toString());
         assertFalse(fire.topLevelBones().isEmpty(), part.name());
      }
   }

   @Test
   void helmetCarriesTheFaceplate() throws IOException {
      BakedGeoModel helmet = load(Mark42Parts.geo(SuitPart.FOURTEEN.get(13)).toString());
      assertNotNull(helmet.getBone("faceplate_42Faceplate"));
   }

   @Test
   void fistAndLaserParse() throws IOException {
      BakedGeoModel fist = load("viltrumitecore:geo/ironman/marks/fist.geo.json");
      assertEquals("glove", fist.topLevelBones().get(0).name);
      BakedGeoModel laser = load("viltrumitecore:geo/ironman/marks/laser_emitters.geo.json");
      assertEquals(PlayerBoneMap.Part.RIGHT_ARM, PlayerBoneMap.of(laser.topLevelBones().get(0).name));
      for (String bone : new String[]{"laser", "emitter", "coverLayer"}) {
         assertNotNull(laser.getBone(bone), bone);
      }
   }
}
