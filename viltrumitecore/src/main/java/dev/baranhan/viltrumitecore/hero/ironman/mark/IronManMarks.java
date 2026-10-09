package dev.baranhan.viltrumitecore.hero.ironman.mark;

import dev.baranhan.viltrumitecore.entity.EmptySuitEntity;
import net.minecraft.server.level.ServerPlayer;

/** Server driver of the marks (spec §4.3, §12.4–§12.7): equip, exit, empty suit, break. */
public final class IronManMarks {
   private IronManMarks() {
   }

   public static void onEmptySuitLeft(ServerPlayer owner, EmptySuitEntity suit) {
   }

   public static boolean enterEmptySuit(ServerPlayer who, EmptySuitEntity suit) {
      return false;
   }
}
