package dev.baranhan.viltrumitecore.client.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumiteflight.util.FlightState;
import org.junit.jupiter.api.Test;

class IronManVisualsTest {
   private static final int WORN = IronManFlags.set(0, IronManFlags.Field.SUIT_WORN, true);
   private static final int DEPLOYING = IronManFlags.set(0, IronManFlags.Field.DEPLOYING, true);
   private static final int RETRACTING = IronManFlags.set(WORN, IronManFlags.Field.RETRACTING, true);

   @Test
   void revealFollowsTheWave() {
      int suit = HeroAction.SUIT.ordinal();
      assertEquals(0.0F, IronManView.reveal(0, -1, 0, 0, 0.0F));
      assertEquals(1.0F, IronManView.reveal(WORN, -1, 0, 0, 0.0F));
      assertEquals(0.5F, IronManView.reveal(DEPLOYING, suit, 10, 20, 0.0F), 1.0E-6F);
      assertEquals(0.25F, IronManView.reveal(RETRACTING, suit, 15, 20, 0.0F), 1.0E-6F);
      // Relog mid-wave: the synced timeline alone gives the state.
      assertEquals(1.0F, IronManView.reveal(DEPLOYING, suit, 25, 20, 0.0F));
   }

   @Test
   void noFlamesWhileGlidingOrWithoutSuit() {
      assertEquals(ThrusterFlames.Mode.NONE, ThrusterFlames.mode(true, true, FlightState.CRUISE));
      assertEquals(ThrusterFlames.Mode.NONE, ThrusterFlames.mode(false, false, FlightState.HOVER));
      assertEquals(ThrusterFlames.Mode.NONE, ThrusterFlames.mode(true, false, FlightState.NONE));
      assertFalse(ThrusterFlames.lengths(ThrusterFlames.Mode.NONE, 1.0F).any());
   }

   @Test
   void flameLengthGrowsWithThrottle() {
      assertEquals(ThrusterFlames.Mode.HOVER, ThrusterFlames.mode(true, false, FlightState.HOVER));
      ThrusterFlames.Lengths slow = ThrusterFlames.lengths(ThrusterFlames.Mode.CRUISE, 0.1F);
      ThrusterFlames.Lengths fast = ThrusterFlames.lengths(ThrusterFlames.Mode.CRUISE, 0.9F);
      ThrusterFlames.Lengths sonic = ThrusterFlames.lengths(ThrusterFlames.Mode.SONIC, 1.0F);
      assertTrue(fast.feet() > slow.feet());
      assertTrue(sonic.feet() >= fast.feet());
      assertTrue(ThrusterFlames.lengths(ThrusterFlames.Mode.HOVER, 0.0F).stabilizers() > 0.0F);
      assertEquals(0.0F, fast.stabilizers());
   }

   @Test
   void reactorGlowsOnTonyAndFlashesAtTheWaveEdge() {
      assertTrue(ReactorGlowLayer.intensity(false, false, false, 0.0F, 0.0F, 0.0F) > 0.0F);
      assertEquals(0.0F, ReactorGlowLayer.intensity(true, false, false, 0.0F, 0.0F, 0.0F));
      assertTrue(ReactorGlowLayer.intensity(false, true, false, 0.0F, 20.0F, 0.0F) > 1.0F);
      assertEquals(0.0F, ReactorGlowLayer.intensity(false, true, false, 10.0F, 20.0F, 0.0F));
      assertTrue(ReactorGlowLayer.intensity(true, false, true, 19.0F, 20.0F, 0.0F) > 1.0F);
   }

   @Test
   void energyBarColours() {
      assertEquals(0xFF50D8FF, IronManHud.energyColor(80.0F, true));
      assertEquals(0xFFFFB020, IronManHud.energyColor(20.0F, true));
      assertTrue(IronManHud.energyColor(0.0F, true) != IronManHud.energyColor(0.0F, false));
   }
}
