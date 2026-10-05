package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.render.animation.GrabAnimationManager;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerModel.class},
   priority = 2000
)
public abstract class GrabModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   public GrabModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void applyDarthVaderChoke(T livingEntity, float f, float g, float h, float headYaw, float headPitch, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         boolean isGrabbing = corePlayer.getGrabbedTarget() != null || corePlayer.isTryingToGrab();
         boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
         if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
            float weight = GrabAnimationManager.calculateWeight(livingEntity, isGrabbing);
            if (!(weight < 0.001F) || isGrabbing) {
               float targetPitch = -1.8453F + this.head.xRot;
               float targetYaw = this.head.yRot + 0.35F;
               float targetRoll = 0.0F;
               this.leftArm.xRot = Mth.lerp(weight, this.leftArm.xRot, targetPitch);
               this.leftArm.yRot = Mth.lerp(weight, this.leftArm.yRot, targetYaw);
               this.leftArm.zRot = Mth.lerp(weight, this.leftArm.zRot, targetRoll);
               PlayerModel<T> model = (PlayerModel<T>)(Object)this;
               model.leftSleeve.copyFrom(this.leftArm);
            }
         }
      }
   }
}
