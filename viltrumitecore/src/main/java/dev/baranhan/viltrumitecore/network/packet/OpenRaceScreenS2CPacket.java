package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.gui.RaceSelectionScreen;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class OpenRaceScreenS2CPacket {
   public OpenRaceScreenS2CPacket() {
   }

   public OpenRaceScreenS2CPacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getDirection().getReceptionSide().isClient()) {
            OpenRaceScreenS2CPacket.ClientHandler.openScreen();
         }
      });
      context.setPacketHandled(true);
   }

   private static class ClientHandler {
      public static void openScreen() {
         Minecraft.getInstance().setScreen(new RaceSelectionScreen());
      }
   }
}
