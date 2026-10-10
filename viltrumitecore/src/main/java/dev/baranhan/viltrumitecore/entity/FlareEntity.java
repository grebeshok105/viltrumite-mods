package dev.baranhan.viltrumitecore.entity;

import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Iron Man countermeasure flare (spec §11.3): short-lived bright point with
 * gravity and drag, {@link IronManRules#FLARE_LIFE} ticks, no damage. Drawn as
 * pixel VFX (client/ironman/FlareRenderer); homing projectiles chase it.
 */
public class FlareEntity extends Entity {
   private int life;

   public FlareEntity(EntityType<? extends FlareEntity> type, Level level) {
      super(type, level);
   }

   public static FlareEntity launch(Level level, Vec3 from, Vec3 velocity) {
      FlareEntity flare = new FlareEntity(ViltrumiteEntities.FLARE.get(), level);
      flare.setPos(from);
      flare.setDeltaMovement(velocity);
      level.addFreshEntity(flare);
      return flare;
   }

   public int life() {
      return this.life;
   }

   @Override
   protected void defineSynchedData() {
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 motion = this.getDeltaMovement().scale(0.92).add(0.0, -0.03, 0.0);
      this.setDeltaMovement(motion);
      this.move(MoverType.SELF, motion);
      if (this.onGround()) {
         this.setDeltaMovement(motion.multiply(0.5, -0.3, 0.5));
      }

      if (++this.life >= IronManRules.FLARE_LIFE && !this.level().isClientSide()) {
         this.discard();
      }
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 128.0 * 128.0;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }
}
