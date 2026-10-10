package dev.baranhan.viltrumitecore.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ViltrumiteEffects {
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "viltrumitecore");
   public static final RegistryObject<MobEffect> SCOURGE_VIRUS = EFFECTS.register("scourge_virus", ScourgeVirusEffect::new);
   public static final RegistryObject<MobEffect> FEAR = EFFECTS.register("fear", FearEffect::new);
   public static final RegistryObject<MobEffect> SUNDER = EFFECTS.register("sunder", SunderEffect::new);

   public static void register(IEventBus eventBus) {
      EFFECTS.register(eventBus);
   }
}
