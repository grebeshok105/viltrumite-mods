package dev.baranhan.viltrumitecore.client.homelander;

import dev.baranhan.viltrumitecore.client.hero.HeroSkins;
import dev.baranhan.viltrumitecore.hero.HeroId;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/** Client entry point for Homelander: skin and client systems. */
public final class HomelanderClient {
   public static final ResourceLocation SKIN = new ResourceLocation("viltrumitecore", "textures/entity/hero/homelander.png");

   private HomelanderClient() {
   }

   public static void registerSkins() {
      // Focus outline first: it wins over later sources on the same entity.
      dev.baranhan.viltrumitecore.client.render.vfx.OutlineTargets.register(FocusClient::colorOf);
      HeroSkins.register(HeroId.HOMELANDER, new HeroSkins.Provider() {
         @Override
         public ResourceLocation defaultSkin() {
            return SKIN;
         }

         @Override
         public String defaultModelName() {
            return "default";
         }

         @Override
         public Optional<ResourceLocation> skinVariant(AbstractClientPlayer player) {
            return Optional.empty();
         }

         @Override
         public boolean suppressCosmetics(AbstractClientPlayer player) {
            return true;
         }

         @Override
         public boolean suppressCape(AbstractClientPlayer player) {
            return true;
         }
      });
   }
}
