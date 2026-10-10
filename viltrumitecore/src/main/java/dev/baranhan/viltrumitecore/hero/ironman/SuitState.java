package dev.baranhan.viltrumitecore.hero.ironman;

/** What Tony wears. Saved by name; later stages append, never reorder. */
public enum SuitState {
   NONE,
   DEPLOYING,
   NANO,
   RETRACTING,
   /** Stage 4: a Veronica mark is fully on. */
   MARK,
   /** Mark parts are flying in / wrapping the body (vulnerable window, spec §12.4). */
   EQUIPPING,
   /** Plates open, Tony steps out (spec §12.5). */
   EXITING,
   /** Control interrupted the equip: the locked parts stay, no flight or weapons (plan stage 4 Task 7). */
   MARK_PARTIAL
}
