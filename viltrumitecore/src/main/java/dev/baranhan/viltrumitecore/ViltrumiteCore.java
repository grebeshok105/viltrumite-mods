package dev.baranhan.viltrumitecore;

import dev.baranhan.viltrumitecore.ability.ViltrumiteAbilities;
import dev.baranhan.viltrumitecore.block.ViltrumiteBlocks;
import dev.baranhan.viltrumitecore.command.CoreCommands;
import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import dev.baranhan.viltrumitecore.effect.ViltrumiteEffects;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import dev.baranhan.viltrumitecore.item.ViltrumiteCreativeTabs;
import dev.baranhan.viltrumitecore.item.ViltrumiteItems;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.particle.ViltrumiteParticles;
import dev.baranhan.viltrumitecore.potion.ViltrumitePotions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod("viltrumitecore")
public class ViltrumiteCore {
   public static final String MOD_ID = "viltrumitecore";
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "viltrumitecore");
   public static final RegistryObject<SoundEvent> PUNCH_IMPACT_EVENT = SOUND_EVENTS.register(
      "punch_impact", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", "punch_impact"))
   );
   public static final RegistryObject<SoundEvent> DASH_EVENT = SOUND_EVENTS.register(
      "dash", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", "dash"))
   );
   public static final RegistryObject<SoundEvent> BLOCK_EVENT = SOUND_EVENTS.register(
      "block", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", "block"))
   );
   public static final RegistryObject<SoundEvent> INFINITY_GUN_SHOOT_EVENT = SOUND_EVENTS.register(
      "infinity_gun_shoot", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", "infinity_gun_shoot"))
   );

   public ViltrumiteCore() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      ViltrumiteCoreConfig.load();
      SOUND_EVENTS.register(modEventBus);
      modEventBus.addListener(this::commonSetup);
      MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);
      ViltrumiteItems.register(modEventBus);
      ViltrumiteCreativeTabs.register(modEventBus);
      ViltrumiteEntities.register(modEventBus);
      ViltrumiteParticles.register(modEventBus);
      ViltrumiteEffects.register(modEventBus);
      ViltrumiteBlocks.register(modEventBus);
      ViltrumitePotions.register(modEventBus);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> {
         CoreMessages.register();
         ViltrumiteAbilities.registerAll();
      });
   }

   private void onCommandRegister(RegisterCommandsEvent event) {
      CoreCommands.register(event.getDispatcher());
   }
}
