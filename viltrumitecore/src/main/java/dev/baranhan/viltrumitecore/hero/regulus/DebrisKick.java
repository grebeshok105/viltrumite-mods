package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroDestruction;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Ground kick: a 14t impact releases an eight-tick forward trench wave. */
public final class DebrisKick {
   private DebrisKick() {
   }

   static final class Wave {
      final Vec3 origin;
      final Vec3 direction;
      final ResourceLocation dimension;
      final int hearts;
      final Set<UUID> hitTargets = new HashSet<>();
      final Set<BlockPos> broken = new HashSet<>();
      int step;
      int groundY;

      Wave(Vec3 origin, Vec3 direction, int hearts, ResourceLocation dimension) {
         this.origin = origin;
         this.direction = new Vec3(direction.x, 0.0, direction.z).normalize();
         this.hearts = hearts;
         this.dimension = dimension;
         this.groundY = BlockPos.containing(origin).getY();
      }

      @Nullable Vec3 nextSlice() {
         return this.step >= RegulusRules.DEBRIS_RANGE ? null : this.origin.add(this.direction.scale(++this.step));
      }

      boolean claim(UUID target) {
         return this.hitTargets.add(target);
      }

      /** Feet pos of the first slice the wave would bite — the cast gate. */
      BlockPos nextSlicePos() {
         Vec3 first = this.origin.add(this.direction);
         return BlockPos.containing(first.x, this.groundY, first.z);
      }
   }

   public static void start(ServerPlayer player, RegulusState state) {
      if (state.busy()) {
         return;
      }
      // A ground kick needs ground: an airborne cast never spawns a wave, so
      // it costs nothing and tells the player why.
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
      if (RegulusHero.ACTION_DEBRIS.equals(state.actionId) && !state.eventFired) {
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0, true, false, true));
         if (shouldFire(state)) {
            state.eventFired = true;
            RegulusPassives.clearOwnSlowness(player, state, state.actionElapsed);
            double yaw = Math.toRadians(player.getYRot());
            Wave wave = new Wave(player.position(), new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)), state.hearts(), player.level().dimension().location());
            // A knock-up during the windup can leave the wave with no ground
            // to bite: abort it the same way as an airborne cast — no
            // cooldown, just feedback.
            if (surfaceAt(player.serverLevel(), wave.nextSlicePos()) == null) {
               player.displayClientMessage(Component.translatable("message.viltrumitecore.debris.no_ground"), true);
               state.clearAction();
            } else {
               state.startCooldown(RegulusAbilities.DEBRIS_KICK, RegulusRules.DEBRIS_COOLDOWN);
               state.debrisWave = wave;
               player.level().playSound(null, player.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.4F, 0.6F);
            }
         }
      }
      if (state.debrisWave != null && !advanceWave(player, state.debrisWave)) {
         state.debrisWave = null;
      }
   }

   private static boolean advanceWave(ServerPlayer player, Wave wave) {
      ServerLevel level = player.serverLevel();
      if (!player.isAlive() || !wave.dimension.equals(level.dimension().location())) {
         return false;
      }
      Vec3 slice = wave.nextSlice();
      if (slice == null) {
         return false;
      }
      BlockPos center = surfaceAt(level, BlockPos.containing(slice.x, wave.groundY, slice.z));
      if (center == null || !HeroDestruction.canDestroy(level, center)) {
         return false;
      }
      wave.groundY = center.getY() + 1;
      Vec3 side = new Vec3(-wave.direction.z, 0.0, wave.direction.x);
      for (int lane = -1; lane <= 1; lane++) {
         Vec3 lanePos = slice.add(side.scale(lane));
         BlockPos ground = surfaceAt(level, BlockPos.containing(lanePos.x, wave.groundY, lanePos.z));
         if (ground == null || !HeroDestruction.canDestroy(level, ground) || !wave.broken.add(ground)) {
            continue;
         }
         BlockState block = level.getBlockState(ground);
         if (!HeroDestruction.destroyBlock(level, ground)) {
            continue;
         }
         BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, block);
         for (int i = 0; i < 6; i++) {
            level.sendParticles(debris, ground.getX() + 0.2 + level.random.nextDouble() * 0.6, ground.getY() + 1.0, ground.getZ() + 0.2 + level.random.nextDouble() * 0.6,
               0, wave.direction.x * 0.7, 0.2 + level.random.nextDouble() * 0.3, wave.direction.z * 0.7, 1.0);
         }
         level.sendParticles(ParticleTypes.POOF, ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 4, 0.3, 0.15, 0.3, 0.02);
      }
      Vec3 surface = new Vec3(slice.x, wave.groundY, slice.z);
      AABB area = new AABB(surface.add(-1.8, -0.5, -1.8), surface.add(1.8, 2.8, 1.8));
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && (e instanceof Mob || e instanceof Player))) {
         Vec3 offset = target.position().subtract(wave.origin);
         double along = offset.dot(wave.direction);
         double lateral = Math.abs(offset.dot(side));
         boolean visible = level.clip(new ClipContext(surface.add(0.0, 0.5, 0.0), target.getBoundingBox().getCenter(),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
         if (along >= wave.step - 1.0 && along <= wave.step + 0.75 && lateral <= 1.8 && visible && applyHit(player, target, along, wave)) {
            wave.claim(target.getUUID());
         }
      }
      if (wave.step % 2 == 0) {
         level.playSound(null, center, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.7F, 0.7F);
      }
      return wave.step < RegulusRules.DEBRIS_RANGE;
   }

   /**
    * Walkable surface within ±2 of the running ground line, top-down so the
    * wave prefers climbing: feet+1 down to feet-3. A two-block ledge or a
    * three-block drop still carries the wave; taller walls and sheer drops
    * stop it. Pure predicates keep the rule unit-testable.
    */
   static int pickSurfaceDy(java.util.function.IntPredicate solidAtDy, java.util.function.IntPredicate clearAtDy) {
      for (int dy = 1; dy >= -3; dy--) {
         if (solidAtDy.test(dy) && clearAtDy.test(dy + 1)) {
            return dy;
         }
      }
      return Integer.MIN_VALUE;
   }

   @Nullable private static BlockPos surfaceAt(ServerLevel level, BlockPos feet) {
      int dy = pickSurfaceDy(
         d -> !level.getBlockState(feet.offset(0, d, 0)).getCollisionShape(level, feet.offset(0, d, 0)).isEmpty(),
         d -> level.getBlockState(feet.offset(0, d, 0)).getCollisionShape(level, feet.offset(0, d, 0)).isEmpty());
      return dy == Integer.MIN_VALUE ? null : feet.offset(0, dy, 0);
   }

   /** Shared gaze collision helper used by Mania and manual heart assignment. */
   static double rayDistance(Vec3 origin, Vec3 far, AABB box) {
      return box.contains(origin) ? 0.0 : box.clip(origin, far).map(origin::distanceTo).orElse(Double.MAX_VALUE);
   }

   /**
    * One hit per target per wave: vanilla i-frames are lifted for the strike
    * so a recently-hurt target still takes damage and knockback. Returns
    * whether the hit landed — only then is the target consumed by the wave.
    */
   private static boolean applyHit(ServerPlayer player, LivingEntity target, double distance, Wave wave) {
      int savedInvulnerable = target.invulnerableTime;
      target.invulnerableTime = 0;
      boolean landed = target.hurt(player.damageSources().playerAttack(player), RegulusRules.debrisDamage(distance, wave.hearts));
      target.invulnerableTime = savedInvulnerable;
      if (landed && HeroRegistry.allowsExternalControl(target, ControlKind.IMPULSE)) {
         target.setDeltaMovement(wave.direction.x, 0.3, wave.direction.z);
         target.hasImpulse = true;
         if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
         }
      }
      return landed;
   }
}
