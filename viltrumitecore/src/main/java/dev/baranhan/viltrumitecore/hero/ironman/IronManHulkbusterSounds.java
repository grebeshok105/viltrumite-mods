package dev.baranhan.viltrumitecore.hero.ironman;

import dev.baranhan.viltrumitecore.ViltrumiteCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Iron Man Stage 5 sounds: the Hulkbuster Mark 48 (own synthesis,
 * tools/sfx/ironman_stage5.sh). Registered on ViltrumiteCore.SOUND_EVENTS.
 */
public final class IronManHulkbusterSounds {
   public static final RegistryObject<SoundEvent> DROP = reg("ironman_hulkbuster_drop");
   public static final RegistryObject<SoundEvent> ASSEMBLE = reg("ironman_hulkbuster_assemble");
   public static final RegistryObject<SoundEvent> EXIT = reg("ironman_hulkbuster_exit");
   public static final RegistryObject<SoundEvent> BREAK = reg("ironman_hulkbuster_break");
   public static final RegistryObject<SoundEvent> STEP = reg("ironman_hulkbuster_step");
   public static final RegistryObject<SoundEvent> SERVO = reg("ironman_hulkbuster_servo");
   public static final RegistryObject<SoundEvent> PUNCH = reg("ironman_hulkbuster_punch");
   public static final RegistryObject<SoundEvent> JACKHAMMER = reg("ironman_hulkbuster_jackhammer");
   public static final RegistryObject<SoundEvent> GRAB = reg("ironman_hulkbuster_grab");
   public static final RegistryObject<SoundEvent> THROW = reg("ironman_hulkbuster_throw");
   public static final RegistryObject<SoundEvent> SLAM = reg("ironman_hulkbuster_slam");
   public static final RegistryObject<SoundEvent> HOP = reg("ironman_hulkbuster_hop");

   private IronManHulkbusterSounds() {
   }

   private static RegistryObject<SoundEvent> reg(String name) {
      return ViltrumiteCore.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("viltrumitecore", name)));
   }

   /** Loads the holders (class init). */
   public static void init() {
   }
}
