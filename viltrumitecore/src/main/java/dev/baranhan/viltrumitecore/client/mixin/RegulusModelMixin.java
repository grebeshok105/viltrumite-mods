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
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
   @Shadow
   @Final
   private ModelPart cloak;

   public RegulusModelMixin(ModelPart root) {
      super(root);
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
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      boolean isLocalFirstPerson = livingEntity == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
      if (isLocalFirstPerson && !ShaderCompat.isShadowPass()) {
         return;
      }

      float partialTick = minecraft.getFrameTime();
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
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
         this.applyRitualHold(ritualWeight);
      }

      HeroAction action = HeroAction.byId(snapshot.actionId());
      if (action != null) {
         float elapsed = (float)snapshot.actionElapsed() + partialTick;
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

      model.hat.copyFrom(this.head);
      model.jacket.copyFrom(this.body);
      model.rightSleeve.copyFrom(this.rightArm);
      model.leftSleeve.copyFrom(this.leftArm);
      model.rightPants.copyFrom(this.rightLeg);
      model.leftPants.copyFrom(this.leftLeg);
      if (this.cloak != null) {
         this.cloak.copyFrom(this.body);
      }
   }

   /** Keyframe lerp: rows are {tick, xDeg, yDeg, zDeg}, first row is reached from the part's vanilla pose. */
   @Unique
   private void poseKeyed(ModelPart part, float elapsed, float[][] keys) {
      float firstTick = keys[0][0];
      if (elapsed < firstTick) {
         return;
      }

      int last = keys.length - 1;
      if (elapsed >= keys[last][0]) {
         part.xRot = (float)Math.toRadians((double)keys[last][1]);
         part.yRot = (float)Math.toRadians((double)keys[last][2]);
         part.zRot = (float)Math.toRadians((double)keys[last][3]);
         return;
      }

      for (int i = 0; i < last; i++) {
         float t0 = keys[i][0];
         float t1 = keys[i + 1][0];
         if (elapsed >= t0 && elapsed < t1) {
            float localT = (elapsed - t0) / (t1 - t0);
            float fromX;
            float fromY;
            float fromZ;
            if (i == 0) {
               fromX = part.xRot;
               fromY = part.yRot;
               fromZ = part.zRot;
            } else {
               fromX = (float)Math.toRadians((double)keys[i][1]);
               fromY = (float)Math.toRadians((double)keys[i][2]);
               fromZ = (float)Math.toRadians((double)keys[i][3]);
            }

            part.xRot = Mth.lerp(localT, fromX, (float)Math.toRadians((double)keys[i + 1][1]));
            part.yRot = Mth.lerp(localT, fromY, (float)Math.toRadians((double)keys[i + 1][2]));
            part.zRot = Mth.lerp(localT, fromZ, (float)Math.toRadians((double)keys[i + 1][3]));
            return;
         }
      }
   }

   /** Weighted blend toward a pose — used by the continuous states. */
   @Unique
   private void poseBlend(ModelPart part, float weight, float xDeg, float yDeg, float zDeg) {
      part.xRot = Mth.lerp(weight, part.xRot, (float)Math.toRadians((double)xDeg));
      part.yRot = Mth.lerp(weight, part.yRot, (float)Math.toRadians((double)yDeg));
      part.zRot = Mth.lerp(weight, part.zRot, (float)Math.toRadians((double)zDeg));
   }

   /** Lion windup: right hand pressed to the chest, fist clench at the event tick. */
   @Unique
   private void applyLionWindup(float elapsed) {
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {7.0F, -62.0F, -34.0F, 8.0F},
            {12.0F, -74.0F, -38.0F, 12.0F},
            {14.0F, -72.0F, -36.0F, 30.0F},
            {15.0F, -70.0F, -35.0F, 18.0F}
         }
      );
      this.poseKeyed(this.leftArm, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, -18.0F, 10.0F, -8.0F}, {15.0F, -14.0F, 8.0F, -6.0F}});
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {10.0F, -9.0F, 0.0F, 0.0F}, {15.0F, -9.0F, 0.0F, 0.0F}});
      this.poseKeyed(this.body, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {10.0F, -5.0F, 0.0F, 0.0F}, {15.0F, -5.0F, 0.0F, 0.0F}});
   }

   /** "King stands" — calm held pose while Lion's Heart is active. */
   @Unique
   private void applyLionStance(float weight, float ageInTicks) {
      float breath = (float)Math.sin((double)(ageInTicks * 0.08F)) * 1.5F;
      this.poseBlend(this.rightArm, weight, -12.0F, -4.0F, 10.0F);
      this.poseBlend(this.leftArm, weight, -12.0F, 4.0F, -10.0F);
      this.poseBlend(this.head, weight, -9.0F, 0.0F, 0.0F);
      this.poseBlend(this.body, weight, -5.0F + breath, 0.0F, 0.0F);
   }

   /** Mania channel: the extended arm slowly pulls the target in. */
   @Unique
   private void applyChannelPull(float weight, float ageInTicks) {
      float tremble = (float)Math.sin((double)(ageInTicks * 3.1F)) * 2.0F;
      this.poseBlend(this.rightArm, weight, -68.0F, -14.0F, 10.0F + tremble);
      this.poseBlend(this.body, weight, 8.0F, 0.0F, 0.0F);
      this.poseBlend(this.head, weight, 6.0F, 0.0F, 0.0F);
   }

   /** Evangelium ritual: both hands hold the book in front of the chest. */
   @Unique
   private void applyRitualHold(float weight) {
      this.poseBlend(this.rightArm, weight, -62.0F, -30.0F, -8.0F);
      this.poseBlend(this.leftArm, weight, -62.0F, 30.0F, 8.0F);
      this.poseBlend(this.head, weight, 14.0F, 0.0F, 0.0F);
      this.poseBlend(this.body, weight, 6.0F, 0.0F, 0.0F);
   }

   /** Debris kick: leg winds up 0-11t, slams the ground at the 14t event. */
   @Unique
   private void applyDebrisKick(float elapsed) {
      this.poseKeyed(
         this.rightLeg,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {8.0F, 35.0F, 0.0F, -8.0F},
            {11.0F, 55.0F, 0.0F, -10.0F},
            {14.0F, -72.0F, 0.0F, 5.0F},
            {20.0F, -60.0F, 0.0F, 4.0F},
            {44.0F, 0.0F, 0.0F, 0.0F}
         }
      );
      this.poseKeyed(this.leftLeg, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {14.0F, 14.0F, 0.0F, -4.0F}, {44.0F, 0.0F, 0.0F, 0.0F}});
      this.poseKeyed(
         this.body,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, -6.0F, 0.0F, 0.0F}, {14.0F, 22.0F, 0.0F, 0.0F}, {20.0F, 18.0F, 0.0F, 0.0F}, {44.0F, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, 30.0F, 0.0F, -15.0F}, {14.0F, -55.0F, 0.0F, -10.0F}, {44.0F, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(
         this.leftArm,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {8.0F, 30.0F, 0.0F, 15.0F}, {14.0F, -55.0F, 0.0F, 10.0F}, {44.0F, 0.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {11.0F, -8.0F, 0.0F, 0.0F}, {14.0F, 12.0F, 0.0F, 0.0F}, {44.0F, 0.0F, 0.0F, 0.0F}});
   }

   /** Mania windup: arm extends forward; the palm clenches at the event tick. */
   @Unique
   private void applyManiaExtend(float elapsed) {
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {8.0F, -48.0F, -10.0F, 0.0F},
            {17.0F, -88.0F, -5.0F, 0.0F},
            {19.0F, -88.0F, -5.0F, 22.0F},
            {20.0F, -85.0F, -8.0F, 14.0F}
         }
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {12.0F, 6.0F, 0.0F, 0.0F}, {20.0F, 6.0F, 0.0F, 0.0F}});
   }

   /** Embrace: arm raises to the point, opens at the event, lowers by recover. */
   @Unique
   private void applyEmbraceRaise(float elapsed) {
      this.poseKeyed(
         this.rightArm,
         elapsed,
         new float[][]{
            {0.0F, 0.0F, 0.0F, 0.0F},
            {13.0F, -168.0F, -5.0F, 8.0F},
            {18.0F, -168.0F, -5.0F, 24.0F},
            {30.0F, -42.0F, 0.0F, 5.0F},
            {33.0F, 0.0F, 0.0F, 0.0F}
         }
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {13.0F, -14.0F, 0.0F, 0.0F}, {30.0F, -4.0F, 0.0F, 0.0F}, {33.0F, 0.0F, 0.0F, 0.0F}});
      this.poseKeyed(this.body, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {13.0F, -8.0F, 0.0F, 0.0F}, {30.0F, 2.0F, 0.0F, 0.0F}, {33.0F, 0.0F, 0.0F, 0.0F}});
   }

   /** Counter: lifted sky-high 0-20t, then the top-down slam at the 27t event. */
   @Unique
   private void applyCounterSlam(float elapsed) {
      float[][] armKeys = {
         {0.0F, 0.0F, 0.0F, 0.0F},
         {6.0F, -40.0F, 0.0F, 0.0F},
         {20.0F, -150.0F, 0.0F, 0.0F},
         {24.0F, -152.0F, 0.0F, 0.0F},
         {27.0F, 42.0F, 0.0F, 0.0F},
         {28.0F, 30.0F, 0.0F, 0.0F}
      };
      this.poseKeyed(this.rightArm, elapsed, mirrorZ(armKeys, -1.0F));
      this.poseKeyed(this.leftArm, elapsed, mirrorZ(armKeys, 1.0F));
      this.poseKeyed(
         this.body,
         elapsed,
         new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {20.0F, -10.0F, 0.0F, 0.0F}, {24.0F, -10.0F, 0.0F, 0.0F}, {27.0F, 30.0F, 0.0F, 0.0F}, {28.0F, 24.0F, 0.0F, 0.0F}}
      );
      this.poseKeyed(this.head, elapsed, new float[][]{{0.0F, 0.0F, 0.0F, 0.0F}, {20.0F, -12.0F, 0.0F, 0.0F}, {27.0F, 15.0F, 0.0F, 0.0F}, {28.0F, 12.0F, 0.0F, 0.0F}});
      float[][] legKeys = {{0.0F, 0.0F, 0.0F, 0.0F}, {10.0F, -14.0F, 0.0F, 0.0F}, {27.0F, -18.0F, 0.0F, 0.0F}, {28.0F, -14.0F, 0.0F, 0.0F}};
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
