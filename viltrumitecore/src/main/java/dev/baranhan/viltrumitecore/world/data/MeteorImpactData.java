package dev.baranhan.viltrumitecore.world.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class MeteorImpactData extends SavedData {
   private final List<BlockPos> impactLocations = new ArrayList<>();

   public static MeteorImpactData load(CompoundTag nbt) {
      MeteorImpactData data = new MeteorImpactData();
      long[] savedArray = nbt.getLongArray("Impacts");

      for (long posLong : savedArray) {
         data.impactLocations.add(BlockPos.of(posLong));
      }

      return data;
   }

   public CompoundTag save(CompoundTag nbt) {
      long[] arrayToSave = new long[this.impactLocations.size()];

      for (int i = 0; i < this.impactLocations.size(); i++) {
         arrayToSave[i] = this.impactLocations.get(i).asLong();
      }

      nbt.putLongArray("Impacts", arrayToSave);
      return nbt;
   }

   public static MeteorImpactData get(ServerLevel level) {
      return (MeteorImpactData)level.getServer().overworld().getDataStorage().computeIfAbsent(MeteorImpactData::load, MeteorImpactData::new, "viltrumite_meteor_impacts");
   }

   public void addImpact(BlockPos pos) {
      this.impactLocations.add(pos);
      this.setDirty();
   }

   public boolean isAreaClear(BlockPos target, double minDistance) {
      double minDistanceSq = minDistance * minDistance;

      for (BlockPos pos : this.impactLocations) {
         double dx = (double)(pos.getX() - target.getX());
         double dz = (double)(pos.getZ() - target.getZ());
         if (dx * dx + dz * dz < minDistanceSq) {
            return false;
         }
      }

      return true;
   }
}
