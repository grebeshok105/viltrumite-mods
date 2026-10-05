package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftClientMixin {
   @Shadow
   public LocalPlayer player;

   @Unique
   private boolean isViltrumiteSkillActive() {
      return !(this.player instanceof ViltrumiteCorePlayer corePlayer)
         ? false
         : corePlayer.getPunchTicks() > 0
            || corePlayer.getChopTicks() > 0
            || corePlayer.isDashing()
            || corePlayer.isBlocking()
            || corePlayer.isBarraging()
            || corePlayer.getThunderclapTicks() > 0;
   }

   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preventAttack(CallbackInfoReturnable<Boolean> cir) {
      if (this.isViltrumiteSkillActive()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"startUseItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preventItemUse(CallbackInfo ci) {
      if (this.isViltrumiteSkillActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"continueAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void preventBlockBreaking(boolean pLeftClick, CallbackInfo ci) {
      if (this.isViltrumiteSkillActive()) {
         ci.cancel();
      }
   }
}
