package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** IRON_HEART_MK3: slam: jump or dive into the ground, strong ring shockwave (spec §13). */
public final class Slam implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
