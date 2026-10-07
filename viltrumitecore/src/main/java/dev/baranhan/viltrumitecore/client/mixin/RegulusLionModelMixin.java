package dev.baranhan.viltrumitecore.client.mixin;

import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.key;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.strike;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.under;

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
 * Lion's Heart: windup (fist to the heart, torso leans back, clench on
 * LION_WINDUP_TICKS), then the calm king stance with breathing while the
 * window is open, and the hunched hand-on-chest overheat stance.
 * Timeline after PunchModelMixin, holds after GrabModelMixin's weight blend.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1180
)
public abstract class RegulusLionModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] WINDUP_MAIN_ARM = {
      under(0),
      key(5, -35.0F, -18.0F, 10.0F, 0.0F, 0.0F, -0.4F),
      key(10, -66.0F, -44.0F, 10.0F, 0.8F, 0.3F, -1.0F),
      key(13, -70.0F, -46.0F, 12.0F, 1.0F, 0.4F, -1.2F),
      key(14, -76.0F, -50.0F, 4.0F, 1.2F, 0.5F, -1.6F),
      key(17, -70.0F, -46.0F, 10.0F, 1.0F, 0.4F, -1.2F),
      under(22)
   };
   @Unique
   private static final float[][] WINDUP_OFF_ARM = {under(0), key(8, 10.0F, 0.0F, -14.0F), key(14, 14.0F, 0.0F, -18.0F), under(22)};
   @Unique
   private static final float[][] WINDUP_BODY = {under(0), key(8, -6.0F, 0.0F, 0.0F), key(14, -10.0F, 0.0F, 0.0F), key(17, -7.0F, 0.0F, 0.0F), under(22)};
   @Unique
   private static final float[][] WINDUP_HEAD = {under(0), key(10, -8.0F, 0.0F, 0.0F), key(14, -12.0F, 0.0F, 0.0F), under(22)};
   @Unique
   private static final float[][] STANCE_HEAD = {key(0, -6.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] STANCE_BODY = {key(0, -4.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] STANCE_MAIN_ARM = {key(0, 6.0F, 0.0F, 7.0F)};
   @Unique
   private static final float[][] STANCE_OFF_ARM = {key(0, 6.0F, 0.0F, -7.0F)};
   @Unique
   private static final float[][] OVERHEAT_HEAD = {key(0, 10.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] OVERHEAT_BODY = {key(0, 20.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] OVERHEAT_MAIN_ARM = {key(0, -60.0F, -42.0F, 8.0F, 0.8F, 0.6F, -0.8F)};
   @Unique
   private static final float[][] OVERHEAT_OFF_ARM = {key(0, -22.0F, 6.0F, -6.0F)};

   public RegulusLionModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusLionSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      float stance = RegulusAnimationManager.weight(entity, RegulusAnimationManager.Layer.LION_STANCE, snapshot.lionActive());
      float overheat = RegulusAnimationManager.weight(entity, RegulusAnimationManager.Layer.LION_OVERHEAT, snapshot.lionActive() && snapshot.lionOverheat());
      float cast = RegulusAnimationManager.castWeight(entity, HeroAction.LIONS_HEART);
      if (stance <= 0.001F && overheat <= 0.001F && cast <= 0.001F) {
         return;
      }
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
      RegulusPoser.Rig rig = RegulusPoser.rig(model, entity, this.cloak);
      if (stance > 0.001F) {
         float breath = (float)Math.sin(ageInTicks * 0.08F) * stance;
         rig.head(STANCE_HEAD, 0.0F, stance);
         rig.body(STANCE_BODY, 0.0F, stance, true);
         rig.mainArm(STANCE_MAIN_ARM, 0.0F, stance, true);
         rig.offArm(STANCE_OFF_ARM, 0.0F, stance, true);
         rig.add(model.body, -1.2F * breath, 0.0F, 0.0F);
         rig.add(rig.mainArm, 0.0F, 0.0F, 1.5F * breath);
         rig.add(rig.offArm, 0.0F, 0.0F, -1.5F * breath);
      }
      if (overheat > 0.001F) {
         float pant = (float)Math.sin(ageInTicks * 0.45F) * overheat;
         rig.head(OVERHEAT_HEAD, 0.0F, overheat);
         rig.body(OVERHEAT_BODY, 0.0F, overheat, false);
         rig.mainArm(OVERHEAT_MAIN_ARM, 0.0F, overheat, false);
         rig.offArm(OVERHEAT_OFF_ARM, 0.0F, overheat, false);
         rig.add(model.body, 2.5F * pant, 0.0F, 0.0F);
      }
      if (cast > 0.001F) {
         float elapsed = RegulusAnimationManager.castTime(entity, Minecraft.getInstance().getFrameTime());
         rig.head(WINDUP_HEAD, elapsed, cast);
         rig.body(WINDUP_BODY, elapsed, cast, false);
         rig.mainArm(WINDUP_MAIN_ARM, elapsed, cast, false);
         rig.offArm(WINDUP_OFF_ARM, elapsed, cast, false);
      }
      rig.finish();
   }
}
