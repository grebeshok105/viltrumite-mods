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
public abstract class ThunderclapModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   public ModelPart cloak;
   @Unique
   private static final float S1_RA_P = -23.35F;
   @Unique
   private static final float S1_RA_Y = 111.83F;
   @Unique
   private static final float S1_RA_R = 61.39F;
   @Unique
   private static final float S1_RA_X = -1.08F;
   @Unique
   private static final float S1_RA_Y_POS = 0.46F;
   @Unique
   private static final float S1_RA_Z = 2.19F;
   @Unique
   private static final float S1_LA_P = -23.29F;
   @Unique
   private static final float S1_LA_Y = -112.52F;
   @Unique
   private static final float S1_LA_R = -61.31F;
   @Unique
   private static final float S1_LA_X = 1.06F;
   @Unique
   private static final float S1_LA_Y_POS = 0.81F;
   @Unique
   private static final float S1_LA_Z = 2.19F;
   @Unique
   private static final float S1_H_P = 6.95F;
   @Unique
   private static final float S1_H_Y = 0.0F;
   @Unique
   private static final float S1_H_R = 0.0F;
   @Unique
   private static final float S1_H_X = 0.0F;
   @Unique
   private static final float S1_H_Y_POS = 0.81F;
   @Unique
   private static final float S1_H_Z = 2.19F;
   @Unique
   private static final float S1_B_P = -10.68F;
   @Unique
   private static final float S1_B_Y = 0.0F;
   @Unique
   private static final float S1_B_R = 0.0F;
   @Unique
   private static final float S1_B_X = 0.0F;
   @Unique
   private static final float S1_B_Y_POS = 0.81F;
   @Unique
   private static final float S1_B_Z = 2.19F;
   @Unique
   private static final float S1_RL_P = -8.56F;
   @Unique
   private static final float S1_RL_Y = 0.0F;
   @Unique
   private static final float S1_RL_R = 0.0F;
   @Unique
   private static final float S1_LL_P = 2.93F;
   @Unique
   private static final float S1_LL_Y = 0.0F;
   @Unique
   private static final float S1_LL_R = 0.0F;
   @Unique
   private static final float S1_C_P = 1.07F;
   @Unique
   private static final float S1_C_Y = 0.0F;
   @Unique
   private static final float S1_C_R = 0.0F;
   @Unique
   private static final float S1_C_X = 0.0F;
   @Unique
   private static final float S1_C_Y_POS = 1.38F;
   @Unique
   private static final float S1_C_Z = -1.29F;
   @Unique
   private static final float S2_RA_P = -89.15F;
   @Unique
   private static final float S2_RA_Y = -26.88F;
   @Unique
   private static final float S2_RA_R = 0.0F;
   @Unique
   private static final float S2_RA_X = 0.66F;
   @Unique
   private static final float S2_RA_Y_POS = 0.73F;
   @Unique
   private static final float S2_RA_Z = -3.96F;
   @Unique
   private static final float S2_LA_P = -88.69F;
   @Unique
   private static final float S2_LA_Y = 26.99F;
   @Unique
   private static final float S2_LA_R = 0.0F;
   @Unique
   private static final float S2_LA_X = -0.65F;
   @Unique
   private static final float S2_LA_Y_POS = 0.73F;
   @Unique
   private static final float S2_LA_Z = -3.95F;
   @Unique
   private static final float S2_H_P = 15.64F;
   @Unique
   private static final float S2_H_Y = 0.0F;
   @Unique
   private static final float S2_H_R = 0.0F;
   @Unique
   private static final float S2_H_X = 0.0F;
   @Unique
   private static final float S2_H_Y_POS = 0.73F;
   @Unique
   private static final float S2_H_Z = -2.75F;
   @Unique
   private static final float S2_B_P = 13.73F;
   @Unique
   private static final float S2_B_Y = 0.0F;
   @Unique
   private static final float S2_B_R = 0.0F;
   @Unique
   private static final float S2_B_X = 0.0F;
   @Unique
   private static final float S2_B_Y_POS = 0.73F;
   @Unique
   private static final float S2_B_Z = -2.75F;
   @Unique
   private static final float S2_RL_P = 0.0F;
   @Unique
   private static final float S2_LL_P = 0.0F;
   @Unique
   private static final float S2_C_P = -25.83F;
   @Unique
   private static final float S2_C_Y = 0.0F;
   @Unique
   private static final float S2_C_R = 0.0F;
   @Unique
   private static final float S2_C_X = 0.0F;
   @Unique
   private static final float S2_C_Y_POS = -0.06F;
   @Unique
   private static final float S2_C_Z = 2.59F;

   public ThunderclapModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void onSetAngles(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
      if (livingEntity instanceof ViltrumiteCorePlayer corePlayer) {
         int tcTicks = corePlayer.getThunderclapTicks();
         if (tcTicks > 0) {
            boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
               float tickDelta = Minecraft.getInstance().getFrameTime();
               float time = (20.0F - ((float)tcTicks - tickDelta)) / 20.0F;
               time = Mth.clamp(time, 0.0F, 1.0F);
               this.head.zRot = 0.0F;
               this.body.yRot = 0.0F;
               this.body.zRot = 0.0F;
               PlayerModel<?> model = (PlayerModel<?>)(Object)this;
               this.applyTCTransform(
                  this.head,
                  time,
                  6.95F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.81F,
                  2.19F,
                  15.64F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.73F,
                  -2.75F,
                  this.head.xRot,
                  this.head.yRot,
                  this.head.zRot,
                  0.0F,
                  0.0F,
                  0.0F
               );
               this.applyTCTransform(
                  this.body,
                  time,
                  -10.68F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.81F,
                  2.19F,
                  13.73F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.73F,
                  -2.75F,
                  this.body.xRot,
                  this.body.yRot,
                  this.body.zRot,
                  0.0F,
                  0.0F,
                  0.0F
               );
               this.applyTCTransform(
                  this.rightArm,
                  time,
                  -23.35F,
                  111.83F,
                  61.39F,
                  -1.08F,
                  0.46F,
                  2.19F,
                  -89.15F,
                  -26.88F,
                  0.0F,
                  0.66F,
                  0.73F,
                  -3.96F,
                  this.rightArm.xRot,
                  this.rightArm.yRot,
                  this.rightArm.zRot,
                  -5.0F,
                  2.0F,
                  0.0F
               );
               this.applyTCTransform(
                  this.leftArm,
                  time,
                  -23.29F,
                  -112.52F,
                  -61.31F,
                  1.06F,
                  0.81F,
                  2.19F,
                  -88.69F,
                  26.99F,
                  0.0F,
                  -0.65F,
                  0.73F,
                  -3.95F,
                  this.leftArm.xRot,
                  this.leftArm.yRot,
                  this.leftArm.zRot,
                  5.0F,
                  2.0F,
                  0.0F
               );
               this.applyTCTransform(
                  this.rightLeg,
                  time,
                  -8.56F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  this.rightLeg.xRot,
                  this.rightLeg.yRot,
                  this.rightLeg.zRot,
                  -1.9F,
                  12.0F,
                  0.0F
               );
               this.applyTCTransform(
                  this.leftLeg,
                  time,
                  2.93F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  this.leftLeg.xRot,
                  this.leftLeg.yRot,
                  this.leftLeg.zRot,
                  1.9F,
                  12.0F,
                  0.0F
               );
               if (this.cloak != null) {
                  this.applyTCTransform(
                     this.cloak,
                     time,
                     1.07F,
                     0.0F,
                     0.0F,
                     0.0F,
                     1.38F,
                     -1.29F,
                     -25.83F,
                     0.0F,
                     0.0F,
                     0.0F,
                     -0.06F,
                     2.59F,
                     this.cloak.xRot,
                     this.cloak.yRot,
                     this.cloak.zRot,
                     0.0F,
                     0.0F,
                     0.0F
                  );
               }

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
   private void applyTCTransform(
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
      float strikeOverMultiplier = 1.15F;
      float windUpOverMultiplier = 1.15F;
      float targetP;
      float targetY;
      float targetR;
      float targetX;
      float targetY_pos;
      float targetZ;
      if (time < 0.15F) {
         float t = time / 0.15F;
         targetP = Mth.lerp(t, vanP, (float)Math.toRadians((double)s1P) * windUpOverMultiplier);
         targetY = Mth.lerp(t, vanY, (float)Math.toRadians((double)s1Y) * windUpOverMultiplier);
         targetR = Mth.lerp(t, vanR, (float)Math.toRadians((double)s1R) * windUpOverMultiplier);
         targetX = Mth.lerp(t, baseX, baseX + s1X * windUpOverMultiplier);
         targetY_pos = Mth.lerp(t, baseY, baseY + s1YPos * windUpOverMultiplier);
         targetZ = Mth.lerp(t, baseZ, baseZ + s1Z * windUpOverMultiplier);
      } else if (time < 0.25F) {
         float t = (time - 0.15F) / 0.1F;
         targetP = Mth.lerp(t, (float)Math.toRadians((double)s1P) * windUpOverMultiplier, (float)Math.toRadians((double)s1P));
         targetY = Mth.lerp(t, (float)Math.toRadians((double)s1Y) * windUpOverMultiplier, (float)Math.toRadians((double)s1Y));
         targetR = Mth.lerp(t, (float)Math.toRadians((double)s1R) * windUpOverMultiplier, (float)Math.toRadians((double)s1R));
         targetX = Mth.lerp(t, baseX + s1X * windUpOverMultiplier, baseX + s1X);
         targetY_pos = Mth.lerp(t, baseY + s1YPos * windUpOverMultiplier, baseY + s1YPos);
         targetZ = Mth.lerp(t, baseZ + s1Z * windUpOverMultiplier, baseZ + s1Z);
      } else if (time < 0.35F) {
         targetP = (float)Math.toRadians((double)s1P);
         targetY = (float)Math.toRadians((double)s1Y);
         targetR = (float)Math.toRadians((double)s1R);
         targetX = baseX + s1X;
         targetY_pos = baseY + s1YPos;
         targetZ = baseZ + s1Z;
      } else if (time < 0.45F) {
         float t = (time - 0.35F) / 0.1F;
         targetP = Mth.lerp(t, (float)Math.toRadians((double)s1P), (float)Math.toRadians((double)s2P) * strikeOverMultiplier);
         targetY = Mth.lerp(t, (float)Math.toRadians((double)s1Y), (float)Math.toRadians((double)s2Y) * strikeOverMultiplier);
         targetR = Mth.lerp(t, (float)Math.toRadians((double)s1R), (float)Math.toRadians((double)s2R) * strikeOverMultiplier);
         targetX = Mth.lerp(t, baseX + s1X, baseX + s2X * strikeOverMultiplier);
         targetY_pos = Mth.lerp(t, baseY + s1YPos, baseY + s2YPos * strikeOverMultiplier);
         targetZ = Mth.lerp(t, baseZ + s1Z, baseZ + s2Z * strikeOverMultiplier);
      } else if (time < 0.55F) {
         float t = (time - 0.45F) / 0.1F;
         targetP = Mth.lerp(t, (float)Math.toRadians((double)s2P) * strikeOverMultiplier, (float)Math.toRadians((double)s2P));
         targetY = Mth.lerp(t, (float)Math.toRadians((double)s2Y) * strikeOverMultiplier, (float)Math.toRadians((double)s2Y));
         targetR = Mth.lerp(t, (float)Math.toRadians((double)s2R) * strikeOverMultiplier, (float)Math.toRadians((double)s2R));
         targetX = Mth.lerp(t, baseX + s2X * strikeOverMultiplier, baseX + s2X);
         targetY_pos = Mth.lerp(t, baseY + s2YPos * strikeOverMultiplier, baseY + s2YPos);
         targetZ = Mth.lerp(t, baseZ + s2Z * strikeOverMultiplier, baseZ + s2Z);
      } else if (time < 0.7F) {
         targetP = (float)Math.toRadians((double)s2P);
         targetY = (float)Math.toRadians((double)s2Y);
         targetR = (float)Math.toRadians((double)s2R);
         targetX = baseX + s2X;
         targetY_pos = baseY + s2YPos;
         targetZ = baseZ + s2Z;
      } else {
         float t = (time - 0.7F) / 0.3F;
         targetP = Mth.lerp(t, (float)Math.toRadians((double)s2P), vanP);
         targetY = Mth.lerp(t, (float)Math.toRadians((double)s2Y), vanY);
         targetR = Mth.lerp(t, (float)Math.toRadians((double)s2R), vanR);
         targetX = Mth.lerp(t, baseX + s2X, baseX);
         targetY_pos = Mth.lerp(t, baseY + s2YPos, baseY);
         targetZ = Mth.lerp(t, baseZ + s2Z, baseZ);
      }

      part.xRot = targetP;
      part.yRot = targetY;
      part.zRot = targetR;
      part.x = targetX;
      part.y = targetY_pos;
      part.z = targetZ;
   }
}
