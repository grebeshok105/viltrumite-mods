package dev.baranhan.viltrumiteflight.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public class PlayerRendererMixin {
   @Inject(
      method = {"renderRightHand"},
      at = {@At("HEAD")}
   )
   private void fixRightArmFP(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
      PlayerRenderer renderer = (PlayerRenderer)this;
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)renderer.getModel();
      model.rightArm.x = -5.0F;
      model.rightArm.y = 2.0F;
      model.rightArm.z = 0.0F;
      model.rightSleeve.x = -5.0F;
      model.rightSleeve.y = 2.0F;
      model.rightSleeve.z = 0.0F;
   }

   @Inject(
      method = {"renderLeftHand"},
      at = {@At("HEAD")}
   )
   private void fixLeftArmFP(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
      PlayerRenderer renderer = (PlayerRenderer)this;
      PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)renderer.getModel();
      model.leftArm.x = 5.0F;
      model.leftArm.y = 2.0F;
      model.leftArm.z = 0.0F;
      model.leftSleeve.x = 5.0F;
      model.leftSleeve.y = 2.0F;
      model.leftSleeve.z = 0.0F;
   }
}
