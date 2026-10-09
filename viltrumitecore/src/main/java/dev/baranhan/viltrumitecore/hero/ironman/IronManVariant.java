package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import javax.annotation.Nullable;

/**
 * Iron Man layout of the generic {@code HeroPublicSnapshot.variant}: which
 * mark (bits 0-3, 0 = nano / none) and which of its parts are on the body
 * (bits 8-23, SuitPart indices). Bits 4-7 and 24-31 are reserved (Stage 5).
 * Every client renders other players from this value.
 */
public final class IronManVariant {
   private static final int MARK_BITS = 0xF;
   private static final int PARTS_SHIFT = 8;
   private static final int PARTS_BITS = 0xFFFF;

   private IronManVariant() {
   }

   public static int pack(@Nullable MarkId mark, int parts) {
      int id = mark == null ? 0 : mark.ordinal() + 1;
      return id & MARK_BITS | (parts & PARTS_BITS) << PARTS_SHIFT;
   }

   @Nullable
   public static MarkId mark(int variant) {
      return MarkId.byId((variant & MARK_BITS) - 1);
   }

   public static int parts(int variant) {
      return variant >>> PARTS_SHIFT & PARTS_BITS;
   }
}
