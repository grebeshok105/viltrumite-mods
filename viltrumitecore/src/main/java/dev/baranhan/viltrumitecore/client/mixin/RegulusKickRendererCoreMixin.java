package dev.baranhan.viltrumitecore.client.mixin;

import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.key;
import static dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming.under;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoseTiming;
import dev.baranhan.viltrumitecore.client.regulus.RegulusPoser;
import dev.baranhan.viltrumitecore.client.render.animation.RegulusAnimationManager;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Debris Kick whole-body motion: slight pull back on the chamber, forward
 * lunge with the torso thrown back on the strike, settle by the anim end.
 * Same hook as PunchRendererCoreMixin: rotate around a hip pivot, then
 * translate; rows are {tick, pitch, yaw, roll, x, y, z}, positive pitch leans
 * back, negative z moves forward.
 */
@Mixin(
   value = {PlayerRenderer.class},
   priority = 1530
)
public class RegulusKickRendererCoreMixin {
   @Unique
   private static final float[][] LUNGE = {
      under(0),
      key(11, -2.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.06F),
      key(14, 6.0F, -4.0F, 0.0F, 0.0F, 0.02F, -0.16F),
      key(24, 5.0F, -3.0F, 0.0F, 0.0F, 0.01F, -0.14F),
      key(38, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.03F),
      under(44)
   };

   @Inject(
      method = {"setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"},
      at = {@At("TAIL")}
   )
   private void regulusKickBodyMotion(AbstractClientPlayer player, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
      if (RegulusPoser.regulus(player) == null) {
         return;
      }
      float weight = RegulusAnimationManager.bodyCastWeight(player, HeroAction.DEBRIS_KICK);
      if (weight <= 0.001F) {
         return;
      }
      float elapsed = RegulusAnimationManager.castTime(player, partialTicks);
      float m = player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
      float pitch = RegulusPoseTiming.sample(LUNGE, elapsed, 0, 0.0F, true) * weight;
      float yaw = RegulusPoseTiming.sample(LUNGE, elapsed, 1, 0.0F, true) * m * weight;
      float roll = RegulusPoseTiming.sample(LUNGE, elapsed, 2, 0.0F, true) * m * weight;
      float x = RegulusPoseTiming.sample(LUNGE, elapsed, 3, 0.0F, true) * m * weight;
      float y = RegulusPoseTiming.sample(LUNGE, elapsed, 4, 0.0F, true) * weight;
      float z = RegulusPoseTiming.sample(LUNGE, elapsed, 5, 0.0F, true) * weight;
      float pivotY = 0.9F;
      poseStack.translate(0.0F, pivotY, 0.0F);
      poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
      poseStack.translate(0.0F, -pivotY, 0.0F);
      poseStack.translate(x, y, z);
   }
}
