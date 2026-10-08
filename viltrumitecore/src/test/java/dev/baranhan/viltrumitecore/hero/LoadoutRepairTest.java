package dev.baranhan.viltrumitecore.hero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.homelander.HomelanderAbilities;
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
}
