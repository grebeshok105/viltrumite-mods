package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
   @Unique
   private float viltrumitecore$preHurtHealth = -1.0F;

   @Inject(
      method = {"hurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void viltrumitecore$routeHeroDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      Player player = (Player)(Object)this;
      this.viltrumitecore$preHurtHealth = player.getHealth();
      if (HeroDamage.isRouting(player)) {
         return;
      }

      HeroDamage.DamageResult result = HeroDamage.route(player, source, amount, HeroDamage.DamageKind.EXTERNAL);
      if (result != HeroDamage.DamageResult.PASS) {
         cir.setReturnValue(result == HeroDamage.DamageResult.APPLIED);
      }
   }

   /**
    * Per-hit bookkeeping: BLOCKED/QUEUED hits never reach the tail, so the
    * health delta here is what this single applied hit actually cost — spec
    * 11.1 interrupts the ritual per hit, not per tick aggregate.
    */
   @Inject(
      method = {"hurt"},
      at = {@At("TAIL")}
   )
   private void viltrumitecore$recordAppliedHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      Player player = (Player)(Object)this;
      float before = this.viltrumitecore$preHurtHealth;
      this.viltrumitecore$preHurtHealth = -1.0F;
      if (!cir.getReturnValue() || before < 0.0F) {
         return;
      }

      float loss = before - player.getHealth();
      if (loss > 0.0F) {
         HeroDamage.recordAppliedLoss(player, loss);
      }
   }
}
