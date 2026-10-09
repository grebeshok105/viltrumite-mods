package dev.baranhan.viltrumitecore.hero;

import java.util.List;

/**
 * What a hero's damage code really does, for analysis (Iron Man scan, spec
 * §11.2). Descriptive only: it mirrors the real damage route
 * (HeroDamage, PlayerStatsMixin, damage layers) and never deals test hits.
 * Unknown → empty lists (the card then says "not detected").
 */
public record ScanInfo(List<ScanLine> protections, List<ScanLine> weakSpots, List<ScanLine> conditions) {
   public static final ScanInfo EMPTY = new ScanInfo(List.of(), List.of(), List.of());

   public ScanInfo {
      protections = protections == null ? List.of() : List.copyOf(protections);
      weakSpots = weakSpots == null ? List.of() : List.copyOf(weakSpots);
      conditions = conditions == null ? List.of() : List.copyOf(conditions);
   }

   public boolean isEmpty() {
      return this.protections.isEmpty() && this.weakSpots.isEmpty() && this.conditions.isEmpty();
   }
}
