package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.TargetLockManager;
import dev.baranhan.viltrumitecore.config.ViltrumiteCameraConfig;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Unique
   private float currentX = 0.0F;
   @Unique
   private float currentY = 0.0F;
   @Unique
   private float currentZ = 0.0F;
   @Unique
   private long lastTime = System.currentTimeMillis();
   @Unique
   private boolean wasLocked = false;

   @Shadow
   protected abstract void move(double var1, double var3, double var5);

   @Shadow
   public abstract boolean isDetached();

   @Inject(
      method = {"setup"},
      at = {@At("TAIL")}
   )
   private void onUpdateTail(CallbackInfo ci) {
      long now = System.currentTimeMillis();
      float delta = (float)(now - this.lastTime) / 1000.0F;
      this.lastTime = now;
      if (delta > 0.1F) {
         delta = 0.1F;
      }

      float targetX = 0.0F;
      float targetY = 0.0F;
      float targetZ = 0.0F;
      if (this.isDetached() && (ViltrumiteCameraConfig.INSTANCE.enableCustomCamera || TargetLockManager.lockedTarget != null)) {
         targetX = ViltrumiteCameraConfig.INSTANCE.cameraOffsetX;
         targetY = ViltrumiteCameraConfig.INSTANCE.cameraOffsetY;
         targetZ = ViltrumiteCameraConfig.INSTANCE.cameraOffsetZ;
      }

      if (TargetLockManager.lockedTarget != null) {
         this.wasLocked = true;
      }

      if ((TargetLockManager.lockedTarget != null || this.wasLocked) && !ViltrumiteCameraConfig.INSTANCE.enableCustomCamera) {
         float lerpFactor = 1.0F - (float)Math.exp((double)(-8.0F * delta));
         this.currentX = Mth.lerp(lerpFactor, this.currentX, targetX);
         this.currentY = Mth.lerp(lerpFactor, this.currentY, targetY);
         this.currentZ = Mth.lerp(lerpFactor, this.currentZ, targetZ);
         if (TargetLockManager.lockedTarget == null && Math.abs(this.currentX) < 0.005F && Math.abs(this.currentY) < 0.005F && Math.abs(this.currentZ) < 0.005F
            )
          {
            this.wasLocked = false;
         }
      } else {
         this.currentX = targetX;
         this.currentY = targetY;
         this.currentZ = targetZ;
         this.wasLocked = false;
      }

      if (Math.abs(this.currentX) > 0.001F || Math.abs(this.currentY) > 0.001F || Math.abs(this.currentZ) > 0.001F) {
         this.move((double)this.currentZ, (double)this.currentY, (double)this.currentX);
      }
   }
}
