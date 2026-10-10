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
 * Debris Kick: the kicking leg chambers back (torso leans back, arms out for
 * balance), strikes forward-down with overshoot exactly on DEBRIS_EVENT_TICK,
 * holds and recovers by DEBRIS_ANIM_TICKS. Whole-body lunge lives in
 * RegulusKickRendererCoreMixin. Timeline after PunchModelMixin, but driven by
 * the leg instead of the arm.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1186
)
public abstract class RegulusKickModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] KICK_MAIN_LEG = {
      under(0),
      key(5, 18.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.6F),
      key(11, 48.0F, 0.0F, 6.0F, 0.0F, -0.4F, 1.6F),
      key(13, -60.0F, 0.0F, -2.0F, 0.0F, -0.8F, -1.2F),
      strike(14, -68.0F, 0.0F, -4.0F, 0.0F, -0.9F, -1.8F),
      key(17, -74.0F, 0.0F, -4.0F, 0.0F, -1.0F, -2.0F),
      key(24, -66.0F, 0.0F, -3.0F, 0.0F, -0.8F, -1.6F),
      key(34, -22.0F, 0.0F, 0.0F, 0.0F, -0.2F, -0.4F),
      under(44)
   };
   @Unique
   private static final float[][] KICK_OFF_LEG = {
      under(0), key(11, -8.0F, 0.0F, -2.0F), key(14, 10.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.6F), key(24, 6.0F, 0.0F, -2.0F), under(44)
   };
   @Unique
   private static final float[][] KICK_BODY = {
      under(0), key(5, -3.0F, 4.0F, 0.0F), key(11, -8.0F, 8.0F, 0.0F), key(14, -18.0F, -8.0F, 0.0F), key(24, -14.0F, -6.0F, 0.0F), key(34, -4.0F, 0.0F, 0.0F), under(44)
   };
   @Unique
   private static final float[][] KICK_HEAD = {
      under(0), key(11, 6.0F, 0.0F, 0.0F), key(14, 14.0F, 0.0F, 0.0F), key(24, 10.0F, 0.0F, 0.0F), under(44)
   };
   @Unique
   private static final float[][] KICK_MAIN_ARM = {
      under(0), key(11, 20.0F, 0.0F, 40.0F), key(14, 35.0F, 0.0F, 55.0F), key(24, 25.0F, 0.0F, 45.0F), key(34, 8.0F, 0.0F, 15.0F), under(44)
   };
   @Unique
   private static final float[][] KICK_OFF_ARM = {
      under(0), key(11, -30.0F, 0.0F, -35.0F), key(14, -45.0F, 10.0F, -50.0F), key(24, -35.0F, 8.0F, -40.0F), key(34, -10.0F, 0.0F, -12.0F), under(44)
   };

   public RegulusKickModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusKickSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      float cast = RegulusAnimationManager.castWeight(entity, HeroAction.DEBRIS_KICK);
      if (cast <= 0.001F) {
         return;
      }
      float elapsed = RegulusAnimationManager.castTime(entity, Minecraft.getInstance().getFrameTime());
      PoseRig rig = RegulusPoser.rig((PlayerModel<?>)(Object)this, entity, this.cloak);
      rig.head(KICK_HEAD, elapsed, cast);
      rig.body(KICK_BODY, elapsed, cast, false);
      rig.mainLeg(KICK_MAIN_LEG, elapsed, cast, false);
      rig.offLeg(KICK_OFF_LEG, elapsed, cast, false);
      rig.mainArm(KICK_MAIN_ARM, elapsed, cast, false);
      rig.offArm(KICK_OFF_ARM, elapsed, cast, false);
      rig.finish();
   }
}
