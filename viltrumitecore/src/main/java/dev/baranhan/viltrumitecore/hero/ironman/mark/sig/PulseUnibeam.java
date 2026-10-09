package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** MARK_17: pulse Unibeam: three short strong pulses, no overheat (spec §13). */
public final class PulseUnibeam implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
