package dev.baranhan.viltrumitecore.hero.ironman;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Server-played Iron Man one-shots (nano wave, landings, hits): everyone
 * nearby hears them, the acting player included. Thruster loops are client
 * side (ThrusterSound). All sounds are own synthesis (tools/sfx).
 */
public final class IronManSounds {
   private IronManSounds() {
   }

   public static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
      player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
   }
}
