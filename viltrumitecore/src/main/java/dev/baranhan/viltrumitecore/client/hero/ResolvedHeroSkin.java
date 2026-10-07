package dev.baranhan.viltrumitecore.client.hero;

import net.minecraft.resources.ResourceLocation;

public record ResolvedHeroSkin(
   ResourceLocation skin,
   ResourceLocation handTexture,
   String modelName,
   boolean suppressOverride,
   boolean suppressCosmetics,
   boolean suppressCape
) {
}
