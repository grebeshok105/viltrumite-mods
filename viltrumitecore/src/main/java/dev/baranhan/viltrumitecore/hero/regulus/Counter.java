package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.hero.HeroDestruction;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.network.packet.RegulusFxS2CPacket;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Counter: Regulus answers. The target is the last attacker (fresh within
 * 240t, within 40 blocks) or, if nobody hit him, whatever he looks at within
 * 24 blocks. Sequence (ticks of the cast):
 * <ul>
 *   <li>0: Regulus blinks in front of the target;</li>
 *   <li>LAUNCH (3): uppercut — the target is thrown COUNTER_LAUNCH_HEIGHT into the sky;</li>
 *   <li>LIFT (20): Regulus appears behind it, in the air;</li>
 *   <li>LIFT+SLAM (27): hammer blow from above — the target dives into the ground
 *   at COUNTER_DIVE_SPEED and smashes a crater where it lands.</li>
 * </ul>
 * The cast can not be interrupted by hits (RegulusHero damage bookkeeping
 * skips it). The 800t cooldown is charged on the slam, hearts sampled then.
 */
public final class Counter {
   private Counter() {
   }

   /** A fresh, live, in-range attacker record. */
   static boolean canActivate(RegulusState state, boolean attackerFresh, boolean attackerAlive, boolean attackerInRange) {
      return state.attackerId != null && attackerFresh && attackerAlive && attackerInRange;
   }

   /** The slam damages the target only while it is still alive and in range. */
   static boolean slamHitsTarget(boolean attackerAlive, boolean attackerInRange) {
      return attackerAlive && attackerInRange;
   }

   /** Latch the target and arm the cast: launch, teleport at LIFT, slam 7t later. */
   static void beginCast(RegulusState state, UUID targetId, Vec3 targetPos) {
      int slamTick = RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS;
      state.beginAction(RegulusHero.ACTION_COUNTER, slamTick + 1, slamTick, slamTick);
      state.actionTargetId = targetId;
      state.actionPoint = targetPos;
      state.counterLiftStart = null;
      state.counterApexY = targetPos.y;
   }

   /** Spot opposite the target's facing — behind its back. */
   static Vec3 behindSpot(Vec3 targetPos, float targetYawDeg) {
      double yawRad = Math.toRadians((double)targetYawDeg);
      Vec3 look = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
      return targetPos.subtract(look.scale(RegulusRules.COUNTER_BEHIND_DISTANCE));
   }

   /** Ease-out launch curve: fast off the fist, hanging at the apex. */
   static double liftProgress(int elapsed) {
      int span = RegulusRules.COUNTER_LIFT_TICKS - RegulusRules.COUNTER_LAUNCH_TICK;
      double t = Mth.clamp((elapsed - RegulusRules.COUNTER_LAUNCH_TICK) / (double)span, 0.0, 1.0);
      return 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t);
   }

   /** Cooldown 800, hearts sampled when the slam lands. */
   static void chargeSlamCooldown(RegulusState state) {
      state.startCooldown(RegulusAbilities.COUNTER, RegulusRules.COUNTER_COOLDOWN);
   }

   public static void start(ServerPlayer player, RegulusState state) {
      LivingEntity target = pickTarget(player, state);
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.viltrumitecore.counter.no_target"), true);
         return;
      }

      beginCast(state, target.getUUID(), target.position());
      // Blink in front of the target: the uppercut needs to be in reach.
      Vec3 toPlayer = player.position().subtract(target.position());
      Vec3 flat = new Vec3(toPlayer.x, 0.0, toPlayer.z);
      Vec3 front = flat.lengthSqr() < 1.0E-4 ? target.position() : target.position().add(flat.normalize().scale(1.3 + target.getBbWidth() * 0.5));
      if (front.distanceToSqr(player.position()) > 4.0) {
         ServerLevel level = player.serverLevel();
         level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.6, 0.3, 0.05);
         player.connection.teleport(front.x, target.getY(), front.z, yawTowards(front, target.position()), 0.0F);
         level.playSound(null, BlockPos.containing(front), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
      }
   }

   /** Recent attacker first; otherwise the living thing in the crosshair. */
   @Nullable
   private static LivingEntity pickTarget(ServerPlayer player, RegulusState state) {
      if (ready(player, state)) {
         LivingEntity attacker = resolveTarget(player.serverLevel(), state.attackerId);
         if (attacker != null) {
            return attacker;
         }
      }

      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(RegulusRules.COUNTER_GAZE_RANGE));
      Vec3 clipped = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
      LivingEntity best = null;
      double nearest = Double.MAX_VALUE;
      for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(eye, clipped).inflate(1.0),
         e -> e != player && e.isAlive() && !e.isSpectator() && (e instanceof Mob || e instanceof Player))) {
         if (state.carriers.contains(entity.getUUID())) {
            continue;
         }

         double distance = DebrisKick.rayDistance(eye, clipped, entity.getBoundingBox().inflate(0.4));
         if (distance < nearest) {
            nearest = distance;
            best = entity;
         }
      }

      return best;
   }

   public static boolean ready(ServerPlayer player, RegulusState state) {
      ServerLevel level = player.serverLevel();
      LivingEntity attacker = resolveTarget(level, state.attackerId);
      boolean fresh = state.attackerId != null && RegulusRules.attackerValid(state.attackerTick, level.getGameTime());
      boolean alive = attacker != null && attacker.isAlive();
      boolean inRange = alive && attacker.distanceToSqr(player) <= RegulusRules.COUNTER_RANGE * RegulusRules.COUNTER_RANGE;
      return canActivate(state, fresh, alive, inRange);
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      ServerLevel level = player.serverLevel();
      if (state.counterDiveTicks >= 0) {
         tickDive(player, state, level);
      }

      if (!RegulusHero.ACTION_COUNTER.equals(state.actionId)) {
         return;
      }

      LivingEntity target = resolveTarget(level, state.actionTargetId);
      boolean alive = target != null && target.isAlive();
      boolean movable = alive && HeroRegistry.allowsExternalControl(target, ControlKind.IMPULSE);
      if (alive) {
         state.actionPoint = target.position();
      }

      int elapsed = state.actionElapsed;
      int slamTick = RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS;
      if (elapsed == RegulusRules.COUNTER_LAUNCH_TICK && alive) {
         uppercut(player, state, level, target);
      }

      if (alive && movable && state.counterLiftStart != null && elapsed > RegulusRules.COUNTER_LAUNCH_TICK && elapsed < slamTick) {
         Vec3 start = state.counterLiftStart;
         double y = start.y + (state.counterApexY - start.y) * liftProgress(elapsed);
         holdAt(target, new Vec3(start.x, y, start.z));
      }

      if (elapsed == RegulusRules.COUNTER_LIFT_TICKS) {
         appearBehind(player, state, level, target);
      }

      if (elapsed > RegulusRules.COUNTER_LIFT_TICKS && elapsed < slamTick && state.actionPoint != null) {
         // Hang in the air behind the target until the blow.
         hover(player);
      }

      if (!state.eventFired && elapsed >= slamTick) {
         slam(player, state, level, target, alive, movable);
      }
   }

   private static void uppercut(ServerPlayer player, RegulusState state, ServerLevel level, LivingEntity target) {
      float damage = RegulusRules.counterDamage(target.getMaxHealth(), state.hearts()) * 0.25F;
      hitClean(player, target, damage);
      Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 24, 0.3, 0.5, 0.3, 0.6);
      level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.1, target.getZ(), 20, 0.5, 0.05, 0.5, 0.15);
      level.playSound(null, BlockPos.containing(at), ViltrumiteCore.REGULUS_PUNCH_HIT.get(), SoundSource.PLAYERS, 2.0F, 0.8F);
      level.playSound(null, BlockPos.containing(at), ViltrumiteCore.REGULUS_AIR_BLADE.get(), SoundSource.PLAYERS, 1.2F, 0.7F);
      if (target instanceof ViltrumiteFlightPlayer flying) {
         flying.stopFlight();
      }

      state.counterLiftStart = target.position();
      state.counterApexY = apexY(level, target, target.position().y + RegulusRules.COUNTER_LAUNCH_HEIGHT);
      RegulusFx.send(player, RegulusFxS2CPacket.COUNTER_LAUNCH, at, 1.0F, null, null);
   }

   /** Stop under a ceiling: the launch never pushes the target into blocks. */
   private static double apexY(ServerLevel level, LivingEntity target, double wantedY) {
      Vec3 head = target.position().add(0.0, target.getBbHeight(), 0.0);
      Vec3 top = new Vec3(head.x, wantedY + target.getBbHeight(), head.z);
      BlockHitResult hit = level.clip(new ClipContext(head, top, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
      if (hit.getType() == HitResult.Type.MISS) {
         return wantedY;
      }

      return Math.max(target.getY(), hit.getLocation().y - target.getBbHeight() - 0.2);
   }

   private static void appearBehind(ServerPlayer player, RegulusState state, ServerLevel level, @Nullable LivingEntity target) {
      Vec3 targetPos = target != null && target.isAlive() ? target.position() : state.actionPoint;
      if (targetPos == null) {
         return;
      }

      level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.6, 0.3, 0.05);
      Vec3 from = player.position();
      Vec3 flat = new Vec3(targetPos.x - from.x, 0.0, targetPos.z - from.z);
      float yaw = flat.lengthSqr() < 1.0E-4 ? player.getYRot() : yawTowards(from, targetPos);
      // "Behind" relative to how Regulus sent it flying: over its far shoulder, a bit above.
      Vec3 spot = behindSpot(targetPos, yaw + 180.0F).add(0.0, 1.2, 0.0);
      player.connection.teleport(spot.x, spot.y, spot.z, yawTowards(spot, targetPos), 50.0F);
      player.setDeltaMovement(Vec3.ZERO);
      player.fallDistance = 0.0F;
      level.playSound(null, BlockPos.containing(spot), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.4F);
      level.sendParticles(ParticleTypes.FLASH, spot.x, spot.y + 1.0, spot.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   private static void hover(ServerPlayer player) {
      player.setDeltaMovement(0.0, 0.0, 0.0);
      player.fallDistance = 0.0F;
      player.hurtMarked = true;
      player.connection.send(new ClientboundSetEntityMotionPacket(player));
   }

   private static void slam(ServerPlayer player, RegulusState state, ServerLevel level, @Nullable LivingEntity target, boolean alive, boolean movable) {
      state.eventFired = true;
      chargeSlamCooldown(state);
      Vec3 at = alive ? target.position().add(0.0, target.getBbHeight() * 0.6, 0.0) : (state.actionPoint != null ? state.actionPoint : player.position());
      level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, BlockPos.containing(at), ViltrumiteCore.REGULUS_PUNCH_HIT.get(), SoundSource.PLAYERS, 2.5F, 0.6F);
      level.playSound(null, BlockPos.containing(at), ViltrumiteCore.PUNCH_IMPACT_EVENT.get(), SoundSource.PLAYERS, 1.5F, 0.8F);

      // Regulus follows the blow down; his own landing makes the shockwave.
      player.setDeltaMovement(0.0, -1.6, 0.0);
      player.hurtMarked = true;
      player.connection.send(new ClientboundSetEntityMotionPacket(player));

      if (alive && movable) {
         state.counterDiveTargetId = target.getUUID();
         state.counterDiveTicks = 0;
      } else {
         impact(player, state, level, alive ? target : null, groundBelow(level, at));
      }
   }

   private static void tickDive(ServerPlayer player, RegulusState state, ServerLevel level) {
      LivingEntity target = resolveTarget(level, state.counterDiveTargetId);
      if (target == null || !target.isAlive()) {
         state.counterDiveTicks = -1;
         state.counterDiveTargetId = null;
         return;
      }

      state.counterDiveTicks++;
      Vec3 from = target.position();
      Vec3 to = from.add(0.0, -RegulusRules.COUNTER_DIVE_SPEED, 0.0);
      BlockHitResult hit = level.clip(new ClipContext(from.add(0.0, 0.1, 0.0), to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, target));
      level.sendParticles(ParticleTypes.CLOUD, from.x, from.y + target.getBbHeight() * 0.5, from.z, 3, 0.2, 0.4, 0.2, 0.01);
      if (hit.getType() != HitResult.Type.MISS || state.counterDiveTicks >= RegulusRules.COUNTER_DIVE_MAX_TICKS) {
         Vec3 ground = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : from;
         state.counterDiveTicks = -1;
         state.counterDiveTargetId = null;
         impact(player, state, level, target, ground);
         return;
      }

      holdAt(target, to);
   }

   /** The target hits the ground: crater, full Counter damage, everything around is thrown. */
   private static void impact(ServerPlayer player, RegulusState state, ServerLevel level, @Nullable LivingEntity target, Vec3 ground) {
      BlockState material = level.getBlockState(BlockPos.containing(ground.x, ground.y - 0.5, ground.z));
      if (material.isAir() || !material.getFluidState().isEmpty()) {
         material = Blocks.DIRT.defaultBlockState();
      }

      crater(level, ground);
      if (target != null && target.isAlive()) {
         float damage = RegulusRules.counterDamage(target.getMaxHealth(), state.hearts());
         hitClean(player, target, damage);
         target.fallDistance = 0.0F;
         if (HeroRegistry.allowsExternalControl(target, ControlKind.IMPULSE)) {
            holdAt(target, new Vec3(ground.x, ground.y, ground.z));
         }
      }

      // Bystanders around the crater are thrown out of it.
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(ground)).inflate(5.0))) {
         if (entity == player || entity == target || !entity.isAlive() || state.carriers.contains(entity.getUUID())) {
            continue;
         }

         entity.hurt(player.damageSources().playerAttack(player), 6.0F);
         if (HeroRegistry.allowsExternalControl(entity, ControlKind.IMPULSE)) {
            Vec3 away = entity.position().subtract(ground);
            Vec3 push = new Vec3(away.x, 0.0, away.z).normalize().scale(1.5);
            entity.setDeltaMovement(push.x, 0.7, push.z);
            entity.hasImpulse = true;
            if (entity instanceof ServerPlayer sp) {
               sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            }
         }
      }

      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, material);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 0.5, ground.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(debris, ground.x, ground.y + 0.5, ground.z, 160, 2.0, 0.6, 2.0, 0.5);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, ground.x, ground.y + 0.5, ground.z, 30, 2.2, 0.4, 2.2, 0.04);
      level.sendParticles(ParticleTypes.POOF, ground.x, ground.y + 0.5, ground.z, 40, 2.5, 0.3, 2.5, 0.12);
      level.playSound(null, BlockPos.containing(ground), ViltrumiteCore.REGULUS_IMPACT_HEAVY.get(), SoundSource.PLAYERS, 4.0F, 0.6F);
      level.playSound(null, BlockPos.containing(ground), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.6F);
      RegulusFx.send(player, RegulusFxS2CPacket.SLAM, ground, 1.0F, material, new float[]{(float)RegulusRules.COUNTER_CRATER_RADIUS * 2.5F, 0.0F, 0.0F});
   }

   /** Bowl under the impact: radius 3, deepest in the middle, via the approved destruction helper. */
   private static void crater(ServerLevel level, Vec3 center) {
      int radius = RegulusRules.COUNTER_CRATER_RADIUS;
      int depth = RegulusRules.COUNTER_CRATER_DEPTH;
      int baseY = Mth.floor(center.y - 0.01) - 1;
      int baseX = Mth.floor(center.x);
      int baseZ = Mth.floor(center.z);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            double d2 = (dx * dx + dz * dz) / (double)(radius * radius);
            if (d2 > 1.0) {
               continue;
            }

            int columnDepth = Math.max(1, (int)Math.round(depth * (1.0 - d2)));
            for (int dy = -1; dy < columnDepth; dy++) {
               pos.set(baseX + dx, baseY - dy, baseZ + dz);
               if (!level.getBlockState(pos).isAir()) {
                  HeroDestruction.destroyBlock(level, pos);
               }
            }
         }
      }
   }

   private static Vec3 groundBelow(ServerLevel level, Vec3 from) {
      BlockHitResult hit = level.clip(new ClipContext(from, from.add(0.0, -48.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null));
      return hit.getType() == HitResult.Type.MISS ? from : hit.getLocation();
   }

   /** Damage that lands through i-frames (each stage of the combo must hit). */
   private static void hitClean(ServerPlayer player, LivingEntity target, float damage) {
      int saved = target.invulnerableTime;
      target.invulnerableTime = 0;
      if (!target.hurt(player.damageSources().playerAttack(player), damage)) {
         target.invulnerableTime = saved;
      }
   }

   /** Put the target exactly here this tick (players via teleport, mobs via teleportTo). */
   private static void holdAt(LivingEntity target, Vec3 pos) {
      target.setDeltaMovement(Vec3.ZERO);
      target.fallDistance = 0.0F;
      if (target instanceof ServerPlayer sp) {
         sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
      } else {
         target.teleportTo(pos.x, pos.y, pos.z);
      }
      target.hasImpulse = true;
   }

   private static float yawTowards(Vec3 from, Vec3 to) {
      return (float)Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
   }

   @Nullable
   private static LivingEntity resolveTarget(ServerLevel level, @Nullable UUID id) {
      if (id == null) {
         return null;
      }

      Entity entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
   }
}
