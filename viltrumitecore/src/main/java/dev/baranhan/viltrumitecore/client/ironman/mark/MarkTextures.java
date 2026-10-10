package dev.baranhan.viltrumitecore.client.ironman.mark;

import dev.baranhan.viltrumitecore.hero.ironman.mark.MarkId;
import net.minecraft.resources.ResourceLocation;

/** Texture paths of the mark visuals (64x64 player-skin layout unless noted). */
public final class MarkTextures {
   public static final ResourceLocation INTERIOR = new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_interior.png");
   /** Mark force field (Satsu ark_force_field, tinted repulsor cyan; additive). */
   public static final ResourceLocation ENERGY_SHIELD = new ResourceLocation("viltrumitecore", "textures/entity/ironman/marks/energy_shield.png");
   public static final ResourceLocation POD = new ResourceLocation("viltrumitecore", "textures/entity/ironman/veronica_pod.png");

   private MarkTextures() {
   }

   public static ResourceLocation skin(MarkId mark) {
      return new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_" + mark.key() + ".png");
   }

   public static ResourceLocation glow(MarkId mark) {
      return new ResourceLocation("viltrumitecore", "textures/entity/hero/ironman_" + mark.key() + "_glow.png");
   }
}
