package dev.baranhan.viltrumitecore.hero.ironman.combat;

/**
 * The tool on the right mouse button (spec §6.1). Synced as an ordinal in
 * {@code IronManFlags.Field.RMB_TOOL} (3 bits): append only.
 * Stage 4 uses SIGNATURE (mark signature), Stage 5 the last two.
 */
public enum RightTool {
   REPULSOR,
   NANO_BLADE,
   NANO_HAMMER,
   SIGNATURE,
   JACKHAMMER,
   HULK_REPULSOR;

   public static RightTool byId(int ordinal) {
      RightTool[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : REPULSOR;
   }

   public boolean nanoWeapon() {
      return this == NANO_BLADE || this == NANO_HAMMER;
   }
}
