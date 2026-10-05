package dev.baranhan.viltrumiteflight.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "viltrumiteflight"
)
public class ViltrumiteFlightCommand {
   @SubscribeEvent
   public static void onCommandRegister(RegisterCommandsEvent event) {
      CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("viltrumite").requires(source -> true))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("flight").requires(source -> source.hasPermission(2)))
                     .then(
                        Commands.argument("target", EntityArgument.player())
                           .executes(context -> executeToggleFlight((CommandSourceStack)context.getSource(), EntityArgument.getPlayer(context, "target")))
                     ))
                  .executes(context -> executeToggleFlight((CommandSourceStack)context.getSource(), ((CommandSourceStack)context.getSource()).getPlayerOrException()))
            )
      );
   }

   private static int executeToggleFlight(CommandSourceStack source, ServerPlayer target) {
      boolean currentFlightState = target.getAbilities().mayfly;
      target.getAbilities().mayfly = !currentFlightState;
      if (currentFlightState) {
         target.getAbilities().flying = false;
      }

      target.onUpdateAbilities();
      String stateMsg = !currentFlightState ? "\u00a7aENABLED" : "\u00a7cDISABLED";
      source.sendSystemMessage(Component.literal("\u00a7fViltrumite flight for \u00a7e" + target.getName().getString() + "\u00a7f: " + stateMsg));
      target.displayClientMessage(Component.literal("\u00a7fViltrumite flight powers " + stateMsg), true);
      return 1;
   }
}
