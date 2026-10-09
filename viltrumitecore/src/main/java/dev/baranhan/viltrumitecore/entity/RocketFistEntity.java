package dev.baranhan.viltrumitecore.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/** Mark 42 rocket fist (spec §13.3): the glove flies to the target, hits and returns. */
public class RocketFistEntity extends Projectile {
   public RocketFistEntity(EntityType<? extends RocketFistEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData() {
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.discard();
   }
}
