package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class BlockToggleC2SPacket {
   public BlockToggleC2SPacket() {
   }

   public BlockToggleC2SPacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteCorePlayer corePlayer) {
            if (!corePlayer.isViltrumite()) {
               return;
            }

            if (corePlayer.isTryingToGrab() || corePlayer.getPunchTicks() > 0 || corePlayer.getChopTicks() > 0) {
               return;
            }

            boolean newState = !corePlayer.isBlocking();
            corePlayer.setBlocking(newState);
         }
      });
      context.setPacketHandled(true);
   }
}
