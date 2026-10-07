package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.hero.HeroSkins;
import dev.baranhan.viltrumitecore.client.hero.ResolvedHeroSkin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerRenderer.class)
public abstract class PlayerHandSkinMixin {
   @Redirect(
      method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkinTextureLocation()Lnet/minecraft/resources/ResourceLocation;"),
      require = 2,
      allow = 2
   )
   private ResourceLocation viltrumitecore$handTexture(AbstractClientPlayer player) {
      ResolvedHeroSkin heroSkin = HeroSkins.resolve(player).orElse(null);
      if (heroSkin != null && !heroSkin.suppressOverride()) {
         return heroSkin.handTexture();
      }
      return player.getSkinTextureLocation();
   }
}
