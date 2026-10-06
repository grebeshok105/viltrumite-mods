package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.client.hero.HeroSkins;
import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.hero.HeroPlayer;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class RegulusClient {
   private static final ResourceLocation DEFAULT_SKIN = new ResourceLocation("viltrumitecore", "textures/entity/hero/regulus.png");
   private static final ResourceLocation MADNESS_SKIN = new ResourceLocation("viltrumitecore", "textures/entity/hero/regulus_madness.png");

   private RegulusClient() {
   }

   public static void registerSkins() {
      HeroSkins.register(HeroId.REGULUS, new HeroSkins.Provider() {
         @Override
         public ResourceLocation defaultSkin() {
            return DEFAULT_SKIN;
         }

         @Override
         public String defaultModelName() {
            return "default";
         }

         @Override
         public Optional<ResourceLocation> skinVariant(AbstractClientPlayer player) {
            if (player instanceof HeroPlayer heroPlayer) {
               HeroPublicSnapshot snapshot = heroPlayer.getHeroSnapshot();
               if (snapshot != null && snapshot.madness()) {
                  return Optional.of(MADNESS_SKIN);
               }
            }
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
