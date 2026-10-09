package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** MARK_42: rocket fist: the glove flies to the target, hits and returns (spec §13). */
public final class RocketFist implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
