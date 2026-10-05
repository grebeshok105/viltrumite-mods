package dev.baranhan.viltrumitecore.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({GameRenderer.class})
public interface GameRendererAccessor {
   @Accessor("fov")
   float getFovMultiplier();

   @Accessor("oldFov")
   float getLastFovMultiplier();
}
