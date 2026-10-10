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
   /**
    * Entering (owner walks in from the front): approach the front point
    * 0–14, turn round 14–24, step back into the shell 24–32 (the owner is
    * snapped exactly onto the suit at {@link #ENTER_SEAL}), then the plates
    * close one group after another 32–46: legs, arms, chest, faceplate.
    */
   public static final int ENTER_TICKS = 46;
   public static final int ENTER_APPROACH = 14;
   public static final int ENTER_TURN = 24;
   public static final int ENTER_SEAL = 32;
   /** Ticks the closed shell stays after the mark is on the owner (hidden by the renderer once the owner shows it). */
   public static final int SEAL_LINGER = 4;
   /** Plates swing open while the owner approaches. */
   public static final int ENTER_DOORS_OPEN = 8;
   /** Closing order (spec §12.6): legs, arms, chest, faceplate; 4 ticks each, overlapping by one. */
   public static final int[] ENTER_CLOSE_FROM = {32, 35, 38, 41};
   public static final int ENTER_CLOSE_TICKS = 4;
   public static final int GROUP_LEGS = 0;
   public static final int GROUP_ARMS = 1;
   public static final int GROUP_CHEST = 2;
   public static final int GROUP_FACE = 3;
   /** Where the owner stands before turning round: this far in front of the suit. */
   public static final double ENTER_FRONT = 0.85;
   /** Entering only from the front: max horizontal distance and min cos of the angle off the suit's facing. */
   public static final double ENTER_FRONT_RANGE = 3.0;
   public static final double ENTER_FRONT_COS = 0.34;
   public static final int LEAVE_TICKS = 60;
   public static final double ENTER_REACH = 4.5;
   public static final double LEAVE_RANGE = 96.0;
   private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(EmptySuitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
   private static final EntityDataAccessor<Byte> MARK = SynchedEntityData.defineId(EmptySuitEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(EmptySuitEntity.class, EntityDataSerializers.BYTE);
   private int phaseTicks;
   /** Server: the mark is on the owner already; the shell lingers (still drawn until the owner's snapshot shows the mark). */
   private boolean sealed;
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
         case ENTERING -> this.tickEntering();
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

   /** The owner walks in: plates open, the owner turns and steps back in, the plates close (ENTER_TICKS). */
   public void startEntering() {
      this.setPhase(Phase.ENTERING);
   }

   private void tickEntering() {
      if (this.sealed) {
         // Kept a few ticks after the mark went on: the entity removal must not reach the client
         // before the owner's mark snapshot, or Tony's own skin flashes for a frame.
         if (this.phaseTicks >= ENTER_TICKS + SEAL_LINGER) {
            this.discard();
         }

         return;
      }

      ServerPlayer owner = IronManOwned.owner(this, this.ownerId());
      IronManState state = owner == null ? null : IronManState.of(owner);
      if (state == null || state.enteringSuitId != this.getId()) {
         // Owner gone or interrupted: the suit stays standing (or flies home without an owner).
         this.setPhase(Phase.STANDING);
         if (owner != null && state != null) {
            IronManMarks.abortEntering(owner, state, this);
         } else {
            this.leave();
         }

         return;
      }

      if (this.phaseTicks == ENTER_SEAL) {
         // The client walked the owner here already; this only removes the last few millimetres.
         owner.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
         owner.setYHeadRot(this.getYRot());
         owner.setYBodyRot(this.getYRot());
      }

      if (this.phaseTicks >= ENTER_TICKS) {
         IronManMarks.finishEntering(owner, state, this);
         dev.baranhan.viltrumitecore.hero.HeroRegistry.syncSnapshot(owner);
         this.sealed = true;
      }
   }

   /** Unit vector the suit faces (its front). */
   public Vec3 facing() {
      return Vec3.directionFromRotation(0.0F, this.getYRot());
   }

   /** Spot in front of the suit where the owner turns round. */
   public Vec3 frontPoint() {
      return this.position().add(this.facing().scale(ENTER_FRONT));
   }

   /** True when {@code at} is in front of the suit, close enough to walk in (spec §12.6: only from the front). */
   public boolean inFront(Vec3 at) {
      double dx = at.x - this.getX();
      double dz = at.z - this.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist > ENTER_FRONT_RANGE) {
         return false;
      }

      if (dist < 1.0E-3) {
         return false;
      }

      Vec3 facing = this.facing();
      return (facing.x * dx + facing.z * dz) / dist > ENTER_FRONT_COS;
   }

   /** Open fraction 0..1 of a plate group at an entering time (ticks, fractional). */
   public static float enterDoor(int group, float t) {
      if (t < ENTER_DOORS_OPEN) {
         return easeInOut(Math.max(0.0F, t) / ENTER_DOORS_OPEN);
      }

      float from = ENTER_CLOSE_FROM[group];
      if (t <= from) {
         return 1.0F;
      }

      return 1.0F - easeInOut(Math.min(1.0F, (t - from) / ENTER_CLOSE_TICKS));
   }

   private static float easeInOut(float x) {
      return x < 0.5F ? 4.0F * x * x * x : 1.0F - (float)Math.pow(-2.0F * x + 2.0F, 3.0) / 2.0F;
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
