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
   priority = 1150
)
public abstract class PunchModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float S1_RA_P = -45.65F;
   @Unique
   private static final float S1_RA_Y = -14.35F;
   @Unique
   private static final float S1_RA_R = 37.83F;
   @Unique
   private static final float S1_RA_X = -0.65F;
   @Unique
   private static final float S1_RA_Y_POS = 0.14F;
   @Unique
   private static final float S1_RA_Z = -2.54F;
   @Unique
   private static final float S1_LA_P = -84.78F;
   @Unique
   private static final float S1_LA_Y = 30.0F;
   @Unique
   private static final float S1_LA_R = 1.3F;
   @Unique
   private static final float S1_LA_X = -0.65F;
   @Unique
   private static final float S1_LA_Y_POS = 0.14F;
   @Unique
   private static final float S1_LA_Z = -2.54F;
   @Unique
   private static final float S1_H_P = 33.48F;
   @Unique
   private static final float S1_H_Y = 10.43F;
   @Unique
   private static final float S1_H_R = 10.43F;
   @Unique
   private static final float S1_H_X = -0.65F;
   @Unique
   private static final float S1_H_Y_POS = 0.14F;
   @Unique
   private static final float S1_H_Z = -2.54F;
   @Unique
   private static final float S1_B_P = 10.43F;
   @Unique
   private static final float S1_B_Y = 33.48F;
   @Unique
   private static final float S1_B_R = 0.0F;
   @Unique
   private static final float S1_B_X = -0.65F;
   @Unique
   private static final float S1_B_Y_POS = 0.14F;
   @Unique
   private static final float S1_B_Z = -2.54F;
   @Unique
   private static final float S1_RL_P = 0.0F;
   @Unique
   private static final float S1_RL_Y = 26.09F;
   @Unique
   private static final float S1_RL_R = 0.0F;
   @Unique
   private static final float S1_RL_X = 0.39F;
   @Unique
   private static final float S1_RL_Y_POS = 0.14F;
   @Unique
   private static final float S1_RL_Z = 0.94F;
   @Unique
   private static final float S1_LL_P = 0.0F;
   @Unique
   private static final float S1_LL_Y = 26.09F;
   @Unique
   private static final float S1_LL_R = 0.0F;
   @Unique
   private static final float S1_LL_X = 0.0F;
   @Unique
   private static final float S1_LL_Y_POS = 0.0F;
   @Unique
   private static final float S1_LL_Z = 0.0F;
   @Unique
   private static final float S1_C_P = -50.87F;
   @Unique
   private static final float S1_C_Y = 27.39F;
   @Unique
   private static final float S1_C_R = 13.04F;
   @Unique
   private static final float S1_C_X = 0.0F;
   @Unique
   private static final float S1_C_Y_POS = 0.0F;
   @Unique
   private static final float S1_C_Z = 3.33F;
   @Unique
   private static final float S2_RA_P = -116.09F;
   @Unique
   private static final float S2_RA_Y = 3.91F;
   @Unique
   private static final float S2_RA_R = 93.91F;
   @Unique
   private static final float S2_RA_X = 1.96F;
   @Unique
   private static final float S2_RA_Y_POS = 2.32F;
   @Unique
   private static final float S2_RA_Z = -7.39F;
   @Unique
   private static final float S2_LA_P = 36.52F;
   @Unique
   private static final float S2_LA_Y = -11.74F;
   @Unique
   private static final float S2_LA_R = -7.83F;
   @Unique
   private static final float S2_LA_X = 2.1F;
   @Unique
   private static final float S2_LA_Y_POS = 1.67F;
   @Unique
   private static final float S2_LA_Z = -1.96F;
   @Unique
   private static final float S2_H_P = 0.0F;
   @Unique
   private static final float S2_H_Y = 0.0F;
   @Unique
   private static final float S2_H_R = 0.0F;
   @Unique
   private static final float S2_H_X = 2.1F;
   @Unique
   private static final float S2_H_Y_POS = 1.67F;
   @Unique
   private static final float S2_H_Z = -4.2F;
   @Unique
   private static final float S2_B_P = 23.48F;
   @Unique
   private static final float S2_B_Y = -23.48F;
   @Unique
   private static final float S2_B_R = 0.0F;
   @Unique
   private static final float S2_B_X = 1.3F;
   @Unique
   private static final float S2_B_Y_POS = 1.67F;
   @Unique
   private static final float S2_B_Z = -4.2F;
   @Unique
   private static final float S2_RL_P = 0.0F;
   @Unique
   private static final float S2_RL_Y = -23.48F;
   @Unique
   private static final float S2_RL_R = 0.0F;
   @Unique
   private static final float S2_RL_X = 0.0F;
   @Unique
   private static final float S2_RL_Y_POS = 0.0F;
   @Unique
   private static final float S2_RL_Z = -0.87F;
   @Unique
   private static final float S2_LL_P = 0.0F;
   @Unique
   private static final float S2_LL_Y = -23.48F;
   @Unique
   private static final float S2_LL_R = 0.0F;
   @Unique
   private static final float S2_LL_X = -0.14F;
   @Unique
   private static final float S2_LL_Y_POS = 0.0F;
   @Unique
   private static final float S2_LL_Z = 0.87F;
   @Unique
   private static final float S2_C_P = -75.65F;
   @Unique
   private static final float S2_C_Y = -31.36F;
   @Unique
   private static final float S2_C_R = 5.22F;
   @Unique
   private static final float S2_C_X = -1.45F;
   @Unique
   private static final float S2_C_Y_POS = 1.3F;
   @Unique
   private static final float S2_C_Z = 4.34F;

   public PunchModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void onSetAngles(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         int punchTicks = corePlayer.getPunchTicks();
         if (punchTicks > 0) {
            boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
               float partialTick = Minecraft.getInstance().getFrameTime();
               float time = (20.0F - ((float)punchTicks - partialTick)) / 20.0F;
               time = Mth.clamp(time, 0.0F, 1.0F);
               boolean sneaking = livingEntity.isCrouching();
               PlayerModel<?> model = (PlayerModel<?>)this;
               boolean isLeft = corePlayer.isLeftArmPunch();
               float m = isLeft ? -1.0F : 1.0F;
               float armBaseY = sneaking ? 5.2F : 2.0F;
               float bodyBaseY = sneaking ? 3.2F : 0.0F;
               float headBaseY = sneaking ? 4.2F : 0.0F;
               this.head.zRot = 0.0F;
               this.body.yRot = 0.0F;
               this.body.zRot = 0.0F;
               this.applyPunchTransform(
                  this.head,
                  time,
                  33.48F,
                  10.43F * m,
                  10.43F * m,
                  -0.65F * m,
                  0.14F,
                  -2.54F,
                  0.0F,
                  0.0F * m,
                  0.0F * m,
                  2.1F * m,
                  1.67F,
                  -4.2F,
                  this.head.xRot,
                  this.head.yRot,
                  this.head.zRot,
                  0.0F,
                  headBaseY,
                  0.0F
               );
               this.applyPunchTransform(
                  this.body,
                  time,
                  10.43F,
                  33.48F * m,
                  0.0F * m,
                  -0.65F * m,
                  0.14F,
                  -2.54F,
                  23.48F,
                  -23.48F * m,
                  0.0F * m,
                  1.3F * m,
                  1.67F,
                  -4.2F,
                  this.body.xRot,
                  this.body.yRot,
                  this.body.zRot,
                  0.0F,
                  bodyBaseY,
                  0.0F
               );
               if (this.cloak != null) {
                  this.applyPunchTransform(
                     this.cloak,
                     time,
                     -50.87F,
                     27.39F * m,
                     13.04F * m,
                     0.0F * m,
                     0.0F,
                     3.33F,
                     -75.65F,
                     -31.36F * m,
                     5.22F * m,
                     -1.45F * m,
                     1.3F,
                     4.34F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F
                  );
               }

               ModelPart punchArm = isLeft ? this.leftArm : this.rightArm;
               ModelPart idleArm = isLeft ? this.rightArm : this.leftArm;
               float punchBaseX = isLeft ? 5.0F : -5.0F;
               float idleBaseX = isLeft ? -5.0F : 5.0F;
               this.applyPunchTransform(
                  punchArm,
                  time,
                  -45.65F,
                  -14.35F * m,
                  37.83F * m,
                  -0.65F * m,
                  0.14F,
                  -2.54F,
                  -116.09F,
                  3.91F * m,
                  93.91F * m,
                  1.96F * m,
                  2.32F,
                  -7.39F,
                  punchArm.xRot,
                  punchArm.yRot,
                  punchArm.zRot,
                  punchBaseX,
                  armBaseY,
                  0.0F
               );
               this.applyPunchTransform(
                  idleArm,
                  time,
                  -84.78F,
                  30.0F * m,
                  1.3F * m,
                  -0.65F * m,
                  0.14F,
                  -2.54F,
                  36.52F,
                  -11.74F * m,
                  -7.83F * m,
                  2.1F * m,
                  1.67F,
                  -1.96F,
                  idleArm.xRot,
                  idleArm.yRot,
                  idleArm.zRot,
                  idleBaseX,
                  armBaseY,
                  0.0F
               );
               ModelPart punchLeg = isLeft ? this.leftLeg : this.rightLeg;
               ModelPart idleLeg = isLeft ? this.rightLeg : this.leftLeg;
               float punchLegBaseX = isLeft ? 1.9F : -1.9F;
               float idleLegBaseX = isLeft ? -1.9F : 1.9F;
               this.applyPunchTransform(
                  punchLeg,
                  time,
                  0.0F,
                  26.09F * m,
                  0.0F * m,
                  0.39F * m,
                  0.14F,
                  0.94F,
                  0.0F,
                  -23.48F * m,
                  0.0F * m,
                  0.0F * m,
                  0.0F,
                  -0.87F,
                  punchLeg.xRot,
                  punchLeg.yRot,
                  punchLeg.zRot,
                  punchLegBaseX,
                  12.0F,
                  0.0F
               );
               this.applyPunchTransform(
                  idleLeg,
                  time,
                  0.0F,
                  26.09F * m,
                  0.0F * m,
                  0.0F * m,
                  0.0F,
                  0.0F,
                  0.0F,
                  -23.48F * m,
                  0.0F * m,
                  -0.14F * m,
                  0.0F,
                  0.87F,
                  idleLeg.xRot,
                  idleLeg.yRot,
                  idleLeg.zRot,
                  idleLegBaseX,
                  12.0F,
                  0.0F
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
   private void applyPunchTransform(
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
      float baseZ
   ) {
      float overMultiplier = 1.15F;
      float targetP;
      float targetY;
      float targetR;
      float targetX;
      float targetY_pos;
      float targetZ;
      if (time < 0.25F) {
         float localT = time / 0.25F;
         targetP = Mth.lerp(localT, vanP, (float)Math.toRadians((double)s1P));
         targetY = Mth.lerp(localT, vanY, (float)Math.toRadians((double)s1Y));
         targetR = Mth.lerp(localT, vanR, (float)Math.toRadians((double)s1R));
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
         targetP = Mth.lerp(localT, (float)Math.toRadians((double)s2P), vanP);
         targetY = Mth.lerp(localT, (float)Math.toRadians((double)s2Y), vanY);
         targetR = Mth.lerp(localT, (float)Math.toRadians((double)s2R), vanR);
         targetX = Mth.lerp(localT, baseX + s2X, baseX);
         targetY_pos = Mth.lerp(localT, baseY + s2YPos, baseY);
         targetZ = Mth.lerp(localT, baseZ + s2Z, baseZ);
      } else {
         targetP = vanP;
         targetY = vanY;
         targetR = vanR;
         targetX = baseX;
         targetY_pos = baseY;
         targetZ = baseZ;
      }

      part.xRot = targetP;
      part.yRot = targetY;
      part.zRot = targetR;
      part.x = targetX;
      part.y = targetY_pos;
      part.z = targetZ;
   }
}
