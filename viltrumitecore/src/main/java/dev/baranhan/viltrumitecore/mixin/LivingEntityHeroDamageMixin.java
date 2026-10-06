package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Non-player victims route through the same hero damage path, and the common
 * lethal boundary (hero totem) is evaluated after vanilla's own totem check.
 */
@Mixin(
   value = {LivingEntity.class},
   priority = 4000
)
public abstract class LivingEntityHeroDamageMixin {
   @Inject(
      method = {"hurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumitecore$routeHeroDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (HeroDamage.isRouting(entity)) {
         return;
      }

      HeroDamage.DamageResult result = HeroDamage.route(entity, source, amount, HeroDamage.DamageKind.EXTERNAL);
      if (result != HeroDamage.DamageResult.PASS) {
         cir.setReturnValue(result == HeroDamage.DamageResult.APPLIED);
      }
   }

   @Inject(
      method = {"checkTotemDeathProtection"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void viltrumitecore$heroTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
      if (!cir.getReturnValue()
         && (Object)this instanceof ServerPlayer player
         && !player.level().isClientSide()
         && HeroDamage.tryHeroTotem(player, source)) {
         cir.setReturnValue(true);
      }
   }
}
