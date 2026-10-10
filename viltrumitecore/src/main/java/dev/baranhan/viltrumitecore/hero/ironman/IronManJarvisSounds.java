package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Iron Man Stage 3 sounds: helmet, scan, flares (own synthesis,
 * tools/sfx/ironman_stage3.sh) and the JARVIS voice lines (Codex-Superheroes
 * recordings, CC0, see CREDITS.md). Registered on ViltrumiteCore.SOUND_EVENTS.
 */
public final class IronManJarvisSounds {
   public static final RegistryObject<SoundEvent> HELMET_CLOSE = reg("ironman_helmet_close");
   public static final RegistryObject<SoundEvent> HELMET_OPEN = reg("ironman_helmet_open");
   public static final RegistryObject<SoundEvent> SCAN_LOOP = reg("ironman_scan_loop");
   public static final RegistryObject<SoundEvent> SCAN_COMPLETE = reg("ironman_scan_complete");
   public static final RegistryObject<SoundEvent> FLARE_LAUNCH = reg("ironman_flare_launch");
   public static final RegistryObject<SoundEvent> FLARE_BURN = reg("ironman_flare_burn");
   public static final RegistryObject<SoundEvent> JARVIS_DETECT = reg("ironman_jarvis_detect");
   public static final RegistryObject<SoundEvent> JARVIS_DETECT_EXCITED = reg("ironman_jarvis_detect_excited");
   public static final RegistryObject<SoundEvent> JARVIS_DIAGNOSTIC = reg("ironman_jarvis_diagnostic");
   public static final RegistryObject<SoundEvent> JARVIS_SUIT_PRESET = reg("ironman_jarvis_suit_preset");

   private IronManJarvisSounds() {
   }

   private static RegistryObject<SoundEvent> reg(String name) {
      return ViltrumiteCore.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", name)));
   }

   /** Loads the holders (class init). */
   public static void init() {
   }
}
