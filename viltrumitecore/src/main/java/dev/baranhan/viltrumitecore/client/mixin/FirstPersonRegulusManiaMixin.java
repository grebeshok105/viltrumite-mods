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
 * Mania first person: coil, snap the hand forward into a grip (overshoot,
 * clench on the event tick), then hold it out pulling while the channel is open.
 * Same hook and transform order as FirstPersonPunchMixin (translate, then
 * X/Y/Z rotation, mirrored by the hand side; positive x is outward).
 */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1532
)
public abstract class FirstPersonRegulusManiaMixin {
   @Unique
   private static final float[][] REACH = {
      under(0),
      key(6, 8.0F, 0.0F, 0.0F, 0.05F, -0.08F, 0.16F),
      key(10, 10.0F, 0.0F, 0.0F, 0.06F, -0.09F, 0.18F),
      key(12, -26.0F, 8.0F, 8.0F, -0.10F, 0.13F, -0.38F),
      strike(13, -28.0F, 8.0F, 8.0F, -0.11F, 0.14F, -0.42F),
      key(16, -30.0F, 8.0F, 8.0F, -0.11F, 0.14F, -0.42F),
      key(19, -28.0F, 8.0F, 18.0F, -0.11F, 0.13F, -0.38F),
      under(25)
   };
   @Unique
   private static final float[][] CHANNEL = {
      key(0, -26.0F, 8.0F, 16.0F, -0.10F, 0.12F, -0.34F)
   };

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void regulusManiaFirstPerson(
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
      float channel = RegulusAnimationManager.firstPersonWeight(
         player, RegulusAnimationManager.Layer.MANIA_CHANNEL, snapshot.controlTargetId() >= 0, true
      );
      if (channel > 0.001F) {
         float pull = (0.5F + 0.5F * (float)Math.sin(((float)player.tickCount + partialTicks) * 0.35F)) * channel;
         RegulusPoser.firstPerson(poseStack, CHANNEL, 0.0F, channel, side);
         poseStack.translate(0.0F, -0.02F * pull, 0.08F * pull);
      }
      float cast = RegulusAnimationManager.firstPersonCastWeight(player, HeroAction.MANIA, true);
      if (cast > 0.001F) {
         RegulusPoser.firstPerson(poseStack, REACH, RegulusAnimationManager.castTime(player, partialTicks), cast, side);
      }
   }
}
