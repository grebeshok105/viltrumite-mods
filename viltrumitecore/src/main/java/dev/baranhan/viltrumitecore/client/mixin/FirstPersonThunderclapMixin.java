package dev.baranhan.viltrumitecore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ItemInHandRenderer.class},
   priority = 1550
)
public abstract class FirstPersonThunderclapMixin {
   @Unique
   private static final float S1_X = 0.92F;
   @Unique
   private static final float S1_Y = 0.36F;
   @Unique
   private static final float S1_Z = 0.23F;
   @Unique
   private static final float S1_RX = 1.54F;
   @Unique
   private static final float S1_RY = -10.94F;
   @Unique
   private static final float S1_RZ = -62.77F;
   @Unique
   private static final float S2_X = 0.93F;
   @Unique
   private static final float S2_Y = 0.13F;
   @Unique
   private static final float S2_Z = -0.31F;
   @Unique
   private static final float S2_RX = 21.51F;
   @Unique
   private static final float S2_RY = 40.83F;
   @Unique
   private static final float S2_RZ = -62.77F;

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )}
   )
   private void onRenderFirstPersonThunderclap(
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
         int clapTicks = corePlayer.getThunderclapTicks();
         if (clapTicks > 0) {
            HumanoidArm currentArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRenderingLeftArm = currentArm == HumanoidArm.LEFT;
            float m = isRenderingLeftArm ? -1.0F : 1.0F;
            float time = (20.0F - ((float)clapTicks - partialTicks)) / 20.0F;
            time = Mth.clamp(time, 0.0F, 1.0F);
            float targetX = 0.0F;
            float targetY = 0.0F;
            float targetZ = 0.0F;
            float targetRX = 0.0F;
            float targetRY = 0.0F;
            float targetRZ = 0.0F;
            float overMultiplier = 1.15F;
            if (time < 0.2F) {
               float localT = time / 0.2F;
               localT *= localT;
               targetX = Mth.lerp(localT, 0.0F, 0.92F);
               targetY = Mth.lerp(localT, 0.0F, 0.36F);
               targetZ = Mth.lerp(localT, 0.0F, 0.23F);
               targetRX = Mth.lerp(localT, 0.0F, 1.54F);
               targetRY = Mth.lerp(localT, 0.0F, -10.94F);
               targetRZ = Mth.lerp(localT, 0.0F, -62.77F);
            } else if (time < 0.38F) {
               targetX = 0.92F;
               targetY = 0.36F;
               targetZ = 0.23F;
               targetRX = 1.54F;
               targetRY = -10.94F;
               targetRZ = -62.77F;
            } else if (time < 0.45F) {
               float localT = (time - 0.38F) / 0.07F;
               targetX = Mth.lerp(localT, 0.92F, 0.93F * overMultiplier);
               targetY = Mth.lerp(localT, 0.36F, 0.13F * overMultiplier);
               targetZ = Mth.lerp(localT, 0.23F, -0.31F * overMultiplier);
               targetRX = Mth.lerp(localT, 1.54F, 21.51F * overMultiplier);
               targetRY = Mth.lerp(localT, -10.94F, 40.83F * overMultiplier);
               targetRZ = Mth.lerp(localT, -62.77F, -62.77F * overMultiplier);
            } else if (time < 0.6F) {
               float localT = (time - 0.45F) / 0.15F;
               targetX = Mth.lerp(localT, 0.93F * overMultiplier, 0.93F);
               targetY = Mth.lerp(localT, 0.13F * overMultiplier, 0.13F);
               targetZ = Mth.lerp(localT, -0.31F * overMultiplier, -0.31F);
               targetRX = Mth.lerp(localT, 21.51F * overMultiplier, 21.51F);
               targetRY = Mth.lerp(localT, 40.83F * overMultiplier, 40.83F);
               targetRZ = Mth.lerp(localT, -62.77F * overMultiplier, -62.77F);
            } else if (time < 0.7F) {
               targetX = 0.93F;
               targetY = 0.13F;
               targetZ = -0.31F;
               targetRX = 21.51F;
               targetRY = 40.83F;
               targetRZ = -62.77F;
            } else {
               float localT = (time - 0.7F) / 0.3F;
               targetX = Mth.lerp(localT, 0.93F, 0.0F);
               targetY = Mth.lerp(localT, 0.13F, 0.0F);
               targetZ = Mth.lerp(localT, -0.31F, 0.0F);
               targetRX = Mth.lerp(localT, 21.51F, 0.0F);
               targetRY = Mth.lerp(localT, 40.83F, 0.0F);
               targetRZ = Mth.lerp(localT, -62.77F, 0.0F);
            }

            poseStack.translate(targetX * m, targetY, targetZ);
            poseStack.mulPose(new Quaternionf().rotateX((float)Math.toRadians((double)targetRX)));
            poseStack.mulPose(new Quaternionf().rotateY((float)Math.toRadians((double)(targetRY * m))));
            poseStack.mulPose(new Quaternionf().rotateZ((float)Math.toRadians((double)(targetRZ * m))));
         }
      }
   }
}
