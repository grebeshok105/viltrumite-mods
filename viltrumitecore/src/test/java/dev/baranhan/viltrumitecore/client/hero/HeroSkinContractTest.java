package dev.baranhan.viltrumitecore.client.hero;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.baranhan.viltrumitecore.hero.HeroId;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class HeroSkinContractTest {

   @Test
   void registersOneProviderPerHeroId() {
      HeroSkins.Provider provider = stub();
      // May already be registered by another test class; double registration
      // for the same id must fail loudly either way.
      try {
         HeroSkins.register(HeroId.VILTRUMITE, provider);
      } catch (IllegalStateException already) {
         // fine: another test registered it
      }

      assertThrows(IllegalStateException.class, () -> HeroSkins.register(HeroId.VILTRUMITE, stub()));
   }

   @Test
   void resolveIsEmptyForNonHeroPlayers() {
      assertTrue(HeroSkins.resolve(null).isEmpty());
   }

   private static HeroSkins.Provider stub() {
      return new HeroSkins.Provider() {
         @Override
         public ResourceLocation defaultSkin() {
            return new ResourceLocation("viltrumitecore", "textures/entity/hero/regulus.png");
         }

         @Override
         public String defaultModelName() {
            return "default";
         }
      };
   }
}
