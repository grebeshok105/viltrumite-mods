package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PlayerGrabStateSyncS2CPacket {
   private final boolean isGrabbed;
   private final UUID grabberId;

   public PlayerGrabStateSyncS2CPacket(boolean isGrabbed, UUID grabberId) {
      this.isGrabbed = isGrabbed;
      this.grabberId = grabberId;
   }

   public PlayerGrabStateSyncS2CPacket(FriendlyByteBuf buf) {
      this.isGrabbed = buf.readBoolean();
      this.grabberId = this.isGrabbed ? buf.readUUID() : null;
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.isGrabbed);
      if (this.isGrabbed && this.grabberId != null) {
         buf.writeUUID(this.grabberId);
      }
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handleClient()));
      context.setPacketHandled(true);
   }

   private void handleClient() {
      if (this.isGrabbed) {
         ViltrumiteCoreClient.iAmBeingGrabbedBy = this.grabberId;
      } else {
         ViltrumiteCoreClient.iAmBeingGrabbedBy = null;
      }
   }
}
