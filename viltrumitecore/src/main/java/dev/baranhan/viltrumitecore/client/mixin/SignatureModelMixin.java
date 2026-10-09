package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.ironman.mark.sig.SignaturePoser;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mark signature arm poses after the Iron Man hover layer (priority 1190); logic in SignaturePoser. */
@Mixin(
   value = {PlayerModel.class},
   priority = 1195
)
public abstract class SignatureModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   protected SignatureModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void signatureSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      SignaturePoser.poseThirdPerson((PlayerModel<?>)(Object)this, entity);
   }
}
