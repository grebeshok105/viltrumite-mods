package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.render.animation.BlockAnimationManager;
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
   priority = 3000
)
public abstract class BlockModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   public BlockModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void applyXGuardBlock(T livingEntity, float f, float g, float h, float headYaw, float headPitch, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         boolean isBlocking = corePlayer.isBlocking();
         float weight = BlockAnimationManager.calculateWeight(livingEntity, isBlocking);
         boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
         if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
            if (!(weight < 0.001F) || isBlocking) {
               float sneakOffset = livingEntity.isCrouching() ? 3.0F : 0.0F;
               float headPitchRad = headPitch * (float) (Math.PI / 180.0);
               float pitchMultiplier = headPitchRad > 0.0F ? 0.85F : 0.3F;
               float dynamicPitch = headPitchRad * pitchMultiplier;
               float targetRightPitch = (float)Math.toRadians(-135.0) + dynamicPitch;
               float targetRightYaw = (float)Math.toRadians(-56.74);
               float targetRightRoll = (float)Math.toRadians(-1.96);
               float targetRightX = -4.51F;
               float targetRightY = 2.0F + sneakOffset;
               float targetRightZ = -1.14F;
               float targetLeftPitch = (float)Math.toRadians(-113.48) + dynamicPitch;
               float targetLeftYaw = (float)Math.toRadians(45.98);
               float targetLeftRoll = (float)Math.toRadians(-7.83);
               float targetLeftX = 4.08F;
               float targetLeftY = 2.0F + sneakOffset;
               float targetLeftZ = -1.63F;
               this.rightArm.xRot = Mth.lerp(weight, this.rightArm.xRot, targetRightPitch);
               this.rightArm.yRot = Mth.lerp(weight, this.rightArm.yRot, targetRightYaw);
               this.rightArm.zRot = Mth.lerp(weight, this.rightArm.zRot, targetRightRoll);
               this.rightArm.x = Mth.lerp(weight, this.rightArm.x, targetRightX);
               this.rightArm.y = Mth.lerp(weight, this.rightArm.y, targetRightY);
               this.rightArm.z = Mth.lerp(weight, this.rightArm.z, targetRightZ);
               this.leftArm.xRot = Mth.lerp(weight, this.leftArm.xRot, targetLeftPitch);
               this.leftArm.yRot = Mth.lerp(weight, this.leftArm.yRot, targetLeftYaw);
               this.leftArm.zRot = Mth.lerp(weight, this.leftArm.zRot, targetLeftRoll);
               this.leftArm.x = Mth.lerp(weight, this.leftArm.x, targetLeftX);
               this.leftArm.y = Mth.lerp(weight, this.leftArm.y, targetLeftY);
               this.leftArm.z = Mth.lerp(weight, this.leftArm.z, targetLeftZ);
               this.head.xRot = Mth.lerp(weight, this.head.xRot, this.head.xRot + (float)Math.toRadians(15.0));
               PlayerModel<T> model = (PlayerModel<T>)(Object)this;
               model.hat.copyFrom(this.head);
               model.rightSleeve.copyFrom(this.rightArm);
               model.leftSleeve.copyFrom(this.leftArm);
            }
         }
      }
   }
}
