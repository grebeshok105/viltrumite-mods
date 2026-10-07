package dev.baranhan.viltrumitecore.client.mixin;

import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.key;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.strike;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.under;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoser;
import dev.baranhan.viltrumitecore.client.render.animation.RegulusAnimationManager;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lion's Heart first person: fist to the heart on the windup, relaxed king
 * stance while active, hand back on the chest while overheated.
 * Same hook and transform order as FirstPersonPunchMixin (translate, then
 * X/Y/Z rotation, mirrored by the hand side; positive x is outward).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1530
)
public abstract class FirstPersonRegulusLionMixin {
   @Unique
   private static final float[][] WINDUP = {
      under(0),
      key(6, 4.0F, 18.0F, -8.0F, -0.12F, 0.05F, 0.06F),
      key(11, 10.0F, 32.0F, -14.0F, -0.28F, 0.12F, 0.12F),
      key(13, 11.0F, 34.0F, -16.0F, -0.31F, 0.13F, 0.14F),
      key(14, 12.0F, 38.0F, -24.0F, -0.33F, 0.12F, 0.17F),
      key(17, 10.0F, 34.0F, -16.0F, -0.30F, 0.12F, 0.14F),
      under(22)
   };
   @Unique
   private static final float[][] STANCE = {
      key(0, -4.0F, 4.0F, 3.0F, 0.04F, -0.06F, 0.02F)
   };
   @Unique
   private static final float[][] OVERHEAT = {
      key(0, 8.0F, 30.0F, -12.0F, -0.26F, 0.08F, 0.12F)
   };

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusLionFirstPerson(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      HeroPublicSnapshot snapshot = RegulusPoser.regulus(player);
      if (snapshot == null) {
         return;
      }
      boolean mainHand = hand == InteractionHand.MAIN_HAND;
      float side = RegulusPoser.handSide(player, hand);
      if (!mainHand) {
         return;
      }
      float stance = RegulusAnimationManager.firstPersonWeight(player, RegulusAnimationManager.Layer.LION_STANCE, snapshot.lionActive(), true);
      float overheat = RegulusAnimationManager.firstPersonWeight(
         player, RegulusAnimationManager.Layer.LION_OVERHEAT, snapshot.lionActive() && snapshot.lionOverheat(), true
      );
      float cast = RegulusAnimationManager.firstPersonCastWeight(player, HeroAction.LIONS_HEART, true);
      float age = (float)player.tickCount + partialTicks;
      if (stance > 0.001F) {
         RegulusPoser.firstPerson(poseStack, STANCE, 0.0F, stance, side);
         poseStack.translate(0.0F, 0.01F * (float)Math.sin(age * 0.08F) * stance, 0.0F);
      }
      if (overheat > 0.001F) {
         RegulusPoser.firstPerson(poseStack, OVERHEAT, 0.0F, overheat, side);
         poseStack.translate(0.0F, 0.012F * (float)Math.sin(age * 0.45F) * overheat, 0.0F);
      }
      if (cast > 0.001F) {
         RegulusPoser.firstPerson(poseStack, WINDUP, RegulusAnimationManager.castTime(player, partialTicks), cast, side);
      }
   }
}
