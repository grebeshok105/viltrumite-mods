package dev.baranhan.viltrumitecore.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Screen flash for the receiving player (Unibeam blinding, core explosion). Strength 0..1, ticks bounded. */
public class FlashS2CPacket {
   private final float strength;
   private final int ticks;

   public FlashS2CPacket(float strength, int ticks) {
      this.strength = Math.max(0.0F, Math.min(1.0F, strength));
      this.ticks = Math.max(0, Math.min(200, ticks));
   }

   public FlashS2CPacket(FriendlyByteBuf buffer) {
      this(buffer.readFloat(), buffer.readVarInt());
   }

   public void toBytes(FriendlyByteBuf buffer) {
      buffer.writeFloat(this.strength);
      buffer.writeVarInt(this.ticks);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
         dev.baranhan.viltrumitecore.client.render.vfx.FlashOverlay.flash(this.strength, this.ticks)));
      context.setPacketHandled(true);
   }
}
