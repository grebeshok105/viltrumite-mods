package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSpec;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.Slam;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import org.junit.jupiter.api.Test;

class SlamTest {
   @Test
   void groundSlamJumpsThenDives() {
      double apex = SignatureRules.apexHeight(SignatureRules.slamJumpSpeed());
      assertEquals(SignatureRules.SLAM_JUMP_HEIGHT, apex, 0.2);
      assertTrue(SignatureRules.apexTicks(SignatureRules.slamJumpSpeed()) > 0);
      assertFalse(Slam.impactDue(Slam.JUMP, true, 10), "no impact while still jumping");
   }

   @Test
   void impactOncePerUse() {
      assertTrue(Slam.impactDue(Slam.DIVE, true, 5));
      assertFalse(Slam.impactDue(Slam.DIVE, true, 1), "grace ticks right after the dive starts");
      assertFalse(Slam.impactDue(Slam.DIVE, false, 5), "no impact in the air");
      assertFalse(Slam.impactDue(Slam.IDLE, true, 5), "a finished slam does not hit again");
   }

   @Test
   void recoilReduced() {
      MarkSpec spec = MarkSpec.of(MarkId.IRON_HEART_MK3);
      assertTrue(spec.recoilMul() < 1.0F);
      assertEquals(0.9, spec.knockbackRes(), 1.0E-9);
      assertEquals(240, SignatureRules.SLAM_COOLDOWN);
   }
}
