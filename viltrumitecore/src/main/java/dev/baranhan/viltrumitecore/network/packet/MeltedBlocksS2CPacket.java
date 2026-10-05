package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.render.vfx.MeltedTunnelRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class MeltedBlocksS2CPacket {
   private final List<BlockPos> blocks;

   public MeltedBlocksS2CPacket(List<BlockPos> blocks) {
      this.blocks = blocks;
   }

   public MeltedBlocksS2CPacket(FriendlyByteBuf buf) {
      int size = buf.readVarInt();
      this.blocks = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         this.blocks.add(buf.readBlockPos());
      }
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.blocks.size());

      for (BlockPos pos : this.blocks) {
         buf.writeBlockPos(pos);
      }
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handleClient()));
      context.setPacketHandled(true);
   }

   private void handleClient() {
      MeltedTunnelRenderer.addMeltedBlocks(this.blocks);
   }
}
