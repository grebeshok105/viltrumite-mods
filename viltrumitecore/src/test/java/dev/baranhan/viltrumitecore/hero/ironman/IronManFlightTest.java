package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumiteflight.util.FlightProfile;
import dev.baranhan.viltrumitecore.hero.HumanHero;
import dev.baranhan.viltrumitecore.hero.homelander.HomelanderHero;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusHero;
import org.junit.jupiter.api.Test;

class IronManFlightTest {

   @Test
   void ironManProfileValues() {
      FlightProfile p = IronManRules.profile(false);
      assertEquals(0.67F, p.speedMul(), 1.0E-6F);
      assertEquals(1.0F / 30.0F, p.throttleUpPerTick(), 1.0E-6F);
      assertEquals(1.0F / 20.0F, p.throttleDownPerTick(), 1.0E-6F);
      assertEquals(0.79F, p.lockCap(), 1.0E-6F);
      assertTrue(p.lockCap() < 0.8F, "the speed lock never holds sonic");
      assertEquals(0.85F, p.inertia(), 1.0E-6F);
      assertEquals(9.0F, p.turnRateSlowDeg(), 1.0E-6F);
      assertEquals(2.5F, p.turnRateFastDeg(), 1.0E-6F);
      assertEquals(0.8F, p.hoverDamping(), 1.0E-6F);
      assertFalse(p.glide());
      assertEquals(0.12F, p.glideSink(), 1.0E-6F);
   }

   @Test
   void profileNullForOtherHeroes() {
      assertNull(new HumanHero().flightProfile(null));
      assertNull(new HomelanderHero().flightProfile(null));
      assertNull(new RegulusHero().flightProfile(null));
   }

   @Test
   void profileNullWithoutSuit() {
      assertNull(new IronManHero().flightProfile(null));
      assertNull(IronManHero.profileFor(false, false));
      assertNull(IronManHero.profileFor(false, true));
   }

   @Test
   void glideProfileWhenEmpty() {
      assertFalse(IronManHero.profileFor(true, false).glide());
      assertTrue(IronManHero.profileFor(true, true).glide());
      assertEquals(IronManRules.profile(true), IronManHero.profileFor(true, true));
   }
}
