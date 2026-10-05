package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import net.minecraft.world.phys.Vec3;

public class BlockAction extends ViltrumiteAction {
   private int blockTimer = 0;

   public BlockAction(ViltrumiteFakePlayer npc) {
      super(npc);
   }

   @Override
   public boolean canStart() {
      if (!this.core.isBarraging() && this.core.getPunchTicks() <= 0 && this.core.getChopTicks() <= 0 && this.core.getThunderclapTicks() <= 0) {
         if (this.npc.getTarget() != null) {
            double dist = (double)this.npc.distanceTo(this.npc.getTarget());
            if (dist <= 6.0) {
               if (this.npc.getTarget() instanceof ViltrumiteCorePlayer targetCore) {
                  if (targetCore.isBarraging()) {
                     return true;
                  }

                  if (targetCore.getPunchTicks() > 10 && this.npc.getRandom().nextInt(100) < 25 + this.npc.getIntelligence() * 15) {
                     return true;
                  }

                  if (targetCore.getChopTicks() > 10 && this.npc.getRandom().nextInt(100) < 10 + this.npc.getIntelligence() * 15) {
                     return true;
                  }
               }

               if (this.npc.getTarget().swinging && this.npc.getRandom().nextInt(100) < this.npc.getIntelligence() * 5) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.blockTimer = 0;
      this.npc.lookAtTarget(this.npc.getTarget());
      this.core.setBarraging(false);
      this.core.setPunchTicks(0);
      this.core.setChopTicks(0);
      this.core.setThunderclapTicks(0);
      this.core.setBlocking(true);
      this.flight.setFlightState(FlightState.HOVER);
      this.flight.setFlightThrottle(0.0F);
      this.flight.setFlightAccelerating(false);
      this.flight.setSpeedLocked(false);
   }

   @Override
   public void tick() {
      this.blockTimer++;
      this.npc.lookAtTarget(this.npc.getTarget());
      if (this.npc.getTarget() != null && (double)this.npc.distanceTo(this.npc.getTarget()) > 2.0) {
         this.flight.setHoverForward(1.0F);
         Vec3 dir = this.npc.getTarget().getEyePosition().subtract(this.npc.getEyePosition()).normalize();
         this.npc.setDeltaMovement(this.npc.getDeltaMovement().add(dir.scale(0.04)));
         this.npc.hasImpulse = true;
      } else {
         this.flight.setHoverForward(0.0F);
         Vec3 currentVel = this.npc.getDeltaMovement();
         if (currentVel.lengthSqr() > 0.05) {
            this.npc.setDeltaMovement(currentVel.scale(0.7));
            this.npc.hasImpulse = true;
         }
      }
   }

   @Override
   public boolean shouldStop() {
      if (!this.core.isBlocking()) {
         return true;
      } else if (this.npc.getTarget() != null && this.npc.getTarget().isAlive()) {
         double dist = (double)this.npc.distanceTo(this.npc.getTarget());
         if (!(this.npc.getTarget() instanceof ViltrumiteCorePlayer targetCore)) {
            if (dist > 6.0 || this.blockTimer >= 20) {
               return true;
            }
         } else {
            boolean isAttacking = targetCore.isBarraging() || targetCore.getPunchTicks() > 0 || targetCore.getChopTicks() > 0;
            if (!isAttacking || dist > 6.0) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   public void stop() {
      this.core.setBlocking(false);
      this.npc.attackCooldown = 0;
   }
}
