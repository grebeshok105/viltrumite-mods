package dev.baranhan.viltrumitecore.hero.ironman.scan;

import dev.baranhan.viltrumitecore.hero.ScanLine;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Weak spots from real vanilla damage rules only (spec §11.2): Smite and
 * instant health hurt the undead, Bane of Arthropods, water hurts
 * water-sensitive mobs. Players: only the hero's own scanInfo. Rules are a
 * data list keyed by vanilla properties, never hero ids. Nothing matched →
 * "not detected" (no invented weak spots).
 */
public final class WeakSpots {
   public static final String NONE = "scan.viltrumitecore.weak.none";

   private record Rule(Predicate<ScanTraits> applies, String key) {
   }

   private static final List<Rule> RULES = List.of(
      new Rule(ScanTraits::undead, "scan.viltrumitecore.weak.undead"),
      new Rule(ScanTraits::arthropod, "scan.viltrumitecore.weak.arthropod"),
      new Rule(ScanTraits::waterSensitive, "scan.viltrumitecore.weak.water")
   );

   private WeakSpots() {
   }

   public static List<ScanLine> of(ScanTraits traits) {
      List<ScanLine> lines = new ArrayList<>();
      for (Rule rule : RULES) {
         if (rule.applies().test(traits)) {
            lines.add(ScanLine.of(rule.key()));
         }
      }

      lines.addAll(traits.hero().weakSpots());
      if (lines.isEmpty()) {
         lines.add(ScanLine.of(NONE));
      }

      return lines;
   }
}
