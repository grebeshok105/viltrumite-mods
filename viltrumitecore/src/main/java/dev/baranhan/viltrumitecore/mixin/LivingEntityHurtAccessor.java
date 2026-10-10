package dev.baranhan.viltrumitecore.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla hurt cooldown amount, read by the damage-layer pipeline (iframes). */
@Mixin({LivingEntity.class})
public interface LivingEntityHurtAccessor {
   @Accessor("lastHurt")
   float viltrumitecore$getLastHurt();

   @Accessor("lastHurt")
   void viltrumitecore$setLastHurt(float value);
}
