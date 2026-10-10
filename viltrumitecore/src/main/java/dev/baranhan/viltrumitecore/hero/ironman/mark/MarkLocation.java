package dev.baranhan.viltrumitecore.hero.ironman.mark;

/**
 * Where the single instance of a mark is (plan stage 4, Review Focus 0).
 * Append only (Stage 6 appends LEGION).
 */
public enum MarkLocation {
   /** In Veronica's storage (default, also on cooldown). */
   STORED,
   /** On Tony (worn or partial). */
   WORN,
   /** Standing in the world as the empty suit. */
   EMPTY,
   /** Parts flying to Tony. */
   IN_DELIVERY
}
