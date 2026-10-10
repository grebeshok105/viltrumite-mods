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
 * Mania: coil, then the arm snaps forward into a grip (overshoot on the reach,
 * clench on MANIA_WINDUP_TICKS); while the channel is open the arm stays out,
 * fist clenched, torso leaning back and pulling the target in. Timeline after
 * PunchModelMixin; channel hold blended like GrabModelMixin.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1182
)
public abstract class RegulusManiaModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] REACH_MAIN_ARM = {
      under(0),
      key(6, 22.0F, 10.0F, 18.0F, 0.0F, 0.0F, 1.0F),
      key(10, 28.0F, 12.0F, 20.0F, 0.0F, 0.0F, 1.2F),
      key(12, -70.0F, -6.0F, 0.0F, 0.0F, 0.0F, -1.6F),
      strike(13, -82.0F, -6.0F, 0.0F, 0.0F, 0.0F, -2.0F),
      key(16, -88.0F, -5.0F, 2.0F, 0.0F, 0.0F, -2.0F),
      key(19, -84.0F, -5.0F, -6.0F, 0.0F, 0.0F, -1.6F),
      under(25)
   };
   @Unique
   private static final float[][] REACH_OFF_ARM = {under(0), key(10, -18.0F, 0.0F, -10.0F), key(13, 24.0F, 0.0F, -14.0F), key(19, 16.0F, 0.0F, -10.0F), under(25)};
   @Unique
   private static final float[][] REACH_BODY = {under(0), key(10, -4.0F, 18.0F, 0.0F), key(13, 12.0F, -14.0F, 0.0F), key(19, 4.0F, -6.0F, 0.0F), under(25)};
   @Unique
   private static final float[][] REACH_MAIN_LEG = {under(0), key(13, 14.0F, 0.0F, 2.0F), key(19, 8.0F, 0.0F, 1.0F), under(25)};
   @Unique
   private static final float[][] REACH_OFF_LEG = {under(0), key(13, -18.0F, 0.0F, -2.0F), key(19, -10.0F, 0.0F, -1.0F), under(25)};
   @Unique
   private static final float[][] CHANNEL_HEAD = {key(0, 4.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] CHANNEL_BODY = {key(0, -10.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] CHANNEL_MAIN_ARM = {key(0, -80.0F, -6.0F, -4.0F, 0.0F, 0.0F, -1.2F)};
   @Unique
   private static final float[][] CHANNEL_OFF_ARM = {key(0, 14.0F, 0.0F, -12.0F)};
   @Unique
   private static final float[][] CHANNEL_MAIN_LEG = {key(0, 8.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] CHANNEL_OFF_LEG = {key(0, -10.0F, 0.0F, 0.0F)};

   public RegulusManiaModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusManiaSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      float channel = RegulusAnimationManager.weight(entity, RegulusAnimationManager.Layer.MANIA_CHANNEL, snapshot.controlTargetId() >= 0);
      float cast = RegulusAnimationManager.castWeight(entity, HeroAction.MANIA);
      if (channel <= 0.001F && cast <= 0.001F) {
         return;
      }
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
      PoseRig rig = RegulusPoser.rig(model, entity, this.cloak);
      if (channel > 0.001F) {
         float pull = (0.5F + 0.5F * (float)Math.sin(ageInTicks * 0.35F)) * channel;
         rig.head(CHANNEL_HEAD, 0.0F, channel);
         rig.body(CHANNEL_BODY, 0.0F, channel, false);
         rig.mainArm(CHANNEL_MAIN_ARM, 0.0F, channel, false);
         rig.offArm(CHANNEL_OFF_ARM, 0.0F, channel, false);
         rig.mainLeg(CHANNEL_MAIN_LEG, 0.0F, channel, true);
         rig.offLeg(CHANNEL_OFF_LEG, 0.0F, channel, true);
         rig.add(rig.mainArm, 10.0F * pull, 0.0F, 0.0F);
         rig.mainArm.z += 1.0F * pull;
         rig.add(model.body, -3.0F * pull, 0.0F, 0.0F);
      }
      if (cast > 0.001F) {
         float elapsed = RegulusAnimationManager.castTime(entity, Minecraft.getInstance().getFrameTime());
         rig.body(REACH_BODY, elapsed, cast, false);
         rig.mainArm(REACH_MAIN_ARM, elapsed, cast, false);
         rig.offArm(REACH_OFF_ARM, elapsed, cast, false);
         rig.mainLeg(REACH_MAIN_LEG, elapsed, cast, false);
         rig.offLeg(REACH_OFF_LEG, elapsed, cast, false);
      }
      rig.finish();
   }
}
