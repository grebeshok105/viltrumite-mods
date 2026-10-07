package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.HeroSuperJump;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Bounded hero input edges: action ordinal + pressed. The server re-validates
 * the action against the sender's hero, equipment page and action lock.
 */
public class HeroInputC2SPacket {
   private final int actionOrdinal;
   private final boolean pressed;

   public HeroInputC2SPacket(HeroAction action, boolean pressed) {
      this.actionOrdinal = action.ordinal();
      this.pressed = pressed;
   }

   public HeroInputC2SPacket(FriendlyByteBuf buffer) {
      this.actionOrdinal = buffer.readByte();
      this.pressed = buffer.readBoolean();
   }

   public void toBytes(FriendlyByteBuf buffer) {
      buffer.writeByte(this.actionOrdinal);
      buffer.writeBoolean(this.pressed);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context context = contextSupplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player == null) {
            return;
         }

         HeroAction action = HeroAction.byId(this.actionOrdinal);
         if (action == null) {
            return;
         }

         if (action == HeroAction.JUMP) {
            if (this.pressed) {
               HeroSuperJump.onInput(player);
            }
            return;
         }

         HeroRegistry.get(player).handleInput(player, action, this.pressed);
      });
      context.setPacketHandled(true);
   }
}
