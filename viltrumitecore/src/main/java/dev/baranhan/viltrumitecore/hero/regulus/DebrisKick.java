package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroDestruction;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Debris Kick: Regulus kicks the ground in front of him. The kick itself
 * never breaks blocks — it rips a spray of shards out of the surface that
 * fly at enormous speed, effectively instantly (resolved within the impact
 * tick). Each shard is a fast segment cast: it hits the first living target
 * (damage + knockback), or smashes into blocks (breaking up to
 * DEBRIS_SHARD_PIERCE of them, mobGriefing respected) and bursts into dust.
 * A flyby whoosh plays along every shard path.
 */
public final class DebrisKick {
   private DebrisKick() {
   }

   public static void start(ServerPlayer player, RegulusState state) {
      if (state.busy()) {
         return;
      }
      // A ground kick needs ground: an airborne cast costs nothing and says why.
      if (!player.onGround()) {
         player.displayClientMessage(Component.translatable("message.viltrumitecore.debris.no_ground"), true);
         return;
      }
      state.beginAction(RegulusHero.ACTION_DEBRIS, RegulusRules.DEBRIS_ANIM_TICKS, RegulusRules.DEBRIS_EVENT_TICK, RegulusRules.DEBRIS_EVENT_TICK);
      RegulusPassives.preserveExternalSlowness(player, state);
   }

   static boolean shouldFire(RegulusState state) {
      return RegulusHero.ACTION_DEBRIS.equals(state.actionId) && !state.eventFired && state.actionElapsed >= state.actionEventTick;
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      if (!RegulusHero.ACTION_DEBRIS.equals(state.actionId) || state.eventFired) {
         return;
      }

      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0, true, false, true));
      if (!shouldFire(state)) {
         return;
      }

      state.eventFired = true;
      RegulusPassives.clearOwnSlowness(player, state, state.actionElapsed);
      ServerLevel level = player.serverLevel();
      BlockPos groundPos = player.blockPosition().below();
      BlockState ground = level.getBlockState(groundPos);
      // Knocked up during the windup: nothing to kick out — free abort.
      if (!player.onGround() && ground.getCollisionShape(level, groundPos).isEmpty()) {
         player.displayClientMessage(Component.translatable("message.viltrumitecore.debris.no_ground"), true);
         state.clearAction();
         return;
      }

      state.startCooldown(RegulusAbilities.DEBRIS_KICK, RegulusRules.DEBRIS_COOLDOWN);
      kick(player, level, materialOf(ground), state.hearts());
   }

   /** The shard material is the real surface; air/fluids fall back to dirt. */
   private static BlockState materialOf(BlockState ground) {
      return ground.isAir() || !ground.getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : ground;
   }

   private static void kick(ServerPlayer player, ServerLevel level, BlockState material, int hearts) {
      float yaw = player.getYRot();
      Vec3 forward = shardDirection(yaw, 0.0F, 0.0, 0.0);
      // Shards leave from the kicked spot: at the toe, just above the ground.
      Vec3 origin = player.position().add(forward.x * 0.8, 0.35, forward.z * 0.8);
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, material);

      // Impact at the foot: the ground bursts (visual only — no block breaks here).
      level.sendParticles(debris, origin.x, origin.y, origin.z, 40, 0.35, 0.1, 0.35, 0.25);
      level.sendParticles(ParticleTypes.POOF, origin.x, origin.y, origin.z, 10, 0.3, 0.05, 0.3, 0.06);
      level.playSound(null, BlockPos.containing(origin), material.getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.6F, 0.55F);
      level.playSound(null, BlockPos.containing(origin), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.6F);

      RandomSource random = player.getRandom();
      Map<UUID, Integer> shardHits = new HashMap<>();
      for (int i = 0; i < RegulusRules.DEBRIS_SHARDS; i++) {
         double yawOffset = shardYawOffset(i, RegulusRules.DEBRIS_SHARDS) + (random.nextDouble() - 0.5) * 4.0;
         double elevation = RegulusRules.DEBRIS_MIN_ELEVATION + random.nextDouble() * (RegulusRules.DEBRIS_MAX_ELEVATION - RegulusRules.DEBRIS_MIN_ELEVATION);
         Vec3 dir = shardDirection(yaw, aimPitch(player.getXRot()), yawOffset, elevation);
         fireShard(player, level, origin, dir, debris, hearts, shardHits);
      }
   }

   /**
    * One shard: resolved instantly along its path. Stops at the first living
    * target, or after breaking DEBRIS_SHARD_PIERCE blocks / hitting an
    * unbreakable one. Always leaves a dust puff where it ends.
    */
   private static void fireShard(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir, BlockParticleOption debris, int hearts, Map<UUID, Integer> shardHits) {
      Vec3 from = origin;
      double remaining = RegulusRules.DEBRIS_RANGE;
      int pierced = 0;
      Vec3 end = origin.add(dir.scale(remaining));

      while (remaining > 0.05) {
         Vec3 to = from.add(dir.scale(remaining));
         BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
         Vec3 blockStop = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();

         LivingEntity target = firstTarget(player, level, from, blockStop);
         if (target != null) {
            Vec3 hitAt = target.getBoundingBox().inflate(0.3).clip(from, blockStop).orElse(target.position());
            applyHit(player, target, dir, hearts, shardHits);
            end = hitAt;
            break;
         }

         if (blockHit.getType() == HitResult.Type.MISS) {
            end = to;
            break;
         }

         BlockPos pos = blockHit.getBlockPos();
         BlockState state = level.getBlockState(pos);
         end = blockHit.getLocation();
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), end.x, end.y, end.z, 10, 0.15, 0.15, 0.15, 0.15);
         if (pierced >= RegulusRules.DEBRIS_SHARD_PIERCE || !HeroDestruction.destroyBlock(level, pos)) {
            break;
         }

         pierced++;
         level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.9F, 0.8F + level.random.nextFloat() * 0.3F);
         double travelled = from.distanceTo(end);
         remaining -= travelled + 0.05;
         from = end.add(dir.scale(0.05));
      }

      trail(level, origin, end, dir, debris);
      // Dust where the shard died.
      level.sendParticles(ParticleTypes.POOF, end.x, end.y, end.z, 5, 0.2, 0.2, 0.2, 0.03);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, end.x, end.y, end.z, 1, 0.1, 0.1, 0.1, 0.01);
      level.sendParticles(debris, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0.1);
      // Flyby whoosh at the middle of the path — heard by anyone it passes.
      Vec3 mid = origin.add(end).scale(0.5);
      level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.45F, 1.7F + level.random.nextFloat() * 0.3F);
      level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.35F, 1.8F + level.random.nextFloat() * 0.2F);
   }

   /** Speed streak: fast-moving shard particles along the path, no lingering cloud. */
   private static void trail(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir, BlockParticleOption debris) {
      double length = from.distanceTo(to);
      for (double d = 0.6; d < length; d += 1.4) {
         Vec3 p = from.add(dir.scale(d));
         // count=0 -> the offset is a velocity: the particle streaks along the shard.
         level.sendParticles(debris, p.x, p.y, p.z, 0, dir.x, dir.y, dir.z, 1.6);
      }
   }

   @Nullable
   private static LivingEntity firstTarget(ServerPlayer player, ServerLevel level, Vec3 from, Vec3 to) {
      AABB sweep = new AABB(from, to).inflate(0.6);
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (Entity entity : level.getEntities(player, sweep, e -> e instanceof LivingEntity living && living.isAlive() && (e instanceof Mob || e instanceof Player) && !e.isSpectator())) {
         double distance = rayDistance(from, to, entity.getBoundingBox().inflate(0.3));
         if (distance < bestDistance) {
            bestDistance = distance;
            best = (LivingEntity)entity;
         }
      }

      return best;
   }

   /**
    * Shard damage stacks up to DEBRIS_MAX_SHARDS_PER_TARGET per target per
    * kick; i-frames are lifted per shard so the spray actually lands. The
    * first landed shard carries the knockback.
    */
   private static void applyHit(ServerPlayer player, LivingEntity target, Vec3 dir, int hearts, Map<UUID, Integer> shardHits) {
      int already = shardHits.getOrDefault(target.getUUID(), 0);
      float damage = RegulusRules.debrisShardDamage(already, hearts);
      if (damage <= 0.0F) {
         return;
      }

      int savedInvulnerable = target.invulnerableTime;
      target.invulnerableTime = 0;
      boolean landed = target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = savedInvulnerable;
      if (!landed) {
         return;
      }

      shardHits.put(target.getUUID(), already + 1);
      if (already == 0 && HeroRegistry.allowsExternalControl(target, ControlKind.IMPULSE)) {
         Vec3 push = new Vec3(dir.x, 0.0, dir.z).normalize().scale(RegulusRules.DEBRIS_KNOCKBACK);
         target.setDeltaMovement(push.x, 0.35, push.z);
         target.hasImpulse = true;
         if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
         }
      }
   }

   /** The spray follows the gaze only mildly: kicks stay low and forward. */
   static float aimPitch(float lookPitch) {
      return Mth.clamp(lookPitch, RegulusRules.DEBRIS_PITCH_UP_LIMIT, RegulusRules.DEBRIS_PITCH_DOWN_LIMIT) * 0.5F;
   }

   /** Evenly fans the shards across the cone, centre-weighted by index. */
   static double shardYawOffset(int index, int count) {
      if (count <= 1) {
         return 0.0;
      }

      double half = RegulusRules.DEBRIS_CONE_DEGREES / 2.0;
      return -half + (RegulusRules.DEBRIS_CONE_DEGREES * index) / (count - 1);
   }

   /**
    * Direction for a shard: yaw in MC degrees, pitch in MC degrees (positive
    * = down), plus a yaw offset and an elevation (positive = up) in degrees.
    */
   static Vec3 shardDirection(float yawDeg, float pitchDeg, double yawOffsetDeg, double elevationDeg) {
      double yaw = Math.toRadians(yawDeg + yawOffsetDeg);
      double pitch = Math.toRadians(pitchDeg - elevationDeg);
      double cosP = Math.cos(pitch);
      return new Vec3(-Math.sin(yaw) * cosP, -Math.sin(pitch), Math.cos(yaw) * cosP).normalize();
   }

   /** Shared gaze collision helper used by Mania and manual heart assignment. */
   static double rayDistance(Vec3 origin, Vec3 far, AABB box) {
      return box.contains(origin) ? 0.0 : box.clip(origin, far).map(origin::distanceTo).orElse(Double.MAX_VALUE);
   }
}
