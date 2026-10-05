package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.effect.ViltrumiteEffects;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityStatsMixin {
   @Inject(
      method = {"canBeAffected"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rejectDebuffs(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof Player player) {
         if (player.level().isClientSide()) {
            return;
         }

         if (!((ViltrumiteCorePlayer)player).isViltrumite() && effect.getEffect() == ViltrumiteEffects.SCOURGE_VIRUS.get()) {
            cir.setReturnValue(false);
         }

         if (!((ViltrumiteCorePlayer)player).isViltrumite()) {
            return;
         }

         if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && effect.getEffect() != ViltrumiteEffects.SCOURGE_VIRUS.get()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(
      method = {"hurt"},
      at = {@At("HEAD")}
   )
   private void onDamageBreakGrab(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      LivingEntity victim = (LivingEntity)(Object)this;
      Entity attacker = source.getEntity();
      if (attacker != null) {
         if (attacker instanceof ViltrumiteCorePlayer coreAttacker
            && coreAttacker.isViltrumite()
            && coreAttacker.getGrabbedTarget() != null
            && coreAttacker.getGrabbedTarget().equals(victim)) {
            coreAttacker.releaseTarget();
         }

         if (victim instanceof ViltrumiteCorePlayer coreVictim && coreVictim.isViltrumite() && coreVictim.getGrabbedTarget() != null) {
            coreVictim.releaseTarget();
         }
      }
   }
}
