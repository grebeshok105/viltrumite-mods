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
 * Debris Kick first person: both arms swing out for balance while the leg
 * chambers and strikes; the camera shake on impact stays in the existing
 * Regulus screen layer.
 * Same hook and transform order as FirstPersonPunchMixin (translate, then
 * X/Y/Z rotation, mirrored by the hand side; positive x is outward).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1531
)
public abstract class FirstPersonRegulusKickMixin {
   @Unique
   private static final float[][] BALANCE = {
      under(0),
      key(11, -8.0F, 0.0F, -12.0F, 0.10F, -0.08F, 0.08F),
      key(14, -14.0F, -6.0F, -20.0F, 0.15F, -0.14F, 0.10F),
      key(24, -12.0F, -5.0F, -16.0F, 0.12F, -0.11F, 0.08F),
      key(34, -3.0F, 0.0F, -4.0F, 0.03F, -0.03F, 0.02F),
      under(44)
   };

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusKickFirstPerson(
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
      float cast = RegulusAnimationManager.firstPersonCastWeight(player, HeroAction.DEBRIS_KICK, mainHand);
      if (cast > 0.001F) {
         RegulusPoser.firstPerson(poseStack, BALANCE, RegulusAnimationManager.castTime(player, partialTicks), cast, side);
      }
   }
}
