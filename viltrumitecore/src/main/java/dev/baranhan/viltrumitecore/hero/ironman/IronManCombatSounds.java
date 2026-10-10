package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Iron Man Stage 2 sound events (own synthesis, tools/sfx/ironman_stage2.sh).
 * Registered on ViltrumiteCore.SOUND_EVENTS; {@link #init()} is called from
 * the mod constructor so the holders exist before registration.
 */
public final class IronManCombatSounds {
   public static final RegistryObject<SoundEvent> REPULSOR_SHOT = reg("ironman_repulsor_shot");
   public static final RegistryObject<SoundEvent> REPULSOR_CHARGE = reg("ironman_repulsor_charge");
   public static final RegistryObject<SoundEvent> REPULSOR_VOLLEY = reg("ironman_repulsor_volley");
   public static final RegistryObject<SoundEvent> REPULSOR_FIZZLE = reg("ironman_repulsor_fizzle");
   public static final RegistryObject<SoundEvent> UNIBEAM_CHARGE = reg("ironman_unibeam_charge");
   public static final RegistryObject<SoundEvent> UNIBEAM_LOOP = reg("ironman_unibeam_loop");
   public static final RegistryObject<SoundEvent> UNIBEAM_OVERHEAT = reg("ironman_unibeam_overheat");
   public static final RegistryObject<SoundEvent> OVERDRAFT_SPUTTER = reg("ironman_overdraft_sputter");
   public static final RegistryObject<SoundEvent> CORE_EXPLOSION = reg("ironman_core_explosion");
   public static final RegistryObject<SoundEvent> NANITE_FORM = reg("ironman_nanite_form");
   public static final RegistryObject<SoundEvent> NANITE_DISSOLVE = reg("ironman_nanite_dissolve");
   public static final RegistryObject<SoundEvent> NANITE_REPAIR = reg("ironman_nanite_repair");
   public static final RegistryObject<SoundEvent> BLADE_SLASH = reg("ironman_blade_slash");
   public static final RegistryObject<SoundEvent> HAMMER_HIT = reg("ironman_hammer_hit");
   public static final RegistryObject<SoundEvent> HAMMER_SLAM = reg("ironman_hammer_slam");
   public static final RegistryObject<SoundEvent> SHIELD_OPEN = reg("ironman_shield_open");
   public static final RegistryObject<SoundEvent> SHIELD_HIT = reg("ironman_shield_hit");
   public static final RegistryObject<SoundEvent> SHIELD_PERFECT = reg("ironman_shield_perfect");
   public static final RegistryObject<SoundEvent> MISSILE_FLAPS = reg("ironman_missile_flaps");
   public static final RegistryObject<SoundEvent> MISSILE_LAUNCH = reg("ironman_missile_launch");
   public static final RegistryObject<SoundEvent> MISSILE_EXPLODE = reg("ironman_missile_explode");
   public static final RegistryObject<SoundEvent> JARVIS_WARNING = reg("ironman_jarvis_warning");

   private IronManCombatSounds() {
   }

   private static RegistryObject<SoundEvent> reg(String name) {
      return ViltrumiteCore.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", name)));
   }

   /** Loads the holders (class init). */
   public static void init() {
   }
}
