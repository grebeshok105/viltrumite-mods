package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegulusHeartsTest {

   @Test
   void carrierEligibilityMatchesSpec() {
      // Spec 5.1: vanilla namespace, non-player, non-Enemy, no heartless tag.
      assertTrue(RegulusRules.carrierEligible(true, false, false, false));
      // Modded-namespace entities are not carriers.
      assertFalse(RegulusRules.carrierEligible(false, false, false, false));
      // Players are never carriers.
      assertFalse(RegulusRules.carrierEligible(true, true, false, false));
      // Hostile entities (Enemy marker) are never carriers.
      assertFalse(RegulusRules.carrierEligible(true, false, true, false));
      // The heartless tag opts an entity out.
      assertFalse(RegulusRules.carrierEligible(true, false, false, true));
      // Every disqualifier alone is enough.
      assertFalse(RegulusRules.carrierEligible(false, true, true, true));
   }

   @Test
   void addCarriersCapsAtTwelveAndDeduplicates() {
      Set<UUID> carriers = new LinkedHashSet<>();
      List<UUID> candidates = new ArrayList<>();
      for (int i = 0; i < RegulusRules.MAX_HEARTS + 4; i++) {
         candidates.add(UUID.randomUUID());
      }
      // A duplicate candidate still counts as one heart.
      candidates.add(candidates.get(0));

      int added = RegulusHearts.addCarriers(carriers, candidates, RegulusRules.MAX_HEARTS);
      assertEquals(RegulusRules.MAX_HEARTS, added);
      assertEquals(RegulusRules.MAX_HEARTS, carriers.size());
      assertFalse(carriers.add(candidates.get(3)), "a carrier holds at most one heart per owner");
   }

   @Test
   void scanKeepsCarriersThatLeftTheRadius() {
      // Hearts persist once bound: a rescan only adds; absence from the
      // candidate list never removes an existing carrier.
      Set<UUID> carriers = new LinkedHashSet<>();
      UUID bound = UUID.randomUUID();
      RegulusHearts.addCarriers(carriers, List.of(bound), RegulusRules.MAX_HEARTS);

      int added = RegulusHearts.addCarriers(carriers, List.of(), RegulusRules.MAX_HEARTS);
      assertEquals(0, added);
      assertTrue(carriers.contains(bound));
   }

   @Test
   void dropCarrierIsIdempotent() {
      RegulusState state = new RegulusState();
      UUID carrier = UUID.randomUUID();
      state.carriers.add(carrier);

      assertTrue(RegulusHearts.dropCarrier(state, carrier));
      // A second drop (unload event after death event, rescan echo) is a no-op
      // so backlash can never fire twice for the same loss.
      assertFalse(RegulusHearts.dropCarrier(state, carrier));
      assertTrue(state.carriers.isEmpty());
   }

   @Test
   void cooldownSamplesHeartsWhenItStarts() {
      RegulusState state = new RegulusState();
      for (int i = 0; i < RegulusRules.MAX_HEARTS; i++) {
         state.carriers.add(UUID.randomUUID());
      }

      state.startCooldown(RegulusAbilities.LIONS_HEART, 1000);
      int stored = state.cooldownOf(RegulusAbilities.LIONS_HEART);
      assertEquals(RegulusRules.cooldown(1000, RegulusRules.MAX_HEARTS), stored); // 640

      // Losing hearts afterwards does not retro-change a running cooldown.
      state.carriers.clear();
      assertEquals(stored, state.cooldownOf(RegulusAbilities.LIONS_HEART));
   }
}
