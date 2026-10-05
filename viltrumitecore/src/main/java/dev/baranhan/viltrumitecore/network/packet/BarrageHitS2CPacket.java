package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.render.vfx.BarrageVFXManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class BarrageHitS2CPacket {
   private final Vec3 hitPos;

   public BarrageHitS2CPacket(Vec3 hitPos) {
      this.hitPos = hitPos;
   }

   public BarrageHitS2CPacket(FriendlyByteBuf buf) {
      this.hitPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeDouble(this.hitPos.x);
      buf.writeDouble(this.hitPos.y);
      buf.writeDouble(this.hitPos.z);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getDirection().getReceptionSide().isClient()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BarrageVFXManager.addSpark(this.hitPos));
         }
      });
      context.setPacketHandled(true);
   }
}
