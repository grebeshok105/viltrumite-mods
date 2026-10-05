package dev.baranhan.viltrumitecore.entity;

import dev.baranhan.viltrumitecore.block.BloodStainBlock;
import dev.baranhan.viltrumitecore.block.ViltrumiteBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class BloodDropEntity extends Entity {
   private static final EntityDataAccessor<Boolean> VILTRUMITE = SynchedEntityData.defineId(BloodDropEntity.class, EntityDataSerializers.BOOLEAN);
   private static final double GRAVITY = 0.055;
   private static final double DRAG = 0.995;
   private static final int MAX_LIFE = 100;
   private int life;

   public BloodDropEntity(EntityType<? extends BloodDropEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public boolean isViltrumiteBlood() {
      return (Boolean)this.entityData.get(VILTRUMITE);
   }

   public void setViltrumiteBlood(boolean viltrumite) {
      this.entityData.set(VILTRUMITE, viltrumite);
   }

   public static void burst(ServerLevel level, Vec3 origin, Vec3 push, int count, boolean viltrumite) {
      RandomSource random = level.random;

      for (int i = 0; i < count; i++) {
         BloodDropEntity drop = new BloodDropEntity((EntityType<? extends BloodDropEntity>)ViltrumiteEntities.BLOOD_DROP.get(), level);
         drop.setViltrumiteBlood(viltrumite);
         drop.setPos(
            origin.x + (random.nextDouble() - 0.5) * 0.6,
            origin.y + (random.nextDouble() - 0.5) * 0.6,
            origin.z + (random.nextDouble() - 0.5) * 0.6
         );
         Vec3 spread = new Vec3(push.x * 0.35 + (random.nextDouble() - 0.5) * 0.45, 0.0, push.z * 0.35 + (random.nextDouble() - 0.5) * 0.45);
         drop.setDeltaMovement(spread);
         level.addFreshEntity(drop);
      }

      level.sendParticles(
         new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
         origin.x,
         origin.y,
         origin.z,
         Mth.clamp(count * 2, 6, 30),
         0.25,
         0.25,
         0.25,
         0.08
      );
   }

   public void tick() {
      super.tick();
      Vec3 motion = this.getDeltaMovement();
      Vec3 from = this.position();
      Vec3 to = from.add(motion);
      BlockHitResult hit = this.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.ANY, this));
      if (hit.getType() != Type.MISS) {
         this.land(hit);
      } else {
         this.setPos(to.x, to.y, to.z);
         this.setDeltaMovement(motion.add(0.0, -0.055, 0.0).scale(0.995));
         if (++this.life > 100) {
            this.discard();
         }
      }
   }

   private void land(BlockHitResult hit) {
      if (!this.level().isClientSide) {
         ServerLevel server = (ServerLevel)this.level();
         Vec3 point = hit.getLocation();
         if (hit.getDirection() == Direction.UP) {
            placeStain(server, hit.getBlockPos().above(), this.isViltrumiteBlood());
         }

         server.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
            point.x,
            point.y + 0.05,
            point.z,
            3,
            0.06,
            0.01,
            0.06,
            0.02
         );
      }

      this.discard();
   }

   private static void placeStain(ServerLevel level, BlockPos pos, boolean viltrumite) {
      BlockState existing = level.getBlockState(pos);
      if (existing.is((net.minecraft.world.level.block.Block)ViltrumiteBlocks.BLOOD_STAIN.get())) {
         BloodStainBlock.refresh(level, pos, existing);
      } else if (existing.isAir() || existing.canBeReplaced()) {
         BlockState stain = (BlockState)((BlockState)((net.minecraft.world.level.block.Block)ViltrumiteBlocks.BLOOD_STAIN.get())
               .defaultBlockState()
               .setValue(BloodStainBlock.SINK, BloodStainBlock.sinkFor(level, pos)))
            .setValue(BloodStainBlock.VILTRUMITE, viltrumite);
         if (stain.canSurvive(level, pos)) {
            level.setBlock(pos, stain, 3);
         }
      }
   }

   protected void defineSynchedData() {
      this.entityData.define(VILTRUMITE, true);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.life = tag.getInt("Life");
      this.setViltrumiteBlood(tag.getBoolean("Viltrumite"));
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("Life", this.life);
      tag.putBoolean("Viltrumite", this.isViltrumiteBlood());
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }

   public boolean shouldRenderAtSqrDistance(double distanceSqr) {
      return distanceSqr < 4096.0;
   }

   public boolean isPickable() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public boolean shouldBeSaved() {
      return false;
   }
}
