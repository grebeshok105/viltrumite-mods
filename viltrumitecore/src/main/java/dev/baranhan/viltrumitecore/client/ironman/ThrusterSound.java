package dev.baranhan.viltrumitecore.client.ironman;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Repulsor thruster loop per flying suit (client, every Iron Man in view):
 * volume and pitch follow the throttle, a second sonic layer fades in from
 * throttle 0.8 / SONIC. Each instance stops itself as soon as the thruster
 * mode is NONE (glide, wave, landing, death, relog), so no loop can stick.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class ThrusterSound extends AbstractTickableSoundInstance {
   private static final Map<UUID, ThrusterSound[]> ACTIVE = new HashMap<>();
   private final AbstractClientPlayer player;
   private final boolean sonicLayer;

   private ThrusterSound(AbstractClientPlayer player, SoundEvent sound, boolean sonicLayer) {
      super(sound, SoundSource.PLAYERS, RandomSource.create());
      this.player = player;
      this.sonicLayer = sonicLayer;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.001F;
      this.pitch = 1.0F;
      this.x = player.getX();
      this.y = player.getY();
      this.z = player.getZ();
   }

   /** Target volume of the main loop / sonic layer for a mode and throttle. Pure for tests. */
   public static float targetVolume(ThrusterFlames.Mode mode, float throttle, boolean sonicLayer) {
      float t = Mth.clamp(throttle, 0.0F, 1.0F);
      if (sonicLayer) {
         return mode == ThrusterFlames.Mode.SONIC ? 1.0F : mode == ThrusterFlames.Mode.CRUISE ? Mth.clamp((t - 0.8F) / 0.2F, 0.0F, 1.0F) * 0.8F : 0.0F;
      }

      return switch (mode) {
         case HOVER -> 0.45F;
         case CRUISE -> 0.55F + 0.35F * t;
         case SONIC -> 0.9F;
         case NONE -> 0.0F;
      };
   }

   @Override
   public void tick() {
      HeroPublicSnapshot snapshot = IronManView.of(this.player);
      ThrusterFlames.Mode mode = snapshot == null ? ThrusterFlames.Mode.NONE : ThrusterFlames.mode(this.player, snapshot);
      if (this.player.isRemoved() || !this.player.isAlive() || mode == ThrusterFlames.Mode.NONE || ThrusterFlames.silent(snapshot)) {
         this.stop();
         return;
      }

      this.x = this.player.getX();
      this.y = this.player.getY() + 0.5;
      this.z = this.player.getZ();
      float throttle = ThrusterFlames.throttle(this.player, 1.0F);
      float target = targetVolume(mode, throttle, this.sonicLayer);
      float pitch = this.sonicLayer ? 0.9F + 0.3F * throttle : 0.75F + 0.5F * throttle + (mode == ThrusterFlames.Mode.HOVER ? 0.05F : 0.0F);
      this.volume = Mth.lerp(target < this.volume ? 0.2F : 0.12F, this.volume, Math.max(0.001F, target));
      this.pitch = Mth.lerp(0.15F, this.pitch, pitch);
   }

   @Override
   public boolean canStartSilent() {
      return true;
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      if (level == null) {
         ACTIVE.clear();
         return;
      }

      ACTIVE.values().removeIf(sounds -> sounds[0].isStopped() && sounds[1].isStopped());
      for (Player player : level.players()) {
         if (!(player instanceof AbstractClientPlayer clientPlayer) || ACTIVE.containsKey(player.getUUID())) {
            continue;
         }

         HeroPublicSnapshot snapshot = IronManView.of(player);
         if (snapshot != null && !ThrusterFlames.silent(snapshot) && ThrusterFlames.mode(clientPlayer, snapshot) != ThrusterFlames.Mode.NONE) {
            ThrusterSound loop = new ThrusterSound(clientPlayer, ViltrumiteCore.IRONMAN_THRUSTER_LOOP.get(), false);
            ThrusterSound sonic = new ThrusterSound(clientPlayer, ViltrumiteCore.IRONMAN_THRUSTER_SONIC.get(), true);
            client.getSoundManager().play(loop);
            client.getSoundManager().play(sonic);
            ACTIVE.put(player.getUUID(), new ThrusterSound[]{loop, sonic});
         }
      }
   }
}
