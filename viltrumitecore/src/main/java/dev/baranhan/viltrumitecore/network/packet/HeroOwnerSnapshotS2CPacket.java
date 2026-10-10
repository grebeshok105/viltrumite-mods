package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.hero.HeroOwnerSnapshot;
import dev.baranhan.viltrumitecore.hero.OwnerSection;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Owner-private snapshot: one typed section (ids + expiry ticks) per packet. */
public class HeroOwnerSnapshotS2CPacket {
   private static final int MAX_IDS = 256;
   private final int sectionId;
   private final int[] ids;
   private final int[] expireTicks;

   public HeroOwnerSnapshotS2CPacket(int[] carrierEntityIds) {
      this(OwnerSection.CARRIERS, HeroOwnerSnapshot.Section.of(carrierEntityIds));
   }

   public HeroOwnerSnapshotS2CPacket(OwnerSection section, HeroOwnerSnapshot.Section value) {
      this.sectionId = section.ordinal();
      this.ids = value.ids();
      this.expireTicks = value.expireTicks();
   }

   public HeroOwnerSnapshotS2CPacket(FriendlyByteBuf buffer) {
      this.sectionId = buffer.readVarInt();
      int count = Math.max(0, Math.min(MAX_IDS, buffer.readVarInt()));
      this.ids = new int[count];
      this.expireTicks = new int[count];
      for (int i = 0; i < count; i++) {
         this.ids[i] = buffer.readInt();
         this.expireTicks[i] = buffer.readVarInt();
      }
   }

   public void toBytes(FriendlyByteBuf buffer) {
      buffer.writeVarInt(this.sectionId);
      int count = Math.min(MAX_IDS, this.ids.length);
      buffer.writeVarInt(count);
      for (int i = 0; i < count; i++) {
         buffer.writeInt(this.ids[i]);
         buffer.writeVarInt(Math.max(0, this.expireTicks[i]));
      }
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
         OwnerSection section = OwnerSection.byId(this.sectionId);
         if (section != null) {
            ClientHeroData.setSection(section, new HeroOwnerSnapshot.Section(this.ids, this.expireTicks));
         }
      }));
      context.setPacketHandled(true);
   }
}
