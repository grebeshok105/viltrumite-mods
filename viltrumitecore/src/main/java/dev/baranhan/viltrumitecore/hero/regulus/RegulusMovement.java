package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.hero.HeroDestruction;
import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.control.ControlKind;
import dev.baranhan.viltrumitecore.hero.control.ControlManager;
import dev.baranhan.viltrumitecore.network.packet.RegulusFxS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Regulus movement: instant super jump (G) and the landing shockwave.
 *
 * <p>The jump is client-authoritative like all player movement: the client
 * applies the launch velocity itself on the key press (see RegulusJumpClient);
 * the server only plays the launch sound/FX.
 *
 * <p>The landing shockwave hangs off LivingFallEvent: that is the one place
 * the real fall distance is still known (vanilla resets fallDistance on the
 * same tick the player touches the ground, before the hero tick runs).
 */
public final class RegulusMovement {
   private RegulusMovement() {
   }

   public static void tick(Player player, RegulusState state) {
      state.wasOnGround = player.onGround();
   }

   /** Super-jump key pressed: the client already launched; play the launch for everyone. */
   public static void onSuperJump(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      float power = 1.0F;
      Vec3 feet = player.position();
      BlockState ground = groundUnder(level, player);
      level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), feet.x, feet.y + 0.1, feet.z, 30 + (int)(50 * power), 0.6, 0.05, 0.6, 0.3);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, feet.x, feet.y + 0.1, feet.z, 4 + (int)(8 * power), 0.6, 0.05, 0.6, 0.03);
      level.sendParticles(ParticleTypes.POOF, feet.x, feet.y + 0.1, feet.z, 12, 0.5, 0.05, 0.5, 0.12);
      level.playSound(null, player.blockPosition(), ViltrumiteCore.REGULUS_KICK_CRACK.get(), SoundSource.PLAYERS, 1.2F + power, 0.7F);
      level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.8F, 0.8F);
      level.playSound(null, player.blockPosition(), ViltrumiteCore.REGULUS_JUMP_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.4F);
      RegulusFx.send(player, RegulusFxS2CPacket.JUMP_LAUNCH, feet, power, ground, null);
   }

   /** LivingFallEvent hook (server): a hard enough landing hits everything around. */
   public static void onLanding(ServerPlayer player, RegulusState state, float fallDistance) {
      if (fallDistance < RegulusRules.SHOCKWAVE_MIN_FALL) {
         return;
      }

      ServerLevel level = player.serverLevel();
      boolean madness = state.madnessTicksLeft > 0;
      float power = RegulusRules.shockwavePower(fallDistance);
      double radius = RegulusRules.shockwaveRadius(fallDistance, madness);
      float damage = RegulusRules.shockwaveDamage(fallDistance, madness);
      Vec3 feet = player.position();
      BlockState ground = groundUnder(level, player);

      AABB area = player.getBoundingBox().inflate(radius, 2.0, radius);
      ControlManager manager = ControlManager.get(level);
      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
         if (entity == player || !entity.isAlive() || entity.distanceToSqr(player) > radius * radius) {
            continue;
         }

         RegulusState ownState = state;
         if (ownState.carriers.contains(entity.getUUID())) {
            continue;
         }

         double falloff = 1.0 - Math.sqrt(entity.distanceToSqr(player)) / (radius + 0.5);
         entity.hurt(player.damageSources().playerAttack(player), damage * (float)(0.5 + 0.5 * falloff));
         // The shove honors the shared impulse policy: a Lion-active hero is
         // never shoved (spec 6.2) and an anchored victim stays pinned.
         if (HeroRegistry.allowsExternalControl(entity, ControlKind.IMPULSE) && !manager.isAnchored(entity)) {
            Vec3 away = entity.position().subtract(feet);
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            Vec3 push = (flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : flat.normalize()).scale(0.9 + 1.4 * falloff * (0.6 + power));
            entity.setDeltaMovement(push.x, 0.45 + 0.5 * falloff, push.z);
            entity.hasImpulse = true;
            if (entity instanceof ServerPlayer target) {
               target.connection.send(new ClientboundSetEntityMotionPacket(target));
            }
         }
      }

      crack(level, feet, power, madness);
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
      int ring = 20 + (int)(28 * power);
      for (int i = 0; i < ring; i++) {
         double az = Math.PI * 2.0 * i / ring;
         double r = 1.0 + level.random.nextDouble() * radius * 0.4;
         double x = feet.x + Math.cos(az) * r;
         double z = feet.z + Math.sin(az) * r;
         // count=0: the offset is a velocity — dust races outward along the ground.
         level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, feet.y + 0.2, z, 0, Math.cos(az), 0.02, Math.sin(az), 0.18 + 0.12 * power);
         level.sendParticles(debris, x, feet.y + 0.2, z, 0, Math.cos(az), 0.6, Math.sin(az), 0.5);
      }
      level.sendParticles(debris, feet.x, feet.y + 0.2, feet.z, 80 + (int)(80 * power), radius * 0.3, 0.2, radius * 0.3, 0.4);
      level.sendParticles(ParticleTypes.POOF, feet.x, feet.y + 0.3, feet.z, 30, radius * 0.35, 0.1, radius * 0.35, 0.08);
      level.sendParticles(ParticleTypes.EXPLOSION, feet.x, feet.y + 0.5, feet.z, 3 + (int)(4 * power), radius * 0.25, 0.2, radius * 0.25, 0.0);
      if (power > 0.5F) {
         level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, feet.x, feet.y + 0.5, feet.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      level.playSound(null, player.blockPosition(), ViltrumiteCore.REGULUS_IMPACT_HEAVY.get(), SoundSource.PLAYERS, 2.0F + 2.0F * power, 0.85F - 0.2F * power);
      level.playSound(null, player.blockPosition(), ground.getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.5F, 0.5F);
      level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F + power, 0.6F);
      RegulusFx.send(player, RegulusFxS2CPacket.SHOCKWAVE, feet, Math.max(0.15F, power) * (madness ? 1.3F : 1.0F), ground, new float[]{(float)radius, 0.0F, 0.0F});
   }

   /** Big landings crack the ground: a shallow scatter of broken surface blocks. */
   private static void crack(ServerLevel level, Vec3 feet, float power, boolean madness) {
      if (power < 0.35F && !madness) {
         return;
      }

      int radius = 1 + (int)(2.5F * power) + (madness ? 1 : 0);
      BlockPos center = BlockPos.containing(feet.x, feet.y - 0.5, feet.z);
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz > radius * radius || level.random.nextFloat() > 0.55F) {
               continue;
            }

            BlockPos pos = center.offset(dx, 0, dz);
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
               HeroDestruction.destroyBlock(level, pos);
            }
         }
      }
   }

   private static BlockState groundUnder(ServerLevel level, Player player) {
      BlockState state = level.getBlockState(player.getOnPos());
      return state.isAir() || !state.getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : state;
   }
}
