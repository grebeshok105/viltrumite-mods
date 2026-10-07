package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.regulus.RegulusImpactFx;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * One-shot Regulus world FX (shard spray, landing shockwave, Counter slam,
 * air blade, launches). The server resolves gameplay instantly; this packet
 * only tells clients what to draw, which sounds to place and how hard to
 * shake the camera.
 */
public class RegulusFxS2CPacket {
   public static final byte SHARDS = 0;
   public static final byte SHOCKWAVE = 1;
   public static final byte SLAM = 2;
   public static final byte AIR_BLADE = 3;
   public static final byte JUMP_LAUNCH = 4;
   public static final byte COUNTER_LAUNCH = 5;

   private static final int MAX_POINTS = 64;

   public final byte kind;
   public final Vec3 origin;
   public final float power;
   public final int blockStateId;
   /** Flat xyz triples: shard end points, or the blade end. */
   public final float[] points;

   public RegulusFxS2CPacket(byte kind, Vec3 origin, float power, int blockStateId, float[] points) {
      this.kind = kind;
      this.origin = origin;
      this.power = power;
      this.blockStateId = blockStateId;
      this.points = points == null ? new float[0] : points;
   }

   public RegulusFxS2CPacket(FriendlyByteBuf buf) {
      this.kind = buf.readByte();
      this.origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
      this.power = buf.readFloat();
      this.blockStateId = buf.readVarInt();
      int count = Math.min(MAX_POINTS, Math.max(0, buf.readVarInt()));
      this.points = new float[count * 3];
      for (int i = 0; i < this.points.length; i++) {
         this.points[i] = buf.readFloat();
      }
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeByte(this.kind);
      buf.writeDouble(this.origin.x);
      buf.writeDouble(this.origin.y);
      buf.writeDouble(this.origin.z);
      buf.writeFloat(this.power);
      buf.writeVarInt(this.blockStateId);
      int count = Math.min(MAX_POINTS, this.points.length / 3);
      buf.writeVarInt(count);
      for (int i = 0; i < count * 3; i++) {
         buf.writeFloat(this.points[i]);
      }
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RegulusImpactFx.handle(this)));
      context.setPacketHandled(true);
   }
}
