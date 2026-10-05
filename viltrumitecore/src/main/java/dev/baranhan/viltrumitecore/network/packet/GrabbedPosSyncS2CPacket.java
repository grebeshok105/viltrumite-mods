package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.ViltrumiteCoreClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class GrabbedPosSyncS2CPacket {
   private final Vec3 pos;
   private final Vec3 velocity;

   public GrabbedPosSyncS2CPacket(Vec3 pos, Vec3 velocity) {
      this.pos = pos;
      this.velocity = velocity;
   }

   public GrabbedPosSyncS2CPacket(FriendlyByteBuf buf) {
      this.pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      this.velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeDouble(this.pos.x);
      buf.writeDouble(this.pos.y);
      buf.writeDouble(this.pos.z);
      buf.writeDouble(this.velocity.x);
      buf.writeDouble(this.velocity.y);
      buf.writeDouble(this.velocity.z);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handleClient()));
      context.setPacketHandled(true);
   }

   private void handleClient() {
      ViltrumiteCoreClient.exactHandPos = this.pos;
      ViltrumiteCoreClient.exactVelocity = this.velocity;
   }
}
