package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class EquipAbilityC2SPacket {
   private final int slotIndex;
   private final String abilityId;

   public EquipAbilityC2SPacket(int slotIndex, String abilityId) {
      this.slotIndex = slotIndex;
      this.abilityId = abilityId;
   }

   public EquipAbilityC2SPacket(FriendlyByteBuf buf) {
      this.slotIndex = buf.readInt();
      this.abilityId = buf.readUtf();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.slotIndex);
      buf.writeUtf(this.abilityId);
   }

   public void handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         if (context.getSender() instanceof ViltrumiteAbilityUser abilityUser) {
            abilityUser.setAbilityInSlot(this.slotIndex, this.abilityId);
         }
      });
      context.setPacketHandled(true);
   }
}
