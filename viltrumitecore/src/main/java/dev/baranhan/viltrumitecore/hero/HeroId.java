package dev.baranhan.viltrumitecore.hero;

import javax.annotation.Nullable;

public enum HeroId {
   HUMAN("human"),
   VILTRUMITE("viltrumite"),
   REGULUS("regulus");

   private final String key;

   HeroId(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }

   @Nullable
   public static HeroId fromKey(@Nullable String key) {
      if (key == null) {
         return null;
      }

      for (HeroId id : values()) {
         if (id.key.equals(key)) {
            return id;
         }
      }

      return null;
   }

   public static HeroId fromLegacyBoolean(boolean isViltrumite) {
      return isViltrumite ? VILTRUMITE : HUMAN;
   }

   /**
    * Legacy ViltrumiteCorePlayer.setViltrumite semantics: the boolean only ever
    * toggles HUMAN<->VILTRUMITE and must never convert or clear a different hero.
    */
   public static HeroId legacySet(HeroId current, boolean isViltrumite) {
      if (current == HUMAN || current == VILTRUMITE) {
         return fromLegacyBoolean(isViltrumite);
      }

      return current;
   }
}
