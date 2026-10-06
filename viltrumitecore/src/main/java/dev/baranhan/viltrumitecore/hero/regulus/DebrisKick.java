package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroDestruction;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.BlockVFXS2CPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Debris Kick (spec 7): 44t cast. Slowness I from start until the 14t strike;
 * at 11t debris only rises out of the ground (visual). At 14t nine hitscan
 * rays fan out in a 35 degree cone, range 16; each ray pierces at most 3
 * blocks, hits the first living target on its path and shoves it away.
 * Damage falls 7 -> 2 by distance and scales by the heart bonus. Cooldown
 * 400 sampled at the event; after the event the tail of the animation no
 * longer locks actions (spec 7.1/13.2).
 */
public final class DebrisKick {
   private static final double RAY_HITBOX_INFLATE = 0.3;
   private static final double KNOCKBACK = 1.0;
   private static final double KNOCKBACK_UP = 0.3;
   private static final int SLOWNESS_REFRESH = 5;

   private DebrisKick() {
   }

   public static void start(ServerPlayer player, RegulusState state) {
      if (state.busy()) {
         return;
      }

      state.beginAction(RegulusHero.ACTION_DEBRIS, RegulusRules.DEBRIS_ANIM_TICKS, RegulusRules.DEBRIS_EVENT_TICK, RegulusRules.DEBRIS_EVENT_TICK);
   }

   /** The strike fires exactly once at the 14t event (spec 7.1). */
   static boolean shouldFire(RegulusState state) {
      return RegulusHero.ACTION_DEBRIS.equals(state.actionId)
         && !state.eventFired
         && state.actionElapsed >= state.actionEventTick;
   }

   static boolean rising(RegulusState state) {
      return RegulusHero.ACTION_DEBRIS.equals(state.actionId)
         && !state.eventFired
         && state.actionElapsed == RegulusRules.DEBRIS_RISE_TICK;
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      if (!RegulusHero.ACTION_DEBRIS.equals(state.actionId) || state.eventFired) {
         return;
      }

      // Slowness I from the start of the animation until the strike lands.
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_REFRESH, 0));
      if (rising(state)) {
         riseVisual(player);
      }

      if (shouldFire(state)) {
         fire(player, state);
      }
   }

   private static void fire(ServerPlayer player, RegulusState state) {
      state.eventFired = true;
      state.startCooldown(RegulusAbilities.DEBRIS_KICK, RegulusRules.DEBRIS_COOLDOWN);
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      int hearts = state.hearts();
      for (double[] offset : RegulusRules.debrisRayOffsets()) {
         castRay(player, level, hearts, offset[0], offset[1]);
      }

      strikeVisual(player, level);
   }

   /**
    * One hitscan ray: find the nearest living target on it, walk the voxel
    * cells in front of that target, destroy the leading cells within the
    * 3-block quota, then apply the distance-scaled hit and the shove.
    */
   private static void castRay(ServerPlayer player, ServerLevel level, int hearts, double yawOffset, double pitchOffset) {
      Vec3 origin = player.getEyePosition();
      Vec3 dir = viewVector(player.getXRot() + (float)pitchOffset, player.getYRot() + (float)yawOffset);
      Vec3 far = origin.add(dir.scale(RegulusRules.DEBRIS_RANGE));

      AABB sweep = new AABB(origin, far).inflate(1.0);
      LivingEntity hit = null;
      double hitDist = RegulusRules.DEBRIS_RANGE;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player && e.isAlive())) {
         Optional<Vec3> entry = target.getBoundingBox().inflate(RAY_HITBOX_INFLATE).clip(origin, far);
         if (entry.isPresent()) {
            double d = entry.get().distanceTo(origin);
            if (d < hitDist) {
               hitDist = d;
               hit = target;
            }
         }
      }

      List<BlockPos> solids = collectSolids(level, origin, dir, hitDist);
      int[] cells = new int[solids.size()];
      for (int i = 0; i < cells.length; i++) {
         cells[i] = HeroDestruction.canDestroy(level, solids.get(i))
            ? RegulusRules.DEBRIS_BLOCK_BREAKABLE
            : RegulusRules.DEBRIS_BLOCK_UNBREAKABLE;
      }

      // The cells the ray passes are exactly the ones it must destroy; the
      // target is hit only if the ray passes every solid cell in front of it.
      int traversal = RegulusRules.debrisRayTraversal(cells, cells.length);
      for (int i = 0; i < traversal; i++) {
         HeroDestruction.destroyBlock(level, solids.get(i));
      }

      if (hit != null && traversal == cells.length) {
         applyHit(player, hit, hitDist, hearts);
      }
   }

   /**
    * Ordered voxel cells containing a non-air block, from just past the eye
    * cell up to maxDist along the ray (Amanatides-Woo traversal).
    */
   private static List<BlockPos> collectSolids(ServerLevel level, Vec3 origin, Vec3 dir, double maxDist) {
      List<BlockPos> solids = new ArrayList<>();
      int cx = Mth.floor(origin.x);
      int cy = Mth.floor(origin.y);
      int cz = Mth.floor(origin.z);
      int stepX = dir.x > 0.0 ? 1 : -1;
      int stepY = dir.y > 0 ? 1 : -1;
      int stepZ = dir.z > 0 ? 1 : -1;
      double tDeltaX = dir.x != 0.0 ? Math.abs(1.0 / dir.x) : Double.MAX_VALUE;
      double tDeltaY = dir.y != 0.0 ? Math.abs(1.0 / dir.y) : Double.MAX_VALUE;
      double tDeltaZ = dir.z != 0.0 ? Math.abs(1.0 / dir.z) : Double.MAX_VALUE;
      double tMaxX = dir.x != 0.0 ? (dir.x > 0.0 ? cx + 1 - origin.x : origin.x - cx) * tDeltaX : Double.MAX_VALUE;
      double tMaxY = dir.y != 0.0 ? (dir.y > 0.0 ? cy + 1 - origin.y : origin.y - cy) * tDeltaY : Double.MAX_VALUE;
      double tMaxZ = dir.z != 0.0 ? (dir.z > 0.0 ? cz + 1 - origin.z : origin.z - cz) * tDeltaZ : Double.MAX_VALUE;

      while (true) {
         double t;
         if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
            cx += stepX;
            t = tMaxX;
            tMaxX += tDeltaX;
         } else if (tMaxY <= tMaxZ) {
            cy += stepY;
            t = tMaxY;
            tMaxY += tDeltaY;
         } else {
            cz += stepZ;
            t = tMaxZ;
            tMaxZ += tDeltaZ;
         }

         if (t > maxDist) {
            break;
         }

         BlockPos pos = new BlockPos(cx, cy, cz);
         if (!level.getBlockState(pos).isAir()) {
            solids.add(pos);
         }
      }

      return solids;
   }

   private static void applyHit(ServerPlayer player, LivingEntity target, double dist, int hearts) {
      float amount = RegulusRules.debrisDamage(dist, hearts);
      // All nine rays are independent hits: lift hurt invulnerability for this
      // hit only and restore it right after, never globally (plan Task 3).
      int savedInvulnerable = target.invulnerableTime;
      target.invulnerableTime = 0;
      boolean landed = target.hurt(player.damageSources().playerAttack(player), amount);
      target.invulnerableTime = savedInvulnerable;
      if (!landed) {
         return;
      }

      // Knockback away from Regulus; hero policy protects control-immune
      // targets (an active Lion's Heart cannot be moved by the world).
      if (HeroRegistry.allowsExternalControl(target, ControlKind.IMPULSE)) {
         Vec3 away = target.position().subtract(player.position());
         Vec3 push = new Vec3(away.x, 0.0, away.z);
         if (push.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            push = new Vec3(look.x, 0.0, look.z);
         }

         // Set, not add: nine simultaneous rays must not compound the shove.
         push = push.normalize().scale(KNOCKBACK);
         target.setDeltaMovement(push.x, KNOCKBACK_UP, push.z);
         target.hasImpulse = true;
         if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
         }
      }
   }

   /** At 11t debris only lifts out of the ground around Regulus (spec 7.1). */
   private static void riseVisual(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      BlockState ground = level.getBlockState(player.blockPosition().below());
      if (ground.isAir()) {
         ground = Blocks.DIRT.defaultBlockState();
      }

      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
      for (int i = 0; i < 24; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double r = 0.4 + level.random.nextDouble() * 1.2;
         level.sendParticles(debris, player.getX() + Math.cos(angle) * r, player.getY() + 0.1, player.getZ() + Math.sin(angle) * r, 1, 0.0, 0.15, 0.0, 0.02);
      }

      sendRing(player, player.position().add(0.0, 0.15, 0.0));
      level.playSound(null, player.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
   }

   /** Strike feedback: a dusty block-shard cone along the fan plus a ring. */
   private static void strikeVisual(ServerPlayer player, ServerLevel level) {
      Vec3 eye = player.getEyePosition();
      BlockState ground = level.getBlockState(player.blockPosition().below());
      if (ground.isAir()) {
         ground = Blocks.STONE.defaultBlockState();
      }

      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
      for (double[] offset : RegulusRules.debrisRayOffsets()) {
         Vec3 dir = viewVector(player.getXRot() + (float)offset[1], player.getYRot() + (float)offset[0]);
         for (int i = 0; i < 6; i++) {
            double d = 1.0 + level.random.nextDouble() * 4.0;
            Vec3 p = eye.add(dir.scale(d));
            level.sendParticles(debris, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.05);
         }
      }

      level.sendParticles(ParticleTypes.POOF, eye.x, eye.y - 0.2, eye.z, 12, 0.5, 0.2, 0.5, 0.05);
      sendRing(player, eye);
      level.playSound(null, player.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
   }

   private static void sendRing(ServerPlayer player, Vec3 pos) {
      CoreMessages.sendToTracking(new BlockVFXS2CPacket(pos, player.getYRot(), player.getXRot()), player);
      CoreMessages.sendToPlayer(new BlockVFXS2CPacket(pos, player.getYRot(), player.getXRot()), player);
   }

   /** Vanilla Entity#calculateViewVector for a rotated view direction. */
   private static Vec3 viewVector(double pitch, double yaw) {
      double yawRad = Math.toRadians(-yaw);
      double pitchRad = Math.toRadians(pitch);
      double cosY = Math.cos(yawRad);
      double sinY = Math.sin(yawRad);
      double cosP = Math.cos(pitchRad);
      double sinP = Math.sin(pitchRad);
      return new Vec3(sinY * cosP, -sinP, cosY * cosP);
   }
}
