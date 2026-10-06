package dev.baranhan.viltrumitecore.hero;

/**
 * Bounded hero input/display actions. The wire packet carries the ordinal, so
 * a forged packet can only ever name a value in this list.
 */
public enum HeroAction {
   LIONS_HEART,
   DEBRIS_KICK,
   MANIA,
   GREEDS_EMBRACE,
   COUNTER,
   JUMP,
   RITUAL;

   public static HeroAction byId(int ordinal) {
      HeroAction[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
   }
}
