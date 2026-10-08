package dev.baranhan.viltrumitecore.hero.homelander;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure focus target selection (spec §6.4): nearest 8 within 24, kept until 48 or death. */
public final class FocusTargets {
   /** A living entity around Homelander; excluded = self, own tamed, armor stand. */
   public record Candidate(int id, double dist, boolean excluded) {
   }

   private FocusTargets() {
   }

   /** Nearest {@code max} non-excluded candidates within the acquire radius. */
   public static List<Integer> select(List<Candidate> all, int max) {
      return all.stream()
         .filter(c -> !c.excluded() && c.dist() <= HomelanderRules.FOCUS_ACQUIRE_RADIUS)
         .sorted(Comparator.comparingDouble(Candidate::dist))
         .limit(max)
         .map(Candidate::id)
         .toList();
   }

   /** A current target stays while alive and within the drop radius. */
   public static boolean keep(double dist, boolean alive) {
      return alive && dist <= HomelanderRules.FOCUS_DROP_RADIUS;
   }

   /**
    * Next target list: current targets that still qualify (in order), then
    * the nearest new ones. Candidates are the living entities around; a
    * current target missing from them is gone or dead.
    */
   public static List<Integer> refresh(List<Integer> current, List<Candidate> all, int max) {
      Map<Integer, Candidate> byId = new HashMap<>();
      for (Candidate candidate : all) {
         byId.put(candidate.id(), candidate);
      }

      List<Integer> next = new ArrayList<>();
      for (int id : current) {
         Candidate candidate = byId.get(id);
         if (candidate != null && !candidate.excluded() && keep(candidate.dist(), true) && next.size() < max) {
            next.add(id);
         }
      }

      for (int id : select(all, max)) {
         if (next.size() >= max) {
            break;
         }

         if (!next.contains(id)) {
            next.add(id);
         }
      }

      return next;
   }
}
