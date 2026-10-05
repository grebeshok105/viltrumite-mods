package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteCosmeticsPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class SpawnNpcC2SPacket {
   private final String name;
   private final String skin;
   private final String cape;
   private final String model;
   private final int intelligence;
   private final int targetModeOrdinal;
   private final float scale;
   private final float baseDamage;
   private final float dmgIgnore;
   private final float healFactor;
   private final float dmgReduction;
   private final float flySpeed;
   private final float throttle;

   public SpawnNpcC2SPacket(
      String name,
      String skin,
      String cape,
      String model,
      int intelligence,
      int targetModeOrdinal,
      float scale,
      float baseDamage,
      float dmgIgnore,
      float healFactor,
      float dmgReduction,
      float flySpeed,
      float throttle
   ) {
      this.name = name;
      this.skin = skin;
      this.cape = cape;
      this.model = model;
      this.intelligence = intelligence;
      this.targetModeOrdinal = targetModeOrdinal;
      this.scale = scale;
      this.baseDamage = baseDamage;
      this.dmgIgnore = dmgIgnore;
      this.healFactor = healFactor;
      this.dmgReduction = dmgReduction;
      this.flySpeed = flySpeed;
      this.throttle = throttle;
   }

   public SpawnNpcC2SPacket(FriendlyByteBuf buf) {
      this.name = buf.readUtf();
      this.skin = buf.readUtf();
      this.cape = buf.readUtf();
      this.model = buf.readUtf();
      this.intelligence = buf.readInt();
      this.targetModeOrdinal = buf.readInt();
      this.scale = buf.readFloat();
      this.baseDamage = buf.readFloat();
      this.dmgIgnore = buf.readFloat();
      this.healFactor = buf.readFloat();
      this.dmgReduction = buf.readFloat();
      this.flySpeed = buf.readFloat();
      this.throttle = buf.readFloat();
   }

   public void toBytes(FriendlyByteBuf buf) {
      buf.writeUtf(this.name);
      buf.writeUtf(this.skin);
      buf.writeUtf(this.cape);
      buf.writeUtf(this.model);
      buf.writeInt(this.intelligence);
      buf.writeInt(this.targetModeOrdinal);
      buf.writeFloat(this.scale);
      buf.writeFloat(this.baseDamage);
      buf.writeFloat(this.dmgIgnore);
      buf.writeFloat(this.healFactor);
      buf.writeFloat(this.dmgReduction);
      buf.writeFloat(this.flySpeed);
      buf.writeFloat(this.throttle);
   }

   public boolean handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(
         () -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
               ServerLevel world = player.serverLevel();
               ViltrumiteFakePlayer.TargetMode mode = ViltrumiteFakePlayer.TargetMode.values()[this.targetModeOrdinal
                  % ViltrumiteFakePlayer.TargetMode.values().length];
               ViltrumiteFakePlayer npc = new ViltrumiteFakePlayer(
                  player.server,
                  world,
                  this.name,
                  this.scale,
                  this.intelligence,
                  mode,
                  this.baseDamage,
                  this.dmgIgnore,
                  this.dmgReduction,
                  this.healFactor,
                  this.flySpeed,
                  this.throttle
               );
               npc.setPos(player.getX(), player.getY(), player.getZ());
               npc.setYRot(player.getYRot());
               npc.setXRot(player.getXRot());
               npc.yHeadRot = player.yHeadRot;
               if (npc instanceof ViltrumiteCosmeticsPlayer cosmetics) {
                  cosmetics.setViltrumiteSkin(this.skin);
                  cosmetics.setViltrumiteCape(this.cape);
                  cosmetics.setViltrumiteModel(this.model);
               }

               player.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(Action.ADD_PLAYER, npc));
               world.addFreshEntity(npc);
               player.sendSystemMessage(
                  Component.literal(
                     "\u00a7aClone Spawned! \u00a7f[Name: \u00a7e"
                        + this.name
                        + "\u00a7f, Scale: \u00a7e"
                        + this.scale
                        + "x\u00a7f, Intel: \u00a7e"
                        + this.intelligence
                        + "\u00a7f]"
                  )
               );
            }
         }
      );
      context.setPacketHandled(true);
      return true;
   }
}
