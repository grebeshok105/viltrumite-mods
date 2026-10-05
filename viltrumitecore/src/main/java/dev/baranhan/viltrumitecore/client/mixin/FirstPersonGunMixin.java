package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class FirstPersonGunMixin {
   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void captureFirstPersonMatrix(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float attackAnim,
      ItemStack stack,
      float equipAnim,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         if (stack.getItem() instanceof InfinityGunItem) {
            Minecraft client = Minecraft.getInstance();
            if (player == client.player && client.options.getCameraType().isFirstPerson()) {
               poseStack.pushPose();
               HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
               float m = currentArm == HumanoidArm.LEFT ? -1.0F : 1.0F;
               float offsetX = 0.45F * m;
               float offsetY = -0.145F;
               float offsetZ = -2.4F;
               poseStack.translate(offsetX, offsetY, offsetZ);
               Vector3f localPos = poseStack.last().pose().getTranslation(new Vector3f());
               Vector3f localDir = new Vector3f(0.0F, 0.0F, -1.0F);
               poseStack.last().normal().transform(localDir);
               poseStack.popPose();
               corePlayer.setFirstPersonLocalHandPos(new Vec3((double)localPos.x(), (double)localPos.y(), (double)localPos.z()));
               Camera camera = client.gameRenderer.getMainCamera();
               Vector3f f = camera.getLookVector();
               Vector3f u = camera.getUpVector();
               Vector3f l = camera.getLeftVector();
               Vec3 forward = new Vec3((double)f.x(), (double)f.y(), (double)f.z());
               Vec3 up = new Vec3((double)u.x(), (double)u.y(), (double)u.z());
               Vec3 left = new Vec3((double)l.x(), (double)l.y(), (double)l.z());
               Vec3 exactWorldPos = camera.getPosition()
                  .add(left.scale((double)(-localPos.x())))
                  .add(up.scale((double)localPos.y()))
                  .add(forward.scale((double)(-localPos.z())));
               corePlayer.setCalculatedHandPos(exactWorldPos);
               Vec3 exactWorldDir = Vec3.ZERO
                  .add(left.scale((double)(-localDir.x())))
                  .add(up.scale((double)localDir.y()))
                  .add(forward.scale((double)(-localDir.z())));
               double horizontalLength = Math.sqrt(exactWorldDir.x * exactWorldDir.x + exactWorldDir.z * exactWorldDir.z);
               float exactHandPitch = (float)Math.toDegrees(-Math.atan2(exactWorldDir.y, horizontalLength));
               float exactHandYaw = (float)Math.toDegrees(Math.atan2(-exactWorldDir.x, exactWorldDir.z));
               corePlayer.setCalculatedHandYaw(exactHandYaw);
               corePlayer.setCalculatedHandPitch(exactHandPitch);
            }
         }
      }
   }
}
