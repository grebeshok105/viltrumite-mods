package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.RocketFist;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RocketFistTest {
   @Test
   void fistReturns() {
      Vec3 target = new Vec3(30.0, 0.0, 0.0);
      Vec3 position = Vec3.ZERO;
      int steps = 0;
      while (!SignatureRules.reached(position, target, 1.0E-6) && steps < 100) {
         position = SignatureRules.homingStep(position, target, SignatureRules.FIST_SPEED);
         steps++;
      }

      assertTrue(SignatureRules.reached(position, target, 1.0E-6), "the glove reaches the target");
      Vec3 home = new Vec3(0.0, 0.0, 0.0);
      int back = 0;
      while (!SignatureRules.reached(position, home, SignatureRules.FIST_REATTACH_RANGE) && back < 100) {
         position = SignatureRules.homingStep(position, home, SignatureRules.FIST_RETURN_SPEED);
         back++;
      }

      assertTrue(SignatureRules.reached(position, home, SignatureRules.FIST_REATTACH_RANGE), "the glove comes back to the hand");
      assertTrue(back > 0);
   }

   @Test
   void homingStepDoesNotOvershoot() {
      Vec3 goal = new Vec3(1.0, 0.0, 0.0);
      assertEquals(goal, SignatureRules.homingStep(Vec3.ZERO, goal, 5.0));
   }

   @Test
   void handUnavailableWhileAway() {
      assertTrue(RocketFist.canLaunch(-1));
      assertFalse(RocketFist.canLaunch(42), "a launched glove blocks the hand until it returns");
   }

   @Test
   void rangeAndDamageMatchSpec() {
      assertEquals(32.0, SignatureRules.FIST_RANGE, 1.0E-9);
      assertEquals(8.0F, SignatureRules.FIST_DAMAGE, 1.0E-9F);
   }
}
