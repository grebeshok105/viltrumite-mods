package dev.baranhan.viltrumiteflight;

import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.network.ModMessages;
import dev.baranhan.viltrumiteflight.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("viltrumiteflight")
public class ViltrumiteFlight {
   public ViltrumiteFlight() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      ViltrumiteConfig.load();
      ModSounds.register(modEventBus);
      modEventBus.addListener(this::commonSetup);
      MinecraftForge.EVENT_BUS.register(this);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(ModMessages::register);
   }
}
