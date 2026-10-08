package dev.baranhan.viltrumitecore.hero.homelander;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumitecore.hero.HeroId;
import org.junit.jupiter.api.Test;

class HomelanderHeroTest {
   private final HomelanderHero hero = new HomelanderHero();

   @Test
   void ownsKitSubset() {
      assertEquals(HeroId.HOMELANDER, this.hero.id());
      assertTrue(this.hero.ownsAbility("viltrumite:punch"));
      assertTrue(this.hero.ownsAbility("viltrumite:dash"));
      assertTrue(this.hero.ownsAbility("viltrumite:thunderclap"));
      assertTrue(this.hero.ownsAbility(HomelanderAbilities.LASERS));
      assertTrue(this.hero.ownsAbility(HomelanderAbilities.FOCUS));
      assertTrue(this.hero.ownsAbility(HomelanderAbilities.ROAR));
   }

   @Test
   void homelanderRefusesExcludedKit() {
      for (String id : new String[]{"viltrumite:grab", "viltrumite:chop", "viltrumite:barrage", "viltrumite:speed",
         "viltrumite:block", "viltrumite:lock", "regulus:mania", null}) {
         assertFalse(this.hero.ownsAbility(id), String.valueOf(id));
      }
   }

   @Test
   void dashOnlyInFlight() {
      assertFalse(HomelanderAbilities.dashAllowed(FlightState.NONE));
      for (FlightState state : FlightState.values()) {
         if (state != FlightState.NONE) {
            assertTrue(HomelanderAbilities.dashAllowed(state), state.name());
         }
      }
   }

   @Test
   void loadoutOrder() {
      String[] loadout = this.hero.defaultLoadout();
      assertEquals(18, loadout.length);
      assertArrayEquals(
         new String[]{"viltrumite:punch", "viltrumite:dash", HomelanderAbilities.LASERS, HomelanderAbilities.FOCUS,
            HomelanderAbilities.ROAR, "viltrumite:thunderclap"},
         java.util.Arrays.copyOf(loadout, 6)
      );
      for (int i = 6; i < 18; i++) {
         assertEquals("", loadout[i]);
      }
   }
}
