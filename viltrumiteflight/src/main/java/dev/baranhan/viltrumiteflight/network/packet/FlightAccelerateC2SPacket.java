package dev.baranhan.viltrumiteflight.network.packet;

import dev.baranhan.viltrumiteflight.util.FlightPermissions;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class FlightAccelerateC2SPacket {
   private final boolean isAccelerating;

   public FlightAccelerateC2SPacket(boolean isAccelerating) {
      this.isAccelerating = isAccelerating;
   }

   public FlightAccelerateC2SPacket(FriendlyByteBuf buf) {
      this.isAccelerating = buf.readBoolean();
   }

   public void toBytes(FriendlyByteBuf buf) {
      buf.writeBoolean(this.isAccelerating);
   }

   public boolean handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null) {
            if (!FlightPermissions.allowsModFlight(player)) {
               FlightPermissions.resetModFlight(player);
               return;
            }

            ViltrumiteFlightPlayer flightPlayer = (ViltrumiteFlightPlayer)player;
            if (flightPlayer.getFlightState() != FlightState.NONE) {
               flightPlayer.setFlightAccelerating(this.isAccelerating);
            }
         }
      });
      return true;
   }
}
