package dev.baranhan.viltrumitecore.hero.ironman.mark;

import java.util.List;

/**
 * Mark 42 passive (spec §13.3): it loses parts one by one as it breaks. Every
 * 1/14 of durability drops the next part of {@link #LOSS_ORDER}; each lost
 * part weakens its system. Pure: the present-parts mask comes from durability.
 */
public final class Mark42Parts {
   /** Part names in loss order (the last one never drops: at 0 the suit breaks). */
   public static final List<String> LOSS_ORDER = List.of(
      "left_shoulder", "right_shoulder", "left_boot", "right_boot", "left_upper_arm", "right_upper_arm", "abdomen",
      "left_thigh", "right_thigh", "left_gauntlet", "right_gauntlet", "helmet", "chest", "back"
   );

   private Mark42Parts() {
   }

   public static int lostCount(float durability, float max) {
      if (max <= 0.0F || durability >= max) {
         return 0;
      }

      int lost = (int)Math.floor((max - Math.max(0.0F, durability)) * LOSS_ORDER.size() / max);
      return Math.max(0, Math.min(LOSS_ORDER.size() - 1, lost));
   }

   /** Mask of lost parts (bits in the Mark 42 part set). */
   public static int lostMask(float durability, float max) {
      int mask = 0;
      int lost = lostCount(durability, max);
      for (int i = 0; i < lost; i++) {
         int index = SuitPart.indexOf(MarkId.MARK_42, LOSS_ORDER.get(i));
         if (index >= 0) {
            mask |= 1 << index;
         }
      }

      return mask;
   }

   public static boolean lost(int lostMask, String part) {
      int index = SuitPart.indexOf(MarkId.MARK_42, part);
      return index >= 0 && (lostMask & 1 << index) != 0;
   }

   /** Lost gauntlet → that hand's weapon ×0.5: the suit weapon factor drops by 0.25 per lost gauntlet. */
   public static float weaponFactor(int lostMask) {
      float factor = 1.0F;
      if (lost(lostMask, "left_gauntlet")) {
         factor -= 0.25F;
      }

      if (lost(lostMask, "right_gauntlet")) {
         factor -= 0.25F;
      }

      return factor;
   }

   /** Lost leg part → flight −10% each. */
   public static float flightFactor(int lostMask) {
      int legs = 0;
      for (String part : List.of("left_boot", "right_boot", "left_thigh", "right_thigh")) {
         if (lost(lostMask, part)) {
            legs++;
         }
      }

      return 1.0F - 0.1F * legs;
   }

   public static boolean helmetLost(int lostMask) {
      return lost(lostMask, "helmet");
   }

   public static boolean chestLost(int lostMask) {
      return lost(lostMask, "chest");
   }
}
