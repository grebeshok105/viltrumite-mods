package dev.baranhan.viltrumitecore.hero.control;

import dev.baranhan.viltrumitecore.hero.CleanupReason;
import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.hero.regulus.RegulusRules;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * World-owned control effects (PULL, FREEZE, STASIS, domes, frozen
 * projectiles). Targets are soft: release fully restores the victim's saved
 * position/flags and then applies the queued damage once, in release order.
 * The legacy viltrumite grab is a separate tag-based system; this manager only
 * consults it for precedence.
 */
public final class ControlManager {
   private static final Map<ServerLevel, ControlManager> INSTANCES = new WeakHashMap<>();

   private final Map<UUID, ControlRecord> controls = new HashMap<>();
   private final Map<UUID, DomeRecord> domes = new HashMap<>();
   private final Map<UUID, ProjectileFreezeRecord> frozenProjectiles = new HashMap<>();

   private ControlManager() {
   }

   public static ControlManager get(ServerLevel level) {
      return INSTANCES.computeIfAbsent(level, key -> new ControlManager());
   }

   // --- target queries -----------------------------------------------------

   /** Target anchored in place (FREEZE/STASIS) or teleported by a dome. */
   public boolean isAnchored(@Nullable LivingEntity target) {
      if (target == null) {
         return false;
      }

      ControlRecord record = this.controls.get(target.getUUID());
      return record != null && (record.kind == ControlKind.FREEZE || record.kind == ControlKind.STASIS);
   }

   /** Any world-owned control, including the non-anchoring PULL. */
   public boolean isControlled(@Nullable LivingEntity target) {
      return target != null && this.controls.containsKey(target.getUUID());
   }

   /** Mod flight must be denied (anchored controls and domes). */
   public boolean preventsFlight(@Nullable LivingEntity target) {
      if (target == null) {
         return false;
      }

      if (this.isAnchored(target)) {
         return true;
      }

      if (target instanceof ServerPlayer player) {
         for (DomeRecord dome : this.domes.values()) {
            if (dome.captured().contains(player.getUUID())) {
               return true;
            }
         }
      }

      return false;
   }

   @Nullable
   public ControlKind controlKind(@Nullable LivingEntity target) {
      ControlRecord record = target == null ? null : this.controls.get(target.getUUID());
      return record == null ? null : record.kind;
   }

   @Nullable
   public UUID effectIdOf(@Nullable LivingEntity target) {
      ControlRecord record = target == null ? null : this.controls.get(target.getUUID());
      return record == null ? null : record.effectId;
   }

   /** Every control currently cast by this player (for "one Mania at a time" checks). */
   public boolean hasControlFrom(UUID caster, ControlKind kind) {
      for (ControlRecord record : this.controls.values()) {
         if (record.caster.equals(caster) && record.kind == kind) {
            return true;
         }
      }

      return false;
   }

   // --- acquire / transition / release -------------------------------------

   /**
    * Acquire a control on a target. Fails fast when the target already carries
    * the same kind or when the target's hero policy refuses the kind.
    */
   public boolean tryAcquire(LivingEntity target, UUID caster, UUID effectId, ControlKind kind) {
      if (!this.isAllowedOnTarget(target, kind)) {
         return false;
      }

      ControlRecord existing = this.controls.get(target.getUUID());
      if (existing != null) {
         return false;
      }

      // Precedence: a legacy viltrumite grab on the target rejects FREEZE/STASIS
      // outright; PULL does not (it ends the channel first — see Mania).
      if ((kind == ControlKind.FREEZE || kind == ControlKind.STASIS) && target.getTags().contains("ViltrumiteGrabbed")) {
         return false;
      }

      ControlRecord record = new ControlRecord(target, caster, effectId, kind);
      this.controls.put(target.getUUID(), record);
      record.anchor(target);
      return true;
   }

   /**
    * Atomic transition: keep the ownership record (saved values stay stored)
    * while switching the applied kind, e.g. PULL -> FREEZE.
    */
   public boolean transition(UUID targetId, UUID effectId, ControlKind next) {
      ControlRecord record = this.controls.get(targetId);
      if (record == null || !record.effectId.equals(effectId)) {
         return false;
      }

      record.unapply();
      record.kind = next;
      ServerLevel level = this.firstLevel();
      LivingEntity target = record.targetId == null ? null : find(level, record.targetId);
      if (target != null) {
         record.apply(target);
      }

      return true;
   }

   /** One release order: remove ownership first, then restore, then damage once. */
   public void release(UUID targetId, UUID effectId, ReleaseReason reason) {
      ControlRecord record = this.controls.remove(targetId);
      if (record == null || !record.effectId.equals(effectId)) {
         if (record != null) {
            this.controls.put(targetId, record);
         }

         return;
      }

      ServerLevel level = this.firstLevel();
      LivingEntity target = find(level, record.targetId);
      if (target != null) {
         record.restore(target);
         if (record.queuedDamage > 0.0F) {
            HeroDamage.applyCleanDamage(target, record.lastDamageSource == null ? target.damageSources().generic() : record.lastDamageSource, record.queuedDamage);
         }
      }
   }

   /**
    * Caster lifecycle: DEATH/DISCONNECT end channels and freezes and PULLs; a
    * HERO_CHANGE also removes the caster's domes. Dome records are world-owned
    * and survive a dead caster.
    */
   public void cleanupCaster(UUID caster, CleanupReason reason) {
      for (Map.Entry<UUID, ControlRecord> entry : new HashMap<>(this.controls).entrySet()) {
         ControlRecord record = entry.getValue();
         if (record.caster.equals(caster)) {
            this.release(entry.getKey(), record.effectId, reason == CleanupReason.HERO_CHANGE ? ReleaseReason.HERO_CHANGE : ReleaseReason.CASTER_DISCONNECT);
         }
      }

      if (reason != CleanupReason.DEATH) {
         this.domes.values().removeIf(dome -> dome.caster().equals(caster));
      }

      this.frozenProjectiles.values().forEach(record -> record.casters.remove(caster));
      this.frozenProjectiles.values().removeIf(record -> record.casters.isEmpty());
   }

   /** Target lifecycle: detach the record entirely (targets vanish on death). */
   public void cleanupTarget(UUID targetId) {
      this.controls.remove(targetId);
   }

   // --- deferred damage -----------------------------------------------------

   /**
    * Queue one blocked external hit against an anchored victim. The caller has
    * already evaluated the payable amount; the cap keeps it bounded.
    */
   public boolean queueDamage(LivingEntity target, DamageSource source, float amount) {
      ControlRecord record = this.controls.get(target.getUUID());
      if (record == null) {
         return false;
      }

      float cap = record.kind == ControlKind.STASIS
         ? RegulusRules.domeDeferredCap(target.getMaxHealth())
         : RegulusRules.freezeDeferredCap(target.getMaxHealth());
      record.queuedDamage = Math.min(cap, record.queuedDamage + amount);
      record.lastDamageSource = source;
      return true;
   }

   public float queuedDamage(LivingEntity target) {
      ControlRecord record = this.controls.get(target.getUUID());
      return record == null ? 0.0F : record.queuedDamage;
   }

   // --- domes ---------------------------------------------------------------

   public void addDome(DomeRecord dome) {
      this.domes.put(dome.id(), dome);
   }

   @Nullable
   public DomeRecord dome(UUID id) {
      return this.domes.get(id);
   }

   public Map<UUID, DomeRecord> domes() {
      return Map.copyOf(this.domes);
   }

   public boolean isInsideDome(UUID entityId) {
      for (DomeRecord dome : this.domes.values()) {
         if (dome.captured().contains(entityId)) {
            return true;
         }
      }

      return false;
   }

   // --- projectiles ----------------------------------------------------------

   public void freezeProjectile(Projectile projectile, UUID caster) {
      ProjectileFreezeRecord record = this.frozenProjectiles.computeIfAbsent(
         projectile.getUUID(), id -> new ProjectileFreezeRecord(projectile)
      );
      record.casters.add(caster);
      projectile.setNoGravity(true);
      projectile.setDeltaMovement(Vec3.ZERO);
   }

   public void releaseProjectilesFor(UUID caster, Vec3 around, double radius, Vec3 direction) {
      this.frozenProjectiles.values().removeIf(record -> {
         if (!record.casters.remove(caster)) {
            return false;
         }

         Projectile projectile = record.projectile();
         if (projectile != null && !projectile.isRemoved() && record.casters.isEmpty()) {
            projectile.setNoGravity(record.savedNoGravity);
            Vec3 impulse = direction.normalize().scale(RegulusRules.LION_RELEASE_IMPULSE);
            projectile.setDeltaMovement(impulse.x, impulse.y, impulse.z);
            return true;
         }

         return record.casters.isEmpty();
      });
   }

   // --- tick -----------------------------------------------------------------

   public void tick(ServerLevel level) {
      long now = level.getGameTime();
      this.controls.values().removeIf(record -> {
         LivingEntity target = find(level, record.targetId);
         if (target == null || !target.isAlive()) {
            // targets vanish on death: detach, no release semantics
            return true;
         }

         record.apply(target);
         return false;
      });

      this.domes.values().removeIf(dome -> now >= dome.expiresAt());
   }

   // --- internals -------------------------------------------------------------

   private boolean isAllowedOnTarget(LivingEntity target, ControlKind kind) {
      if (target instanceof ServerPlayer player && player instanceof dev.baranhan.viltrumitecore.hero.HeroPlayer heroPlayer) {
         return dev.baranhan.viltrumitecore.hero.HeroRegistry.get(player).allowsExternalControl(target, kind);
      }

      return true;
   }

   private ServerLevel firstLevel() {
      // Controls live inside the ServerLevel they were acquired on; callers keep
      // a lookup table of instances, so use the instance's own level.
      for (Map.Entry<ServerLevel, ControlManager> entry : INSTANCES.entrySet()) {
         if (entry.getValue() == this) {
            return entry.getKey();
         }
      }

      return null;
   }

   @Nullable
   private static LivingEntity find(@Nullable ServerLevel level, UUID id) {
      if (level == null || id == null) {
         return null;
      }

      Entity entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
   }

   /** Saves and re-applies everything a control changed on its victim. */
   static final class ControlRecord {
      final UUID targetId;
      final UUID caster;
      final UUID effectId;
      ControlKind kind;

      Vec3 anchor;
      boolean savedNoGravity;
      boolean savedNoAi;
      float queuedDamage;
      DamageSource lastDamageSource;

      ControlRecord(LivingEntity target, UUID caster, UUID effectId, ControlKind kind) {
         this.targetId = target.getUUID();
         this.caster = caster;
         this.effectId = effectId;
         this.kind = kind;
         this.savedNoGravity = target.isNoGravity();
         this.savedNoAi = target instanceof Mob mob && mob.isNoAi();
         this.anchor = target.position();
      }

      void anchor(LivingEntity target) {
         this.apply(target);
      }

      void apply(LivingEntity target) {
         if (this.kind == ControlKind.FREEZE || this.kind == ControlKind.STASIS) {
            target.setNoGravity(true);
            if (target instanceof Mob mob) {
               mob.setNoAi(true);
            }

            target.setDeltaMovement(Vec3.ZERO);
            if (target.distanceToSqr(this.anchor) > 0.01) {
               if (target instanceof ServerPlayer sp) {
                  sp.connection.teleport(this.anchor.x, this.anchor.y, this.anchor.z, sp.getYRot(), sp.getXRot());
               } else {
                  target.teleportTo(this.anchor.x, this.anchor.y, this.anchor.z);
               }
            }
         }
      }

      void unapply() {
      }

      void restore(LivingEntity target) {
         target.setNoGravity(this.savedNoGravity);
         if (target instanceof Mob mob) {
            mob.setNoAi(this.savedNoAi);
         }

         if (this.kind == ControlKind.FREEZE || this.kind == ControlKind.STASIS) {
            // The freeze clears the victim's own velocity; leave gravity to
            // reassert naturally.
            target.hurtMarked = true;
         }
      }
   }

   static final class ProjectileFreezeRecord {
      private final UUID projectileId;
      private final ServerLevel level;
      final Set<UUID> casters = ConcurrentHashMap.newKeySet();
      boolean savedNoGravity;

      ProjectileFreezeRecord(Projectile projectile) {
         this.projectileId = projectile.getUUID();
         this.level = (ServerLevel)projectile.level();
         this.savedNoGravity = projectile.isNoGravity();
      }

      @Nullable
      Projectile projectile() {
         Entity entity = this.level.getEntity(this.projectileId);
         return entity instanceof Projectile p ? p : null;
      }
   }
}
