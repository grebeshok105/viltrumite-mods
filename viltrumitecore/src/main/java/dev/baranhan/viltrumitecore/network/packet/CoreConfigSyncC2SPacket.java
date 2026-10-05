package dev.baranhan.viltrumitecore.network.packet;

import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import dev.baranhan.viltrumitecore.util.ViltrumiteStatHolder;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class CoreConfigSyncC2SPacket {
   private final float baseDamage;
   private final float damageReductionPercent;
   private final float damageIgnoreThreshold;
   private final float healFactor;
   private final float punchBlockDropChance;
   private final float dashBlockDropChance;
   private final boolean shouldAskRace;
   private final boolean isViltrumiteByDefault;
   private final double spaceLimitY;
   private final float meteorSpawnChancePercent;
   private final double meteorMinDistance;
   private final float worldEventChancePercent;
   private final int worldEventCooldownMinutes;
   private final boolean bloodEnabled;
   private final boolean shouldHumansBleed;

   public CoreConfigSyncC2SPacket(
      float baseDamage,
      float damageReductionPercent,
      float damageIgnoreThreshold,
      float healFactor,
      float punchBlockDropChance,
      float dashBlockDropChance,
      boolean shouldAskRace,
      boolean isViltrumiteByDefault,
      double spaceLimitY,
      float meteorSpawnChancePercent,
      double meteorMinDistance,
      float worldEventChancePercent,
      int worldEventCooldownMinutes,
      boolean bloodEnabled,
      boolean shouldHumansBleed
   ) {
      this.baseDamage = baseDamage;
      this.damageReductionPercent = damageReductionPercent;
      this.damageIgnoreThreshold = damageIgnoreThreshold;
      this.healFactor = healFactor;
      this.punchBlockDropChance = punchBlockDropChance;
      this.dashBlockDropChance = dashBlockDropChance;
      this.shouldAskRace = shouldAskRace;
      this.isViltrumiteByDefault = isViltrumiteByDefault;
      this.spaceLimitY = spaceLimitY;
      this.meteorSpawnChancePercent = meteorSpawnChancePercent;
      this.meteorMinDistance = meteorMinDistance;
      this.worldEventChancePercent = worldEventChancePercent;
      this.worldEventCooldownMinutes = worldEventCooldownMinutes;
      this.bloodEnabled = bloodEnabled;
      this.shouldHumansBleed = shouldHumansBleed;
   }

   public CoreConfigSyncC2SPacket(FriendlyByteBuf buf) {
      this.baseDamage = buf.readFloat();
      this.damageReductionPercent = buf.readFloat();
      this.damageIgnoreThreshold = buf.readFloat();
      this.healFactor = buf.readFloat();
      this.punchBlockDropChance = buf.readFloat();
      this.dashBlockDropChance = buf.readFloat();
      this.shouldAskRace = buf.readBoolean();
      this.isViltrumiteByDefault = buf.readBoolean();
      this.spaceLimitY = buf.readDouble();
      this.meteorSpawnChancePercent = buf.readFloat();
      this.meteorMinDistance = buf.readDouble();
      this.worldEventChancePercent = buf.readFloat();
      this.worldEventCooldownMinutes = buf.readInt();
      this.bloodEnabled = buf.readBoolean();
      this.shouldHumansBleed = buf.readBoolean();
   }

   public void toBytes(FriendlyByteBuf buf) {
      buf.writeFloat(this.baseDamage);
      buf.writeFloat(this.damageReductionPercent);
      buf.writeFloat(this.damageIgnoreThreshold);
      buf.writeFloat(this.healFactor);
      buf.writeFloat(this.punchBlockDropChance);
      buf.writeFloat(this.dashBlockDropChance);
      buf.writeBoolean(this.shouldAskRace);
      buf.writeBoolean(this.isViltrumiteByDefault);
      buf.writeDouble(this.spaceLimitY);
      buf.writeFloat(this.meteorSpawnChancePercent);
      buf.writeDouble(this.meteorMinDistance);
      buf.writeFloat(this.worldEventChancePercent);
      buf.writeInt(this.worldEventCooldownMinutes);
      buf.writeBoolean(this.bloodEnabled);
      buf.writeBoolean(this.shouldHumansBleed);
   }

   public boolean handle(Supplier<Context> supplier) {
      Context context = supplier.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player != null && player.hasPermissions(2)) {
            if (player instanceof ViltrumiteStatHolder stats) {
               stats.setBaseDamage(this.baseDamage);
               stats.setDamageReduction(this.damageReductionPercent);
               stats.setDamageIgnoreThreshold(this.damageIgnoreThreshold);
               stats.setHealFactor(this.healFactor);
            }

            ViltrumiteCoreConfig.INSTANCE.punchBlockDropChance = this.punchBlockDropChance;
            ViltrumiteCoreConfig.INSTANCE.dashBlockDropChance = this.dashBlockDropChance;
            ViltrumiteCoreConfig.INSTANCE.shouldAskRace = this.shouldAskRace;
            ViltrumiteCoreConfig.INSTANCE.isViltrumiteByDefault = this.isViltrumiteByDefault;
            ViltrumiteCoreConfig.INSTANCE.spaceLimitY = this.spaceLimitY;
            ViltrumiteCoreConfig.INSTANCE.meteorSpawnChancePercent = this.meteorSpawnChancePercent;
            ViltrumiteCoreConfig.INSTANCE.meteorMinDistance = this.meteorMinDistance;
            ViltrumiteCoreConfig.INSTANCE.worldEventChancePercent = this.worldEventChancePercent;
            ViltrumiteCoreConfig.INSTANCE.worldEventCooldownMinutes = this.worldEventCooldownMinutes;
            ViltrumiteCoreConfig.INSTANCE.bloodEnabled = this.bloodEnabled;
            ViltrumiteCoreConfig.INSTANCE.shouldHumansBleed = this.shouldHumansBleed;
            ViltrumiteCoreConfig.save();
         }
      });
      return true;
   }
}
