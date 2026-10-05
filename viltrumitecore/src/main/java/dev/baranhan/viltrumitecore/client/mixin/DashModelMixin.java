package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerModel.class},
   priority = 1100
)
public abstract class DashModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Unique
   private static final float RA_P = 15.65F;
   @Unique
   private static final float RA_Y = 19.57F;
   @Unique
   private static final float RA_R = 69.46F;
   @Unique
   private static final float RA_X = -0.22F;
   @Unique
   private static final float RA_Y_POS = 0.59F;
   @Unique
   private static final float RA_Z = -3.32F;
   @Unique
   private static final float LA_P = -100.76F;
   @Unique
   private static final float LA_Y = 42.07F;
   @Unique
   private static final float LA_R = 11.74F;
   @Unique
   private static final float LA_X = -1.96F;
   @Unique
   private static final float LA_Y_POS = 1.73F;
   @Unique
   private static final float LA_Z = -5.22F;
   @Unique
   private static final float H_P = 0.0F;
   @Unique
   private static final float H_Y = 0.0F;
   @Unique
   private static final float H_R = 0.0F;
   @Unique
   private static final float H_X = 0.0F;
   @Unique
   private static final float H_Y_POS = 1.68F;
   @Unique
   private static final float H_Z = -3.32F;
   @Unique
   private static final float B_P = 18.59F;
   @Unique
   private static final float B_Y = 0.0F;
   @Unique
   private static final float B_R = 0.0F;
   @Unique
   private static final float B_X = 0.0F;
   @Unique
   private static final float B_Y_POS = 1.68F;
   @Unique
   private static final float B_Z = -3.32F;
   @Unique
   private static final float RL_P = -21.52F;
   @Unique
   private static final float RL_Y = 0.0F;
   @Unique
   private static final float RL_R = 0.0F;
   @Unique
   private static final float RL_X = 0.0F;
   @Unique
   private static final float RL_Y_POS = 0.0F;
   @Unique
   private static final float RL_Z = 0.0F;
   @Unique
   private static final float LL_P = 27.39F;
   @Unique
   private static final float LL_Y = 0.0F;
   @Unique
   private static final float LL_R = 0.0F;
   @Unique
   private static final float LL_X = 0.0F;
   @Unique
   private static final float LL_Y_POS = 0.0F;
   @Unique
   private static final float LL_Z = 0.0F;

   public DashModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim"},
      at = {@At("TAIL")}
   )
   private void onSetupAnim(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
         if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
            float tickDelta = Minecraft.getInstance().getPartialTick();
            float progress = corePlayer.getDashProgress(tickDelta);
            if (!(progress <= 0.0F)) {
               PlayerModel<?> model = (PlayerModel<?>)(Object)this;
               boolean sneaking = livingEntity.isCrouching();
               float armBaseY = sneaking ? 5.2F : 2.0F;
               float bodyBaseY = sneaking ? 3.2F : 0.0F;
               float headBaseY = sneaking ? 4.2F : 0.0F;
               this.head.zRot = 0.0F;
               this.body.yRot = 0.0F;
               this.body.zRot = 0.0F;
               this.applyDashTransform(
                  this.head,
                  progress,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.68F,
                  -3.32F,
                  0.0F,
                  headBaseY,
                  0.0F,
                  this.head.xRot,
                  this.head.yRot,
                  this.head.zRot
               );
               this.applyDashTransform(
                  this.body,
                  progress,
                  18.59F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.68F,
                  -3.32F,
                  0.0F,
                  bodyBaseY,
                  0.0F,
                  this.body.xRot,
                  this.body.yRot,
                  this.body.zRot
               );
               this.applyDashTransform(
                  this.rightArm,
                  progress,
                  15.65F,
                  19.57F,
                  69.46F,
                  -0.22F,
                  0.59F,
                  -3.32F,
                  -5.0F,
                  armBaseY,
                  0.0F,
                  this.rightArm.xRot,
                  this.rightArm.yRot,
                  this.rightArm.zRot
               );
               this.applyDashTransform(
                  this.leftArm,
                  progress,
                  -100.76F,
                  42.07F,
                  11.74F,
                  -1.96F,
                  1.73F,
                  -5.22F,
                  5.0F,
                  armBaseY,
                  0.0F,
                  this.leftArm.xRot,
                  this.leftArm.yRot,
                  this.leftArm.zRot
               );
               this.applyDashTransform(
                  this.rightLeg,
                  progress,
                  -21.52F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  -1.9F,
                  12.0F,
                  0.0F,
                  this.rightLeg.xRot,
                  this.rightLeg.yRot,
                  this.rightLeg.zRot
               );
               this.applyDashTransform(
                  this.leftLeg,
                  progress,
                  27.39F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.9F,
                  12.0F,
                  0.0F,
                  this.leftLeg.xRot,
                  this.leftLeg.yRot,
                  this.leftLeg.zRot
               );
               model.hat.copyFrom(this.head);
               model.jacket.copyFrom(this.body);
               model.rightSleeve.copyFrom(this.rightArm);
               model.leftSleeve.copyFrom(this.leftArm);
               model.rightPants.copyFrom(this.rightLeg);
               model.leftPants.copyFrom(this.leftLeg);
            }
         }
      }
   }

   @Unique
   private void applyDashTransform(
      ModelPart part,
      float progress,
      float tP,
      float tY,
      float tR,
      float tX,
      float tYPos,
      float tZ,
      float bX,
      float bY,
      float bZ,
      float vanP,
      float vanY,
      float vanR
   ) {
      part.xRot = Mth.lerp(progress, vanP, (float)Math.toRadians((double)tP));
      part.yRot = Mth.lerp(progress, vanY, (float)Math.toRadians((double)tY));
      part.zRot = Mth.lerp(progress, vanR, (float)Math.toRadians((double)tR));
      part.x = Mth.lerp(progress, bX, bX + tX);
      part.y = Mth.lerp(progress, bY, bY + tYPos);
      part.z = Mth.lerp(progress, bZ, bZ + tZ);
   }
}
