package dev.baranhan.viltrumitecore.hero.ironman.veronica;

import dev.baranhan.viltrumitecore.entity.VeronicaPodEntity;
import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import net.minecraft.server.level.ServerPlayer;

/** Server driver of Veronica (spec §12.1–§12.3): call, pod life, menu, choice. */
public final class IronManVeronica {
   private IronManVeronica() {
   }

   public static void press(ServerPlayer player, IronManState state) {
   }

   public static void onPodLanded(ServerPlayer owner, VeronicaPodEntity pod) {
   }

   public static void onPodLeft(ServerPlayer owner, VeronicaPodEntity pod) {
   }

   public static void choose(ServerPlayer player, int choice) {
   }
}
