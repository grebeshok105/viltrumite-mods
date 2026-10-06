package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Owner-private snapshot: heart carrier entity ids for the owner's HUD. */
public class HeroOwnerSnapshotS2CPacket {
   private final int[] carrierEntityIds;

   public HeroOwnerSnapshotS2CPacket(int[] carrierEntityIds) {
      this.carrierEntityIds = carrierEntityIds;
   }

   public HeroOwnerSnapshotS2CPacket(FriendlyByteBuf buffer) {
      this.carrierEntityIds = new int[buffer.readVarInt()];
      for (int i = 0; i < this.carrierEntityIds.length; i++) {
         this.carrierEntityIds[i] = buffer.readInt();
      }
   }

   public void toBytes(FriendlyByteBuf buffer) {
      buffer.writeVarInt(this.carrierEntityIds.length);
      for (int id : this.carrierEntityIds) {
         buffer.writeInt(id);
      }
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHeroData.setCarriers(this.carrierEntityIds)));
      context.setPacketHandled(true);
   }
}
