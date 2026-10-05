package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.render.vfx.ChopBloodVFXManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class ChopHitS2CPacket {
   private final Vec3 hitPos;
   private final Vec3 chopDir;

   public ChopHitS2CPacket(Vec3 hitPos, Vec3 chopDir) {
      this.hitPos = hitPos;
      this.chopDir = chopDir;
   }

   public ChopHitS2CPacket(FriendlyByteBuf buf) {
      this.hitPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      this.chopDir = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeDouble(this.hitPos.x);
      buf.writeDouble(this.hitPos.y);
      buf.writeDouble(this.hitPos.z);
      buf.writeDouble(this.chopDir.x);
      buf.writeDouble(this.chopDir.y);
      buf.writeDouble(this.chopDir.z);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getDirection().getReceptionSide().isClient()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChopBloodVFXManager.addHit(this.hitPos, this.chopDir));
         }
      });
      context.setPacketHandled(true);
   }
}
