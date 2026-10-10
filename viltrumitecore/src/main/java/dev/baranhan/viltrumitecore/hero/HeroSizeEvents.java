package dev.baranhan.viltrumitecore.hero;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Body scale seam (both sides): a hero may scale its player's hitbox and eye
 * height ({@link HeroDefinition#bodyScale}). The caller refreshes dimensions
 * when the scale changes; vanilla pose checks then use the scaled box.
 */
@EventBusSubscriber(modid = "viltrumitecore")
public final class HeroSizeEvents {
   private HeroSizeEvents() {
   }

   @SubscribeEvent
   public static void onSize(EntityEvent.Size event) {
      if (!(event.getEntity() instanceof Player player) || !(player instanceof HeroPlayer)) {
         return;
      }

      float scale = HeroRegistry.get(player).bodyScale(player);
      if (scale == 1.0F || !Float.isFinite(scale) || scale <= 0.0F) {
         return;
      }

      float eye = event.getNewEyeHeight();
      event.setNewSize(event.getNewSize().scale(scale));
      event.setNewEyeHeight(eye * scale);
   }
}
