package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.render.vfx.BlockVFXManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class BlockVFXS2CPacket {
   private final Vec3 pos;
   private final float yaw;
   private final float pitch;

   public BlockVFXS2CPacket(Vec3 pos, float yaw, float pitch) {
      this.pos = pos;
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public BlockVFXS2CPacket(FriendlyByteBuf buf) {
      this.pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      this.yaw = buf.readFloat();
      this.pitch = buf.readFloat();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeDouble(this.pos.x);
      buf.writeDouble(this.pos.y);
      buf.writeDouble(this.pos.z);
      buf.writeFloat(this.yaw);
      buf.writeFloat(this.pitch);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handleClient()));
      context.setPacketHandled(true);
   }

   private void handleClient() {
      BlockVFXManager.spawnRing(this.pos, this.yaw, this.pitch);
   }
}
