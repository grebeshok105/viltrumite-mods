package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class SpeedToggleC2SPacket {
   public SpeedToggleC2SPacket() {
   }

   public SpeedToggleC2SPacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player instanceof ViltrumiteCorePlayer corePlayer) {
            if (!corePlayer.isViltrumite()) {
               return;
            }

            if (player instanceof ViltrumiteFlightPlayer flightPlayer && flightPlayer.getFlightState() != FlightState.NONE) {
               return;
            }

            boolean newState = !corePlayer.isSuperSpeed();
            corePlayer.setSuperSpeed(newState);
         }
      });
      context.setPacketHandled(true);
   }
}
