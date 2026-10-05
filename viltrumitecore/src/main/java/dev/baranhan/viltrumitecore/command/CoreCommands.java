package dev.baranhan.viltrumitecore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteCosmeticsPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CoreCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "viltrumite"
                              )
                              .requires(source -> true))
                           .then(Commands.literal("skin").then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                              String skinName = StringArgumentType.getString(context, "name");
                              ServerPlayer playerEntity = ((CommandSourceStack)context.getSource()).getPlayerOrException();
                              ViltrumiteCosmeticsPlayer player = (ViltrumiteCosmeticsPlayer)playerEntity;
                              player.setViltrumiteSkin(skinName);
                              String feedbackMsg = skinName.equalsIgnoreCase("off") ? "\u00a7cDISABLED" : "\u00a7e" + skinName;
                              ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("\u00a7fViltrumite skin: " + feedbackMsg), false);
                              return 1;
                           }))))
                        .then(Commands.literal("cape").then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                           String capeName = StringArgumentType.getString(context, "name");
                           ServerPlayer playerEntity = ((CommandSourceStack)context.getSource()).getPlayerOrException();
                           ViltrumiteCosmeticsPlayer player = (ViltrumiteCosmeticsPlayer)playerEntity;
                           player.setViltrumiteCape(capeName);
                           String feedbackMsg = capeName.equalsIgnoreCase("off") ? "\u00a7cDISABLED" : "\u00a7e" + capeName;
                           ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("\u00a7fViltrumite cape: " + feedbackMsg), false);
                           return 1;
                        }))))
                     .then(Commands.literal("model").then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                        String modelName = StringArgumentType.getString(context, "name").toLowerCase();
                        ServerPlayer playerEntity = ((CommandSourceStack)context.getSource()).getPlayerOrException();
                        ViltrumiteCosmeticsPlayer player = (ViltrumiteCosmeticsPlayer)playerEntity;
                        if (!modelName.equals("default") && !modelName.equals("slim")) {
                           ((CommandSourceStack)context.getSource())
                              .sendSuccess(() -> Component.literal("\u00a7cInvalid model! Use 'default' or 'slim'."), false);
                           return 0;
                        } else {
                           player.setViltrumiteModel(modelName);
                           ((CommandSourceStack)context.getSource())
                              .sendSuccess(() -> Component.literal("\u00a7fViltrumite model set to: \u00a7e" + modelName), false);
                           return 1;
                        }
                     }))))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("power").requires(source -> source.hasPermission(2)))
                           .executes(context -> grantPower((CommandSourceStack)context.getSource(), ((CommandSourceStack)context.getSource()).getPlayerOrException())))
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .executes(context -> grantPower((CommandSourceStack)context.getSource(), EntityArgument.getPlayer(context, "target")))
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("depower").requires(source -> source.hasPermission(2)))
                        .executes(context -> revokePower((CommandSourceStack)context.getSource(), ((CommandSourceStack)context.getSource()).getPlayerOrException())))
                     .then(
                        Commands.argument("target", EntityArgument.player())
                           .executes(context -> revokePower((CommandSourceStack)context.getSource(), EntityArgument.getPlayer(context, "target")))
                     )
               ))
            .then(WorldEventCommands.build())
      );
   }

   private static int grantPower(CommandSourceStack source, ServerPlayer target) {
      if (target != null) {
         ViltrumiteCorePlayer corePlayer = (ViltrumiteCorePlayer)target;
         corePlayer.setViltrumite(true);
         target.displayClientMessage(Component.literal("\u00a7fViltrumite powers \u00a7aENABLED"), true);
         source.sendSuccess(() -> Component.literal("\u00a7fViltrumite powers for \u00a7e" + target.getName().getString() + "\u00a7f: \u00a7aENABLED"), false);
      }

      return 1;
   }

   private static int revokePower(CommandSourceStack source, ServerPlayer target) {
      if (target != null) {
         ViltrumiteCorePlayer corePlayer = (ViltrumiteCorePlayer)target;
         corePlayer.setViltrumite(false);
         if (!target.isCreative() && !target.isSpectator()) {
            target.getAbilities().mayfly = false;
            target.getAbilities().flying = false;
            target.onUpdateAbilities();
         }

         corePlayer.setSuperSpeed(false);
         target.displayClientMessage(Component.literal("\u00a7fViltrumite powers \u00a7cDISABLED"), true);
         source.sendSuccess(() -> Component.literal("\u00a7fViltrumite powers for \u00a7e" + target.getName().getString() + "\u00a7f: \u00a7cDISABLED"), false);
      }

      return 1;
   }
}
