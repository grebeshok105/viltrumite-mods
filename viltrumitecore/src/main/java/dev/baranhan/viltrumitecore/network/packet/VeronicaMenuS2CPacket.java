package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.ironman.veronica.VeronicaView;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Opens (or refreshes) the Veronica suit menu on the owner's client. */
public class VeronicaMenuS2CPacket {
   private final VeronicaView view;

   public VeronicaMenuS2CPacket(VeronicaView view) {
      this.view = view;
   }

   public VeronicaMenuS2CPacket(FriendlyByteBuf buffer) {
      this.view = VeronicaView.read(buffer);
   }

   public void toBytes(FriendlyByteBuf buffer) {
      this.view.write(buffer);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
         dev.baranhan.viltrumitecore.client.ironman.veronica.VeronicaClient.open(this.view)));
      context.setPacketHandled(true);
   }
}
