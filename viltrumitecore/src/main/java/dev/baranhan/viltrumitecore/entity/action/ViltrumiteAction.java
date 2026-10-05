package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;

public abstract class ViltrumiteAction {
   protected ViltrumiteFakePlayer npc;
   protected ViltrumiteCorePlayer core;
   protected ViltrumiteFlightPlayer flight;

   public ViltrumiteAction(ViltrumiteFakePlayer npc) {
      this.npc = npc;
      this.core = (ViltrumiteCorePlayer)npc;
      this.flight = (ViltrumiteFlightPlayer)npc;
   }

   public abstract boolean canStart();

   public abstract void start();

   public abstract void tick();

   public abstract boolean shouldStop();

   public abstract void stop();
}
