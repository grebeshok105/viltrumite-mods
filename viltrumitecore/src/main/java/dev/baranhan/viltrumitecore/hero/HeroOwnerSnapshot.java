package dev.baranhan.viltrumitecore.hero;

import java.util.Arrays;

/**
 * Owner-private hero snapshot: data only the hero's own client may see. The
 * id list means "owner-only entity ids" and is read per hero: Regulus heart
 * carriers, Homelander focus targets. Sent to the owner only.
 */
public record HeroOwnerSnapshot(int[] carrierEntityIds) {
   public static final HeroOwnerSnapshot EMPTY = new HeroOwnerSnapshot(new int[0]);

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      } else {
         return other instanceof HeroOwnerSnapshot snapshot && Arrays.equals(this.carrierEntityIds, snapshot.carrierEntityIds);
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.carrierEntityIds);
   }
}
