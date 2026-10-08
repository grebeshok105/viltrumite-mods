package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class HeroPublicSnapshotTest {

   private static HeroPublicSnapshot withResource(Vec3 target) {
      return new HeroPublicSnapshot(HeroId.HOMELANDER, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1,
         new int[]{120, 0, 0, 0, 0, 0}, false, -1, target, 735, true, 3);
   }

   @Test
   void encodeDecodeKeepsResourceFields() {
      HeroPublicSnapshot decoded = HeroPublicSnapshot.decode(withResource(null).encode());
      assertEquals(HeroId.HOMELANDER, decoded.heroId());
      assertEquals(735, decoded.resource());
      assertTrue(decoded.resourceLocked());
      assertEquals(3, decoded.heroFlags());
      assertTrue(decoded.heroFlag(0));
      assertTrue(decoded.heroFlag(1));
      assertFalse(decoded.heroFlag(2));
      assertEquals(120, decoded.cooldowns()[0]);
      assertNull(decoded.actionTarget());
      assertEquals(withResource(null), decoded);
   }

   @Test
   void encodeDecodeKeepsTargetAndResource() {
      HeroPublicSnapshot decoded = HeroPublicSnapshot.decode(withResource(new Vec3(1.5, 2, -3)).encode());
      assertEquals(new Vec3(1.5, 2, -3), decoded.actionTarget());
      assertEquals(735, decoded.resource());
   }

   @Test
   void oldEncodingDecodesWithDefaults() {
      // v1.13 encoding without target and without resource fields.
      String old = "regulus;-1;0;0;4;0;0;0;0;0;0;-1;-1;0;0;0;0;0;0;0;-1";
      HeroPublicSnapshot decoded = HeroPublicSnapshot.decode(old);
      assertEquals(HeroId.REGULUS, decoded.heroId());
      assertEquals(4, decoded.hearts());
      assertEquals(0, decoded.resource());
      assertFalse(decoded.resourceLocked());
      assertEquals(0, decoded.heroFlags());
   }

   @Test
   void distinctResourceIsNotEqual() {
      HeroPublicSnapshot a = withResource(null);
      HeroPublicSnapshot b = new HeroPublicSnapshot(HeroId.HOMELANDER, -1, 0, 0, 0, false, 0, 0, false, false, 0, 0, -1,
         new int[]{120, 0, 0, 0, 0, 0}, false, -1, null, 736, true, 3);
      assertFalse(a.equals(b));
   }
}
