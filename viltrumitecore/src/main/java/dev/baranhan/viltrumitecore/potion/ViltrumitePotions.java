package dev.baranhan.viltrumitecore.potion;

import dev.baranhan.viltrumitecore.effect.ViltrumiteEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ViltrumitePotions {
   public static final String MOD_ID = "viltrumitecore";
   public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, "viltrumitecore");
   public static final RegistryObject<Potion> SCOURGE_VIRUS = POTIONS.register(
      "scourge_virus", () -> new Potion("scourge_virus", new MobEffectInstance[]{new MobEffectInstance((MobEffect)ViltrumiteEffects.SCOURGE_VIRUS.get(), 600)})
   );
   public static final RegistryObject<Potion> LONG_SCOURGE_VIRUS = POTIONS.register(
      "long_scourge_virus",
      () -> new Potion("scourge_virus", new MobEffectInstance[]{new MobEffectInstance((MobEffect)ViltrumiteEffects.SCOURGE_VIRUS.get(), 1800)})
   );
   public static final RegistryObject<Potion> STRONG_SCOURGE_VIRUS = POTIONS.register(
      "strong_scourge_virus",
      () -> new Potion("scourge_virus", new MobEffectInstance[]{new MobEffectInstance((MobEffect)ViltrumiteEffects.SCOURGE_VIRUS.get(), 300, 1)})
   );

   public static void register(IEventBus modEventBus) {
      POTIONS.register(modEventBus);
   }

   private ViltrumitePotions() {
   }
}
