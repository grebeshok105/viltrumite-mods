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
 * Evangelium ritual hold: the book open in one hand, the other hand hovering
 * over the pages, gentle sway. Two weights (book in main / off hand) so the
 * pose mirrors smoothly with the hand that holds the book.
 */
@Mixin(
   value = {PlayerModel.class},
   priority = 1178
)
public abstract class RegulusRitualModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Shadow
   @Final
   private ModelPart cloak;
   @Unique
   private static final float[][] RITUAL_HEAD = {key(0, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F)};
   @Unique
   private static final float[][] RITUAL_BODY = {key(0, 6.0F, 0.0F, 0.0F)};
   @Unique
   private static final float[][] RITUAL_BOOK_ARM = {key(0, -52.0F, -24.0F, 6.0F, 0.6F, 0.0F, -0.8F)};
   @Unique
   private static final float[][] RITUAL_FREE_ARM = {key(0, -78.0F, 34.0F, -4.0F, -0.8F, -0.6F, -1.2F)};

   public RegulusRitualModelMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusRitualSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      HeroPublicSnapshot snapshot = RegulusPoser.thirdPerson(entity);
      if (snapshot == null) {
         return;
      }
      boolean ritual = snapshot.ritualTicks() >= 0;
      boolean mainHandBook = RegulusAnimationManager.bookHand(entity) == net.minecraft.world.InteractionHand.MAIN_HAND;
      float mainWeight = RegulusAnimationManager.weight(entity, RegulusAnimationManager.Layer.RITUAL_MAIN_HAND, ritual && mainHandBook);
      float offWeight = RegulusAnimationManager.weight(entity, RegulusAnimationManager.Layer.RITUAL_OFF_HAND, ritual && !mainHandBook);
      PlayerModel<?> model = (PlayerModel<?>)(Object)this;
      float mainSide = entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? -1.0F : 1.0F;
      this.regulusApplyRitual(model, entity, mainWeight, mainSide, ageInTicks);
      this.regulusApplyRitual(model, entity, offWeight, -mainSide, ageInTicks);
   }

   @Unique
   private void regulusApplyRitual(PlayerModel<?> model, T entity, float weight, float bookSide, float ageInTicks) {
      if (weight <= 0.001F) {
         return;
      }
      RegulusPoser.Rig rig = RegulusPoser.rig(model, entity, bookSide, this.cloak);
      rig.head(RITUAL_HEAD, 0.0F, weight);
      rig.body(RITUAL_BODY, 0.0F, weight, false);
      rig.mainArm(RITUAL_BOOK_ARM, 0.0F, weight, false);
      rig.offArm(RITUAL_FREE_ARM, 0.0F, weight, false);
      float sway = (float)Math.sin(ageInTicks * 0.1F) * weight;
      float hover = (float)Math.sin(ageInTicks * 0.23F) * weight;
      rig.add(model.body, 0.0F, 3.0F * sway, 2.0F * sway);
      rig.add(rig.mainArm, 1.5F * sway, 0.0F, 0.0F);
      rig.add(rig.offArm, 5.0F * hover, 0.0F, 3.0F * hover);
      rig.finish();
   }
}
