package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import javax.annotation.Nullable;

/**
 * Regulus ability ids, their input actions and the public cooldown slot index.
 * Index 5 is the Evangelium book cooldown (item use, not a panel slot).
 */
public final class RegulusAbilities {
   public static final String LIONS_HEART = "regulus:lions_heart";
   public static final String DEBRIS_KICK = "regulus:debris_kick";
   public static final String MANIA = "regulus:mania";
   public static final String GREEDS_EMBRACE = "regulus:greeds_embrace";
   public static final String COUNTER = "regulus:counter";
   public static final String EVANGELIUM = "regulus:evangelium";

   private static final String[] IDS = {LIONS_HEART, DEBRIS_KICK, MANIA, GREEDS_EMBRACE, COUNTER, EVANGELIUM};
   private static final HeroAction[] ACTIONS = {
      HeroAction.LIONS_HEART, HeroAction.DEBRIS_KICK, HeroAction.MANIA, HeroAction.GREEDS_EMBRACE, HeroAction.COUNTER, HeroAction.RITUAL
   };

   private RegulusAbilities() {
   }

   public static String[] slotIds() {
      return new String[]{LIONS_HEART, DEBRIS_KICK, MANIA, GREEDS_EMBRACE, COUNTER};
   }

   public static boolean isRegulusAbility(@Nullable String abilityId) {
      return abilityId != null && abilityId.startsWith("regulus:");
   }

   @Nullable
   public static HeroAction actionFor(@Nullable String abilityId) {
      for (int i = 0; i < IDS.length; i++) {
         if (IDS[i].equals(abilityId)) {
            return ACTIONS[i];
         }
      }

      return null;
   }

   @Nullable
   public static String abilityFor(HeroAction action) {
      for (int i = 0; i < ACTIONS.length; i++) {
         if (ACTIONS[i] == action) {
            return IDS[i];
         }
      }

      return null;
   }

   /** Public snapshot cooldown slot for an ability id, -1 when unknown. */
   public static int cooldownIndex(@Nullable String abilityId) {
      for (int i = 0; i < IDS.length; i++) {
         if (IDS[i].equals(abilityId)) {
            return i;
         }
      }

      return -1;
   }
}
