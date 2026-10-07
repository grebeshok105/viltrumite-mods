package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroDestruction;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Counter (spec §10): madness-only. The press arms on the last living attacker
 * record (fresh within 240t, within 40 blocks) — no target shows an actionbar
 * hint and costs no cooldown. The cast lifts Regulus ~20t, teleports behind the
 * offender, then slams 7t later. The target is recomputed at slam; a dead or
 * out-of-range offender means slam + crater at its last known position with no
 * target damage. The 800t cooldown is charged on the slam, hearts sampled then.
 */
public final class Counter {
   private Counter() {
   }

   /** §10.2: madness plus a fresh, live, in-range attacker record. */
   static boolean canActivate(RegulusState state, boolean attackerFresh, boolean attackerAlive, boolean attackerInRange) {
      return state.madnessTicksLeft > 0 && state.attackerId != null && attackerFresh && attackerAlive && attackerInRange;
   }

   /** The slam damages the target only while it is still alive and in range. */
   static boolean slamHitsTarget(boolean attackerAlive, boolean attackerInRange) {
      return attackerAlive && attackerInRange;
   }

   /** Latch the offender and arm the cast: lift ends at the teleport, slam fires 7t later. */
   static void beginCast(RegulusState state) {
      int slamTick = RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS;
      state.beginAction(RegulusHero.ACTION_COUNTER, slamTick + 1, slamTick, slamTick);
      state.actionTargetId = state.attackerId;
      state.actionPoint = state.attackerLastPos;
   }

   /** Spot opposite the target's facing — behind its back. */
   static Vec3 behindSpot(Vec3 targetPos, float targetYawDeg) {
      double yawRad = Math.toRadians((double)targetYawDeg);
      Vec3 look = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
      return targetPos.subtract(look.scale(RegulusRules.COUNTER_BEHIND_DISTANCE));
   }

   /** Cooldown 800, hearts sampled when the slam lands. */
   static void chargeSlamCooldown(RegulusState state) {
      state.startCooldown(RegulusAbilities.COUNTER, RegulusRules.COUNTER_COOLDOWN);
   }

   public static void start(ServerPlayer player, RegulusState state) {
      if (state.madnessTicksLeft <= 0) {
         return;
      }

      if (!ready(player, state)) {
         player.displayClientMessage(Component.translatable("message.viltrumitecore.counter.no_target"), true);
         return;
      }
      beginCast(state);
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
      if (!RegulusHero.ACTION_COUNTER.equals(state.actionId)) {
         return;
      }

      ServerLevel level = player.serverLevel();
      LivingEntity target = resolveTarget(level, state.actionTargetId);
      if (target != null && target.isAlive()) {
         // Track the offender while it lives; its last position is the fallback slam point.
         state.actionPoint = target.position();
      }

      if (state.actionElapsed < RegulusRules.COUNTER_LIFT_TICKS) {
         lift(player);
      } else if (state.actionElapsed == RegulusRules.COUNTER_LIFT_TICKS) {
         teleportBehind(player, state, target);
      }

      if (!state.eventFired && state.actionElapsed >= RegulusRules.COUNTER_LIFT_TICKS + RegulusRules.COUNTER_SLAM_TICKS) {
         slam(player, state, level, target);
      }
   }

   private static void lift(ServerPlayer player) {
      Vec3 delta = player.getDeltaMovement();
      player.setDeltaMovement(delta.x, RegulusRules.COUNTER_LIFT_VELOCITY, delta.z);
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      player.connection.send(new ClientboundSetEntityMotionPacket(player));
   }

   private static void teleportBehind(ServerPlayer player, RegulusState state, @Nullable LivingEntity target) {
      if (target != null && target.isAlive()) {
         Vec3 spot = behindSpot(target.position(), target.getYRot());
         player.connection.teleport(spot.x, spot.y, spot.z, yawTowards(spot, target.position()), 20.0F);
      } else if (state.actionPoint != null) {
         // The offender is gone: finish the sequence at its last known position.
         Vec3 spot = state.actionPoint.add(0.0, 0.5, 0.0);
         player.connection.teleport(spot.x, spot.y, spot.z, player.getYRot(), 20.0F);
      }

      player.setDeltaMovement(Vec3.ZERO);
      player.fallDistance = 0.0F;
   }

   private static void slam(ServerPlayer player, RegulusState state, ServerLevel level, @Nullable LivingEntity target) {
      boolean alive = target != null && target.isAlive();
      boolean inRange = alive && target.distanceToSqr(player) <= RegulusRules.COUNTER_RANGE * RegulusRules.COUNTER_RANGE;
      boolean hits = slamHitsTarget(alive, inRange);

      Vec3 slamPos = hits ? target.position() : (state.actionPoint != null ? state.actionPoint : player.position());
      crater(level, slamPos);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, slamPos.x, slamPos.y + 0.5, slamPos.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, BlockPos.containing(slamPos), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);

      if (hits) {
         float damage = RegulusRules.counterDamage(target.getMaxHealth(), state.hearts());
         target.hurt(player.damageSources().playerAttack(player), damage);
         if (target instanceof ViltrumiteFlightPlayer flying) {
            flying.stopFlight();
         }
      }

      state.eventFired = true;
      chargeSlamCooldown(state);
   }

   /** Depth-8 bowl under the slam point, radius 3, via the approved destruction helper. */
   private static void crater(ServerLevel level, Vec3 center) {
      int radius = RegulusRules.COUNTER_CRATER_RADIUS;
      int depth = RegulusRules.COUNTER_CRATER_DEPTH;
      int baseY = Mth.floor(center.y) - 1;
      int baseX = Mth.floor(center.x);
      int baseZ = Mth.floor(center.z);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz > radius * radius) {
               continue;
            }

            for (int dy = 0; dy < depth; dy++) {
               pos.set(baseX + dx, baseY - dy, baseZ + dz);
               BlockState state = level.getBlockState(pos);
               if (state.isAir()) {
                  continue;
               }

               if (!HeroDestruction.destroyBlock(level, pos)) {
                  break;
               }
            }
         }
      }
   }

   private static float yawTowards(Vec3 from, Vec3 to) {
      return (float)Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
   }

   @Nullable
   private static LivingEntity resolveTarget(ServerLevel level, @Nullable UUID id) {
      if (id == null) {
         return null;
      }

      return level.getEntity(id) instanceof LivingEntity living ? living : null;
   }
}
