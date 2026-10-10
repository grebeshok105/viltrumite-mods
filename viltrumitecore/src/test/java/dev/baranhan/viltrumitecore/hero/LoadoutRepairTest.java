package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.homelander.HomelanderAbilities;
import dev.baranhan.viltrumitecore.hero.ironman.IronManAbilities;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LoadoutRepairTest {
   @Test
   void migratedViltrumiteSlotsResetForHomelander() {
      String[] slots = {LegacyKit.PUNCH, LegacyKit.GRAB, LegacyKit.BLOCK, "", ""};
      assertTrue(HeroRegistry.needsLoadoutReset(slots, HomelanderAbilities::owns));
   }

   @Test
   void ownedOrEmptySlotsStay() {
      String[] slots = {LegacyKit.PUNCH, "", HomelanderAbilities.LASERS, null};
      assertFalse(HeroRegistry.needsLoadoutReset(slots, HomelanderAbilities::owns));
   }

   @Test
   void oldIronManSaveGetsTheLaterAbilities() {
      // A stage 1 save: only the suit key in slot 10.
      String[] slots = new String[18];
      Arrays.fill(slots, "");
      slots[IronManAbilities.SUIT_SLOT] = IronManAbilities.SUIT;
      assertFalse(HeroRegistry.needsLoadoutReset(slots, IronManAbilities::owns));
      // Old save: no offered set yet, the slots count as given.
      assertArrayEquals(IronManAbilities.defaultLoadout(),
         HeroRegistry.fillMissingDefaults(slots, IronManAbilities.defaultLoadout(), HeroRegistry.defaultIds(slots)));
   }

   @Test
   void removedAbilityStaysRemoved() {
      String[] defaults = IronManAbilities.defaultLoadout();
      String[] slots = defaults.clone();
      slots[IronManAbilities.MISSILES_SLOT] = "";
      assertArrayEquals(slots, HeroRegistry.fillMissingDefaults(slots, defaults, HeroRegistry.defaultIds(defaults)));
   }

   @Test
   void fillingKeepsThePlayersSlots() {
      String[] defaults = {"a", "b", "c", ""};
      String[] slots = {"b", "", "x", null};
      java.util.Set<String> none = java.util.Set.of();
      assertArrayEquals(new String[]{"b", "a", "x", "c"}, HeroRegistry.fillMissingDefaults(slots, defaults, none));
      assertArrayEquals(new String[]{"a", "b"}, HeroRegistry.fillMissingDefaults(new String[]{"a", "b"}, new String[]{"a", "b", "c"}, none));
   }
}
