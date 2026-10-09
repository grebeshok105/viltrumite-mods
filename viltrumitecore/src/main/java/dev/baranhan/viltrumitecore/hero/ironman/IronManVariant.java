package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import javax.annotation.Nullable;

/**
 * Iron Man layout of the generic {@code HeroPublicSnapshot.variant}: which
 * mark (bits 0-3, 0 = nano / none), Hulkbuster parts on the body (bits 4-7:
 * legs, torso, arms, helmet), mark parts on the body (bits 8-23, SuitPart
 * indices) and the Hulkbuster durability as 0..255 of its maximum (bits 24-31).
 * Every client renders other players from this value.
 */
public final class IronManVariant {
   private static final int MARK_BITS = 0xF;
   private static final int PARTS_SHIFT = 8;
   private static final int PARTS_BITS = 0xFFFF;

   private IronManVariant() {
   }

   public static int pack(@Nullable MarkId mark, int parts) {
      return pack(mark, parts, 0, 0.0F);
   }

   /** {@code hulkDurability} 0..1 of the Hulkbuster maximum. */
   public static int pack(@Nullable MarkId mark, int parts, int hulkParts, float hulkDurability) {
      int id = mark == null ? 0 : mark.ordinal() + 1;
      int hulk = Math.round(Math.max(0.0F, Math.min(1.0F, hulkDurability)) * 255.0F);
      return id & MARK_BITS | (hulkParts & 0xF) << 4 | (parts & PARTS_BITS) << PARTS_SHIFT | hulk << 24;
   }

   public static int hulkParts(int variant) {
      return variant >>> 4 & 0xF;
   }

   /** Hulkbuster durability 0..1 of its maximum. */
   public static float hulkDurability(int variant) {
      return (variant >>> 24 & 0xFF) / 255.0F;
   }

   @Nullable
   public static MarkId mark(int variant) {
      return MarkId.byId((variant & MARK_BITS) - 1);
   }

   public static int parts(int variant) {
      return variant >>> PARTS_SHIFT & PARTS_BITS;
   }
}
