package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.ironman.IronManFirstPerson;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iron Man first-person arm layer: combat poses and the shield guard (same hook as FirstPersonBlockMixin). */
@Mixin(ItemInHandRenderer.class)
public abstract class IronManFirstPersonShieldMixin {
   @Inject(
      method = "renderArmWithItem",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = Shift.AFTER)
   )
   private void viltrumitecore$ironManShield(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float attackAnim, ItemStack stack,
      float equipAnim, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
      HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
      IronManFirstPerson.applyCombat(player, arm, poseStack, partialTicks);
      IronManFirstPerson.applyGuard(player, arm, poseStack);
   }
}
