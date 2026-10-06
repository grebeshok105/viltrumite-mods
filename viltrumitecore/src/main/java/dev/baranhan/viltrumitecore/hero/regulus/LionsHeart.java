package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroDamage;
import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lion's Heart (spec 6): 14t windup -> shrink-only window (60+40H) -> overheat.
 * While active: all external damage blocked, internal damage passes, hunger
 * frozen, negative effects stripped, projectiles within 4 blocks frozen,
 * movement and melee normal, all ability slots greyed except the off-toggle
 * and Evangelium. Off at HP<=4 (cd 600) or manually (cd 100); on off, frozen
 * projectiles drop and nearby opponents get a ~1.5 repulse impulse.
 */
public final class LionsHeart {
   private LionsHeart() {
   }

   public static void toggle(ServerPlayer player, RegulusState state) {
      if (state.lionActive) {
         deactivate(player, state, false);
         return;
      }

      if (state.busy() || state.madnessTicksLeft > 0) {
         return;
      }

      state.beginAction(RegulusHero.ACTION_LION, RegulusRules.LION_WINDUP_TICKS + 1, RegulusRules.LION_WINDUP_TICKS, RegulusRules.LION_WINDUP_TICKS);
   }

   /** Windup decision: fire the activation event exactly at the 14t mark. */
   static boolean shouldActivate(RegulusState state) {
      return RegulusHero.ACTION_LION.equals(state.actionId)
         && !state.eventFired
         && state.actionElapsed >= state.actionEventTick;
   }

   /** Per-tick window recompute: heart loss shortens, new hearts never extend. */
   static void tickWindow(RegulusState state, int hearts) {
      if (state.lionWindowFloorHearts < 0) {
         state.lionWindowFloorHearts = hearts;
      }

      state.lionWindowMax = RegulusRules.shrinkLionWindow(state.lionWindowMax, state.lionWindowFloorHearts, hearts);
      state.lionWindowFloorHearts = Math.min(state.lionWindowFloorHearts, hearts);
   }

   static boolean overheating(RegulusState state) {
      return state.lionElapsed > state.lionWindowMax;
   }

   public static void tick(ServerPlayer player, RegulusState state) {
      if (shouldActivate(state)) {
         activate(player, state);
      }

      if (!state.lionActive) {
         return;
      }

      state.lionElapsed++;
      tickWindow(state, state.hearts());
      clearHarmfulEffects(player);
      freezeNearbyProjectiles(player);

      if (overheating(state)) {
         state.overheatTicks++;
         HeroDamage.applyInternal(player, RegulusRules.overheatDps(state.overheatTicks) / 20.0F);
      }

      // Any internal HP loss to 4 or below collapses the heart (spec 6.3).
      if (RegulusRules.lionForcedOff(player.getHealth())) {
         deactivate(player, state, true);
      }
   }

   private static void activate(ServerPlayer player, RegulusState state) {
      state.eventFired = true;
      state.clearAction();
      state.lionActive = true;
      state.lionWindowMax = RegulusRules.lionWindow(state.hearts());
      state.lionElapsed = 0;
      state.overheatTicks = 0;
      state.lionStartTick = player.serverLevel().getGameTime();
      state.lionWindowFloorHearts = state.hearts();
      player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
   }

   /** forced=true on HP collapse (cd 600); forced=false on manual toggle (cd 100). */
   public static void deactivate(ServerPlayer player, RegulusState state, boolean forced) {
      if (!state.lionActive) {
         return;
      }

      endLionState(state);
      state.startCooldown(RegulusAbilities.LIONS_HEART, RegulusRules.lionCooldownBase(forced));
      releaseFrozenProjectiles(player);
      repulseOpponents(player, state);
   }

   /**
    * Cleanup path (death/disconnect/hero change): state drops silently and the
    * caster's frozen projectiles are restored; no cooldown, no repulse.
    */
   public static void forceOff(ServerPlayer player, RegulusState state) {
      if (!state.lionActive) {
         return;
      }

      endLionState(state);
      releaseFrozenProjectiles(player);
   }

   private static void endLionState(RegulusState state) {
      state.lionActive = false;
      state.lionElapsed = 0;
      state.overheatTicks = 0;
      state.lionWindowMax = 0;
      state.lionWindowFloorHearts = -1;
   }

   /** Negative effects do not survive on an active Lion's Heart (spec 6.2). */
   private static void clearHarmfulEffects(ServerPlayer player) {
      if (player.getActiveEffects().isEmpty()) {
         return;
      }

      List<MobEffect> harmful = new ArrayList<>();
      for (MobEffectInstance instance : player.getActiveEffects()) {
         if (!instance.getEffect().isBeneficial()) {
            harmful.add(instance.getEffect());
         }
      }

      for (MobEffect effect : harmful) {
         player.removeEffect(effect);
      }
   }

   /**
    * Projectiles inside the 4-block aura freeze mid-air: velocity zeroed and
    * gravity suspended until the heart turns off (spec 6.2).
    */
   private static void freezeNearbyProjectiles(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      ControlManager manager = ControlManager.get(level);
      double radius = RegulusRules.LION_PROJECTILE_RADIUS;
      AABB area = player.getBoundingBox().inflate(radius);
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
         if (projectile.distanceToSqr(player) <= radius * radius) {
            manager.freezeProjectile(projectile, player.getUUID());
         }
      }
   }

   private static void releaseFrozenProjectiles(ServerPlayer player) {
      ControlManager.releaseProjectilesFor(player.getUUID());
   }

   /** On switch-off a ~1.5 impulse pushes the nearest opponents away (spec 6.4). */
   private static void repulseOpponents(ServerPlayer player, RegulusState state) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      Vec3 center = player.position();
      AABB area = player.getBoundingBox().inflate(RegulusRules.LION_REPULSE_RADIUS);
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, entity -> isOpponent(player, entity, state))) {
         Vec3 away = entity.position().subtract(center);
         Vec3 push = new Vec3(away.x, 0.0, away.z);
         if (push.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            push = new Vec3(look.x, 0.0, look.z);
         }

         push = push.normalize().scale(RegulusRules.LION_RELEASE_IMPULSE);
         entity.setDeltaMovement(entity.getDeltaMovement().add(push.x, 0.35, push.z));
         entity.hasImpulse = true;
         if (entity instanceof ServerPlayer targetPlayer) {
            targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
         }
      }
   }

   /**
    * Opponents only (spec 6.4 "ближайших противников"): enemy players and
    * hostile mobs — never the caster's allies, pets, or own heart carriers.
    */
   private static boolean isOpponent(ServerPlayer player, LivingEntity entity, RegulusState state) {
      if (entity == player) {
         return false;
      }

      return RegulusRules.repulseTarget(
         entity instanceof Player || entity instanceof Enemy,
         player.isAlliedTo(entity),
         state.carriers.contains(entity.getUUID())
      );
   }
}
