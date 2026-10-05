package dev.baranhan.viltrumiteflight.client.mixin;

import dev.baranhan.viltrumiteflight.client.PoseDataManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerModel.class})
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   public ModelPart cloak;

   public PlayerModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim"},
      at = {@At("TAIL")}
   )
   private void onSetAngles(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      PlayerModel<?> playerModel = (PlayerModel<?>)(Object)this;
      boolean sneaking = livingEntity.isCrouching();
      if (!sneaking) {
         float headYawVan = this.head.yRot;
         float headPitchVan = this.head.xRot;
         float headRollVan = this.head.zRot;
         float headBaseY = sneaking ? 4.2F : 0.0F;
         this.applyTransform(this.head, playerModel.hat, PoseDataManager.TP.head, 0.0F, headBaseY, 0.0F, headPitchVan, headYawVan, headRollVan);
         float bodyBaseY = sneaking ? 3.2F : 0.0F;
         this.applyTransform(this.body, playerModel.jacket, PoseDataManager.TP.body, 0.0F, bodyBaseY, 0.0F, 0.0F, 0.0F, 0.0F);
         if (this.cloak != null) {
            this.applyTransform(this.cloak, null, PoseDataManager.TP.cape, 0.0F, bodyBaseY, 0.0F, 0.0F, 0.0F, 0.0F);
         }

         float armBaseY = sneaking ? 5.2F : 2.0F;
         this.applyTransform(
            this.rightArm,
            playerModel.rightSleeve,
            PoseDataManager.TP.rightArm,
            -5.0F,
            armBaseY,
            0.0F,
            this.rightArm.xRot,
            this.rightArm.yRot,
            this.rightArm.zRot,
            true
         );
         this.applyTransform(
            this.leftArm,
            playerModel.leftSleeve,
            PoseDataManager.TP.leftArm,
            5.0F,
            armBaseY,
            0.0F,
            this.leftArm.xRot,
            this.leftArm.yRot,
            this.leftArm.zRot,
            true
         );
         this.applyTransform(
            this.rightLeg,
            playerModel.rightPants,
            PoseDataManager.TP.rightLeg,
            -1.9F,
            12.0F,
            0.0F,
            this.rightLeg.xRot,
            this.rightLeg.yRot,
            this.rightLeg.zRot,
            true
         );
         this.applyTransform(
            this.leftLeg,
            playerModel.leftPants,
            PoseDataManager.TP.leftLeg,
            1.9F,
            12.0F,
            0.0F,
            this.leftLeg.xRot,
            this.leftLeg.yRot,
            this.leftLeg.zRot,
            true
         );
      }
   }

   @Unique
   private void applyTransform(
      ModelPart part, ModelPart layer, PoseDataManager.PartTransform transform, float baseX, float baseY, float baseZ, float vanP, float vanY, float vanR
   ) {
      this.applyTransform(part, layer, transform, baseX, baseY, baseZ, vanP, vanY, vanR, false);
   }

   @Unique
   private void applyTransform(
      ModelPart part,
      ModelPart layer,
      PoseDataManager.PartTransform transform,
      float baseX,
      float baseY,
      float baseZ,
      float vanP,
      float vanY,
      float vanR,
      boolean isLimb
   ) {
      if (transform != null) {
         float customP = (float)Math.toRadians((double)transform.pitch);
         float customY = (float)Math.toRadians((double)transform.yaw);
         float customR = (float)Math.toRadians((double)transform.roll);
         part.xRot = vanP + customP;
         part.yRot = vanY + customY;
         part.zRot = vanR + customR;
         part.x = baseX + transform.x;
         part.y = baseY + transform.y;
         part.z = baseZ + transform.z;
         if (layer != null) {
            layer.copyFrom(part);
         }
      }
   }
}
