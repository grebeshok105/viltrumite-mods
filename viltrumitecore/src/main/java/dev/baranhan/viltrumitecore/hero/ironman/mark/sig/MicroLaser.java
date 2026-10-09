package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** MARK_7: micro-lasers from the forearms: thin precise RMB beam, one target, breaks shields (spec §13). */
public final class MicroLaser implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
