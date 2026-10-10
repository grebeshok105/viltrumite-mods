package dev.baranhan.viltrumitecore.entity;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A plate chunk of a broken mark (spec §4.3): falls, bounces, lies and fades
 * after {@link #LIFE} ticks. The client draws the part slice in the mark skin.
 * Purely visual: no collision with entities, no damage, not saved.
 */
public class SuitDebrisEntity extends Entity {
   public static final int LIFE = 200;
   private static final EntityDataAccessor<Byte> MARK = SynchedEntityData.defineId(SuitDebrisEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> PART = SynchedEntityData.defineId(SuitDebrisEntity.class, EntityDataSerializers.BYTE);

   public SuitDebrisEntity(EntityType<? extends SuitDebrisEntity> type, Level level) {
      super(type, level);
   }

   public static SuitDebrisEntity spawn(Level level, Vec3 at, MarkId mark, int part, Vec3 velocity) {
      SuitDebrisEntity debris = new SuitDebrisEntity(ViltrumiteEntities.SUIT_DEBRIS.get(), level);
      debris.entityData.set(MARK, (byte)mark.ordinal());
      debris.entityData.set(PART, (byte)part);
      debris.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
      debris.setDeltaMovement(velocity);
      level.addFreshEntity(debris);
      return debris;
   }

   @Override
   protected void defineSynchedData() {
      this.entityData.define(MARK, (byte)0);
      this.entityData.define(PART, (byte)0);
   }

   @Nullable
   public MarkId mark() {
      return MarkId.byId(this.entityData.get(MARK));
   }

   public int part() {
      return this.entityData.get(PART);
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 motion = this.getDeltaMovement().add(0.0, -0.06, 0.0);
      this.move(MoverType.SELF, motion);
      if (this.onGround()) {
         motion = new Vec3(motion.x * 0.6, Math.abs(motion.y) > 0.12 ? -motion.y * 0.35 : 0.0, motion.z * 0.6);
      } else {
         motion = motion.scale(0.98);
      }

      this.setDeltaMovement(motion);
      if (!this.level().isClientSide() && this.tickCount >= LIFE) {
         this.discard();
      }
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 96.0 * 96.0;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.discard();
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }
}
