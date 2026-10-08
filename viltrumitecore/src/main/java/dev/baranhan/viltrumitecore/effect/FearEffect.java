package dev.baranhan.viltrumitecore.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Homelander's gaze (spec §6.4). A marker effect: the slowness and the mob
 * flight are applied by Focus; players get the vignette, heartbeat and title
 * on their client (FearClient).
 */
public class FearEffect extends MobEffect {
   public FearEffect() {
      super(MobEffectCategory.HARMFUL, 0x3A1A4A);
   }
}
