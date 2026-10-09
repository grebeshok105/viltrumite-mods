package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** MARK_39: back booster: instant dash to sonic in any direction, also a vertical take-off (spec §13). */
public final class Starboost implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
