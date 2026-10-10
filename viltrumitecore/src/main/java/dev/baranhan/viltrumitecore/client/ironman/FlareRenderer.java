package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.baranhan.viltrumitecore.client.render.vfx.PixelVfx;
import dev.baranhan.viltrumitecore.entity.FlareEntity;
import dev.baranhan.viltrumitecore.hero.ironman.IronManRules;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Countermeasure flare (spec §11.3): a bright white-orange pixel point with a
 * warm halo that flickers and fades over its 60 t life. Drawn in the additive
 * pass of IronManCombatVfx; smoke particles come from its client tick.
 */
public final class FlareRenderer {
   private FlareRenderer() {
   }

   /** Brightness 0..1 over the life: quick flash, then a flickering burn that fades. */
   public static float brightness(int life, float partialTick) {
      float t = (life + partialTick) / IronManRules.FLARE_LIFE;
      float flash = life < 3 ? 1.4F : 1.0F;
      return Math.max(0.0F, Math.min(1.4F, flash * (1.0F - t * t)));
   }

   public static void draw(BufferBuilder buffer, Vec3 cameraPos, Camera camera, FlareEntity flare, float partialTick) {
      Vec3 pos = flare.getPosition(partialTick);
      float b = brightness(flare.life(), partialTick);
      if (b <= 0.01F) {
         return;
      }

      float flicker = 0.8F + 0.2F * Mth.sin((flare.tickCount + partialTick) * 2.7F + flare.getId());
      int core = Math.min(255, (int)(255 * b * flicker));
      PixelVfx.crossGlow(buffer, cameraPos, camera, pos, 0.5F * b, 255, 150, 60, (int)(120 * b));
      PixelVfx.crossGlow(buffer, cameraPos, camera, pos, 0.18F * b, 255, 240, 210, core);
   }
}
