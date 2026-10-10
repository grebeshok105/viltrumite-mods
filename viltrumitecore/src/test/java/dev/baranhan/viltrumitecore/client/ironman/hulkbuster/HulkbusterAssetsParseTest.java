package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
   void sindBodyParsesWithTheBonesTheRendererUses() throws IOException {
      BakedGeoModel body = load("geo/ironman/hulkbuster/mark48.geo.json");
      assertEquals(List.of("bone"), body.topLevelBones().stream().map(b -> b.name).toList());
      for (String name : List.of(HulkbusterAssets.HEAD_BONE, HulkbusterAssets.LEFT_ARM_BONE, "bodyBuster", "rightArmBuster", "lowerRightArm", "leftArmBuster",
         "lowerLeftArm", "rightLegBuster", "leftLegBuster", "facePlate")) {
         assertNotNull(body.getBone(name), name);
      }

      BakedGeoModel jackhammer = load("geo/ironman/hulkbuster/jackhammer.geo.json");
      assertNotNull(jackhammer.getBone("wholeHandLeft"));
      assertNotNull(load("geo/ironman/hulkbuster/fire.geo.json").getBone("repulsorFootRight"));
      assertFalse(load("geo/ironman/hulkbuster/jackhammer_fire.geo.json").topLevelBones().isEmpty());
   }

   @Test
   void firstPersonArmsSitUnderTheVanillaArmBones() throws IOException {
      BakedGeoModel arms = load("geo/ironman/hulkbuster/fp_arm.geo.json");
      assertEquals(List.of("armorRightArm", "armorLeftArm"), arms.topLevelBones().stream().map(b -> b.name).toList());
      assertEquals(PlayerBoneMap.Part.RIGHT_ARM, PlayerBoneMap.of("armorRightArm"));
      assertEquals(PlayerBoneMap.Part.LEFT_ARM, PlayerBoneMap.of("armorLeftArm"));
      assertNotNull(arms.getBone("rightArmBuster"));
      assertNotNull(arms.getBone("leftArmBuster"));
   }

   @Test
   void dockingGroupsParseAsVanillaLimbBones() throws IOException {
      BakedGeoModel parts = load("geo/ironman/hulkbuster/parts.geo.json");
      List<String> tops = parts.topLevelBones().stream().map(b -> b.name).toList();
      for (String[] group : HulkbusterAssembly.BONES) {
         for (String name : group) {
            assertTrue(tops.contains(name), name);
            assertNotNull(PlayerBoneMap.of(name), name);
            assertTrue(hasCubes(parts.getBone(name)), name);
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

   private static boolean hasCubes(dev.baranhan.viltrumitecore.client.anim.geo.GeoBone bone) {
      return !bone.cubes.isEmpty() || bone.children.stream().anyMatch(HulkbusterAssetsParseTest::hasCubes);
   }
}
