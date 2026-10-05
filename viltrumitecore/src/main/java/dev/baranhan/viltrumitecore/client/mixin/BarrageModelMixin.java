package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerModel.class},
   priority = 1175
)
public abstract class BarrageModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   public ModelPart cloak;
   @Unique
   private static final float S1_RA_P = 45.18F;
   @Unique
   private static final float S1_RA_Y = 0.0F;
   @Unique
   private static final float S1_RA_R = 44.84F;
   @Unique
   private static final float S1_RA_X = -0.49F;
   @Unique
   private static final float S1_RA_Y_POS = 0.32F;
   @Unique
   private static final float S1_RA_Z = 0.36F;
   @Unique
   private static final float S1_LA_P = 1.43F;
   @Unique
   private static final float S1_LA_Y = 14.05F;
   @Unique
   private static final float S1_LA_R = -7.57F;
   @Unique
   private static final float S1_LA_X = -0.24F;
   @Unique
   private static final float S1_LA_Y_POS = 0.32F;
   @Unique
   private static final float S1_LA_Z = -2.62F;
   @Unique
   private static final float S1_H_P = 9.66F;
   @Unique
   private static final float S1_H_Y = 9.44F;
   @Unique
   private static final float S1_H_R = 0.0F;
   @Unique
   private static final float S1_H_X = -0.49F;
   @Unique
   private static final float S1_H_Y_POS = 0.32F;
   @Unique
   private static final float S1_H_Z = -1.86F;
   @Unique
   private static final float S1_B_P = 8.26F;
   @Unique
   private static final float S1_B_Y = 14.37F;
   @Unique
   private static final float S1_B_R = 0.0F;
   @Unique
   private static final float S1_B_X = -0.49F;
   @Unique
   private static final float S1_B_Y_POS = 0.32F;
   @Unique
   private static final float S1_B_Z = -1.86F;
   @Unique
   private static final float S1_RL_P = 0.0F;
   @Unique
   private static final float S1_RL_Y = 7.76F;
   @Unique
   private static final float S1_RL_R = 0.0F;
   @Unique
   private static final float S1_RL_X = -0.26F;
   @Unique
   private static final float S1_RL_Y_POS = 0.0F;
   @Unique
   private static final float S1_RL_Z = -0.01F;
   @Unique
   private static final float S1_LL_P = 0.0F;
   @Unique
   private static final float S1_LL_Y = 7.6F;
   @Unique
   private static final float S1_LL_R = 0.0F;
   @Unique
   private static final float S1_LL_X = 0.09F;
   @Unique
   private static final float S1_LL_Y_POS = 0.0F;
   @Unique
   private static final float S1_LL_Z = -0.77F;
   @Unique
   private static final float S1_C_P = -20.22F;
   @Unique
   private static final float S1_C_Y = -24.23F;
   @Unique
   private static final float S1_C_R = -5.38F;
   @Unique
   private static final float S1_C_X = 0.3F;
   @Unique
   private static final float S1_C_Y_POS = 0.07F;
   @Unique
   private static final float S1_C_Z = 2.52F;
   @Unique
   private static final float S2_RA_P = -90.42F;
   @Unique
   private static final float S2_RA_Y = -7.9F;
   @Unique
   private static final float S2_RA_R = -1.38F;
   @Unique
   private static final float S2_RA_X = 3.94F;
   @Unique
   private static final float S2_RA_Y_POS = 0.87F;
   @Unique
   private static final float S2_RA_Z = -6.24F;
   @Unique
   private static final float S2_LA_P = 0.0F;
   @Unique
   private static final float S2_LA_Y = -34.38F;
   @Unique
   private static final float S2_LA_R = -10.25F;
   @Unique
   private static final float S2_LA_X = 0.21F;
   @Unique
   private static final float S2_LA_Y_POS = 0.76F;
   @Unique
   private static final float S2_LA_Z = -0.14F;
   @Unique
   private static final float S2_H_P = 2.66F;
   @Unique
   private static final float S2_H_Y = -13.55F;
   @Unique
   private static final float S2_H_R = 9.4F;
   @Unique
   private static final float S2_H_X = 0.62F;
   @Unique
   private static final float S2_H_Y_POS = 0.28F;
   @Unique
   private static final float S2_H_Z = -2.36F;
   @Unique
   private static final float S2_B_P = 7.08F;
   @Unique
   private static final float S2_B_Y = -24.08F;
   @Unique
   private static final float S2_B_R = 0.0F;
   @Unique
   private static final float S2_B_X = 0.62F;
   @Unique
   private static final float S2_B_Y_POS = 0.28F;
   @Unique
   private static final float S2_B_Z = -2.36F;
   @Unique
   private static final float S2_RL_P = 0.0F;
   @Unique
   private static final float S2_RL_Y = -13.16F;
   @Unique
   private static final float S2_RL_R = 0.0F;
   @Unique
   private static final float S2_RL_X = 0.0F;
   @Unique
   private static final float S2_RL_Y_POS = 0.0F;
   @Unique
   private static final float S2_RL_Z = -1.48F;
   @Unique
   private static final float S2_LL_P = 0.0F;
   @Unique
   private static final float S2_LL_Y = -14.77F;
   @Unique
   private static final float S2_LL_R = 0.0F;
   @Unique
   private static final float S2_LL_X = 0.06F;
   @Unique
   private static final float S2_LL_Y_POS = 0.0F;
   @Unique
   private static final float S2_LL_Z = -0.05F;
   @Unique
   private static final float S2_C_P = -13.8F;
   @Unique
   private static final float S2_C_Y = -23.56F;
   @Unique
   private static final float S2_C_R = -3.2F;
   @Unique
   private static final float S2_C_X = -0.07F;
   @Unique
   private static final float S2_C_Y_POS = 0.28F;
   @Unique
   private static final float S2_C_Z = 2.42F;

   public BarrageModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void onSetupAnim(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         int barrageTicks = corePlayer.getBarrageTicks();
         if (barrageTicks != 0) {
            if (livingEntity instanceof Player p) {
               p.xCloak = p.getX();
               p.yCloak = p.getY();
               p.zCloak = p.getZ();
               p.xCloakO = p.xo;
               p.yCloakO = p.yo;
               p.zCloakO = p.zo;
            }

            boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
               float tickDelta = Minecraft.getInstance().getFrameTime();
               int phase = 0;
               float progress = 0.0F;
               float pFrame = 0.0F;
               boolean isRight = true;
               byte var23;
               if (barrageTicks > 0) {
                  float exactTime = (float)(barrageTicks - 1) + tickDelta;
                  if (exactTime < 0.0F) {
                     exactTime = 0.0F;
                  }

                  if (exactTime < 5.0F) {
                     var23 = 1;
                     progress = exactTime / 5.0F;
                     isRight = true;
                  } else {
                     var23 = 2;
                     float punchTime = exactTime - 5.0F;
                     int punchIndex = (int)(punchTime / 3.0F);
                     pFrame = punchTime % 3.0F / 3.0F;
                     isRight = punchIndex % 2 == 0;
                  }
               } else {
                  var23 = 3;
                  float exactTimex = (float)barrageTicks + tickDelta;
                  float timeSinceRelease = 9.0F + exactTimex;
                  if (timeSinceRelease < 4.0F) {
                     progress = 0.0F;
                  } else {
                     progress = (timeSinceRelease - 4.0F) / 5.0F;
                  }

                  isRight = !corePlayer.isLeftBarrageArm();
               }

               boolean sneaking = livingEntity.isCrouching();
               boolean isGrounded = true;
               if (livingEntity instanceof ViltrumiteFlightPlayer flightPlayer) {
                  isGrounded = flightPlayer.getFlightState() == FlightState.NONE;
               }

               float armBaseY = sneaking ? 5.2F : 2.0F;
               float bodyBaseY = sneaking ? 3.2F : 0.0F;
               float headBaseY = sneaking ? 4.2F : 0.0F;
               this.head.zRot = 0.0F;
               this.body.yRot = 0.0F;
               this.body.zRot = 0.0F;
               PlayerModel<?> model = (PlayerModel<?>)this;
               this.applyBarrageTransform(
                  this.head,
                  this.getT(9.66F, 9.44F, 0.0F, -0.49F, 0.32F, -1.86F, !isRight),
                  this.getT(2.66F, -13.55F, 9.4F, 0.62F, 0.28F, -2.36F, !isRight),
                  this.getT(9.66F, 9.44F, 0.0F, -0.49F, 0.32F, -1.86F, isRight),
                  this.head.xRot,
                  this.head.yRot,
                  this.head.zRot,
                  0.0F,
                  headBaseY,
                  0.0F,
                  var23,
                  progress,
                  pFrame,
                  false,
                  false,
                  false
               );
               this.applyBarrageTransform(
                  this.body,
                  this.getT(8.26F, 14.37F, 0.0F, -0.49F, 0.32F, -1.86F, !isRight),
                  this.getT(7.08F, -24.08F, 0.0F, 0.62F, 0.28F, -2.36F, !isRight),
                  this.getT(8.26F, 14.37F, 0.0F, -0.49F, 0.32F, -1.86F, isRight),
                  this.body.xRot,
                  this.body.yRot,
                  this.body.zRot,
                  0.0F,
                  bodyBaseY,
                  0.0F,
                  var23,
                  progress,
                  pFrame,
                  false,
                  false,
                  false
               );
               if (this.cloak != null) {
                  this.applyBarrageTransform(
                     this.cloak,
                     this.getT(-20.22F, -24.23F, -5.38F, 0.3F, 0.07F, 2.52F, !isRight),
                     this.getT(-13.8F, -23.56F, -3.2F, -0.07F, 0.28F, 2.42F, !isRight),
                     this.getT(-20.22F, -24.23F, -5.38F, 0.3F, 0.07F, 2.52F, isRight),
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     var23,
                     progress,
                     pFrame,
                     false,
                     false,
                     false
                  );
               }

               this.applyBarrageTransform(
                  this.rightArm,
                  isRight ? this.getT(45.18F, 0.0F, 44.84F, -0.49F, 0.32F, 0.36F, false) : this.getT(1.43F, 14.05F, -7.57F, -0.24F, 0.32F, -2.62F, true),
                  isRight ? this.getT(-90.42F, -7.9F, -1.38F, 3.94F, 0.87F, -6.24F, false) : this.getT(0.0F, -34.38F, -10.25F, 0.21F, 0.76F, -0.14F, true),
                  isRight ? this.getT(1.43F, 14.05F, -7.57F, -0.24F, 0.32F, -2.62F, true) : this.getT(45.18F, 0.0F, 44.84F, -0.49F, 0.32F, 0.36F, false),
                  this.rightArm.xRot,
                  this.rightArm.yRot,
                  this.rightArm.zRot,
                  -5.0F,
                  armBaseY,
                  0.0F,
                  var23,
                  progress,
                  pFrame,
                  false,
                  isRight,
                  false
               );
               this.applyBarrageTransform(
                  this.leftArm,
                  isRight ? this.getT(1.43F, 14.05F, -7.57F, -0.24F, 0.32F, -2.62F, false) : this.getT(45.18F, 0.0F, 44.84F, -0.49F, 0.32F, 0.36F, true),
                  isRight ? this.getT(0.0F, -34.38F, -10.25F, 0.21F, 0.76F, -0.14F, false) : this.getT(-90.42F, -7.9F, -1.38F, 3.94F, 0.87F, -6.24F, true),
                  isRight ? this.getT(45.18F, 0.0F, 44.84F, -0.49F, 0.32F, 0.36F, true) : this.getT(1.43F, 14.05F, -7.57F, -0.24F, 0.32F, -2.62F, false),
                  this.leftArm.xRot,
                  this.leftArm.yRot,
                  this.leftArm.zRot,
                  5.0F,
                  armBaseY,
                  0.0F,
                  var23,
                  progress,
                  pFrame,
                  false,
                  !isRight,
                  true
               );
               this.applyBarrageTransform(
                  this.rightLeg,
                  isRight ? this.getT(0.0F, 7.76F, 0.0F, -0.26F, 0.0F, -0.01F, false) : this.getT(0.0F, 7.6F, 0.0F, 0.09F, 0.0F, -0.77F, true),
                  isRight ? this.getT(0.0F, -13.16F, 0.0F, 0.0F, 0.0F, -1.48F, false) : this.getT(0.0F, -14.77F, 0.0F, 0.06F, 0.0F, -0.05F, true),
                  isRight ? this.getT(0.0F, 7.6F, 0.0F, 0.09F, 0.0F, -0.77F, true) : this.getT(0.0F, 7.76F, 0.0F, -0.26F, 0.0F, -0.01F, false),
                  this.rightLeg.xRot,
                  this.rightLeg.yRot,
                  this.rightLeg.zRot,
                  -1.9F,
                  12.0F,
                  0.0F,
                  var23,
                  progress,
                  pFrame,
                  isGrounded,
                  false,
                  false
               );
               this.applyBarrageTransform(
                  this.leftLeg,
                  isRight ? this.getT(0.0F, 7.6F, 0.0F, 0.09F, 0.0F, -0.77F, false) : this.getT(0.0F, 7.76F, 0.0F, -0.26F, 0.0F, -0.01F, true),
                  isRight ? this.getT(0.0F, -14.77F, 0.0F, 0.06F, 0.0F, -0.05F, false) : this.getT(0.0F, -13.16F, 0.0F, 0.0F, 0.0F, -1.48F, true),
                  isRight ? this.getT(0.0F, 7.76F, 0.0F, -0.26F, 0.0F, -0.01F, true) : this.getT(0.0F, 7.6F, 0.0F, 0.09F, 0.0F, -0.77F, false),
                  this.leftLeg.xRot,
                  this.leftLeg.yRot,
                  this.leftLeg.zRot,
                  1.9F,
                  12.0F,
                  0.0F,
                  var23,
                  progress,
                  pFrame,
                  isGrounded,
                  false,
                  false
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
   private float[] getT(float p, float y, float r, float x, float yp, float z, boolean mirror) {
      return mirror
         ? new float[]{(float)Math.toRadians((double)p), (float)Math.toRadians((double)(-y)), (float)Math.toRadians((double)(-r)), -x, yp, z}
         : new float[]{(float)Math.toRadians((double)p), (float)Math.toRadians((double)y), (float)Math.toRadians((double)r), x, yp, z};
   }

   @Unique
   private float[] getMidT(float[] t1, float[] t2) {
      return new float[]{
         (t1[0] + t2[0]) / 2.0F, (t1[1] + t2[1]) / 2.0F, (t1[2] + t2[2]) / 2.0F, (t1[3] + t2[3]) / 2.0F, (t1[4] + t2[4]) / 2.0F, (t1[5] + t2[5]) / 2.0F
      };
   }

   @Unique
   private void applyBarrageTransform(
      ModelPart part,
      float[] cS1,
      float[] cS2,
      float[] nS1,
      float vanP,
      float vanY,
      float vanR,
      float baseX,
      float baseY,
      float baseZ,
      int phase,
      float progress,
      float p,
      boolean isAdditive,
      boolean applyCustomMidpoint,
      boolean mirrorCustomMidpoint
   ) {
      float tP = vanP;
      float tY = vanY;
      float tR = vanR;
      float tX = baseX;
      float tYPos = baseY;
      float tZ = baseZ;
      float startP = isAdditive ? 0.0F : vanP;
      float startY = isAdditive ? 0.0F : vanY;
      float startR = isAdditive ? 0.0F : vanR;
      float[] cS1_5 = this.getMidT(cS1, cS2);
      if (applyCustomMidpoint) {
         float[] customMid = this.getT(21.95F, -0.45F, 88.8F, 0.0F, 0.0F, 0.0F, mirrorCustomMidpoint);
         cS1_5[0] = customMid[0];
         cS1_5[1] = customMid[1];
         cS1_5[2] = customMid[2];
      }

      if (phase == 1) {
         tP = Mth.lerp(progress, startP, cS1[0]);
         tY = Mth.lerp(progress, startY, cS1[1]);
         tR = Mth.lerp(progress, startR, cS1[2]);
         tX = Mth.lerp(progress, baseX, baseX + cS1[3]);
         tYPos = Mth.lerp(progress, baseY, baseY + cS1[4]);
         tZ = Mth.lerp(progress, baseZ, baseZ + cS1[5]);
      } else if (phase == 3) {
         tP = Mth.lerp(progress, cS2[0], startP);
         tY = Mth.lerp(progress, cS2[1], startY);
         tR = Mth.lerp(progress, cS2[2], startR);
         tX = Mth.lerp(progress, baseX + cS2[3], baseX);
         tYPos = Mth.lerp(progress, baseY + cS2[4], baseY);
         tZ = Mth.lerp(progress, baseZ + cS2[5], baseZ);
      } else if (phase == 2) {
         float strikeOver = 1.15F;
         if (p < 0.125F) {
            float t = p / 0.125F;
            tP = Mth.lerp(t, cS1[0], cS1_5[0]);
            tY = Mth.lerp(t, cS1[1], cS1_5[1]);
            tR = Mth.lerp(t, cS1[2], cS1_5[2]);
            tX = Mth.lerp(t, baseX + cS1[3], baseX + cS1_5[3]);
            tYPos = Mth.lerp(t, baseY + cS1[4], baseY + cS1_5[4]);
            tZ = Mth.lerp(t, baseZ + cS1[5], baseZ + cS1_5[5]);
         } else if (p < 0.25F) {
            float t = (p - 0.125F) / 0.125F;
            tP = Mth.lerp(t, cS1_5[0], cS2[0] * strikeOver);
            tY = Mth.lerp(t, cS1_5[1], cS2[1] * strikeOver);
            tR = Mth.lerp(t, cS1_5[2], cS2[2] * strikeOver);
            tX = Mth.lerp(t, baseX + cS1_5[3], baseX + cS2[3] * strikeOver);
            tYPos = Mth.lerp(t, baseY + cS1_5[4], baseY + cS2[4] * strikeOver);
            tZ = Mth.lerp(t, baseZ + cS1_5[5], baseZ + cS2[5] * strikeOver);
         } else if (p < 0.55F) {
            float t = (p - 0.25F) / 0.3F;
            tP = Mth.lerp(t, cS2[0] * strikeOver, cS2[0]);
            tY = Mth.lerp(t, cS2[1] * strikeOver, cS2[1]);
            tR = Mth.lerp(t, cS2[2] * strikeOver, cS2[2]);
            tX = Mth.lerp(t, baseX + cS2[3] * strikeOver, baseX + cS2[3]);
            tYPos = Mth.lerp(t, baseY + cS2[4] * strikeOver, baseY + cS2[4]);
            tZ = Mth.lerp(t, baseZ + cS2[5] * strikeOver, baseZ + cS2[5]);
         } else if (p < 0.75F) {
            tP = cS2[0];
            tY = cS2[1];
            tR = cS2[2];
            tX = baseX + cS2[3];
            tYPos = baseY + cS2[4];
            tZ = baseZ + cS2[5];
         } else {
            float t = (p - 0.75F) / 0.25F;
            tP = Mth.lerp(t, cS2[0], nS1[0]);
            tY = Mth.lerp(t, cS2[1], nS1[1]);
            tR = Mth.lerp(t, cS2[2], nS1[2]);
            tX = Mth.lerp(t, baseX + cS2[3], baseX + nS1[3]);
            tYPos = Mth.lerp(t, baseY + cS2[4], baseY + nS1[4]);
            tZ = Mth.lerp(t, baseZ + cS2[5], baseZ + nS1[5]);
         }
      }

      float additiveBlend = 0.3F;
      if (phase == 1) {
         additiveBlend = Mth.lerp(progress, 1.0F, 0.3F);
      } else if (phase == 3) {
         additiveBlend = Mth.lerp(progress, 0.3F, 1.0F);
      }

      if (isAdditive) {
         part.xRot = vanP * additiveBlend + tP;
         part.yRot = vanY * additiveBlend + tY;
         part.zRot = vanR * additiveBlend + tR;
      } else {
         part.xRot = tP;
         part.yRot = tY;
         part.zRot = tR;
      }

      part.x = tX;
      part.y = tYPos;
      part.z = tZ;
   }
}
