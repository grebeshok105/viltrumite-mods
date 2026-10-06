package dev.baranhan.viltrumitecore.hero.regulus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RegulusHeartsTest {

   @Test
   void carrierEligibilityMatchesSpec() {
      // Spec 5.1: vanilla namespace, real creature, non-player, non-Enemy, no heartless tag.
      assertTrue(RegulusRules.carrierEligible(true, false, false, false, true));
      // Modded-namespace entities are not carriers.
      assertFalse(RegulusRules.carrierEligible(false, false, false, false, true));
      // Players are never carriers.
      assertFalse(RegulusRules.carrierEligible(true, true, false, false, true));
      // Hostile entities (Enemy marker) are never carriers.
      assertFalse(RegulusRules.carrierEligible(true, false, true, false, true));
      // The heartless tag opts an entity out.
      assertFalse(RegulusRules.carrierEligible(true, false, false, true, true));
      // Every disqualifier alone is enough.
      assertFalse(RegulusRules.carrierEligible(false, true, true, true, false));
   }

   @Test
   void decorationEntitiesAreNotCarriers() {
      // Armor stands satisfy the literal spec bullets but are not "живое
      // существо": they would grant free permanent hearts. The creature flag
      // (Mob at the call site) excludes them.
      assertFalse(RegulusRules.carrierEligible(true, false, false, false, false));
   }

   @Test
   void carrierStatusSplitsDeathFromGone() {
      assertEquals(RegulusHearts.CarrierStatus.GONE, RegulusHearts.carrierStatus(false, false));
      assertEquals(RegulusHearts.CarrierStatus.BOUND, RegulusHearts.carrierStatus(true, true));
      assertEquals(RegulusHearts.CarrierStatus.DEAD, RegulusHearts.carrierStatus(true, false));
   }

   @Test
   void carrierLevelBindingSurvivesRescanAndClearsOnDrop() {
      RegulusState state = new RegulusState();
      UUID carrier = UUID.randomUUID();
      ResourceLocation overworld = new ResourceLocation("minecraft", "overworld");
      ResourceLocation nether = new ResourceLocation("minecraft", "nether");

      // The dimension at bind time wins: a re-scan elsewhere never rebinds the
      // heart to the owner's new dimension.
      RegulusHearts.bindCarrierLevels(state, List.of(carrier), overworld);
      RegulusHearts.bindCarrierLevels(state, List.of(carrier), nether);
      assertEquals(overworld, state.carrierLevels.get(carrier));

      // Losing the heart clears its level binding too.
      state.carriers.add(carrier);
      RegulusHearts.dropCarrier(state, carrier);
      assertFalse(state.carrierLevels.containsKey(carrier));
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

      java.util.List<UUID> added = RegulusHearts.addCarriers(carriers, candidates, RegulusRules.MAX_HEARTS);
      assertEquals(RegulusRules.MAX_HEARTS, added.size());
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

      java.util.List<UUID> added = RegulusHearts.addCarriers(carriers, List.of(), RegulusRules.MAX_HEARTS);
      assertTrue(added.isEmpty());
      assertTrue(carriers.contains(bound));
   }

   @Test
   void carrierLevelsBindOnlyActuallyBoundCandidates() {
      // Candidates rejected by the cap must not leave stale dimension bindings
      // behind (a3 audit finding).
      RegulusState state = new RegulusState();
      List<UUID> candidates = new ArrayList<>();
      for (int i = 0; i < RegulusRules.MAX_HEARTS + 3; i++) {
         candidates.add(UUID.randomUUID());
      }

      java.util.List<UUID> added = RegulusHearts.addCarriers(state.carriers, candidates, RegulusRules.MAX_HEARTS);
      RegulusHearts.bindCarrierLevels(state, added, new ResourceLocation("minecraft", "overworld"));

      assertEquals(RegulusRules.MAX_HEARTS, state.carrierLevels.size());
      assertTrue(state.carrierLevels.containsKey(candidates.get(0)));
      assertFalse(
         state.carrierLevels.containsKey(candidates.get(RegulusRules.MAX_HEARTS)),
         "an over-cap candidate must not record a dimension binding"
      );
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
