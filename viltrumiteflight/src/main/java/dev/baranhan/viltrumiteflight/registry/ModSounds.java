package dev.baranhan.viltrumiteflight.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
   public static final String MOD_ID = "viltrumiteflight";
   public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "viltrumiteflight");
   public static final RegistryObject<SoundEvent> SONIC_BOOM = registerSoundEvent("sonic_boom");
   public static final RegistryObject<SoundEvent> WIND_LOOP = registerSoundEvent("wind_loop");
   public static final RegistryObject<SoundEvent> TAKEOFF = registerSoundEvent("takeoff");

   private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
      return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumiteflight", name)));
   }

   public static void register(IEventBus eventBus) {
      SOUNDS.register(eventBus);
   }
}
