package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Iron Man Stage 4 sounds: Veronica, flying parts, exit / enter, mark break and
 * the seven signatures (own synthesis, tools/sfx/ironman_stage4.sh).
 * Registered on ViltrumiteCore.SOUND_EVENTS; files under sounds/ironman/.
 */
public final class IronManMarkSounds {
   public static final RegistryObject<SoundEvent> VERONICA_FALL = reg("ironman_veronica_fall");
   public static final RegistryObject<SoundEvent> VERONICA_IMPACT = reg("ironman_veronica_impact");
   public static final RegistryObject<SoundEvent> VERONICA_OPEN = reg("ironman_veronica_open");
   public static final RegistryObject<SoundEvent> VERONICA_LEAVE = reg("ironman_veronica_leave");
   public static final RegistryObject<SoundEvent> PART_FLY = reg("ironman_part_fly");
   public static final RegistryObject<SoundEvent> PART_CLAMP = reg("ironman_part_clamp");
   public static final RegistryObject<SoundEvent> HELMET_LOCK = reg("ironman_helmet_lock");
   public static final RegistryObject<SoundEvent> MARK_EXIT = reg("ironman_mark_exit");
   public static final RegistryObject<SoundEvent> MARK_ENTER = reg("ironman_mark_enter");
   public static final RegistryObject<SoundEvent> MARK_BREAK = reg("ironman_mark_break");
   public static final RegistryObject<SoundEvent> MICRO_LASER = reg("ironman_micro_laser");
   public static final RegistryObject<SoundEvent> ROCKET_FIST_LAUNCH = reg("ironman_rocket_fist_launch");
   public static final RegistryObject<SoundEvent> ROCKET_FIST_HIT = reg("ironman_rocket_fist_hit");
   public static final RegistryObject<SoundEvent> CAMO_ON = reg("ironman_camo_on");
   public static final RegistryObject<SoundEvent> CAMO_OFF = reg("ironman_camo_off");
   public static final RegistryObject<SoundEvent> STARBOOST = reg("ironman_starboost");
   public static final RegistryObject<SoundEvent> PULSE_UNIBEAM = reg("ironman_pulse_unibeam");
   public static final RegistryObject<SoundEvent> SHOULDER_GUN = reg("ironman_shoulder_gun");
   public static final RegistryObject<SoundEvent> SLAM_IMPACT = reg("ironman_slam_impact");

   private IronManMarkSounds() {
   }

   private static RegistryObject<SoundEvent> reg(String name) {
      return ViltrumiteCore.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", name)));
   }

   /** Loads the holders (class init). */
   public static void init() {
   }
}
