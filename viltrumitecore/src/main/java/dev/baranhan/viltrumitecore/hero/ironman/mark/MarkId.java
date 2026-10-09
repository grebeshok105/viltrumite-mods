package dev.baranhan.viltrumitecore.hero.ironman.mark;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * The seven Veronica marks (spec §13.1). Append only: the ordinal goes into
 * NBT-free wire data (snapshot variant, menu packets) and the roster keys use
 * {@link #key()}.
 */
public enum MarkId {
   MARK_7,
   MARK_42,
   MARK_15,
   MARK_39,
   MARK_17,
   WAR_MACHINE_MK2,
   IRON_HEART_MK3;

   private static final MarkId[] VALUES = values();

   /** Stable lower-case key: NBT, textures ({@code ironman_<key>.png}), lang ({@code mark.viltrumitecore.<key>}). */
   public String key() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   @Nullable
   public static MarkId byId(int ordinal) {
      return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
   }

   @Nullable
   public static MarkId byKey(@Nullable String key) {
      for (MarkId id : VALUES) {
         if (id.key().equals(key)) {
            return id;
         }
      }

      return null;
   }

   public static int count() {
      return VALUES.length;
   }
}
