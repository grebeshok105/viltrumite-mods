package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.RegulusFxS2CPacket;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server side of the one-shot Regulus FX packet: everyone who sees the caster gets it. */
public final class RegulusFx {
   private RegulusFx() {
   }

   public static void send(ServerPlayer source, byte kind, Vec3 origin, float power, @Nullable BlockState material, @Nullable float[] points) {
      int stateId = material == null ? 0 : Block.getId(material);
      CoreMessages.sendToTrackingAndSelf(new RegulusFxS2CPacket(kind, origin, power, stateId, points), source);
   }
}
