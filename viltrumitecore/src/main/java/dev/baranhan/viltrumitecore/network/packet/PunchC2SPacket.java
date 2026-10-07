package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class PunchC2SPacket {
   private final boolean isLeft;

   public PunchC2SPacket(boolean isLeft) {
      this.isLeft = isLeft;
   }

   public PunchC2SPacket(FriendlyByteBuf buf) {
      this.isLeft = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.isLeft);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteCorePlayer corePlayer) {
            boolean regulus = dev.baranhan.viltrumitecore.hero.regulus.RegulusHero.stateOf(context.getSender()) != null;
            if (regulus ? !dev.baranhan.viltrumitecore.hero.regulus.RegulusHero.canPunch(context.getSender()) || !dev.baranhan.viltrumitecore.hero.regulus.RegulusHero.punchEquipped(context.getSender())
               : !corePlayer.isViltrumite() || HeroDamage.isAnchored(context.getSender())) {
               return;
            }

            if (corePlayer.getChopTicks() > 0 || corePlayer.getPunchTicks() > 0 && corePlayer.getPunchCooldown() > 0) {
               return;
            }

            corePlayer.setLeftArmPunch(this.isLeft);
            corePlayer.setPunchTicks(20);
         }
      });
      context.setPacketHandled(true);
   }
}
