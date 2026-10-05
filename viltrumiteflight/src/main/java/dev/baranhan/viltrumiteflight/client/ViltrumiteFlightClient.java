package dev.baranhan.viltrumiteflight.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.baranhan.viltrumiteflight.client.gui.FirstPersonScreen;
import dev.baranhan.viltrumiteflight.client.gui.ThirdPersonScreen;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfigClient;
import dev.baranhan.viltrumiteflight.config.ViltrumiteFlightCameraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "viltrumiteflight",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ViltrumiteFlightClient {
   public static float currentCameraRoll = 0.0F;

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         ViltrumiteConfigClient.load();
         ViltrumiteFlightCameraConfig.load();
      });
   }

   @EventBusSubscriber(
      modid = "viltrumiteflight",
      bus = Bus.FORGE,
      value = {Dist.CLIENT}
   )
   public static class ClientForgeEvents {
      @SubscribeEvent
      public static void onCommandRegister(RegisterClientCommandsEvent event) {
         event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal("firstpersontool").executes(context -> {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new FirstPersonScreen()));
            return 1;
         }));
         event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal("thirdpersontool").executes(context -> {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new ThirdPersonScreen()));
            return 1;
         }));
      }
   }
}
