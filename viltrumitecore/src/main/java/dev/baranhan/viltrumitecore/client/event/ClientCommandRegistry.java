package dev.baranhan.viltrumitecore.client.event;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.baranhan.viltrumitecore.client.gui.NpcSpawnerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public class ClientCommandRegistry {
   @SubscribeEvent
   public static void onClientCommandRegister(RegisterClientCommandsEvent event) {
      event.getDispatcher().register((LiteralArgumentBuilder)Commands.literal("spawnclone").executes(context -> {
         Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new NpcSpawnerScreen()));
         return 1;
      }));
   }
}
