package dev.baranhan.viltrumitecore.hero.ironman.mark.sig;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkSignature;
import net.minecraft.server.level.ServerPlayer;

/** WAR_MACHINE_MK2: shoulder gun: hold to fire at the crosshair, energy instead of ammo (spec §13). */
public final class ShoulderGun implements MarkSignature {
   @Override
   public void press(ServerPlayer player, IronManState state) {
   }
}
