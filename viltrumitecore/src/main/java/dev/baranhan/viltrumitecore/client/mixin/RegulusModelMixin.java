package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming;
import dev.baranhan.viltrumitecore.client.render.animation.RegulusAnimationManager;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third-person Regulus poses (spec 13.3). Reads the synced HeroPublicSnapshot;
 * one-shot casts run on actionElapsed keyframes, continuous states (lion
 * stance, mania channel pull, ritual hold) blend in through the weight
 * manager. Priority 1180: after Barrage (1175), before Grab (2000).
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1180
)
public abstract class RegulusModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Unique
   private final float[] regulusBasePositions = new float[18];
   @Unique
   private boolean regulusPositionOffsets;
   @Unique
   private float regulusMirror = 1.0F;

   public RegulusModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("HEAD")}
   )
   private void restoreRegulusPivots(T entity, float f, float g, float h, float yaw, float pitch, CallbackInfo ci) {
      if (!this.regulusPositionOffsets) {
         return;
      }
      ModelPart[] parts = {this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg};
      for (int i = 0; i < parts.length; i++) {
         parts[i].x = this.regulusBasePositions[i * 3];
         parts[i].y = this.regulusBasePositions[i * 3 + 1];
         parts[i].z = this.regulusBasePositions[i * 3 + 2];
      }
      this.regulusPositionOffsets = false;
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusSetupAnim(T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (!(livingEntity instanceof HeroPlayer heroPlayer)) {
         return;
      }

      HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
      if (snapshot == null || snapshot.heroId() != HeroId.REGULUS) {
         RegulusAnimationManager.reset(livingEntity);
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      boolean isLocalFirstPerson = livingEntity == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
      if (isLocalFirstPerson && !ShaderCompat.isShadowPass()) {
         return;
      }

      float partialTick = minecraft.getFrameTime();
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
      this.regulusMirror = livingEntity.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
      this.head.zRot = 0.0F;
      this.body.yRot = 0.0F;
      this.body.zRot = 0.0F;

      // Continuous poses blend first; the cast keyframes then own the limbs.
      float lionWeight = RegulusAnimationManager.calculateWeight(
         livingEntity, RegulusAnimationManager.Pose.LION, snapshot.lionActive()
      );
      if (lionWeight > 0.01F) {
         this.applyLionStance(lionWeight, ageInTicks);
      }

      float channelWeight = RegulusAnimationManager.calculateWeight(
         livingEntity, RegulusAnimationManager.Pose.CHANNEL, snapshot.controlTargetId() >= 0
      );
      if (channelWeight > 0.01F) {
         this.applyChannelPull(channelWeight, ageInTicks);
      }

      float ritualWeight = RegulusAnimationManager.calculateWeight(
         livingEntity, RegulusAnimationManager.Pose.RITUAL, snapshot.ritualTicks() >= 0
      );
      if (ritualWeight > 0.01F) {
         this.applyRitualHold(ritualWeight, livingEntity.getUsedItemHand() == InteractionHand.OFF_HAND);
      }

      HeroAction action = HeroAction.byId(snapshot.actionId());
      if (action != null) {
         float elapsed = RegulusAnimationManager.actionTime(livingEntity, snapshot, partialTick);
         switch (action) {
            case LIONS_HEART:
               this.applyLionWindup(elapsed);
               break;
            case DEBRIS_KICK:
               this.applyDebrisKick(elapsed);
               break;
            case MANIA:
               this.applyManiaExtend(elapsed);
               break;
            case GREEDS_EMBRACE:
               this.applyEmbraceRaise(elapsed);
               break;
            case COUNTER:
               this.applyCounterSlam(elapsed);
               break;
            default:
               break;
         }
      }

      float offsetWeight = Math.max(lionWeight, Math.max(channelWeight, ritualWeight));
      if (action != null && snapshot.actionLength() > 0) {
         float elapsed = RegulusAnimationManager.actionTime(livingEntity, snapshot, partialTick);
         RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(action);
         if (timing != null) {
            float windup = timing.eventTick() > 0 ? elapsed / timing.eventTick() : 0.0F;
            float recovery = (timing.length() - elapsed) / Math.max(1, timing.length() - timing.eventTick());
            offsetWeight = Math.max(offsetWeight, Mth.clamp(Math.min(windup, recovery), 0.0F, 1.0F));
         }
      }
      this.applyPositionOffsets(offsetWeight, action);

      model.hat.copyFrom(this.head);
      model.jacket.copyFrom(this.body);
      model.rightSleeve.copyFrom(this.rightArm);
      model.leftSleeve.copyFrom(this.leftArm);
      model.rightPants.copyFrom(this.rightLeg);
      model.leftPants.copyFrom(this.leftLeg);
   }

   @Unique
   private void applyPositionOffsets(float weight, HeroAction action) {
      if (weight <= 0.001F) {
         return;
      }
      ModelPart[] parts = {this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg};
      for (int i = 0; i < parts.length; i++) {
         this.regulusBasePositions[i * 3] = parts[i].x;
         this.regulusBasePositions[i * 3 + 1] = parts[i].y;
         this.regulusBasePositions[i * 3 + 2] = parts[i].z;
      }
      this.regulusPositionOffsets = true;
      this.body.z -= 0.4F * weight;
      this.head.z -= 0.3F * weight;
      this.rightArm.z -= 0.5F * weight;
      this.leftArm.z -= 0.5F * weight;
      if (action == HeroAction.DEBRIS_KICK) {
         this.mirroredPart(this.rightLeg).z -= 1.2F * weight;
         this.mirroredPart(this.leftLeg).z += 0.4F * weight;
      } else if (action == HeroAction.COUNTER) {
         this.body.y -= 0.6F * weight;
      }
   }

   /** Keyframe lerp: rows are {tick, xDeg, yDeg, zDeg}, first row is reached from the part's vanilla pose. */
   @Unique
   private void poseKeyed(ModelPart part, float elapsed, float[][] keys) {
      part = this.mirroredPart(part);
      float baseX = (float)Math.toDegrees(part.xRot);
      float baseY = (float)Math.toDegrees(part.yRot);
      float baseZ = (float)Math.toDegrees(part.zRot);
      boolean additive = part == this.head || part == this.body;
      part.xRot = (float)Math.toRadians(RegulusPoseTiming.keyedAngle(elapsed, additive ? 0.0F : baseX, keys, 1) + (additive ? baseX : 0.0F));
      part.yRot = (float)Math.toRadians(this.regulusMirror * RegulusPoseTiming.keyedAngle(elapsed, additive ? 0.0F : baseY * this.regulusMirror, keys, 2) + (additive ? baseY : 0.0F));
      part.zRot = (float)Math.toRadians(this.regulusMirror * RegulusPoseTiming.keyedAngle(elapsed, additive ? 0.0F : baseZ * this.regulusMirror, keys, 3) + (additive ? baseZ : 0.0F));
   }

   /** Weighted blend toward a pose — used by the continuous states. */
   @Unique
   private void poseBlend(ModelPart part, float weight, float xDeg, float yDeg, float zDeg) {
      part = this.mirroredPart(part);
      part.xRot = Mth.lerp(weight, part.xRot, (float)Math.toRadians((double)xDeg));
      part.yRot = Mth.lerp(weight, part.yRot, (float)Math.toRadians(yDeg * this.regulusMirror));
      part.zRot = Mth.lerp(weight, part.zRot, (float)Math.toRadians(zDeg * this.regulusMirror));
   }

   @Unique
   private void poseBlendAdditive(ModelPart part, float weight, float xDeg, float yDeg, float zDeg) {
      part.xRot += Mth.lerp(weight, 0.0F, (float)Math.toRadians(xDeg));
      part.yRot += Mth.lerp(weight, 0.0F, (float)Math.toRadians(yDeg * this.regulusMirror));
      part.zRot += Mth.lerp(weight, 0.0F, (float)Math.toRadians(zDeg * this.regulusMirror));
   }

   @Unique
   private ModelPart mirroredPart(ModelPart part) {
      if (this.regulusMirror > 0.0F) {
         return part;
      }
      if (part == this.rightArm) {
         return this.leftArm;
      }
      if (part == this.leftArm) {
         return this.rightArm;
      }
      if (part == this.rightLeg) {
         return this.leftLeg;
      }
      if (part == this.leftLeg) {
         return this.rightLeg;
      }
      return part;
   }

   /** Lion windup: right hand pressed to the chest, fist clench at the event tick. */
   @Unique
   private void applyLionWindup(float elapsed) {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.LIONS_HEART);
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {7.0F, -62.0F, -34.0F, 8.0F},
            {12.0F, -74.0F, -38.0F, 12.0F},
            {(float)timing.eventTick(), -72.0F, -36.0F, 30.0F},
            {(float)timing.length(), 0.0F, 0.0F, 0.0F}
         }
      );
      this.poseKeyed(this.leftArm, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, -18.0F, 10.0F, -8.0F}, {(float)timing.length(), 0.0F, 0.0F, 0.0F}});
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {10.0F, -9.0F, 0.0F, 0.0F}, {(float)timing.length(), 0.0F, 0.0F, 0.0F}});
      this.poseKeyed(this.body, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {10.0F, -5.0F, 0.0F, 0.0F}, {(float)timing.length(), 0.0F, 0.0F, 0.0F}});
   }

   /** "King stands" — calm held pose while Lion's Heart is active. */
   @Unique
   private void applyLionStance(float weight, float ageInTicks) {
      float breath = (float)Math.sin((double)(ageInTicks * 0.08F)) * 1.5F;
      this.poseBlend(this.rightArm, weight, -12.0F, -4.0F, 10.0F);
      this.poseBlend(this.leftArm, weight, -12.0F, 4.0F, -10.0F);
      this.poseBlendAdditive(this.head, weight, -9.0F, 0.0F, 0.0F);
      this.poseBlendAdditive(this.body, weight, -5.0F + breath, 0.0F, 0.0F);
   }

   /** Mania channel: the extended arm slowly pulls the target in. */
   @Unique
   private void applyChannelPull(float weight, float ageInTicks) {
      float tremble = (float)Math.sin((double)(ageInTicks * 3.1F)) * 2.0F;
      this.poseBlend(this.rightArm, weight, -68.0F, -14.0F, 10.0F + tremble);
      this.poseBlendAdditive(this.body, weight, 8.0F, 0.0F, 0.0F);
      this.poseBlendAdditive(this.head, weight, 6.0F, 0.0F, 0.0F);
   }

   /** Evangelium ritual: both hands hold the book in front of the chest. */
   @Unique
   private void applyRitualHold(float weight, boolean offHand) {
      float side = offHand ? -1.0F : 1.0F;
      this.poseBlend(offHand ? this.leftArm : this.rightArm, weight, -68.0F, -24.0F * side, -8.0F * side);
      this.poseBlend(offHand ? this.rightArm : this.leftArm, weight, -62.0F, 30.0F * side, 8.0F * side);
      this.poseBlendAdditive(this.head, weight, 14.0F, 0.0F, 0.0F);
      this.poseBlendAdditive(this.body, weight, 6.0F, 0.0F, 0.0F);
   }

   /** Debris kick: leg winds up to the rise tick, slams the ground at the event. */
   @Unique
   private void applyDebrisKick(float elapsed) {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.DEBRIS_KICK);
      float riseTick = (float)RegulusPoseTiming.DEBRIS_RISE_TICK;
      float eventTick = (float)timing.eventTick();
      float endTick = (float)timing.length();
      this.poseKeyed(
         this.rightLeg,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {8.0F, 35.0F, 0.0F, -8.0F},
            {riseTick, 55.0F, 0.0F, -10.0F},
            {eventTick, -72.0F, 0.0F, 5.0F},
            {20.0F, -60.0F, 0.0F, 4.0F},
            {endTick, 0.0F, 0.0F, 0.0F}
         }
      );
      this.poseKeyed(this.leftLeg, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {eventTick, 14.0F, 0.0F, -4.0F}, {endTick, 0.0F, 0.0F, 0.0F}});
      this.poseKeyed(
         this.body,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, -6.0F, 0.0F, 0.0F}, {eventTick, 22.0F, 0.0F, 0.0F}, {20.0F, 18.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, 30.0F, 0.0F, -15.0F}, {eventTick, -55.0F, 0.0F, -10.0F}, {endTick, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(
         this.leftArm,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, 30.0F, 0.0F, 15.0F}, {eventTick, -55.0F, 0.0F, 10.0F}, {endTick, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {riseTick, -8.0F, 0.0F, 0.0F}, {eventTick, 12.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}});
   }

   /** Mania windup: arm extends forward; the palm clenches at the event tick. */
   @Unique
   private void applyManiaExtend(float elapsed) {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.MANIA);
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {8.0F, -48.0F, -10.0F, 0.0F},
            {17.0F, -88.0F, -5.0F, 0.0F},
            {(float)timing.eventTick(), -88.0F, -5.0F, 22.0F},
            {(float)timing.length(), 0.0F, 0.0F, 0.0F}
         }
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {12.0F, 6.0F, 0.0F, 0.0F}, {(float)timing.length(), 0.0F, 0.0F, 0.0F}});
   }

   /** Embrace: arm raises to the point, opens at the event, lowers by recover. */
   @Unique
   private void applyEmbraceRaise(float elapsed) {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.GREEDS_EMBRACE);
      float lockTick = (float)RegulusPoseTiming.EMBRACE_LOCK_TICK;
      float eventTick = (float)timing.eventTick();
      float endTick = (float)timing.length();
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {lockTick, -168.0F, -5.0F, 8.0F},
            {eventTick, -168.0F, -5.0F, 24.0F},
            {30.0F, -42.0F, 0.0F, 5.0F},
            {endTick, 0.0F, 0.0F, 0.0F}
         }
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {lockTick, -14.0F, 0.0F, 0.0F}, {30.0F, -4.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}});
      this.poseKeyed(this.body, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {lockTick, -8.0F, 0.0F, 0.0F}, {30.0F, 2.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}});
   }

   /** Counter: lifted sky-high through the lift ticks, then the top-down slam at the event. */
   @Unique
   private void applyCounterSlam(float elapsed) {
      RegulusPoseTiming.Timing timing = RegulusPoseTiming.timing(HeroAction.COUNTER);
      float liftTick = (float)RegulusPoseTiming.COUNTER_LIFT_TICKS;
      float eventTick = (float)timing.eventTick();
      float endTick = (float)timing.length();
      float[][] armKeys = {
         {0.0F, 0.0F, 0.0F, 0.0F},
         {6.0F, -40.0F, 0.0F, 0.0F},
         {liftTick, -150.0F, 0.0F, 0.0F},
         {24.0F, -152.0F, 0.0F, 0.0F},
         {eventTick, 42.0F, 0.0F, 0.0F},
         {endTick, 0.0F, 0.0F, 0.0F}
      };
      this.poseKeyed(this.rightArm, elapsed, mirrorZ(armKeys, -1.0F));
      this.poseKeyed(this.leftArm, elapsed, mirrorZ(armKeys, 1.0F));
      this.poseKeyed(
         this.body,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {liftTick, -10.0F, 0.0F, 0.0F}, {24.0F, -10.0F, 0.0F, 0.0F}, {eventTick, 30.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {liftTick, -12.0F, 0.0F, 0.0F}, {eventTick, 15.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}});
      float[][] legKeys = {{0.0F, 0.0F, 0.0F, 0.0F}, {10.0F, -14.0F, 0.0F, 0.0F}, {eventTick, -18.0F, 0.0F, 0.0F}, {endTick, 0.0F, 0.0F, 0.0F}};
      this.poseKeyed(this.rightLeg, elapsed, legKeys);
      this.poseKeyed(this.leftLeg, elapsed, legKeys);
   }

   /** Counter arms are symmetric apart from the roll sign. */
   @Unique
   private float[][] mirrorZ(float[][] keys, float sign) {
      float[][] out = new float[keys.length][];
      for (int i = 0; i < keys.length; i++) {
         out[i] = new float[]{keys[i][0], keys[i][1], keys[i][2] * sign, keys[i][3] * sign};
      }

      return out;
   }
}
