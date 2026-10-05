package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({HumanoidModel.class})
public abstract class SuperSpeedModelMixin<T extends LivingEntity> {
   @ModifyVariable(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private float modifyLimbAngle(float limbSwing, T entity) {
      if (entity instanceof ViltrumiteCorePlayer corePlayer && corePlayer.isSuperSpeed()) {
         return limbSwing * 3.5F;
      }

      return limbSwing;
   }

   @ModifyVariable(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = @At("HEAD"),
      ordinal = 1,
      argsOnly = true
   )
   private float modifyLimbDistance(float limbSwingAmount, T entity) {
      if (entity instanceof ViltrumiteCorePlayer corePlayer && corePlayer.isSuperSpeed() && limbSwingAmount > 0.05F) {
         return Math.min(1.3F, limbSwingAmount * 2.0F);
      }

      return limbSwingAmount;
   }
}
