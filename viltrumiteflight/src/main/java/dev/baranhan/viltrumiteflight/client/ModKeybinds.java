package dev.baranhan.viltrumiteflight.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumiteflight",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ModKeybinds {
   public static KeyMapping flightKeyBinding;

   @SubscribeEvent
   public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
   }
}
