package dev.baranhan.viltrumitecore.hero.regulus;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.hero.HeroShockwave;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Regulus movement on top of the shared hero mechanics: the super jump
 * (HeroSuperJump, key G) gets Regulus sounds, and the landing shockwave
 * (HeroShockwave) uses Regulus numbers, the madness multiplier and spares
 * his own heart carriers.
 */
public final class RegulusMovement {
   private RegulusMovement() {
   }

   public static void tick(Player player, RegulusState state) {
      state.wasOnGround = player.onGround();
   }

   /** Regulus extras after the shared super-jump launch FX. */
   public static void onSuperJump(ServerPlayer player) {
      player.serverLevel().playSound(null, player.blockPosition(), ViltrumiteCore.REGULUS_KICK_CRACK.get(), SoundSource.PLAYERS, 2.2F, 0.7F);
      player.serverLevel().playSound(null, player.blockPosition(), ViltrumiteCore.REGULUS_JUMP_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.4F);
   }

   /** LivingFallEvent (via RegulusHero.onLanded): a hard enough landing hits everything around. */
   public static void onLanding(ServerPlayer player, RegulusState state, float fallDistance) {
      double multiplier = state.madnessTicksLeft > 0 ? RegulusRules.MADNESS_SHOCKWAVE_MULTIPLIER : 1.0;
      HeroShockwave.land(player, fallDistance, RegulusRules.LANDING, multiplier, e -> state.carriers.contains(e.getUUID()), ViltrumiteCore.REGULUS_IMPACT_HEAVY.get());
   }
}
