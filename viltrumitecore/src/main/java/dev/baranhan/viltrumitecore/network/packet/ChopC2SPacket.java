package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class ChopC2SPacket {
   private final boolean isLeft;
   private final int chopType;

   public ChopC2SPacket(boolean isLeft, int chopType) {
      this.isLeft = isLeft;
      this.chopType = chopType;
   }

   public ChopC2SPacket(FriendlyByteBuf buf) {
      this.isLeft = buf.readBoolean();
      this.chopType = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.isLeft);
      buf.writeInt(this.chopType);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteCorePlayer corePlayer) {
            if (!corePlayer.isViltrumite()) {
               return;
            }

            if (corePlayer.getChopTicks() > 0 || corePlayer.getPunchTicks() > 0) {
               return;
            }

            corePlayer.setLeftChop(this.isLeft);
            corePlayer.setChopType(this.chopType);
            corePlayer.setChopTicks(20);
         }
      });
      context.setPacketHandled(true);
   }
}
