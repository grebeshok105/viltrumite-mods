package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.TargetLockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MouseMixin {
   @Shadow
   private double accumulatedDX;
   @Shadow
   private double accumulatedDY;

   @Inject(
      method = {"turnPlayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onUpdateMouse(CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.screen == null && TargetLockManager.lockedTarget != null) {
         this.accumulatedDX = 0.0;
         this.accumulatedDY = 0.0;
         ci.cancel();
         TargetLockManager.updateCameraEveryFrame(minecraft);
      }
   }
}
