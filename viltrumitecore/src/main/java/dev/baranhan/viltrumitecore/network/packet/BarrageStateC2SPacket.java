package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class BarrageStateC2SPacket {
   private final boolean isBarraging;

   public BarrageStateC2SPacket(boolean isBarraging) {
      this.isBarraging = isBarraging;
   }

   public BarrageStateC2SPacket(FriendlyByteBuf buf) {
      this.isBarraging = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.isBarraging);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null && player instanceof ViltrumiteCorePlayer corePlayer && dev.baranhan.viltrumitecore.hero.LegacyKit.allows(player, dev.baranhan.viltrumitecore.hero.LegacyKit.BARRAGE)) {
            // Starting a barrage while anchored is denied; stopping one stays allowed.
            if (this.isBarraging && HeroDamage.isAnchored(player)) {
               return;
            }

            corePlayer.setBarraging(this.isBarraging);
         }
      });
      context.setPacketHandled(true);
   }
}
