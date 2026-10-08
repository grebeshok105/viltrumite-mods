package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class ThunderclapC2SPacket {
   public ThunderclapC2SPacket() {
   }

   public ThunderclapC2SPacket(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(
         () -> {
            if (context.getSender() instanceof ViltrumiteCorePlayer corePlayer
               && dev.baranhan.viltrumitecore.hero.LegacyKit.allows(context.getSender(), dev.baranhan.viltrumitecore.hero.LegacyKit.THUNDERCLAP)
               && corePlayer.getThunderclapTicks() <= 0
               && corePlayer.getPunchTicks() <= 0
               && corePlayer.getChopTicks() <= 0
               && !corePlayer.isBlocking()
               && !HeroDamage.isAnchored(context.getSender())) {
               corePlayer.setThunderclapTicks(20);
            }
         }
      );
      context.setPacketHandled(true);
   }
}
