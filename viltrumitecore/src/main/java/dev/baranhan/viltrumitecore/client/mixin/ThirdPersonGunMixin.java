package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.client.util.ShaderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerModel.class},
   priority = 3000
)
public abstract class ThirdPersonGunMixin<T extends LivingEntity> extends HumanoidModel<T> {
   @Unique
   private static final float TARGET_PITCH = (float)Math.toRadians(-82.7);
   @Unique
   private static final float TARGET_YAW = (float)Math.toRadians(-2.17);
   @Unique
   private static final float TARGET_PITCH_LEFT = (float)Math.toRadians(-81.7);
   @Unique
   private static final float TARGET_YAW_LEFT = (float)Math.toRadians(52.17);
   @Unique
   private static final float RECOIL_OFFSET = (float)Math.toRadians(-10.0);

   public ThirdPersonGunMixin(ModelPart root) {
      super(root);
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void applyThirdPersonGunAnim(T livingEntity, float f, float g, float h, float headYaw, float headPitch, CallbackInfo ci) {
      ItemStack mainHandItem = livingEntity.getMainHandItem();
      ItemStack offHandItem = livingEntity.getOffhandItem();
      boolean inMainHand = mainHandItem.getItem() instanceof InfinityGunItem;
      boolean inOffHand = offHandItem.getItem() instanceof InfinityGunItem;
      if (inMainHand || inOffHand) {
         ItemStack gunStack = inMainHand ? mainHandItem : offHandItem;
         boolean isLocalFirstPerson = livingEntity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
         if (!isLocalFirstPerson || ShaderCompat.isShadowPass()) {
            ViltrumiteCorePlayer corePlayer = (ViltrumiteCorePlayer)livingEntity;
            float dynamicPitch = headPitch * (float) (Math.PI / 180.0);
            float dynamicYaw = headYaw * (float) (Math.PI / 180.0);
            float maxYawTwist = (float)Math.toRadians(15.0);
            float clampedYaw = Mth.clamp(dynamicYaw, -maxYawTwist, maxYawTwist);
            float bodyPitch = (float)Math.toRadians((double)corePlayer.getCalculatedBodyPitch());
            float recoilWeight = 0.0F;
            if (gunStack.hasTag()) {
               int gunTimer = gunStack.getTag().getInt("GunTimer");
               recoilWeight = Mth.clamp((float)gunTimer / 14.0F, 0.0F, 1.0F);
            }

            float currentRecoil = RECOIL_OFFSET * recoilWeight;
            this.rightArm.xRot = TARGET_PITCH + dynamicPitch + bodyPitch + currentRecoil;
            this.leftArm.xRot = TARGET_PITCH_LEFT + dynamicPitch + bodyPitch + currentRecoil;
            this.rightArm.yRot = TARGET_YAW + clampedYaw;
            this.leftArm.yRot = TARGET_YAW_LEFT + clampedYaw;
            PlayerModel<T> model = (PlayerModel<T>)this;
            model.rightSleeve.copyFrom(this.rightArm);
            model.leftSleeve.copyFrom(this.leftArm);
         }
      }
   }
}
