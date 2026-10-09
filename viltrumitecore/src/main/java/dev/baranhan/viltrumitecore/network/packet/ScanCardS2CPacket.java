package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.ironman.scan.ScanCard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Iron Man scan result card, owner only. */
public class ScanCardS2CPacket {
   private final ScanCard card;

   public ScanCardS2CPacket(ScanCard card) {
      this.card = card;
   }

   public ScanCardS2CPacket(FriendlyByteBuf buffer) {
      this.card = ScanCard.read(buffer);
   }

   public void toBytes(FriendlyByteBuf buffer) {
      this.card.write(buffer);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
         dev.baranhan.viltrumitecore.client.ironman.scan.ScanCardRenderer.accept(this.card)));
      context.setPacketHandled(true);
   }
}
