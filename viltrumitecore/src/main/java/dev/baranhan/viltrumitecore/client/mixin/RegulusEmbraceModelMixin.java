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
 * Greed's Embrace: the arm rises palm-up through EMBRACE_LOCK_TICK, peaks with
 * overshoot on EMBRACE_APPEAR_TICK (the dome appears), holds, and lowers back
 * by EMBRACE_RECOVER_TICK. Same piecewise timeline as ChopModelMixin.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1184
)
public abstract class RegulusEmbraceModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] RAISE_MAIN_ARM = {
      under(0),
      key(7, -60.0F, -8.0F, 14.0F, 0.0F, 0.0F, -0.4F),
      key(13, -118.0F, -4.0F, 18.0F, 0.0F, -0.6F, -0.8F),
      strike(18, -122.0F, 0.0F, 18.0F, 0.0F, -0.6F, -0.8F),
      key(21, -132.0F, 0.0F, 18.0F, 0.0F, -0.6F, -0.8F),
      key(28, -130.0F, 0.0F, 18.0F, 0.0F, -0.6F, -0.8F),
      key(32, -40.0F, 0.0F, 10.0F, 0.0F, 0.0F, -0.2F),
      under(37)
   };
   @Unique
   private static final float[][] RAISE_OFF_ARM = {
      under(0), key(13, -22.0F, 0.0F, -16.0F), key(18, -26.0F, 0.0F, -22.0F), key(28, -24.0F, 0.0F, -20.0F), key(32, -8.0F, 0.0F, -6.0F), under(37)
   };
   @Unique
   private static final float[][] RAISE_BODY = {
      under(0), key(13, -6.0F, 6.0F, 0.0F), key(18, -9.0F, 8.0F, 0.0F), key(28, -8.0F, 6.0F, 0.0F), key(32, -2.0F, 0.0F, 0.0F), under(37)
   };
   @Unique
   private static final float[][] RAISE_HEAD = {
      under(0), key(13, -14.0F, 0.0F, 0.0F), key(18, -20.0F, 0.0F, 0.0F), key(28, -16.0F, 0.0F, 0.0F), key(32, -4.0F, 0.0F, 0.0F), under(37)
   };

   public RegulusEmbraceModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusEmbraceSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      float cast = RegulusAnimationManager.castWeight(entity, HeroAction.GREEDS_EMBRACE);
      if (cast <= 0.001F) {
         return;
      }
      float elapsed = RegulusAnimationManager.castTime(entity, Minecraft.getInstance().getFrameTime());
      RegulusPoser.Rig rig = RegulusPoser.rig((PlayerModel<?>)(Object)this, entity, this.cloak);
      rig.head(RAISE_HEAD, elapsed, cast);
      rig.body(RAISE_BODY, elapsed, cast, false);
      rig.mainArm(RAISE_MAIN_ARM, elapsed, cast, false);
      rig.offArm(RAISE_OFF_ARM, elapsed, cast, false);
      rig.finish();
   }
}
