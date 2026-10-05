package dev.baranhan.viltrumitecore.effect;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class ScourgeVirusEffect extends MobEffect {
   private static final int BASE_INTERVAL = 60;

   public ScourgeVirusEffect() {
      super(MobEffectCategory.HARMFUL, 5038156);
      this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "8d4a2f61-3c77-4c1e-9f2b-5a0e7d6c1b93", -0.15, Operation.MULTIPLY_TOTAL);
   }

   public boolean isDurationEffectTick(int duration, int amplifier) {
      int interval = Math.max(20, 60 >> amplifier);
      return duration % interval == 0;
   }

   public void applyEffectTick(LivingEntity entity, int amplifier) {
      if (entity.getHealth() > 1.0F) {
         entity.hurt(entity.damageSources().magic(), 1.0F + (float)amplifier);
         if (entity.level().isClientSide) {
            return;
         }

         if (entity instanceof ViltrumiteCorePlayer corePlayer) {
            corePlayer.setViltrumite(entity.getRandom().nextBoolean());
            if (corePlayer.isViltrumite()) {
               return;
            }

            corePlayer.setSuperSpeed(false);
            corePlayer.releaseTarget();
            corePlayer.setBarraging(false);
            ViltrumiteFlightPlayer flightPlayer = (ViltrumiteFlightPlayer)entity;
            if (flightPlayer.getFlightState() == FlightState.CRUISE || flightPlayer.getFlightState() == FlightState.SONIC) {
               flightPlayer.stopFlight();
            }
         }
      }
   }

   public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
      super.removeAttributeModifiers(entity, attributes, amplifier);
      if (!entity.level().isClientSide && entity instanceof ViltrumiteCorePlayer corePlayer) {
         corePlayer.setViltrumite(true);
      }
   }
}
