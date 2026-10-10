package dev.baranhan.viltrumitecore.client.ironman.mark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerBoneMap;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.SuitPart;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Satsu mark assets (bake_suit_skin.py, convert_ironman_stage4_marks.py) parse with the bones the client uses. */
class SatsuMarkAssetsTest {
   private static final Path ROOT = Path.of("src/main/resources/assets/viltrumitecore");
   private static final Path BIN = Path.of("src/main/binassets/assets/viltrumitecore");

   private static BakedGeoModel load(String relative) throws IOException {
      JsonObject root = JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();
      return BakedGeoModel.parse(root);
   }

   private static String path(String namespaced) {
      return namespaced.substring(namespaced.indexOf(':') + 1);
   }

   @Test
   void everySatsuPartIsOneArmorBoneOnItsLimb() throws IOException {
      for (MarkId mark : MarkId.values()) {
         if (mark == MarkId.MARK_42) {
            continue;
         }

         for (SuitPart part : SuitPart.of(mark)) {
            BakedGeoModel geo = load(path(MarkParts.geo(mark, part).toString()));
            assertEquals(1, geo.topLevelBones().size(), part.name());
            PlayerBoneMap.Part limb = PlayerBoneMap.of(geo.topLevelBones().get(0).name);
            assertEquals(part.bone().name(), limb.name(), part.name());
            assertFalse(geo.topLevelBones().get(0).cubes.isEmpty(), part.name());
         }
      }
   }

   @Test
   void sevenPartArmsCarryTheShoulder() {
      assertEquals("left_arm_full", MarkParts.piece(SuitPart.SEVEN.get(2)));
      assertEquals("left_arm", MarkParts.piece(SuitPart.NINE.get(2)));
      assertEquals("head", MarkParts.piece(SuitPart.NINE.get(8)));
   }

   @Test
   void extrasAndSignaturePartsParse() throws IOException {
      for (MarkId mark : List.of(MarkId.MARK_7, MarkId.MARK_17, MarkId.WAR_MACHINE_MK2, MarkId.IRON_HEART_MK3)) {
         BakedGeoModel geo = load(path(MarkExtras.geo(mark).toString()));
         assertFalse(geo.topLevelBones().isEmpty(), mark.key());
         geo.topLevelBones().forEach(b -> assertNotNull(PlayerBoneMap.of(b.name), b.name));
         assertTrue(Files.isRegularFile(BIN.resolve("textures/entity/ironman/marks/" + mark.key() + "_suit.png.b64")), mark.key());
      }

      assertNull(MarkExtras.geo(MarkId.MARK_15));
      BakedGeoModel flaps = load("geo/ironman/marks/extras/mark_7.geo.json");
      for (String bone : List.of("leftupflap", "rightupflap", "lowrightflap", "lowleftflap")) {
         assertNotNull(flaps.getBone(bone), bone);
      }

      assertNotNull(load("geo/ironman/marks/booster.geo.json").getBone("jetpack"));
      assertNotNull(load("geo/ironman/marks/booster_blast.geo.json").getBone("boosterBlast"));
      BakedGeoModel turret = load("geo/ironman/marks/turret.geo.json");
      assertNotNull(turret.getBone("turret"));
      assertNotNull(turret.getBone("turretweapon"));
   }

   @Test
   void turretClipsAndEmptySuitParse() throws IOException {
      AnimationParser.lastWarnings.clear();
      JsonObject clips = JsonParser.parseString(Files.readString(ROOT.resolve("animations/ironman/marks/war_machine_turret.animation.json"))).getAsJsonObject();
      assertTrue(AnimationParser.parse(clips).keySet().containsAll(List.of("start_on", "end")));
      assertTrue(AnimationParser.lastWarnings.isEmpty(), AnimationParser.lastWarnings.toString());
      BakedGeoModel shell = load("geo/ironman/marks/empty_suit.geo.json");
      for (String bone : List.of("armorhead", "armorbody", "armorrightarm", "armorleftarm", "armorrightleg", "armorleftleg")) {
         assertNotNull(shell.getBone(bone), bone);
      }

      assertFalse(load("geo/ironman/marks/suit_expulsion.geo.json").getBone("armorbody").cubes.isEmpty());
   }
}
