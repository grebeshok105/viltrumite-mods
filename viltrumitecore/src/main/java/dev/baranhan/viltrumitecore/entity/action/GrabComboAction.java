package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;

public class GrabComboAction extends ViltrumiteAction {
   private int stateTicks = 0;
   private int comboType = 0;
   private boolean hasGrabbed = false;
   private float targetYaw = 0.0F;

   public GrabComboAction(ViltrumiteFakePlayer npc) {
      super(npc);
   }

   @Override
   public boolean canStart() {
      return this.npc.getTarget() != null
         && (double)this.npc.distanceTo(this.npc.getTarget()) < 3.0
         && this.npc.attackCooldown <= 0
         && this.core.getPunchTicks() <= 0
         && !this.core.isBarraging()
         && this.npc.getRandom().nextInt(20) == 0;
   }

   @Override
   public void start() {
      this.stateTicks = 0;
      this.hasGrabbed = false;
      this.comboType = this.npc.getRandom().nextInt(3);
      this.npc.lookAtTarget(this.npc.getTarget());
      this.core.setTryingToGrab(true);
      this.core.startDash();
   }

   @Override
   public void tick() {
      this.stateTicks++;
      if (!this.hasGrabbed) {
         if (this.core.getGrabbedTarget() != null) {
            this.hasGrabbed = true;
            this.stateTicks = 0;
         } else if (this.stateTicks > 20) {
            this.core.setTryingToGrab(false);
         }
      } else {
         if (this.comboType == 0) {
            if (this.stateTicks < 30) {
               this.flight.setFlightAccelerating(true);
               this.npc.setYRot(this.npc.getYRot() + (this.npc.getRandom().nextFloat() - 0.5F) * 10.0F);
               this.smoothPitch(this.npc.getXRot(), -45.0F, 0.1F);
            } else if (this.stateTicks < 50) {
               this.flight.setFlightAccelerating(false);
               this.smoothPitch(this.npc.getXRot(), 45.0F, 0.15F);
            } else if (this.stateTicks == 50) {
               this.core.releaseTarget();
               this.core.setLeftArmPunch(this.npc.getRandom().nextBoolean());
               this.core.setPunchTicks(20);
               this.flight.setFlightThrottle(0.0F);
            }
         } else if (this.comboType == 1) {
            if (this.stateTicks == 1) {
               this.targetYaw = this.npc.getYRot() + (float)(this.npc.getRandom().nextBoolean() ? 90 : -90);
            } else if (this.stateTicks < 10) {
               this.smoothYaw(this.npc.getYRot(), this.targetYaw, 0.3F);
            } else if (this.stateTicks == 10) {
               this.core.startDash();
               this.flight.setFlightAccelerating(true);
            } else if (this.stateTicks == 25) {
               this.core.releaseTarget();
               if (this.npc.getRandom().nextBoolean()) {
                  this.core.setLeftArmPunch(this.npc.getRandom().nextBoolean());
                  this.core.setPunchTicks(20);
               } else {
                  this.core.setLeftChop(this.npc.getRandom().nextBoolean());
                  this.core.setChopType(this.npc.getRandom().nextInt(2));
                  this.core.setChopTicks(20);
               }
            }
         } else if (this.comboType == 2) {
            if (this.stateTicks < 30) {
               this.flight.setFlightAccelerating(true);
               this.npc.setYRot(this.npc.getYRot() + (this.npc.getRandom().nextFloat() - 0.5F) * 15.0F);
               this.smoothPitch(this.npc.getXRot(), -30.0F, 0.1F);
            } else if (this.stateTicks < 50) {
               this.smoothPitch(this.npc.getXRot(), 90.0F, 0.1F);
            } else if (this.stateTicks == 50) {
               this.flight.setFlightAccelerating(true);
            } else if (this.stateTicks == 90) {
               this.core.releaseTarget();
               this.flight.setFlightAccelerating(false);
            }
         }
      }
   }

   @Override
   public boolean shouldStop() {
      if (!this.hasGrabbed && this.stateTicks > 20) {
         return true;
      } else if (this.hasGrabbed && this.core.getGrabbedTarget() == null) {
         return true;
      } else if (this.comboType == 0 && this.stateTicks > 55) {
         return true;
      } else {
         return this.comboType == 1 && this.stateTicks > 30 ? true : this.comboType == 2 && this.stateTicks > 95;
      }
   }

   @Override
   public void stop() {
      this.core.setTryingToGrab(false);
      if (this.core.getGrabbedTarget() != null) {
         this.core.releaseTarget();
      }

      this.flight.setFlightAccelerating(false);
      this.flight.setFlightThrottle(0.0F);
      this.npc.attackCooldown = 40;
   }

   private void smoothPitch(float currentPitch, float targetPitch, float speed) {
      float newPitch = currentPitch + (targetPitch - currentPitch) * speed;
      this.npc.setXRot(newPitch);
   }

   private void smoothYaw(float currentYaw, float targetYaw, float speed) {
      float delta = ((targetYaw - currentYaw) % 360.0F + 540.0F) % 360.0F - 180.0F;
      float newYaw = currentYaw + delta * speed;
      this.npc.setYRot(newYaw);
      this.npc.setYHeadRot(newYaw);
      this.npc.setYBodyRot(newYaw);
   }
}
