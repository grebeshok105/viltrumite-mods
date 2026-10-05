package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;

public class IdleAction extends ViltrumiteAction {
   public IdleAction(ViltrumiteFakePlayer npc) {
      super(npc);
   }

   @Override
   public boolean canStart() {
      return this.npc.getTarget() == null || !this.npc.getTarget().isAlive();
   }

   @Override
   public void start() {
      this.flight.setFlightState(FlightState.NONE);
      this.flight.setFlightThrottle(0.0F);
      this.flight.setFlightAccelerating(false);
      this.flight.setSpeedLocked(false);
      this.flight.setHoverForward(0.0F);
      this.npc.getAbilities().flying = false;
      this.npc.onUpdateAbilities();
   }

   @Override
   public void tick() {
   }

   @Override
   public boolean shouldStop() {
      return this.npc.getTarget() != null && this.npc.getTarget().isAlive();
   }

   @Override
   public void stop() {
   }
}
