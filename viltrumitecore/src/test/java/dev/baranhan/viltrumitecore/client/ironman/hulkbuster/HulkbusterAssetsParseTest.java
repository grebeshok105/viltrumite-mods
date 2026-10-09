package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.baranhan.viltrumitecore.client.anim.animation.Animation;
import dev.baranhan.viltrumitecore.client.anim.animation.AnimationParser;
import dev.baranhan.viltrumitecore.client.anim.geo.BakedGeoModel;
import dev.baranhan.viltrumitecore.client.anim.render.PlayerBoneMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HulkbusterAssetsParseTest {
   private static final Path ROOT = Path.of("src/main/resources/assets/viltrumitecore");

   private static BakedGeoModel load(String relative) throws IOException {
      JsonObject root = JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();
      return BakedGeoModel.parse(root);
   }

   @Test
   void bodyParsesWithTheTimelineBones() throws IOException {
      BakedGeoModel body = load("geo/ironman/hulkbuster/mark48.geo.json");
      assertEquals(List.of("torso", "right_leg", "left_leg"), body.topLevelBones().stream().map(b -> b.name).toList());
      for (String name : List.of("head", "back_plate_left", "back_plate_right", "right_upper_arm", "right_forearm", "right_fist", "left_fist",
         "jackhammer", "flame_feet_left", "flame_feet_right", "flame_back_left", "flame_back_right", "right_foot")) {
         assertNotNull(body.getBone(name), name);
      }
   }

   @Test
   void firstPersonArmsSitUnderTheVanillaArmBones() throws IOException {
      BakedGeoModel arms = load("geo/ironman/hulkbuster/fp_arm.geo.json");
      assertEquals(List.of("armorRightArm", "armorLeftArm"), arms.topLevelBones().stream().map(b -> b.name).toList());
      assertEquals(PlayerBoneMap.Part.RIGHT_ARM, PlayerBoneMap.of("armorRightArm"));
      assertEquals(PlayerBoneMap.Part.LEFT_ARM, PlayerBoneMap.of("armorLeftArm"));
      assertNotNull(arms.getBone("right_fist"));
      assertNotNull(arms.getBone("jackhammer"));
   }

   @Test
   void dockingGroupsParseAsVanillaLimbBones() throws IOException {
      BakedGeoModel parts = load("geo/ironman/hulkbuster/parts.geo.json");
      List<String> tops = parts.topLevelBones().stream().map(b -> b.name).toList();
      for (String[] group : HulkbusterAssembly.BONES) {
         for (String name : group) {
            assertTrue(tops.contains(name), name);
            assertNotNull(PlayerBoneMap.of(name), name);
            assertTrue(!parts.getBone(name).cubes.isEmpty(), name);
         }
      }

      assertEquals(Arrays.stream(HulkbusterAssembly.BONES).mapToInt(g -> g.length).sum(), tops.size());
   }

   @Test
   void clipsParseWithTheLengthsAndLoopsOfThePoseCode() throws IOException {
      AnimationParser.lastWarnings.clear();
      JsonObject root = JsonParser.parseString(Files.readString(ROOT.resolve("animations/ironman/hulkbuster/mark48.animation.json"))).getAsJsonObject();
      Map<String, Animation> clips = AnimationParser.parse(root);
      assertTrue(AnimationParser.lastWarnings.isEmpty(), AnimationParser.lastWarnings.toString());
      for (HulkbusterPoses.Clip clip : HulkbusterPoses.Clip.values()) {
         Animation animation = clips.get(clip.animation());
         assertNotNull(animation, clip.animation());
         assertEquals(clip.length(), animation.lengthSeconds(), 1.0E-9, clip.animation());
         assertEquals(clip.loop(), animation.loopType() == Animation.LoopType.LOOP, clip.animation());
      }

      assertEquals(HulkbusterPoses.Clip.values().length, clips.size());
   }
}
