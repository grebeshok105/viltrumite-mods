package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.homelander.HomelanderPoser;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Homelander first-person arm layer; same hook as FirstPersonPunchMixin. */
@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1560
)
public abstract class FirstPersonHomelanderMixin {
   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void homelanderFirstPerson(
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
      boolean mainHand = hand == InteractionHand.MAIN_HAND;
      HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      HomelanderPoser.poseFirstPerson(poseStack, player, side, mainHand, partialTicks);
   }
}
