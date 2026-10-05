package dev.baranhan.viltrumitecore.event;

import dev.baranhan.viltrumitecore.config.ViltrumiteCoreConfig;
import dev.baranhan.viltrumitecore.entity.MeteorEntity;
import dev.baranhan.viltrumitecore.entity.ViltrumiteEntities;
import dev.baranhan.viltrumitecore.world.data.MeteorImpactData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE
)
public class MeteorSpawnEvent {
   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent event) {
      if (event.phase == Phase.END
         && event.level instanceof ServerLevel serverLevel
         && serverLevel.dimension() == ServerLevel.OVERWORLD
         && serverLevel.random.nextFloat() < ViltrumiteCoreConfig.INSTANCE.meteorSpawnChancePercent * 0.01F) {
         spawnMeteor(serverLevel);
      }
   }

   private static void spawnMeteor(ServerLevel level) {
      if (!level.players().isEmpty()) {
         ServerPlayer player = (ServerPlayer)level.players().get(level.random.nextInt(level.players().size()));
         int offsetX = level.random.nextInt(200) - 100;
         int offsetZ = level.random.nextInt(200) - 100;
         BlockPos targetPos = player.blockPosition().offset(offsetX, 0, offsetZ);
         int groundY = level.getHeight(Types.WORLD_SURFACE, targetPos.getX(), targetPos.getZ());
         BlockPos impactTarget = new BlockPos(targetPos.getX(), groundY, targetPos.getZ());
         MeteorImpactData data = MeteorImpactData.get(level);
         if (data.isAreaClear(impactTarget, ViltrumiteCoreConfig.INSTANCE.meteorMinDistance)) {
            data.addImpact(impactTarget);
            MeteorEntity meteor = (MeteorEntity)((EntityType)ViltrumiteEntities.METEOR.get()).create(level);
            if (meteor != null) {
               double distanceY = 250.0 - (double)groundY;
               double ticksToFall = distanceY / 1.5;
               double spawnX = (double)impactTarget.getX() - ticksToFall * 0.5;
               double spawnZ = (double)impactTarget.getZ() - ticksToFall * 0.5;
               meteor.moveTo(spawnX, 250.0, spawnZ);
               meteor.setDeltaMovement(new Vec3(0.5, -1.5, 0.5));
               level.addFreshEntity(meteor);
            }
         }
      }
   }
}
