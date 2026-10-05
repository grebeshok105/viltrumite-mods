package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class GrabToggleC2SPacket {
   public GrabToggleC2SPacket() {
   }

   public GrabToggleC2SPacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteCorePlayer corePlayer) {
            if (!corePlayer.isViltrumite() || corePlayer.isBlocking()) {
               return;
            }

            if (corePlayer.getGrabbedTarget() != null) {
               corePlayer.releaseTarget();
            } else if (corePlayer.isTryingToGrab()) {
               corePlayer.setTryingToGrab(false);
            } else {
               corePlayer.setTryingToGrab(true);
            }
         }
      });
      context.setPacketHandled(true);
   }
}
