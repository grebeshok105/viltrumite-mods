package dev.baranhan.viltrumitecore.hero;

/**
 * Mouse buttons a hero may claim. The wire packet carries the ordinal: append
 * only. MIDDLE is driven by the heart key mapping (default MMB).
 */
public enum MouseButton {
   PRIMARY,
   SECONDARY,
   MIDDLE;

   public static MouseButton byId(int ordinal) {
      MouseButton[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
   }
}
