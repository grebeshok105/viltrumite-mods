package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.ability.ViltrumiteAbilities;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.util.ViltrumiteAbilityUser;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
         ServerPlayer player = context.getSender();
         if (!(player instanceof ViltrumiteAbilityUser abilityUser)) {
            return;
         }

         if (this.slotIndex < 0 || this.slotIndex >= 18) {
            return;
         }

         // Empty id clears the slot; otherwise the ability must be registered
         // and owned by the sender's current hero.
         if (!this.abilityId.isEmpty()) {
            if (ViltrumiteAbilities.get(this.abilityId) == null || !HeroRegistry.get(player).ownsAbility(this.abilityId)) {
               return;
            }
         }

         abilityUser.setAbilityInSlot(this.slotIndex, this.abilityId);
      });
      context.setPacketHandled(true);
   }
}
