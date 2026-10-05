package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.render.vfx.ChopBloodVFXManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class ChopBleedS2CPacket {
   private final double x;
   private final double y;
   private final double z;

   public ChopBleedS2CPacket(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public ChopBleedS2CPacket(FriendlyByteBuf buf) {
      this.x = buf.readDouble();
      this.y = buf.readDouble();
      this.z = buf.readDouble();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeDouble(this.x);
      buf.writeDouble(this.y);
      buf.writeDouble(this.z);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getDirection().getReceptionSide().isClient()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChopBloodVFXManager.addBleed(new Vec3(this.x, this.y, this.z)));
         }
      });
      context.setPacketHandled(true);
   }
}
