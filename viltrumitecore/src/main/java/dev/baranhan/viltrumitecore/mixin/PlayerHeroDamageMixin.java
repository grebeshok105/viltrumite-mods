package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player victims route external hits through the hero damage path. Nested
 * hurt() calls while routing skip interception via the re-entry guard.
 */
@Mixin(
   value = {Player.class},
   priority = 4000
)
public abstract class PlayerHeroDamageMixin {
   @Inject(
      method = {"hurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumitecore$routeHeroDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      Player player = (Player)(Object)this;
      if (HeroDamage.isRouting(player)) {
         return;
      }

      HeroDamage.DamageResult result = HeroDamage.route(player, source, amount, HeroDamage.DamageKind.EXTERNAL);
      if (result != HeroDamage.DamageResult.PASS) {
         cir.setReturnValue(result == HeroDamage.DamageResult.APPLIED);
      }
   }

}
