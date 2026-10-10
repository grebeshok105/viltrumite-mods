package dev.baranhan.viltrumitecore.hero;

/**
 * Typed sections of the owner-private hero snapshot. The wire packet carries
 * the ordinal: append only. CARRIERS = the original id list (Regulus heart
 * carriers, Homelander focus targets).
 */
public enum OwnerSection {
   CARRIERS,
   THREATS,
   MARKS,
   SCAN;

   public static OwnerSection byId(int ordinal) {
      OwnerSection[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
   }
}
