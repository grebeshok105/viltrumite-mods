package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Madness FOV change (spec 14): a constant slight zoom-in while the mad state
 * runs, on top of the shader zoom pulse. Same hook style vanilla uses for
 * item-use FOV effects.
 */
@Mixin({GameRenderer.class})
public class RegulusFovMixin {
   @Inject(
      method = {"getFov(Lnet/minecraft/client/Camera;FZ)D"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void regulusMadnessFov(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
      if (!useFovSetting) {
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player instanceof HeroPlayer heroPlayer) {
         HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
         if (snapshot != null && snapshot.heroId() == HeroId.REGULUS && snapshot.madness()) {
            cir.setReturnValue(cir.getReturnValue() * 0.9);
         }
      }
   }
}
