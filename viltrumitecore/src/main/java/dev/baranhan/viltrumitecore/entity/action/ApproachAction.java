package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import net.minecraft.world.phys.Vec3;

public class ApproachAction extends ViltrumiteAction {
   private Vec3 lastPos = Vec3.ZERO;
   private int stuckTicks = 0;

   public ApproachAction(ViltrumiteFakePlayer npc) {
      super(npc);
   }

   @Override
   public boolean canStart() {
      return this.npc.getTarget() != null && (double)this.npc.distanceTo(this.npc.getTarget()) > 2.0;
   }

   @Override
   public void start() {
      this.npc.getAbilities().mayfly = true;
      this.npc.getAbilities().flying = true;
      this.npc.onUpdateAbilities();
      this.lastPos = this.npc.position();
      this.stuckTicks = 0;
   }

   @Override
   public void tick() {
      this.npc.lookAtTarget(this.npc.getTarget());
      double distance = (double)this.npc.distanceTo(this.npc.getTarget());
      if (this.flight.getFlightState() == FlightState.NONE && this.npc.getTarget().getY() > this.npc.getY()) {
         this.flight.setFlightState(FlightState.HOVER);
         this.npc.getAbilities().mayfly = true;
         this.npc.getAbilities().flying = true;
         this.npc.onUpdateAbilities();
         if (this.npc.onGround()) {
            this.npc.setDeltaMovement(this.npc.getDeltaMovement().add(0.0, 0.4, 0.0));
            this.npc.hasImpulse = true;
         }
      }

      if (this.npc.attackCooldown <= 0
         && this.core.getThunderclapTicks() <= 0
         && this.core.getPunchTicks() <= 0
         && this.core.getChopTicks() <= 0
         && this.core.getBarrageTicks() == 0
         && this.npc.getRandom().nextInt(80) == 1) {
         this.core.setThunderclapTicks(20);
         this.npc.attackCooldown = 40 + this.npc.getRandom().nextInt(20);
      }

      if (distance > 12.0) {
         this.flight.setFlightAccelerating(true);
         this.flight.setHoverForward(0.0F);
      } else {
         this.flight.setFlightAccelerating(false);
         this.flight.setHoverForward(1.0F);
         float currentThrottle = this.flight.getFlightThrottle();
         if (currentThrottle > 0.0F) {
            this.flight.setFlightThrottle(Math.max(0.0F, currentThrottle - 0.2F));
         } else {
            Vec3 dir = this.npc.getTarget().getEyePosition().subtract(this.npc.getEyePosition()).normalize();
            Vec3 currentVel = this.npc.getDeltaMovement();
            float speed = 0.3F;
            if (currentVel.lengthSqr() < (double)(speed * speed * 4.0F)) {
               this.npc.setDeltaMovement(currentVel.add(dir.scale((double)speed * 0.25)));
            }

            this.npc.hasImpulse = true;
         }
      }

      Vec3 currentPos = this.npc.position();
      double dX = this.npc.getTarget().getX() - this.npc.getX();
      double dZ = this.npc.getTarget().getZ() - this.npc.getZ();
      double horizontalDistToTarget = Math.sqrt(dX * dX + dZ * dZ);
      if (horizontalDistToTarget > 2.0) {
         double horizontalMovement = Math.sqrt(
            Math.pow(currentPos.x - this.lastPos.x, 2.0) + Math.pow(currentPos.z - this.lastPos.z, 2.0)
         );
         if (horizontalMovement < 0.05) {
            this.stuckTicks++;
         } else {
            this.stuckTicks = 0;
         }

         if (this.stuckTicks >= 3) {
            this.flight.setFlightState(FlightState.HOVER);
            this.npc.getAbilities().mayfly = true;
            this.npc.getAbilities().flying = true;
            this.npc.onUpdateAbilities();
            this.npc.setDeltaMovement(this.npc.getDeltaMovement().add(0.0, 0.4, 0.0));
            this.npc.hasImpulse = true;
            this.stuckTicks = 0;
         }
      } else {
         this.stuckTicks = 0;
      }

      this.lastPos = currentPos;
   }

   @Override
   public boolean shouldStop() {
      return this.npc.getTarget() == null || (double)this.npc.distanceTo(this.npc.getTarget()) <= 2.0;
   }

   @Override
   public void stop() {
      this.flight.setHoverForward(0.0F);
   }
}
