package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Regulus movement: charged super-jump and the landing shockwave. Fall damage
 * is cancelled by the LivingFallEvent hook in HeroEvents; this class only
 * applies the launch and the wave.
 */
public final class RegulusMovement {
   private RegulusMovement() {
   }

   public static void tick(Player player, RegulusState state) {
      boolean onGround = player.onGround();

      // A Mania channel grounds the caster for its duration (spec 8.2).
      if (state.jumpHeld && state.channelTargetId == null) {
         if (onGround) {
            state.jumpCharge = Math.min(state.jumpCharge + 1, RegulusRules.JUMP_CHARGE_TICKS);
         } else {
            state.jumpCharge = 0;
         }
      } else if (state.jumpCharge > 0) {
         if (onGround) {
            float velocity = RegulusRules.jumpVelocity(state.jumpCharge);
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(delta.x, velocity, delta.z);
            player.hasImpulse = true;
            player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.6F, 1.2F);
         }

         state.jumpCharge = 0;
      }

      // Landing shockwave: a hard enough landing knocks everything nearby back.
      if (!state.wasOnGround && onGround && player.fallDistance >= RegulusRules.SHOCKWAVE_MIN_FALL) {
         shockwave(player, state);
      }

      state.wasOnGround = onGround;
   }

   private static void shockwave(Player player, RegulusState state) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      AABB area = player.getBoundingBox().inflate(RegulusRules.SHOCKWAVE_RADIUS, 1.5, RegulusRules.SHOCKWAVE_RADIUS);
      float damage = RegulusRules.shockwaveDamage(state.hearts());
      ControlManager manager = ControlManager.get(level);
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
         if (entity == player || !entity.isAlive()) {
            continue;
         }

         entity.hurt(player.damageSources().mobAttack(player), damage);
         // The shove honors the shared impulse policy: a Lion-active hero is
         // never shoved (spec 6.2) and an anchored victim stays pinned.
         if (HeroRegistry.allowsExternalControl(entity, ControlKind.IMPULSE) && !manager.isAnchored(entity)) {
            Vec3 push = entity.position().subtract(player.position()).normalize().scale(1.2);
            entity.push(push.x, 0.4, push.z);
         }
      }

      level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.8F);
   }
}
