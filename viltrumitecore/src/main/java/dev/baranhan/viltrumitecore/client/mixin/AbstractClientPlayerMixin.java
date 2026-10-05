package dev.baranhan.viltrumitecore.client.mixin;

import dev.baranhan.viltrumitecore.client.CosmeticLoader;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteCosmeticsPlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {AbstractClientPlayer.class},
   priority = 900
)
public abstract class AbstractClientPlayerMixin {
   @Unique
   private float smoothSpeedFov = 1.0F;
   @Unique
   private int stretchTimer = 0;
   @Unique
   private float smoothGunZoom = 1.0F;

   @Inject(
      method = {"getSkinTextureLocation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectSkinTexture(CallbackInfoReturnable<ResourceLocation> cir) {
      AbstractClientPlayer player = (AbstractClientPlayer)this;
      if (player instanceof ViltrumiteCosmeticsPlayer cosmeticsPlayer) {
         String skinName = cosmeticsPlayer.getViltrumiteSkin();
         if (!skinName.equals("off") && CosmeticLoader.SKINS.containsKey(skinName)) {
            cir.setReturnValue(CosmeticLoader.SKINS.get(skinName));
         }
      }
   }

   @Inject(
      method = {"getModelName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectModel(CallbackInfoReturnable<String> cir) {
      AbstractClientPlayer player = (AbstractClientPlayer)this;
      if (player instanceof ViltrumiteCosmeticsPlayer cosmeticsPlayer) {
         String skinName = cosmeticsPlayer.getViltrumiteSkin();
         if (!skinName.equals("off") && CosmeticLoader.SKINS.containsKey(skinName)) {
            cir.setReturnValue(cosmeticsPlayer.getViltrumiteModel());
         }
      }
   }

   @Inject(
      method = {"getCloakTextureLocation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetCapeTexture(CallbackInfoReturnable<ResourceLocation> cir) {
      AbstractClientPlayer player = (AbstractClientPlayer)this;
      if (player instanceof ViltrumiteCosmeticsPlayer cosmeticsPlayer) {
         String capeName = cosmeticsPlayer.getViltrumiteCape();
         if (!capeName.equals("off") && CosmeticLoader.CAPES.containsKey(capeName)) {
            cir.setReturnValue(CosmeticLoader.CAPES.get(capeName));
         }
      }
   }

   @Inject(
      method = {"getFieldOfViewModifier"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetFovMultiplier(CallbackInfoReturnable<Float> cir) {
      AbstractClientPlayer player;
      float vanillaFov;
      boolean var10000;
      label79: {
         player = (AbstractClientPlayer)this;
         vanillaFov = (Float)cir.getReturnValue();
         if (player instanceof ViltrumiteCorePlayer corePlayer && corePlayer.isSuperSpeed()) {
            var10000 = true;
            break label79;
         }

         var10000 = false;
      }

      boolean isSpeeding = var10000;
      float safeTargetFov = Math.min(vanillaFov, 1.15F);
      if (isSpeeding) {
         this.stretchTimer = 20;
         if (this.smoothSpeedFov < safeTargetFov) {
            this.smoothSpeedFov = safeTargetFov;
         }

         this.smoothSpeedFov = Mth.lerp(0.1F, this.smoothSpeedFov, 1.25F);
         cir.setReturnValue(this.smoothSpeedFov);
      } else if (this.stretchTimer > 0) {
         this.stretchTimer--;
         this.smoothSpeedFov = Mth.lerp(0.15F, this.smoothSpeedFov, safeTargetFov);
         cir.setReturnValue(this.smoothSpeedFov);
      }

      float finalFov = (Float)cir.getReturnValue();
      if (player instanceof ViltrumiteCorePlayer corePlayer) {
         float tickDelta = Minecraft.getInstance().getFrameTime();
         int punchTicks = corePlayer.getPunchTicks();
         if (punchTicks > 0) {
            float exactTicks = (float)punchTicks - tickDelta;
            float time = (20.0F - exactTicks) / 20.0F;
            time = Mth.clamp(time, 0.0F, 1.0F);
            float punchFovMultiplier = 1.0F;
            if (time < 0.25F) {
               float localT = time / 0.25F;
               punchFovMultiplier = Mth.lerp(localT, 1.0F, 0.7F);
            } else if (time < 0.35F) {
               float localT = (time - 0.25F) / 0.1F;
               punchFovMultiplier = Mth.lerp(localT, 0.7F, 1.0F);
            }

            finalFov *= punchFovMultiplier;
            cir.setReturnValue(finalFov);
         }

         int chopTicks = corePlayer.getChopTicks();
         if (chopTicks > 0) {
            float exactTicks = (float)chopTicks - tickDelta;
            float time = (20.0F - exactTicks) / 20.0F;
            time = Mth.clamp(time, 0.0F, 1.0F);
            float chopFovMultiplier = 1.0F;
            if (time < 0.25F) {
               float localT = time / 0.25F;
               chopFovMultiplier = Mth.lerp(localT, 1.0F, 0.85F);
            } else if (time < 0.35F) {
               float localT = (time - 0.25F) / 0.1F;
               chopFovMultiplier = Mth.lerp(localT, 0.85F, 1.0F);
            }

            finalFov *= chopFovMultiplier;
            cir.setReturnValue(finalFov);
         }

         int clapTicks = corePlayer.getThunderclapTicks();
         if (clapTicks > 0) {
            float exactTicks = (float)clapTicks - tickDelta;
            float time = (20.0F - exactTicks) / 20.0F;
            time = Mth.clamp(time, 0.0F, 1.0F);
            float clapFovMultiplier = 1.0F;
            if (time < 0.2F) {
               float localT = time / 0.2F;
               clapFovMultiplier = Mth.lerp(localT, 1.0F, 1.3F);
            } else if (time < 0.35F) {
               clapFovMultiplier = 1.3F;
            } else if (time < 0.45F) {
               float localT = (time - 0.35F) / 0.1F;
               clapFovMultiplier = Mth.lerp(localT, 1.3F, 1.0F);
            }

            finalFov *= clapFovMultiplier;
            cir.setReturnValue(finalFov);
         }

         if (corePlayer.isDashing()) {
            if (player instanceof ViltrumiteFlightPlayer flightPlayer
               && (flightPlayer.getFlightState() == FlightState.CRUISE || flightPlayer.getFlightState() == FlightState.SONIC)) {
               return;
            }

            float dashProgress = corePlayer.getDashProgress(tickDelta);
            if (dashProgress > 0.0F) {
               float fovZoomOut = 0.5F;
               finalFov *= 1.0F + dashProgress * fovZoomOut;
               cir.setReturnValue(finalFov);
            }
         }
      }
   }
}
