package dev.baranhan.viltrumitecore.mixin;

import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ordinary melee reads ATTACK_DAMAGE through the hero contract once, so a
 * Regulus gets his +2%-per-heart bonus on vanilla swings (spec 5.5) without
 * touching ability/counter/internal damage, which scale in their own rules.
 */
@Mixin(
   value = {Player.class},
   priority = 4000
)
public abstract class PlayerHeroMeleeMixin {
   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D"
      )
   )
   private double viltrumitecore$scaleHeroMelee(Player instance, Attribute attribute) {
      double base = instance.getAttributeValue(attribute);
      return attribute == Attributes.ATTACK_DAMAGE ? base * HeroRegistry.get(instance).meleeDamageFactor(instance) : base;
   }
}
