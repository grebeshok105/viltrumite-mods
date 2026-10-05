package dev.baranhan.viltrumitecore.entity.action;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class MeleeCombatAction extends ViltrumiteAction {
   public MeleeCombatAction(ViltrumiteFakePlayer npc) {
      super(npc);
   }

   @Override
   public boolean canStart() {
      return this.npc.getTarget() != null && (double)this.npc.distanceTo(this.npc.getTarget()) <= 3.0;
   }

   @Override
   public void start() {
      this.flight.setFlightState(FlightState.HOVER);
      this.flight.setFlightThrottle(0.0F);
      this.flight.setFlightAccelerating(false);
      this.flight.setSpeedLocked(false);
      this.npc.getAbilities().mayfly = true;
      this.npc.getAbilities().flying = true;
   }

   @Override
   public void tick() {
      this.npc.lookAtTarget(this.npc.getTarget());
      double dist = (double)this.npc.distanceTo(this.npc.getTarget());
      Vec3 currentVel = this.npc.getDeltaMovement();
      if (currentVel.lengthSqr() > 0.05) {
         this.npc.setDeltaMovement(currentVel.scale(0.6));
         this.npc.hasImpulse = true;
      }

      if (dist > 1.8) {
         this.flight.setHoverForward(1.0F);
         Vec3 dir = this.npc.getTarget().getEyePosition().subtract(this.npc.getEyePosition()).normalize();
         this.npc.setDeltaMovement(this.npc.getDeltaMovement().add(dir.scale(0.05)));
         this.npc.hasImpulse = true;
      } else if (dist < 1.0) {
         this.flight.setHoverForward(0.0F);
         Vec3 dirAway = this.npc.getEyePosition().subtract(this.npc.getTarget().getEyePosition()).normalize();
         this.npc.setDeltaMovement(this.npc.getDeltaMovement().add(dirAway.scale(0.08)));
         this.npc.hasImpulse = true;
      } else {
         this.flight.setHoverForward(0.0F);
         this.npc.setDeltaMovement(this.npc.getDeltaMovement().scale(0.5));
         this.npc.hasImpulse = true;
      }

      boolean fastCombat = !(this.npc.getTarget() instanceof Player) || this.npc.getTarget() instanceof ViltrumiteFakePlayer;
      if (this.core.getPunchTicks() <= 0
         && this.core.getChopTicks() <= 0
         && this.core.getBarrageTicks() == 0
         && this.core.getThunderclapTicks() <= 0
         && this.npc.attackCooldown <= 0) {
         int rand = this.npc.getRandom().nextInt(100);
         if (dist > 1.5) {
            if (rand < 20) {
               this.core.setThunderclapTicks(20);
            } else if (rand < 50) {
               this.core.setBarraging(true);
               this.core.setBarrageTicks(1);
            } else {
               this.core.setLeftArmPunch(this.npc.getRandom().nextBoolean());
               this.core.setPunchTicks(20);
               if (fastCombat) {
                  this.core.setPunchCooldown(10);
               }
            }
         } else if (rand < 10) {
            this.core.setThunderclapTicks(20);
         } else if (rand < 30) {
            this.core.setBarraging(true);
            this.core.setBarrageTicks(1);
         } else if (rand < 50) {
            this.core.setLeftArmPunch(this.npc.getRandom().nextBoolean());
            this.core.setPunchTicks(20);
            if (fastCombat) {
               this.core.setPunchCooldown(10);
            }
         } else {
            this.core.setLeftChop(this.npc.getRandom().nextBoolean());
            this.core.setChopType(this.npc.getRandom().nextInt(2));
            this.core.setChopTicks(20);
         }

         if (fastCombat) {
            this.npc.attackCooldown = 10 + this.npc.getRandom().nextInt(10);
         } else {
            this.npc.attackCooldown = 30 + this.npc.getRandom().nextInt(12);
         }
      }
   }

   @Override
   public boolean shouldStop() {
      return this.core.isBarraging() && this.core.getBarrageTicks() > 30
         ? true
         : this.npc.getTarget() == null || (double)this.npc.distanceTo(this.npc.getTarget()) > 4.0;
   }

   @Override
   public void stop() {
      if (this.core.isBarraging()) {
         this.core.setBarraging(false);
         boolean fastCombat = !(this.npc.getTarget() instanceof Player) || this.npc.getTarget() instanceof ViltrumiteFakePlayer;
         if (fastCombat) {
            this.core.setBarrageCooldown(15);
            this.npc.attackCooldown = 10;
         } else {
            this.npc.attackCooldown = 25;
         }
      }
   }
}
