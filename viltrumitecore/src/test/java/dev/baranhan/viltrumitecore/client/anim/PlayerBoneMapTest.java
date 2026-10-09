package dev.baranhan.viltrumitecore.client.anim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
      // Default pose → zero bone offset (geo pivots equal vanilla pivots).
      assertArrayEquals(new float[]{0, 0, 0}, PlayerBoneMap.position(PlayerBoneMap.Part.RIGHT_ARM, -5.0F, 2.0F, 0.0F), 1.0E-5F);
      assertArrayEquals(new float[]{0, 0, 0}, PlayerBoneMap.position(PlayerBoneMap.Part.LEFT_LEG, 1.9F, 12.0F, 0.0F), 1.0E-5F);
      // Sneak lowers the body pivot by 3.2 px → the geo bone moves down.
      assertArrayEquals(new float[]{0, -3.2F, 0}, PlayerBoneMap.position(PlayerBoneMap.Part.BODY, 0.0F, 3.2F, 0.0F), 1.0E-5F);
      assertArrayEquals(new float[]{-0.5F, -0.25F, 0.75F}, PlayerBoneMap.rotation(0.5F, 0.25F, 0.75F), 1.0E-6F);
   }

   @Test
   void unknownBoneSkipped() {
      assertNull(PlayerBoneMap.of("shoulder_rockets"));
      assertNull(PlayerBoneMap.of("head"));
      assertNull(PlayerBoneMap.of(null));
   }
}
