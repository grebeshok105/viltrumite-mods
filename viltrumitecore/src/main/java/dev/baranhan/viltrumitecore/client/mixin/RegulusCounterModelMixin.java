package dev.baranhan.viltrumitecore.client.mixin;

import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.key;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.strike;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.under;

import dev.baranhan.viltrumitecore.client.anim.pose.PoseRig;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoser;
import dev.baranhan.viltrumitecore.client.render.animation.RegulusAnimationManager;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counter: tucked lift with both arms gathered overhead through
 * COUNTER_LIFT_TICKS, then the two-handed overhead slam that lands with
 * overshoot after COUNTER_SLAM_TICKS. Body tilt in RegulusCounterRendererCoreMixin.
 * Two-arm timeline after BarrageModelMixin / ThunderclapModelMixin.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1188
)
public abstract class RegulusCounterModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] SLAM_MAIN_ARM = {
      under(0),
      key(6, -70.0F, 0.0F, 24.0F),
      key(16, -165.0F, 0.0F, 10.0F, 0.0F, -0.6F, 0.0F),
      key(20, -172.0F, 0.0F, 6.0F, 0.0F, -0.8F, 0.0F),
      key(25, -176.0F, 0.0F, 4.0F, 0.0F, -0.8F, 0.0F),
      key(26, -90.0F, 0.0F, 4.0F, 0.0F, 0.0F, -1.2F),
      strike(27, -20.0F, 0.0F, 6.0F, 0.0F, 0.0F, -1.0F),
      key(30, -24.0F, 0.0F, 6.0F, 0.0F, 0.0F, -0.8F),
      under(36)
   };
   @Unique
   private static final float[][] SLAM_OFF_ARM = {
      under(0),
      key(6, -70.0F, 0.0F, -24.0F),
      key(16, -165.0F, 0.0F, -10.0F, 0.0F, -0.6F, 0.0F),
      key(20, -172.0F, 0.0F, -6.0F, 0.0F, -0.8F, 0.0F),
      key(25, -176.0F, 0.0F, -4.0F, 0.0F, -0.8F, 0.0F),
      key(26, -90.0F, 0.0F, -4.0F, 0.0F, 0.0F, -1.2F),
      strike(27, -20.0F, 0.0F, -6.0F, 0.0F, 0.0F, -1.0F),
      key(30, -24.0F, 0.0F, -6.0F, 0.0F, 0.0F, -0.8F),
      under(36)
   };
   @Unique
   private static final float[][] SLAM_BODY = {
      under(0), key(10, 6.0F, 0.0F, 0.0F), key(20, -12.0F, 0.0F, 0.0F), key(25, -14.0F, 0.0F, 0.0F), key(27, 34.0F, 0.0F, 0.0F), key(31, 30.0F, 0.0F, 0.0F), under(36)
   };
   @Unique
   private static final float[][] SLAM_HEAD = {
      under(0), key(20, -16.0F, 0.0F, 0.0F), key(25, -18.0F, 0.0F, 0.0F), key(27, 6.0F, 0.0F, 0.0F), key(31, 4.0F, 0.0F, 0.0F), under(36)
   };
   @Unique
   private static final float[][] SLAM_MAIN_LEG = {
      under(0), key(8, -30.0F, 0.0F, 4.0F, 0.0F, -0.8F, -0.6F), key(20, -42.0F, 0.0F, 6.0F, 0.0F, -1.2F, -0.8F), key(25, -30.0F, 0.0F, 4.0F, 0.0F, -0.8F, -0.6F),
      key(27, 12.0F, 0.0F, 2.0F), key(31, 10.0F, 0.0F, 2.0F), under(36)
   };
   @Unique
   private static final float[][] SLAM_OFF_LEG = {
      under(0), key(8, -30.0F, 0.0F, -4.0F, 0.0F, -0.8F, -0.6F), key(20, -42.0F, 0.0F, -6.0F, 0.0F, -1.2F, -0.8F), key(25, -30.0F, 0.0F, -4.0F, 0.0F, -0.8F, -0.6F),
      key(27, 12.0F, 0.0F, -2.0F), key(31, 10.0F, 0.0F, -2.0F), under(36)
   };

   public RegulusCounterModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusCounterSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      float cast = RegulusAnimationManager.castWeight(entity, HeroAction.COUNTER);
      if (cast <= 0.001F) {
         return;
      }
      float elapsed = RegulusAnimationManager.castTime(entity, Minecraft.getInstance().getFrameTime());
      PoseRig rig = RegulusPoser.rig((PlayerModel<?>)(Object)this, entity, this.cloak);
      rig.head(SLAM_HEAD, elapsed, cast);
      rig.body(SLAM_BODY, elapsed, cast, false);
      rig.mainArm(SLAM_MAIN_ARM, elapsed, cast, false);
      rig.offArm(SLAM_OFF_ARM, elapsed, cast, false);
      rig.mainLeg(SLAM_MAIN_LEG, elapsed, cast, false);
      rig.offLeg(SLAM_OFF_LEG, elapsed, cast, false);
      rig.finish();
   }
}
