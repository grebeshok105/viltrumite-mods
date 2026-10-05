package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class SwapAbilityBarC2SPacket {
   public SwapAbilityBarC2SPacket() {
   }

   public SwapAbilityBarC2SPacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteAbilityUser abilityUser) {
            int currentPage = abilityUser.getActivePage();
            int nextPage = (currentPage + 1) % 3;
            abilityUser.setActivePage(nextPage);
         }
      });
      context.setPacketHandled(true);
   }
}
