package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"com.bobmowzie.mowziesmobs.server.entity.wroughtnaut.EntityWroughtnaut"},
   remap = false
)
public abstract class WroughtnautDamageBypassMixin {
   @Inject(
      method = {"hurt", "hurt"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void bypassViltrumiteDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      Entity attacker = source.getEntity();
      if (amount > 0.0F && attacker instanceof Player && attacker instanceof ViltrumiteCorePlayer corePlayer) {
         LivingEntity me = (LivingEntity)(Object)this;
         if (!corePlayer.isViltrumite()) {
            return;
         }

         // Shared control guard first (plan §76): an anchored Wroughtnaut takes
         // no immediate health write — the hit queues against the deferred cap.
         HeroDamage.DamageResult result = HeroDamage.route(me, source, amount, HeroDamage.DamageKind.EXTERNAL);
         if (result != HeroDamage.DamageResult.PASS) {
            cir.setReturnValue(result == HeroDamage.DamageResult.APPLIED);
            return;
         }

         float newHealth = me.getHealth() - amount;
         me.setHealth(newHealth);
         me.hurtTime = 10;
         me.invulnerableTime = 10;
         if (newHealth <= 0.0F) {
            me.die(source);
         }

         me.level().playSound(null, me.getX(), me.getY(), me.getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 1.0F, 0.7F);
         cir.setReturnValue(true);
      }
   }
}
