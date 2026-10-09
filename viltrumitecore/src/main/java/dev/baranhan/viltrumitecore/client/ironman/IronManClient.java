package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.client.hero.HeroSkins;
import dev.baranhan.viltrumitecore.hero.HeroId;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/** Client entry point for Iron Man: Tony skin and client systems. */
public final class IronManClient {
   /** Tony Stark without armor (classic 4 px arms). */
   public static final ResourceLocation SKIN = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman.png");

   private IronManClient() {
   }

   public static void registerSkins() {
      HeroSkins.register(HeroId.IRON_MAN, new HeroSkins.Provider() {
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
            // Reveal frame of the nano wave (also the first-person hand texture).
            int frame = IronManSkinView.frame(player, net.minecraft.client.Minecraft.getInstance().getFrameTime());
            return frame <= 0 ? Optional.empty() : Optional.of(IronManSkinFrames.skin(frame));
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
