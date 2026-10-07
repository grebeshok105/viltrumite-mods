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
 * Greed's Embrace first person: the hand rises palm-up into view, peaks on
 * the dome tick, holds and lowers by the recover tick.
 * Same hook and transform order as FirstPersonPunchMixin (translate, then
 * X/Y/Z rotation, mirrored by the hand side; positive x is outward).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1533
)
public abstract class FirstPersonRegulusEmbraceMixin {
   @Unique
   private static final float[][] RAISE = {
      under(0),
      key(7, 14.0F, 4.0F, 0.0F, -0.03F, 0.12F, -0.06F),
      key(13, 36.0F, 6.0F, -6.0F, -0.05F, 0.30F, -0.14F),
      strike(18, 40.0F, 6.0F, -10.0F, -0.05F, 0.33F, -0.15F),
      key(21, 42.0F, 6.0F, -10.0F, -0.05F, 0.34F, -0.15F),
      key(28, 40.0F, 6.0F, -10.0F, -0.05F, 0.33F, -0.15F),
      key(32, 10.0F, 2.0F, -2.0F, -0.01F, 0.08F, -0.04F),
      under(37)
   };

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusEmbraceFirstPerson(
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
      float cast = RegulusAnimationManager.firstPersonCastWeight(player, HeroAction.GREEDS_EMBRACE, true);
      if (cast > 0.001F) {
         RegulusPoser.firstPerson(poseStack, RAISE, RegulusAnimationManager.castTime(player, partialTicks), cast, side);
      }
   }
}
