package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerModel.class},
   priority = 1170
)
public abstract class ChopModelMixin2<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   public ModelPart cloak;
   @Unique
   private static final float S1_RA_P = -164.35F;
   @Unique
   private static final float S1_RA_Y = -3.91F;
   @Unique
   private static final float S1_RA_R = 91.96F;
   @Unique
   private static final float S1_RA_X = 1.74F;
   @Unique
   private static final float S1_RA_Y_POS = 1.59F;
   @Unique
   private static final float S1_RA_Z = -3.84F;
   @Unique
   private static final float S1_LA_P = 6.52F;
   @Unique
   private static final float S1_LA_Y = -16.96F;
   @Unique
   private static final float S1_LA_R = 0.0F;
   @Unique
   private static final float S1_LA_X = -0.51F;
   @Unique
   private static final float S1_LA_Y_POS = 0.72F;
   @Unique
   private static final float S1_LA_Z = 1.52F;
   @Unique
   private static final float S1_H_P = 10.43F;
   @Unique
   private static final float S1_H_Y = -11.74F;
   @Unique
   private static final float S1_H_R = 0.0F;
   @Unique
   private static final float S1_H_X = 0.0F;
   @Unique
   private static final float S1_H_Y_POS = 0.0F;
   @Unique
   private static final float S1_H_Z = 0.0F;
   @Unique
   private static final float S1_B_P = 0.0F;
   @Unique
   private static final float S1_B_Y = -13.04F;
   @Unique
   private static final float S1_B_R = 0.0F;
   @Unique
   private static final float S1_B_X = 0.0F;
   @Unique
   private static final float S1_B_Y_POS = 0.0F;
   @Unique
   private static final float S1_B_Z = 0.0F;
   @Unique
   private static final float S1_RL_P = 0.0F;
   @Unique
   private static final float S1_RL_Y = -14.35F;
   @Unique
   private static final float S1_RL_R = 0.0F;
   @Unique
   private static final float S1_RL_X = 0.0F;
   @Unique
   private static final float S1_RL_Y_POS = 0.0F;
   @Unique
   private static final float S1_RL_Z = -0.51F;
   @Unique
   private static final float S1_LL_P = 0.0F;
   @Unique
   private static final float S1_LL_Y = -15.65F;
   @Unique
   private static final float S1_LL_R = 0.0F;
   @Unique
   private static final float S1_LL_X = 0.0F;
   @Unique
   private static final float S1_LL_Y_POS = 0.0F;
   @Unique
   private static final float S1_LL_Z = 0.36F;
   @Unique
   private static final float S1_C_P = 0.0F;
   @Unique
   private static final float S1_C_Y = 0.0F;
   @Unique
   private static final float S1_C_R = 0.0F;
   @Unique
   private static final float S1_C_X = 0.0F;
   @Unique
   private static final float S1_C_Y_POS = 0.0F;
   @Unique
   private static final float S1_C_Z = 0.0F;
   @Unique
   private static final float S2_RA_P = 0.0F;
   @Unique
   private static final float S2_RA_Y = 0.0F;
   @Unique
   private static final float S2_RA_R = 95.22F;
   @Unique
   private static final float S2_RA_X = -1.01F;
   @Unique
   private static final float S2_RA_Y_POS = -0.58F;
   @Unique
   private static final float S2_RA_Z = 0.0F;
   @Unique
   private static final float S2_LA_P = 0.0F;
   @Unique
   private static final float S2_LA_Y = 0.0F;
   @Unique
   private static final float S2_LA_R = 0.0F;
   @Unique
   private static final float S2_LA_X = 0.0F;
   @Unique
   private static final float S2_LA_Y_POS = 0.0F;
   @Unique
   private static final float S2_LA_Z = 0.0F;
   @Unique
   private static final float S2_H_P = 2.61F;
   @Unique
   private static final float S2_H_Y = 10.43F;
   @Unique
   private static final float S2_H_R = -6.52F;
   @Unique
   private static final float S2_H_X = 0.0F;
   @Unique
   private static final float S2_H_Y_POS = 0.0F;
   @Unique
   private static final float S2_H_Z = 0.0F;
   @Unique
   private static final float S2_B_P = 0.0F;
   @Unique
   private static final float S2_B_Y = 0.0F;
   @Unique
   private static final float S2_B_R = 0.0F;
   @Unique
   private static final float S2_B_X = 0.0F;
   @Unique
   private static final float S2_B_Y_POS = 0.0F;
   @Unique
   private static final float S2_B_Z = 0.0F;
   @Unique
   private static final float S2_RL_P = 0.0F;
   @Unique
   private static final float S2_RL_Y = 0.0F;
   @Unique
   private static final float S2_RL_R = 0.0F;
   @Unique
   private static final float S2_RL_X = 0.0F;
   @Unique
   private static final float S2_RL_Y_POS = 0.0F;
   @Unique
   private static final float S2_RL_Z = 0.0F;
   @Unique
   private static final float S2_LL_P = 0.0F;
   @Unique
   private static final float S2_LL_Y = 0.0F;
   @Unique
   private static final float S2_LL_R = 0.0F;
   @Unique
   private static final float S2_LL_X = 0.0F;
   @Unique
   private static final float S2_LL_Y_POS = 0.0F;
   @Unique
   private static final float S2_LL_Z = 0.0F;
   @Unique
   private static final float S2_C_P = 0.0F;
   @Unique
   private static final float S2_C_Y = 0.0F;
   @Unique
   private static final float S2_C_R = 0.0F;
   @Unique
   private static final float S2_C_X = 0.0F;
   @Unique
   private static final float S2_C_Y_POS = 0.0F;
   @Unique
   private static final float S2_C_Z = 0.0F;

   public ChopModelMixin2(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim"},
      at = {@At("TAIL")}
   )
   private void onSetupAnim(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         int chopTicks = corePlayer.getChopTicks();
         if (chopTicks > 0) {
            if (corePlayer.getChopType() == 1) {
               boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
               if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
                  float tickDelta = Minecraft.getInstance().getPartialTick();
                  float time = (20.0F - ((float)chopTicks - tickDelta)) / 20.0F;
                  time = Mth.clamp(time, 0.0F, 1.0F);
                  boolean sneaking = livingEntity.isCrouching();
                  boolean isOnGround = livingEntity.onGround();
                  PlayerModel<?> model = (PlayerModel<?>)(Object)this;
                  boolean isLeft = corePlayer.isLeftChop();
                  float m = isLeft ? -1.0F : 1.0F;
                  float armBaseY = sneaking ? 5.2F : 2.0F;
                  float bodyBaseY = sneaking ? 3.2F : 0.0F;
                  float headBaseY = sneaking ? 4.2F : 0.0F;
                  this.head.zRot = 0.0F;
                  this.body.yRot = 0.0F;
                  this.body.zRot = 0.0F;
                  this.applyChopTransform2(
                     this.head,
                     time,
                     10.43F,
                     -11.74F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     2.61F,
                     10.43F * m,
                     -6.52F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     this.head.xRot,
                     this.head.yRot,
                     this.head.zRot,
                     0.0F,
                     headBaseY,
                     0.0F,
                     false
                  );
                  this.applyChopTransform2(
                     this.body,
                     time,
                     0.0F,
                     -13.04F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     this.body.xRot,
                     this.body.yRot,
                     this.body.zRot,
                     0.0F,
                     bodyBaseY,
                     0.0F,
                     false
                  );
                  if (this.cloak != null) {
                     this.applyChopTransform2(
                        this.cloak,
                        time,
                        0.0F,
                        0.0F * m,
                        0.0F * m,
                        0.0F * m,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F * m,
                        0.0F * m,
                        0.0F * m,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        false
                     );
                  }

                  ModelPart chopArm = isLeft ? this.leftArm : this.rightArm;
                  ModelPart idleArm = isLeft ? this.rightArm : this.leftArm;
                  float chopBaseX = isLeft ? 5.0F : -5.0F;
                  float idleBaseX = isLeft ? -5.0F : 5.0F;
                  this.applyChopTransform2(
                     chopArm,
                     time,
                     -164.35F,
                     -3.91F * m,
                     91.96F * m,
                     1.74F * m,
                     1.59F,
                     -3.84F,
                     0.0F,
                     0.0F * m,
                     95.22F * m,
                     -1.01F * m,
                     -0.58F,
                     0.0F,
                     chopArm.xRot,
                     chopArm.yRot,
                     chopArm.zRot,
                     chopBaseX,
                     armBaseY,
                     0.0F,
                     false
                  );
                  this.applyChopTransform2(
                     idleArm,
                     time,
                     6.52F,
                     -16.96F * m,
                     0.0F * m,
                     -0.51F * m,
                     0.72F,
                     1.52F,
                     0.0F,
                     0.0F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     idleArm.xRot,
                     idleArm.yRot,
                     idleArm.zRot,
                     idleBaseX,
                     armBaseY,
                     0.0F,
                     false
                  );
                  ModelPart chopLeg = isLeft ? this.leftLeg : this.rightLeg;
                  ModelPart idleLeg = isLeft ? this.rightLeg : this.leftLeg;
                  float chopLegBaseX = isLeft ? 1.9F : -1.9F;
                  float idleLegBaseX = isLeft ? -1.9F : 1.9F;
                  this.applyChopTransform2(
                     chopLeg,
                     time,
                     0.0F,
                     -14.35F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     -0.51F,
                     0.0F,
                     0.0F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     chopLeg.xRot,
                     chopLeg.yRot,
                     chopLeg.zRot,
                     chopLegBaseX,
                     12.0F,
                     0.0F,
                     isOnGround
                  );
                  this.applyChopTransform2(
                     idleLeg,
                     time,
                     0.0F,
                     -15.65F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.36F,
                     0.0F,
                     0.0F * m,
                     0.0F * m,
                     0.0F * m,
                     0.0F,
                     0.0F,
                     idleLeg.xRot,
                     idleLeg.yRot,
                     idleLeg.zRot,
                     idleLegBaseX,
                     12.0F,
                     0.0F,
                     isOnGround
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
   }

   @Unique
   private void applyChopTransform2(
      ModelPart part,
      float time,
      float s1P,
      float s1Y,
      float s1R,
      float s1X,
      float s1YPos,
      float s1Z,
      float s2P,
      float s2Y,
      float s2R,
      float s2X,
      float s2YPos,
      float s2Z,
      float vanP,
      float vanY,
      float vanR,
      float baseX,
      float baseY,
      float baseZ,
      boolean isAdditive
   ) {
      float startP = isAdditive ? 0.0F : vanP;
      float startY = isAdditive ? 0.0F : vanY;
      float startR = isAdditive ? 0.0F : vanR;
      float overMultiplier = 1.15F;
      float targetP;
      float targetY;
      float targetR;
      float targetX;
      float targetY_pos;
      float targetZ;
      if (time < 0.25F) {
         float localT = time / 0.25F;
         targetP = Mth.lerp(localT, startP, (float)Math.toRadians((double)s1P));
         targetY = Mth.lerp(localT, startY, (float)Math.toRadians((double)s1Y));
         targetR = Mth.lerp(localT, startR, (float)Math.toRadians((double)s1R));
         targetX = Mth.lerp(localT, baseX, baseX + s1X);
         targetY_pos = Mth.lerp(localT, baseY, baseY + s1YPos);
         targetZ = Mth.lerp(localT, baseZ, baseZ + s1Z);
      } else if (time < 0.35F) {
         float localT = (time - 0.25F) / 0.1F;
         targetP = Mth.lerp(localT, (float)Math.toRadians((double)s1P), (float)Math.toRadians((double)s2P) * overMultiplier);
         targetY = Mth.lerp(localT, (float)Math.toRadians((double)s1Y), (float)Math.toRadians((double)s2Y) * overMultiplier);
         targetR = Mth.lerp(localT, (float)Math.toRadians((double)s1R), (float)Math.toRadians((double)s2R) * overMultiplier);
         targetX = Mth.lerp(localT, baseX + s1X, baseX + s2X * overMultiplier);
         targetY_pos = Mth.lerp(localT, baseY + s1YPos, baseY + s2YPos * overMultiplier);
         targetZ = Mth.lerp(localT, baseZ + s1Z, baseZ + s2Z * overMultiplier);
      } else if (time < 0.5F) {
         float localT = (time - 0.35F) / 0.15F;
         targetP = Mth.lerp(localT, (float)Math.toRadians((double)s2P) * overMultiplier, (float)Math.toRadians((double)s2P));
         targetY = Mth.lerp(localT, (float)Math.toRadians((double)s2Y) * overMultiplier, (float)Math.toRadians((double)s2Y));
         targetR = Mth.lerp(localT, (float)Math.toRadians((double)s2R) * overMultiplier, (float)Math.toRadians((double)s2R));
         targetX = Mth.lerp(localT, baseX + s2X * overMultiplier, baseX + s2X);
         targetY_pos = Mth.lerp(localT, baseY + s2YPos * overMultiplier, baseY + s2YPos);
         targetZ = Mth.lerp(localT, baseZ + s2Z * overMultiplier, baseZ + s2Z);
      } else if (time < 0.65F) {
         targetP = (float)Math.toRadians((double)s2P);
         targetY = (float)Math.toRadians((double)s2Y);
         targetR = (float)Math.toRadians((double)s2R);
         targetX = baseX + s2X;
         targetY_pos = baseY + s2YPos;
         targetZ = baseZ + s2Z;
      } else if (time < 0.9F) {
         float localT = (time - 0.65F) / 0.25F;
         targetP = Mth.lerp(localT, (float)Math.toRadians((double)s2P), startP);
         targetY = Mth.lerp(localT, (float)Math.toRadians((double)s2Y), startY);
         targetR = Mth.lerp(localT, (float)Math.toRadians((double)s2R), startR);
         targetX = Mth.lerp(localT, baseX + s2X, baseX);
         targetY_pos = Mth.lerp(localT, baseY + s2YPos, baseY);
         targetZ = Mth.lerp(localT, baseZ + s2Z, baseZ);
      } else {
         targetP = startP;
         targetY = startY;
         targetR = startR;
         targetX = baseX;
         targetY_pos = baseY;
         targetZ = baseZ;
      }

      if (isAdditive) {
         part.xRot = vanP * 0.5F + targetP;
         part.yRot = vanY * 0.5F + targetY;
         part.zRot = vanR * 0.5F + targetR;
      } else {
         part.xRot = targetP;
         part.yRot = targetY;
         part.zRot = targetR;
      }

      part.x = targetX;
      part.y = targetY_pos;
      part.z = targetZ;
   }
}
