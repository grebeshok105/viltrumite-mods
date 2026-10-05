package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;

public class DashCatchAction extends ViltrumiteAction {
   public DashCatchAction(ViltrumiteFakePlayer npc) {
      super(npc);
   }

   @Override
   public boolean canStart() {
      if (this.npc.getTarget() != null) {
         double dist = (double)this.npc.distanceTo(this.npc.getTarget());
         if (this.core.isDashing()) {
            return false;
         } else {
            boolean catchDash = dist > 4.0 && dist <= 12.0 && this.npc.getRandom().nextInt(100) == 1;
            boolean stuckDash = this.npc.horizontalCollision && dist > 2.0 && this.npc.getRandom().nextInt(5) == 1;
            return catchDash || stuckDash;
         }
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.npc.lookAtTarget(this.npc.getTarget());
      this.core.startDash();
   }

   @Override
   public void tick() {
      this.npc.lookAtTarget(this.npc.getTarget());
   }

   @Override
   public boolean shouldStop() {
      return !this.core.isDashing() || this.npc.getTarget() == null;
   }

   @Override
   public void stop() {
   }
}
