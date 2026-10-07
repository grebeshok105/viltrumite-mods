package dev.baranhan.viltrumitecore.hero;

import dev.baranhan.viltrumitecore.hero.fx.HeroFx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Instant super jump shared by every hero (one press of the super-jump key).
 * A hero opts in with {@link HeroDefinition#superJumpVelocity}. Player
 * movement is client-authoritative, so the client applies the velocity
 * ({@code client.hero.SuperJumpClient}) and then sends {@link HeroAction#JUMP};
 * the server only validates and plays the launch for everyone.
 */
public final class HeroSuperJump {
   /** Part of the look direction added to the launch, so the jump can travel. */
   public static final double FORWARD = 0.35;

   private HeroSuperJump() {
   }

   /** Server side of the JUMP input: shared launch FX, then the hero's own extras. */
   public static void onInput(ServerPlayer player) {
      HeroDefinition hero = HeroRegistry.get(player);
      if (hero.superJumpVelocity(player) <= 0.0F || HeroDamage.isAnchored(player) || !hero.canAct(player, HeroAction.JUMP)) {
         return;
      }

      Vec3 feet = player.position();
      BlockState ground = HeroShockwave.groundUnder(player.serverLevel(), player);
      player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), feet.x, feet.y + 0.1, feet.z, 80, 0.6, 0.05, 0.6, 0.3);
      player.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, feet.x, feet.y + 0.1, feet.z, 12, 0.6, 0.05, 0.6, 0.03);
      player.serverLevel().sendParticles(ParticleTypes.POOF, feet.x, feet.y + 0.1, feet.z, 12, 0.5, 0.05, 0.5, 0.12);
      player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.8F, 0.8F);
      HeroFx.launch(player, feet, 1.0F, ground);
      hero.onSuperJump(player);
   }

   /** Pure: peak height of a launch with vanilla player gravity (0.08) and drag (0.98). */
   public static double apex(double velocity) {
      double y = 0.0;
      double v = velocity;
      while (v > 0.0) {
         y += v;
         v = (v - 0.08) * 0.98;
      }

      return y;
   }
}
