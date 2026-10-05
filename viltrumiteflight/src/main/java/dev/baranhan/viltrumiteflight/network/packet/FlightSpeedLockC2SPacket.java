package dev.baranhan.viltrumiteflight.network.packet;

import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class FlightSpeedLockC2SPacket {
   public FlightSpeedLockC2SPacket() {
   }

   public FlightSpeedLockC2SPacket(FriendlyByteBuf buf) {
   }

   public void toBytes(FriendlyByteBuf buf) {
   }

   public boolean handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null) {
            ViltrumiteFlightPlayer vPlayer = (ViltrumiteFlightPlayer)player;
            vPlayer.setSpeedLocked(!vPlayer.isSpeedLocked());
         }
      });
      return true;
   }
}
