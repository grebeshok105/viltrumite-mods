package dev.baranhan.viltrumitecore.client.ironman.hulkbuster;

import dev.baranhan.viltrumitecore.hero.ironman.hulkbuster.HulkbusterLayer;

/**
 * Docking groups of the Hulkbuster parts (pure). Bit i of {@code IronManVariant.hulkParts}
 * is group i: legs, torso, arms, helmet. Each locks at {@code HulkbusterLayer.PART_LOCK[i]}
 * assembly ticks, after the drop. The flight runs from the drop start to that lock.
 */
public final class HulkbusterAssembly {
   public static final int LEGS = 0;
   public static final int TORSO = 1;
   public static final int ARMS = 2;
   public static final int HELMET = 3;
   public static final int GROUPS = HulkbusterLayer.PARTS;
   /** Bones of each group in parts.geo.json (the armor names of the vanilla limbs). */
   public static final String[][] BONES = {
      {"armorRightLeg", "armorLeftLeg"},
      {"armorBody"},
      {"armorRightArm", "armorLeftArm"},
      {"armorHead"}
   };
   /** Group centre above the feet in armor pixels: the wrap pivot and the flight anchor. */
   private static final float[] CENTRE_PX = {6.5F, 18.5F, 18.0F, 29.0F};
   /** Ticks of the open-and-close wrap after a lock. */
   public static final int WRAP_TICKS = 8;

   private HulkbusterAssembly() {
   }

   public static boolean locked(int mask, int group) {
      return group >= 0 && group < GROUPS && (mask >> group & 1) != 0;
   }

   /** Group centre above the feet in blocks (x and z are zero for every group). */
   public static double centreAboveFeet(int group) {
      return CENTRE_PX[group] / 16.0;
   }

   /** Ticks since the drop began: the drop phase, then the assembly. */
   public static double dropTicks(int phase, double elapsedTicks) {
      if (phase == HulkbusterLayer.Phase.ASSEMBLING.ordinal()) {
         return HulkbusterLayer.DROP_TICKS + elapsedTicks;
      }

      return elapsedTicks;
   }

   /** Flight progress 0..1 of a group from the drop start to its lock tick. */
   public static float flightProgress(int group, double dropTicks) {
      double lock = HulkbusterLayer.DROP_TICKS + HulkbusterLayer.PART_LOCK[group];
      return clamp((float)(dropTicks / lock));
   }

   /**
    * Wrap after the lock: 0 closed, rising to open, back to 0 after {@link #WRAP_TICKS}.
    * Outside the assembly every locked group is closed (1 here means no scale change).
    */
   public static float wrapProgress(int group, int phase, double assembleTicks) {
      if (phase != HulkbusterLayer.Phase.ASSEMBLING.ordinal()) {
         return 1.0F;
      }

      return clamp((float)((assembleTicks - HulkbusterLayer.PART_LOCK[group]) / WRAP_TICKS));
   }

   private static float clamp(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }
}
