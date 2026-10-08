package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.homelander.HomelanderPoser;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Homelander third-person layers (lasers, focus, roar); logic lives in HomelanderPoser. */
@Mixin(
   value = {PlayerModel.class},
   priority = 1180
)
public abstract class HomelanderModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;

   protected HomelanderModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void homelanderSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HomelanderPoser.poseThirdPerson((PlayerModel<?>)(Object)this, entity, this.cloak);
   }
}
