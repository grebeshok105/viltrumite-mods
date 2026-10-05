package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.client.render.animation.GrabAnimationManager;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.HandPosSyncC2SPacket;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class FirstPersonGrabMixin {
   @Unique
   private static final float GRAB_X = 1.14F;
   @Unique
   private static final float GRAB_Y = 0.22F;
   @Unique
   private static final float GRAB_Z = -0.49F;
   @Unique
   private static final float GRAB_RX = 10.19F;
   @Unique
   private static final float GRAB_RY = 38.98F;
   @Unique
   private static final float GRAB_RZ = -58.58F;

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void onRenderFirstPersonGrab(
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
         boolean isGrabbing = corePlayer.getGrabbedTarget() != null || corePlayer.isTryingToGrab();
         float weight = GrabAnimationManager.calculateWeight(player, isGrabbing);
         if (!(weight < 0.001F) || isGrabbing) {
            HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            if (currentArm == HumanoidArm.LEFT) {
               float m = -1.0F;
               float x = Mth.lerp(weight, 0.0F, 1.14F * m);
               float y = Mth.lerp(weight, 0.0F, 0.22F);
               float z = Mth.lerp(weight, 0.0F, -0.49F);
               float rx = Mth.lerp(weight, 0.0F, 10.19F);
               float ry = Mth.lerp(weight, 0.0F, 38.98F * m);
               float rz = Mth.lerp(weight, 0.0F, -58.58F * m);
               poseStack.translate(x, y, z);
               poseStack.mulPose(new Quaternionf().rotateX((float)Math.toRadians((double)rx)));
               poseStack.mulPose(new Quaternionf().rotateY((float)Math.toRadians((double)ry)));
               poseStack.mulPose(new Quaternionf().rotateZ((float)Math.toRadians((double)rz)));
               Minecraft client = Minecraft.getInstance();
               if (player == client.player && client.options.getCameraType().isFirstPerson()) {
                  poseStack.pushPose();
                  poseStack.translate(-1.5, 0.0, -1.5);
                  Vector3f localPos = poseStack.last().pose().getTranslation(new Vector3f());
                  poseStack.popPose();
                  corePlayer.setFirstPersonLocalHandPos(new Vec3((double)localPos.x(), (double)localPos.y(), (double)localPos.z()));
                  Camera camera = client.gameRenderer.getMainCamera();
                  Vector3f worldCalc = new Vector3f(localPos);
                  worldCalc.rotateX((float)Math.toRadians((double)(-camera.getXRot())));
                  worldCalc.rotateY((float)Math.toRadians((double)(-(camera.getYRot() + 180.0F))));
                  Vec3 exactWorldPos = camera.getPosition().add((double)worldCalc.x(), (double)worldCalc.y(), (double)worldCalc.z());
                  corePlayer.setCalculatedHandPos(exactWorldPos);
                  CoreMessages.sendToServer(new HandPosSyncC2SPacket(exactWorldPos.x, exactWorldPos.y, exactWorldPos.z));
               }
            }
         }
      }
   }
}
