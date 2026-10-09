package dev.baranhan.viltrumitecore.hero.ironman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumiteflight.util.FlightMotion;
import dev.baranhan.viltrumiteflight.util.FlightProfile;
import dev.baranhan.viltrumiteflight.util.FlightState;
import org.junit.jupiter.api.Test;

class FlightEnergyTest {
   private static IronManState worn() {
      IronManState state = new IronManState();
      state.suit.toggle();
      for (int i = 0; i < IronManRules.SUIT_DEPLOY_TICKS; i++) {
         state.suit.tick();
      }
      assertTrue(state.suit.worn());
      return state;
   }

   @Test
   void hoverDrainsOnePerSecond() {
      IronManState state = worn();
      for (int i = 0; i < 20; i++) {
         IronManHero.flightEnergyTick(state, FlightState.HOVER);
      }
      assertEquals(IronManRules.ENERGY_MAX - 1.0F, state.energy.value(), 1.0E-3);
   }

   @Test
   void sonicDrainsSixPerSecond() {
      IronManState state = worn();
      for (int i = 0; i < 20; i++) {
         IronManHero.flightEnergyTick(state, FlightState.SONIC);
      }
      assertEquals(IronManRules.ENERGY_MAX - 6.0F, state.energy.value(), 1.0E-3);
   }

   @Test
   void emptyEnergyForcesGlide() {
      IronManState state = worn();
      state.energy.drain(IronManRules.ENERGY_MAX);
      IronManHero.flightEnergyTick(state, FlightState.CRUISE);
      assertTrue(state.glide);
      assertEquals(IronManRules.profile(true), IronManHero.profileFor(true, state.glide));
      // landing ends the glide
      IronManHero.flightEnergyTick(state, FlightState.NONE);
      assertFalse(state.glide);
   }

   @Test
   void glideBlocksSonic() {
      FlightProfile glide = IronManRules.profile(true);
      float throttle = 0.9F;
      for (int i = 0; i < 40; i++) {
         throttle = FlightMotion.throttle(throttle, true, true, glide);
      }
      assertTrue(throttle < 0.8F, "no sonic while gliding, even with Ctrl and lock");
   }
}
