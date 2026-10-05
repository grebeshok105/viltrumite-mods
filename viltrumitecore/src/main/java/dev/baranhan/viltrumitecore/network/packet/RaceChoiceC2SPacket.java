package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class RaceChoiceC2SPacket {
   private final boolean choseViltrumite;

   public RaceChoiceC2SPacket(boolean choseViltrumite) {
      this.choseViltrumite = choseViltrumite;
   }

   public RaceChoiceC2SPacket(FriendlyByteBuf buf) {
      this.choseViltrumite = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.choseViltrumite);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteCorePlayer corePlayer) {
            corePlayer.setViltrumite(this.choseViltrumite);
            corePlayer.setChosenRace(true);
         }
      });
      context.setPacketHandled(true);
   }
}
