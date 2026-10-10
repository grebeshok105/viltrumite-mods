package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.ironman.IronManPoser;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iron Man third-person poses after the flight and Homelander layers; logic lives in IronManPoser. */
@Mixin(
   value = {PlayerModel.class},
   priority = 1190
)
public abstract class IronManModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   protected IronManModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void ironmanSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      IronManPoser.poseThirdPerson((PlayerModel<?>)(Object)this, entity);
   }
}
