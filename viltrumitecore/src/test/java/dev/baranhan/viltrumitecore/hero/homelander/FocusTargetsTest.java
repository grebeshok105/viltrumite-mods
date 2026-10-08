package dev.baranhan.viltrumitecore.hero.homelander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.homelander.FocusTargets.Candidate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FocusTargetsTest {

   @Test
   void picksNearest8() {
      List<Candidate> all = new ArrayList<>();
      for (int i = 10; i >= 1; i--) {
         all.add(new Candidate(i, i * 2.0, false));
      }
      assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), FocusTargets.select(all, 8));
   }

   @Test
   void skipsExcluded() {
      List<Candidate> all = List.of(new Candidate(1, 1.0, true), new Candidate(2, 2.0, false), new Candidate(3, 30.0, false));
      assertEquals(List.of(2), FocusTargets.select(all, 8));
   }

   @Test
   void dropsDeadAndFarTargets() {
      assertFalse(FocusTargets.keep(49.0, true));
      assertFalse(FocusTargets.keep(5.0, false));
      // A current target that is no longer among the candidates (dead, gone) is dropped.
      List<Candidate> all = List.of(new Candidate(2, 3.0, false), new Candidate(5, 60.0, false));
      assertEquals(List.of(2), FocusTargets.refresh(List.of(1, 5), all, 8));
   }

   @Test
   void keepsTargetBetween24And48() {
      assertTrue(FocusTargets.keep(40.0, true));
      List<Candidate> all = new ArrayList<>();
      all.add(new Candidate(99, 40.0, false));
      for (int i = 1; i <= 10; i++) {
         all.add(new Candidate(i, i, false));
      }
      List<Integer> next = FocusTargets.refresh(List.of(99), all, 8);
      assertEquals(8, next.size());
      assertEquals(99, next.get(0));
      assertEquals(List.of(99, 1, 2, 3, 4, 5, 6, 7), next);
   }
}
