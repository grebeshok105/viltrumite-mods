package dev.baranhan.viltrumiteflight.client.mixin;

import dev.baranhan.viltrumiteflight.client.render.AtmosphericHeatFeatureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public class PlayerFeatureRendererMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void addCustomFeatures(Context ctx, boolean slim, CallbackInfo ci) {
      PlayerRenderer renderer = (PlayerRenderer)this;
      ((LivingEntityRendererAccessor)this).invokeAddFeature(new AtmosphericHeatFeatureRenderer(renderer));
   }
}
