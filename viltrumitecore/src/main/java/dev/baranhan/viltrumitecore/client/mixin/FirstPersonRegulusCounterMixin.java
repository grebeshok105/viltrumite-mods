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
 * Counter first person: both hands rise overhead with the lift, then slam
 * down together after the teleport.
 * Same hook and transform order as FirstPersonPunchMixin (translate, then
 * X/Y/Z rotation, mirrored by the hand side; positive x is outward).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1534
)
public abstract class FirstPersonRegulusCounterMixin {
   @Unique
   private static final float[][] SLAM = {
      under(0),
      key(6, 16.0F, 0.0F, 0.0F, -0.02F, 0.12F, -0.04F),
      key(16, 54.0F, 0.0F, -6.0F, -0.06F, 0.42F, -0.06F),
      key(20, 60.0F, 0.0F, -8.0F, -0.08F, 0.46F, -0.04F),
      key(25, 62.0F, 0.0F, -8.0F, -0.08F, 0.48F, -0.04F),
      key(26, 20.0F, 0.0F, -8.0F, -0.10F, 0.20F, -0.26F),
      strike(27, -34.0F, 0.0F, -6.0F, -0.10F, -0.20F, -0.30F),
      key(30, -30.0F, 0.0F, -6.0F, -0.08F, -0.17F, -0.26F),
      under(36)
   };

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusCounterFirstPerson(
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
      float cast = RegulusAnimationManager.firstPersonCastWeight(player, HeroAction.COUNTER, mainHand);
      if (cast > 0.001F) {
         RegulusPoser.firstPerson(poseStack, SLAM, RegulusAnimationManager.castTime(player, partialTicks), cast, side);
      }
   }
}
