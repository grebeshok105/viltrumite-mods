package dev.baranhan.viltrumitecore.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.baranhan.viltrumitecore.client.anim.render.PlayerBoneMap;
import org.junit.jupiter.api.Test;

class PlayerBoneMapTest {
   @Test
   void mapsAllSixArmorBones() {
      assertEquals(PlayerBoneMap.Part.HEAD, PlayerBoneMap.of("armorHead"));
      assertEquals(PlayerBoneMap.Part.BODY, PlayerBoneMap.of("armorBody"));
      assertEquals(PlayerBoneMap.Part.RIGHT_ARM, PlayerBoneMap.of("armorRightArm"));
      assertEquals(PlayerBoneMap.Part.LEFT_ARM, PlayerBoneMap.of("armorLeftArm"));
      assertEquals(PlayerBoneMap.Part.RIGHT_LEG, PlayerBoneMap.of("armorRightLeg"));
      assertEquals(PlayerBoneMap.Part.LEFT_LEG, PlayerBoneMap.of("armorLeftLeg"));
   }

   @Test
   void unknownBoneSkipped() {
      assertNull(PlayerBoneMap.of("antenna"));
      assertNull(PlayerBoneMap.of("bb_main"));
      assertNull(PlayerBoneMap.of(null));
   }

   @Test
   void pivotsMatchVanillaPlayerModel() {
      assertEquals(-5.0F, PlayerBoneMap.Part.RIGHT_ARM.pivotX);
      assertEquals(2.0F, PlayerBoneMap.Part.LEFT_ARM.pivotY);
      assertEquals(12.0F, PlayerBoneMap.Part.LEFT_LEG.pivotY);
   }
}
