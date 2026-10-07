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
 * Evangelium ritual first person: the book hand brings the open book to the
 * middle, the free hand hovers over the pages.
 * Same hook and transform order as FirstPersonPunchMixin (translate, then
 * X/Y/Z rotation, mirrored by the hand side; positive x is outward).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1535
)
public abstract class FirstPersonRegulusRitualMixin {
   @Unique
   private static final float[][] BOOK_HAND = {
      key(0, 10.0F, 24.0F, -6.0F, -0.16F, 0.10F, 0.02F)
   };
   @Unique
   private static final float[][] FREE_HAND = {
      key(0, -18.0F, 30.0F, 20.0F, -0.30F, 0.22F, -0.12F)
   };

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusRitualFirstPerson(
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
      boolean ritual = snapshot.ritualTicks() >= 0;
      boolean bookHand = RegulusAnimationManager.bookHand(player) == hand;
      float bookWeight = RegulusAnimationManager.firstPersonWeight(player, RegulusAnimationManager.Layer.RITUAL_MAIN_HAND, ritual && bookHand, mainHand);
      float freeWeight = RegulusAnimationManager.firstPersonWeight(player, RegulusAnimationManager.Layer.RITUAL_OFF_HAND, ritual && !bookHand, mainHand);
      float hover = (float)Math.sin(((float)player.tickCount + partialTicks) * 0.23F);
      if (bookWeight > 0.001F) {
         RegulusPoser.firstPerson(poseStack, BOOK_HAND, 0.0F, bookWeight, side);
      }
      if (freeWeight > 0.001F) {
         RegulusPoser.firstPerson(poseStack, FREE_HAND, 0.0F, freeWeight, side);
         poseStack.translate(0.0F, 0.015F * hover * freeWeight, 0.0F);
      }
   }
}
