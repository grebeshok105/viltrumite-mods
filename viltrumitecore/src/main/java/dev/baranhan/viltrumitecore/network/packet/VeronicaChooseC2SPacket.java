package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.ironman.veronica.IronManVeronica;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Suit chosen in the Veronica menu: mark ordinal or VeronicaView.HULKBUSTER. The server validates everything. */
public class VeronicaChooseC2SPacket {
   private final int choice;

   public VeronicaChooseC2SPacket(int choice) {
      this.choice = choice;
   }

   public VeronicaChooseC2SPacket(FriendlyByteBuf buffer) {
      this.choice = buffer.readVarInt();
   }

   public void toBytes(FriendlyByteBuf buffer) {
      buffer.writeVarInt(this.choice);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null) {
            IronManVeronica.choose(player, this.choice);
         }
      });
      context.setPacketHandled(true);
   }
}
