package dev.baranhan.viltrumitecore.hero.ironman;

/**
 * The single bit layout of {@code HeroPublicSnapshot.heroFlags} for Iron Man,
 * for all stages. Append new fields into the reserved bits 30-31 (27-29 are Stage 2 pose bits); never move
 * an existing field (clients of the same version decode the same layout).
 */
public final class IronManFlags {
   public enum Field {
      // Stage 1
      SUIT_WORN(0, 1),
      DEPLOYING(1, 1),
      RETRACTING(2, 1),
      GLIDE(3, 1),
      HEAVY_LANDING(4, 1),
      // Stage 2
      OVERHEAT_COUNT(5, 2),
      OVERHEAT_LOCK(7, 1),
      RMB_TOOL(8, 3),
      SHIELD_UP(11, 1),
      DAMAGED_ZONES(12, 3),
      UNIBEAM_PHASE(15, 2),
      MISSILE_FLAPS(17, 1),
      OVERDRAFT_SPUTTER(18, 1),
      // Stage 3
      HELMET_CLOSED(19, 1),
      SCAN_ACTIVE(20, 1),
      // Stage 4
      MARK_CAMO(21, 1),
      EQUIP_PHASE(22, 2),
      // Stage 5
      HULKBUSTER_PHASE(24, 3),
      // Stage 2 (pose sync): repulsor recoil, which palm fired, overdraft charge running.
      RECOIL(27, 1),
      SHOT_HAND(28, 1),
      OVERDRAFT(29, 1);
      // 30-31 reserved.

      private final int shift;
      private final int width;

      Field(int shift, int width) {
         this.shift = shift;
         this.width = width;
      }

      public int shift() {
         return this.shift;
      }

      public int width() {
         return this.width;
      }

      int mask() {
         return ((1 << this.width) - 1) << this.shift;
      }
   }

   private IronManFlags() {
   }

   public static int get(int flags, Field field) {
      return (flags & field.mask()) >>> field.shift;
   }

   public static boolean is(int flags, Field field) {
      return get(flags, field) != 0;
   }

   /** Writes the value clamped to the field width; other fields are untouched. */
   public static int set(int flags, Field field, int value) {
      int max = (1 << field.width) - 1;
      int clamped = Math.max(0, Math.min(max, value));
      return (flags & ~field.mask()) | (clamped << field.shift);
   }

   public static int set(int flags, Field field, boolean value) {
      return set(flags, field, value ? 1 : 0);
   }
}
