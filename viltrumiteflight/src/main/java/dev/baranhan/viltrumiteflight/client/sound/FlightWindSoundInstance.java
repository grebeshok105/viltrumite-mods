package dev.baranhan.viltrumiteflight.client.sound;

import dev.baranhan.viltrumiteflight.config.ViltrumiteConfigClient;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlightWindSoundInstance extends AbstractTickableSoundInstance {
   private final AbstractClientPlayer player;
   private final ViltrumiteFlightPlayer omniPlayer;

   public FlightWindSoundInstance(AbstractClientPlayer player, SoundEvent windSound) {
      super(windSound, SoundSource.PLAYERS, RandomSource.create());
      this.player = player;
      this.omniPlayer = (ViltrumiteFlightPlayer)player;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.001F;
      this.pitch = 1.0F;
   }

   public void tick() {
      if (!this.player.isRemoved() && this.omniPlayer.getFlightState() != FlightState.NONE) {
         this.x = this.player.getX();
         this.y = this.player.getY();
         this.z = this.player.getZ();
         float throttle = this.omniPlayer.getFlightThrottle();
         float targetVolume = 0.001F;
         float targetPitch = 1.0F;
         if (throttle > 0.2F) {
            targetVolume = (throttle - 0.2F) * 1.25F * ViltrumiteConfigClient.INSTANCE.windVolumeMultiplier;
            targetPitch = 1.1F + throttle * 0.7F;
         }

         float fadeSpeed = targetVolume < this.volume ? 0.15F : 0.1F;
         this.volume = Mth.lerp(fadeSpeed, this.volume, targetVolume);
         this.pitch = Mth.lerp(fadeSpeed, this.pitch, targetPitch);
      } else {
         this.stop();
      }
   }
}
