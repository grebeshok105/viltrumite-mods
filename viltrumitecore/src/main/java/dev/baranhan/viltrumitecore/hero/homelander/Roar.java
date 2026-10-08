package dev.baranhan.viltrumitecore.hero.homelander;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.hero.fx.HeroFx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Roar (spec §6.5): a 60° / 12-block cone in front of the eyes. Knockback away
 * from Homelander and Slowness II for 3 s; no damage. Cooldown 15 s.
 */
public final class Roar {
   public static final int ANIM_TICKS = 20;
   private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(HomelanderRules.ROAR_CONE_DEG / 2.0));

   private Roar() {
   }

   /** look need not be normalized; toTarget is eye-to-target. */
   public static boolean inCone(Vec3 look, Vec3 toTarget) {
      double dist = toTarget.length();
      if (dist > HomelanderRules.ROAR_RANGE) {
         return false;
      }

      if (dist < 1.0E-6) {
         return true;
      }

      double lookLength = look.length();
      return lookLength > 1.0E-6 && look.dot(toTarget) / (lookLength * dist) >= HALF_ANGLE_COS;
   }

   /** Snapshot actionElapsed for the pose timeline, -1 when idle. */
   public static int animElapsed(int animLeft) {
      return animLeft <= 0 ? -1 : ANIM_TICKS - animLeft;
   }

   static void fire(ServerPlayer player, HomelanderState state, boolean allowed) {
      if (!allowed) {
         HomelanderFeedback.refuse(player);
         return;
      }

      state.roarCooldown = HomelanderRules.ROAR_COOLDOWN;
      state.roarAnim = ANIM_TICKS;
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), ViltrumiteCore.HOMELANDER_ROAR.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
      player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), ViltrumiteCore.HOMELANDER_ROAR_DEEP.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
      HeroFx.shockwave(player, eye.add(look.scale(2.0)), 0.7F, null, (float)HomelanderRules.ROAR_RANGE * 0.5F);
      double range = HomelanderRules.ROAR_RANGE;
      for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
         e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && !e.isSpectator())) {
         Vec3 center = target.getBoundingBox().getCenter();
         Vec3 toTarget = center.subtract(eye);
         if (!inCone(look, toTarget)) {
            continue;
         }

         double falloff = 1.0 - Math.min(1.0, toTarget.length() / range);
         Vec3 push = new Vec3(toTarget.x, 0.0, toTarget.z);
         push = push.lengthSqr() < 1.0E-6 ? new Vec3(look.x, 0.0, look.z) : push;
         push = push.normalize().scale(0.8 + 1.6 * falloff);
         target.push(push.x, 0.25 + 0.25 * falloff, push.z);
         target.hurtMarked = true;
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HomelanderRules.ROAR_SLOW_TICKS, 1), player);
      }
   }
}
