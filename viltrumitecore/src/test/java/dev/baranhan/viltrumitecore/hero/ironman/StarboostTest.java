package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.*;

import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.SignatureRules;
import dev.baranhan.viltrumitecore.hero.ironman.mark.sig.Starboost;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class StarboostTest {
   @Test
   void boostSetsFullThrottle() {
      assertEquals(1.0F, Starboost.throttleAfterBoost(), 1.0E-9F);
   }

   @Test
   void verticalTakeoffFromGround() {
      Vec3 up = Starboost.boostVelocity(new Vec3(0.0, 1.0, 0.0), 1.5F);
      assertEquals(1.5, up.y, 1.0E-9);
      assertEquals(0.0, up.x, 1.0E-9);
      assertEquals(0.0, up.z, 1.0E-9);
   }

   @Test
   void cost15() {
      assertEquals(15.0F, SignatureRules.BOOST_COST, 1.0E-9F);
      assertEquals(100, SignatureRules.BOOST_COOLDOWN);
   }
}
