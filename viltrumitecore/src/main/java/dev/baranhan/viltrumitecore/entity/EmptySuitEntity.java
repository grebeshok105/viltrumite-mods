package dev.baranhan.viltrumitecore.entity;

import dev.baranhan.viltrumitecore.hero.ironman.IronManState;
import dev.baranhan.viltrumitecore.hero.ironman.mark.IronManMarks;
import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The empty mark Tony stepped out of (spec §12.5–§12.6): a player-shaped
 * shell in the mark skin. One per owner; the owner enters it with RMB
 * (HeroInteractable). It is not a wall or a shield: no collision, projectiles
 * pass, vanilla picking misses it, it takes no damage. Durability stays in the
 * owner's MarkRoster. It flies away when the owner is gone, far, dead, in
 * another dimension or placed a newer empty suit. Not saved.
 */
public class EmptySuitEntity extends Entity implements HeroInteractable {
   /** Exit animation: open 10, hold 10, close 10 (Tony steps out meanwhile). */
   public static final int OPEN_TICKS = 30;
   public static final int ENTER_TICKS = 10;
   public static final int LEAVE_TICKS = 60;
   public static final double ENTER_REACH = 4.5;
   public static final double LEAVE_RANGE = 96.0;
   private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(EmptySuitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
   private static final EntityDataAccessor<Byte> MARK = SynchedEntityData.defineId(EmptySuitEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(EmptySuitEntity.class, EntityDataSerializers.BYTE);
   private int phaseTicks;
   private int clientPhaseStart;

   public enum Phase {
      /** Just stepped out of: plates open and close again. */
      OPENING,
      STANDING,
      /** The owner walks in: plates open, then the entity is gone. */
      ENTERING,
      LEAVING
   }

   public EmptySuitEntity(EntityType<? extends EmptySuitEntity> type, Level level) {
      super(type, level);
   }

   public static EmptySuitEntity place(ServerPlayer owner, MarkId mark, Vec3 at, float yaw) {
      EmptySuitEntity suit = new EmptySuitEntity(ViltrumiteEntities.EMPTY_SUIT.get(), owner.level());
      suit.entityData.set(OWNER, Optional.of(owner.getUUID()));
      suit.entityData.set(MARK, (byte)mark.ordinal());
      suit.moveTo(at.x, at.y, at.z, yaw, 0.0F);
      suit.setYHeadRot(yaw);
      owner.level().addFreshEntity(suit);
      return suit;
   }

   @Override
   protected void defineSynchedData() {
      this.entityData.define(OWNER, Optional.empty());
      this.entityData.define(MARK, (byte)0);
      this.entityData.define(PHASE, (byte)Phase.OPENING.ordinal());
   }

   public Optional<UUID> ownerId() {
      return this.entityData.get(OWNER);
   }

   @Nullable
   public MarkId mark() {
      return MarkId.byId(this.entityData.get(MARK));
   }

   public Phase phase() {
      byte id = this.entityData.get(PHASE);
      return id >= 0 && id < Phase.values().length ? Phase.values()[id] : Phase.STANDING;
   }

   public int clientPhaseAge() {
      return this.tickCount - this.clientPhaseStart;
   }

   public int phaseTicks() {
      return this.phaseTicks;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (PHASE.equals(key)) {
         this.clientPhaseStart = this.tickCount;
      }
   }

   private void setPhase(Phase phase) {
      this.entityData.set(PHASE, (byte)phase.ordinal());
      this.phaseTicks = 0;
   }

   @Override
   public void tick() {
      super.tick();
      Phase phase = this.phase();
      if (phase == Phase.LEAVING) {
         double up = Math.min(2.5, 0.08 + this.phaseTicks * 0.05);
         this.setDeltaMovement(0.0, up, 0.0);
         this.setPos(this.position().add(0.0, up, 0.0));
      } else {
         Vec3 motion = this.getDeltaMovement().multiply(0.6, 1.0, 0.6).add(0.0, this.onGround() ? 0.0 : -0.08, 0.0);
         this.setDeltaMovement(motion);
         this.move(MoverType.SELF, motion);
         if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
         }
      }

      if (this.level().isClientSide()) {
         return;
      }

      this.phaseTicks++;
      switch (phase) {
         case OPENING -> {
            if (this.phaseTicks >= OPEN_TICKS) {
               this.setPhase(Phase.STANDING);
            }
         }
         case ENTERING -> {
            if (this.phaseTicks >= ENTER_TICKS) {
               this.discard();
            }
         }
         case LEAVING -> {
            if (this.phaseTicks >= LEAVE_TICKS || this.getY() > this.level().getMaxBuildHeight() + 64) {
               this.discard();
            }
         }
         default -> {
         }
      }

      if (phase != Phase.LEAVING && phase != Phase.ENTERING && !this.ownerStillHere()) {
         this.leave();
      }
   }

   private boolean ownerStillHere() {
      ServerPlayer owner = IronManOwned.owner(this, this.ownerId());
      if (owner == null) {
         return false;
      }

      IronManState state = IronManState.of(owner);
      return state != null && state.emptySuitId == this.getId() && owner.distanceToSqr(this) <= LEAVE_RANGE * LEAVE_RANGE;
   }

   /** Fly away (to Veronica / the sky); the owner's roster gets the mark back. */
   public void leave() {
      if (this.phase() == Phase.LEAVING) {
         return;
      }

      this.setPhase(Phase.LEAVING);
      this.noPhysics = true;
      ServerPlayer owner = IronManOwned.owner(this, this.ownerId());
      if (owner != null) {
         IronManMarks.onEmptySuitLeft(owner, this);
      }
   }

   /** The owner walks in: plates open, the entity disappears after ENTER_TICKS. */
   public void startEntering() {
      this.setPhase(Phase.ENTERING);
   }

   @Override
   public boolean canHeroInteract(Player who) {
      if (this.phase() != Phase.STANDING || !this.ownerId().map(who.getUUID()::equals).orElse(false)) {
         return false;
      }

      return IronManMarks.mayEnter(who);
   }

   @Override
   public boolean heroInteract(ServerPlayer who) {
      if (!this.canHeroInteract(who) || who.distanceToSqr(this) > ENTER_REACH * ENTER_REACH || !who.hasLineOfSight(this)) {
         return false;
      }

      return IronManMarks.enterEmptySuit(who, this);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean canBeHitByProjectile() {
      return false;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public boolean canBeCollidedWith() {
      return false;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 128.0 * 128.0;
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
