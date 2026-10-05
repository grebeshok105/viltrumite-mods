package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class FlightConfigSyncC2SPacket {
   private final float maxFlightSpeed;
   private final float throttleSpeed;
   private final boolean isHeatEnabled;
   private final boolean breakBlocksOnTakeoff;

   public FlightConfigSyncC2SPacket(float maxFlightSpeed, float throttleSpeed, boolean isHeatEnabled, boolean breakBlocksOnTakeoff) {
      this.maxFlightSpeed = maxFlightSpeed;
      this.throttleSpeed = throttleSpeed;
      this.isHeatEnabled = isHeatEnabled;
      this.breakBlocksOnTakeoff = breakBlocksOnTakeoff;
   }

   public FlightConfigSyncC2SPacket(FriendlyByteBuf buf) {
      this.maxFlightSpeed = buf.readFloat();
      this.throttleSpeed = buf.readFloat();
      this.isHeatEnabled = buf.readBoolean();
      this.breakBlocksOnTakeoff = buf.readBoolean();
   }

   public void toBytes(FriendlyByteBuf buf) {
      buf.writeFloat(this.maxFlightSpeed);
      buf.writeFloat(this.throttleSpeed);
      buf.writeBoolean(this.isHeatEnabled);
      buf.writeBoolean(this.breakBlocksOnTakeoff);
   }

   public boolean handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null && player.hasPermissions(2)) {
            if (player instanceof ViltrumiteFlightPlayer flightPlayer) {
               flightPlayer.setMaxFlightSpeed(this.maxFlightSpeed);
               flightPlayer.setThrottleSpeed(this.throttleSpeed);
            }

            ViltrumiteConfig.INSTANCE.isHeatEnabled = this.isHeatEnabled;
            ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff = this.breakBlocksOnTakeoff;
            ViltrumiteConfig.save();
         }
      });
      return true;
   }
}
