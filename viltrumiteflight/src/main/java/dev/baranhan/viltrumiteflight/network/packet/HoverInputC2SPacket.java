package dev.baranhan.viltrumiteflight.network.packet;

import dev.baranhan.viltrumiteflight.util.FlightPermissions;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class HoverInputC2SPacket {
   private final float forward;
   private final float sideways;

   public HoverInputC2SPacket(float forward, float sideways) {
      this.forward = forward;
      this.sideways = sideways;
   }

   public HoverInputC2SPacket(FriendlyByteBuf buf) {
      this.forward = buf.readFloat();
      this.sideways = buf.readFloat();
   }

   public void toBytes(FriendlyByteBuf buf) {
      buf.writeFloat(this.forward);
      buf.writeFloat(this.sideways);
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

            ViltrumiteFlightPlayer omniPlayer = (ViltrumiteFlightPlayer)player;
            if (omniPlayer.getFlightState() != FlightState.NONE
               && Float.isFinite(this.forward) && Float.isFinite(this.sideways)
               && Math.abs(this.forward) <= 1.0F && Math.abs(this.sideways) <= 1.0F) {
               omniPlayer.setHoverForward(this.forward);
               omniPlayer.setHoverSideways(this.sideways);
            }
         }
      });
      return true;
   }
}
