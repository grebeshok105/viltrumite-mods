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
 * Regulus base layer + Madness stance. Lowest Regulus priority: restores the
 * pivots earlier Regulus frames moved (HEAD), resets head/body roll and yaw
 * like the Viltrumite mixins, then blends the hunched Madness stance in
 * additively so walking/arm swing keep running underneath.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1176
)
public abstract class RegulusMadnessModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] MADNESS_HEAD = {key(0, -14.0F, 0.0F, 0.0F, 0.0F, 0.6F, -0.4F)};
   @Unique
   private static final float[][] MADNESS_BODY = {key(0, 18.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] MADNESS_MAIN_ARM = {key(0, -16.0F, -4.0F, 10.0F, 0.0F, 0.4F, 0.0F)};
   @Unique
   private static final float[][] MADNESS_OFF_ARM = {key(0, -16.0F, 4.0F, -10.0F, 0.0F, 0.4F, 0.0F)};
   @Unique
   private static final float[][] MADNESS_LEGS = {key(0, -4.0F, 0.0F, 0.0F)};

   public RegulusMadnessModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("HEAD")}
   )
   private void regulusRestorePivots(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      RegulusPoser.restorePivots((PlayerModel<?>)(Object)this);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusMadnessSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
      RegulusPoser.beginFrame(model);
      float weight = RegulusAnimationManager.weight(entity, RegulusAnimationManager.Layer.MADNESS, snapshot.madness());
      if (weight <= 0.001F) {
         return;
      }
      RegulusPoser.Rig rig = RegulusPoser.rig(model, entity, this.cloak);
      rig.head(MADNESS_HEAD, 0.0F, weight);
      rig.body(MADNESS_BODY, 0.0F, weight, true);
      rig.mainArm(MADNESS_MAIN_ARM, 0.0F, weight, true);
      rig.offArm(MADNESS_OFF_ARM, 0.0F, weight, true);
      rig.mainLeg(MADNESS_LEGS, 0.0F, weight, true);
      rig.offLeg(MADNESS_LEGS, 0.0F, weight, true);
      float breath = (float)Math.sin(ageInTicks * 0.22F) * weight;
      float tremor = (float)Math.sin(ageInTicks * 1.7F) * weight;
      rig.add(model.body, 2.0F * breath, 0.0F, 0.0F);
      rig.add(rig.mainArm, 0.0F, 0.0F, 1.2F * tremor);
      rig.add(rig.offArm, 0.0F, 0.0F, -1.2F * tremor);
      rig.finish();
   }
}
