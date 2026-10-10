package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumiteflight.util.FlightState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class FlyByTest {
   @Test
   void noMouseClaimWithoutSuit() {
      for (FlightState state : FlightState.values()) {
         assertFalse(IronManHero.claimsPrimary(false, state));
      }
   }

   @Test
   void lmbClaimedOnlyInFlight() {
      assertFalse(IronManHero.claimsPrimary(true, FlightState.NONE));
      assertFalse(IronManHero.claimsPrimary(true, null));
      assertTrue(IronManHero.claimsPrimary(true, FlightState.HOVER));
      assertTrue(IronManHero.claimsPrimary(true, FlightState.CRUISE));
      assertTrue(IronManHero.claimsPrimary(true, FlightState.SONIC));
   }

   @Test
   void flyByDamageGrowsWithSpeed() {
      float slow = FlyBy.compute(new Vec3(0.2, 0, 0)).damage();
      float fast = FlyBy.compute(new Vec3(3.0, 0, 0)).damage();
      assertTrue(slow >= IronManRules.FLYBY_BASE);
      assertTrue(fast > slow);
      assertEquals(IronManRules.FLYBY_MAX, FlyBy.compute(new Vec3(50, 0, 0)).damage(), 1.0E-4);
   }

   @Test
   void flyByKeepsVelocity() {
      FlyBy.Result result = FlyBy.compute(new Vec3(2.0, -0.5, 1.0));
      assertEquals(Vec3.ZERO, result.selfImpulse());
      Vec3 dir = result.knockback().normalize();
      assertTrue(dir.x > 0 && dir.z > 0, "knockback follows the flight direction");
   }

   @Test
   void ramHitsEachTargetOncePer10Ticks() {
      Map<UUID, Long> last = new HashMap<>();
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      assertTrue(SonicRam.due(last, a, 100));
      assertTrue(SonicRam.due(last, b, 101));
      for (long t = 101; t < 100 + IronManRules.RAM_REHIT_TICKS; t++) {
         assertFalse(SonicRam.due(last, a, t));
      }
      assertTrue(SonicRam.due(last, a, 100 + IronManRules.RAM_REHIT_TICKS));
      assertFalse(SonicRam.due(last, b, 105));
   }
}
