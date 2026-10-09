package dev.baranhan.viltrumitecore.client.ironman.jarvis;

import dev.baranhan.viltrumitecore.client.hero.ClientHeroData;
import dev.baranhan.viltrumitecore.client.ironman.HelmetAnim;
import dev.baranhan.viltrumitecore.client.ironman.IronManView;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.OwnerSection;
import dev.baranhan.viltrumitecore.hero.ironman.IronManCombatSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManFlags;
import dev.baranhan.viltrumitecore.hero.ironman.IronManJarvisSounds;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * JARVIS voice lines on the owner's client (spec §11.1), only with the helmet
 * closed. Triggers come from changes of the synced snapshot and the owner
 * sections. Recorded lines (Codex-Superheroes, CC0) play where a matching
 * recording exists; every other line is a subtitle + UI chime (no synthetic
 * voice). {@link VoiceGate} keeps lines from stacking.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class JarvisVoice {
   /** Trigger → recording (null = chime), subtitle key, line length and cooldown (ticks). */
   public enum Line {
      /** jarvis_mark85_preset.ogg ("preset loaded"), 7.0 s. */
      SUIT_UP(() -> IronManJarvisSounds.JARVIS_SUIT_PRESET.get(), "jarvis.viltrumitecore.suit_up", 141, 2400),
      /** jarvis_detect.ogg ("hero detected"), 10.4 s. */
      THREAT(() -> IronManJarvisSounds.JARVIS_DETECT.get(), "jarvis.viltrumitecore.threat", 209, 1200),
      /** jarvis_detect_excited.ogg ("critical threat"), 10.9 s. */
      THREAT_CRITICAL(() -> IronManJarvisSounds.JARVIS_DETECT_EXCITED.get(), "jarvis.viltrumitecore.threat_critical", 218, 1200),
      /** jarvis_diagnostic.ogg ("diagnostic"), 4.5 s. */
      SCAN_DONE(() -> IronManJarvisSounds.JARVIS_DIAGNOSTIC.get(), "jarvis.viltrumitecore.scan_done", 90, 200),
      LOW_ENERGY(null, "jarvis.viltrumitecore.low_energy", 40, 600),
      OVERHEAT(null, "jarvis.viltrumitecore.overheat", 40, 200),
      SECOND_OVERHEAT(null, "hud.viltrumitecore.ironman.overheat_warning", 60, 200),
      OVERDRAFT(null, "hud.viltrumitecore.ironman.overdraft", 60, 100),
      COUNTERMEASURES(null, "jarvis.viltrumitecore.countermeasures", 30, 300);

      @Nullable
      private final Supplier<SoundEvent> sound;
      private final String subtitle;
      private final int duration;
      private final int cooldown;

      Line(@Nullable Supplier<SoundEvent> sound, String subtitle, int duration, int cooldown) {
         this.sound = sound;
         this.subtitle = subtitle;
         this.duration = duration;
         this.cooldown = cooldown;
      }

      public String subtitle() {
         return this.subtitle;
      }

      public boolean recorded() {
         return this.sound != null;
      }
   }

   private static final VoiceGate GATE = new VoiceGate();
   private static final Set<Integer> KNOWN_THREATS = new HashSet<>();
   private static boolean seen;
   private static boolean wasReady;
   private static float lastEnergy;
   private static int lastOverheats;
   private static boolean lastOverdraft;
   private static int lastCounter;
   @Nullable
   private static Component subtitle;
   private static long subtitleUntil;
   private static long subtitleStart;

   private JarvisVoice() {
   }

   @Nullable
   public static Component subtitle(long now) {
      return subtitle != null && now < subtitleUntil ? subtitle : null;
   }

   /** 0..1 alpha of the current subtitle (fade in / out). */
   public static float subtitleAlpha(long now, float partialTick) {
      if (subtitle(now) == null) {
         return 0.0F;
      }

      float in = Math.min(1.0F, (now - subtitleStart + partialTick) / 4.0F);
      float out = Math.min(1.0F, (subtitleUntil - now - partialTick) / 10.0F);
      return Math.max(0.0F, Math.min(in, out));
   }

   /** JARVIS is up: suit ready and the helmet closed. */
   public static boolean online(@Nullable HeroPublicSnapshot snapshot) {
      return snapshot != null && IronManView.suitReady(snapshot) && HelmetAnim.closedFlag(snapshot);
   }

   static boolean critical(Entity entity) {
      return entity instanceof WitherBoss || entity instanceof EnderDragon || entity instanceof Warden;
   }

   /** Scan card arrived (ScanCardRenderer). */
   public static void onScanCard() {
      say(Line.SCAN_DONE);
   }

   static void say(Line line) {
      Minecraft client = Minecraft.getInstance();
      if (client.level == null || !online(IronManView.of(client.player))) {
         return;
      }

      long now = client.level.getGameTime();
      if (!GATE.tryPlay(line.name(), now, line.duration, line.cooldown)) {
         return;
      }

      SoundEvent sound = line.sound == null ? IronManCombatSounds.JARVIS_WARNING.get() : line.sound.get();
      client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, line.sound == null ? 1.25F : 1.0F));
      subtitle = Component.translatable(line.subtitle);
      subtitleStart = now;
      subtitleUntil = now + Math.max(60, line.duration);
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (event.phase != TickEvent.Phase.END || client.level == null || client.isPaused()) {
         return;
      }

      LocalPlayer player = client.player;
      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         seen = false;
         KNOWN_THREATS.clear();
         return;
      }

      boolean ready = IronManView.suitReady(snapshot);
      float energy = IronManView.energy(snapshot);
      int overheats = IronManFlags.get(snapshot.heroFlags(), IronManFlags.Field.OVERHEAT_COUNT);
      boolean overdraft = IronManFlags.is(snapshot.heroFlags(), IronManFlags.Field.OVERDRAFT);
      int counter = snapshot.extraCooldown(1);
      if (seen) {
         if (ready && !wasReady) {
            say(Line.SUIT_UP);
         }

         if (energy < IronManRules.JARVIS_LOW_ENERGY && lastEnergy >= IronManRules.JARVIS_LOW_ENERGY) {
            say(Line.LOW_ENERGY);
         }

         if (overheats > lastOverheats) {
            say(overheats >= 2 ? Line.SECOND_OVERHEAT : Line.OVERHEAT);
         }

         if (overdraft && !lastOverdraft) {
            say(Line.OVERDRAFT);
         }

         if (counter > lastCounter + 20) {
            say(Line.COUNTERMEASURES);
         }

         threats(client);
      } else {
         // First look at this player: remember the current threats without announcing them.
         KNOWN_THREATS.clear();
         for (int id : ClientHeroData.section(OwnerSection.THREATS)) {
            KNOWN_THREATS.add(id);
         }
      }

      seen = true;
      wasReady = ready;
      lastEnergy = energy;
      lastOverheats = overheats;
      lastOverdraft = overdraft;
      lastCounter = counter;
   }

   /** New player / boss threats get a line; mobs only get brackets (no spam). */
   private static void threats(Minecraft client) {
      int[] ids = ClientHeroData.section(OwnerSection.THREATS);
      Set<Integer> now = new HashSet<>();
      Line line = null;
      for (int id : ids) {
         now.add(id);
         if (!KNOWN_THREATS.contains(id)) {
            Entity entity = client.level.getEntity(id);
            if (entity != null && critical(entity)) {
               line = Line.THREAT_CRITICAL;
            } else if (entity instanceof Player && line == null) {
               line = Line.THREAT;
            }
         }
      }

      KNOWN_THREATS.clear();
      KNOWN_THREATS.addAll(now);
      if (line != null) {
         say(line);
      }
   }
}
