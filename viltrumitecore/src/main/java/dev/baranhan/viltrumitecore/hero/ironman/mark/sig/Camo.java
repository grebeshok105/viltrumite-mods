package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** MARK_15: camouflage ~8 s: refraction ripple, mobs beyond 8 blocks lose Tony, first hit x2 (spec §13). */
public final class Camo implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
