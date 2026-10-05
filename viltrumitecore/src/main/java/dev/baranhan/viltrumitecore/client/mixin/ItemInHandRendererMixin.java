package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.item.InfinityGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public class ItemInHandRendererMixin {
   @Inject(
      method = {"itemUsed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cancelGunSway(InteractionHand hand, CallbackInfo ci) {
      if (Minecraft.getInstance().player != null) {
         ItemStack stack = Minecraft.getInstance().player.getItemInHand(hand);
         if (stack.getItem() instanceof InfinityGunItem) {
            ci.cancel();
         }
      }
   }
}
