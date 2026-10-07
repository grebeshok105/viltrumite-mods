package dev.baranhan.viltrumitecore.client.regulus;

import dev.baranhan.viltrumitecore.client.AbilityInputManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * The Regulus heart key (default MMB) always beats vanilla pick-block: when
 * both share a button, Forge fires the pick-block interaction hook first and
 * we cancel it for a Regulus — no creative pick, no survival hotbar swap.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class RegulusInputPriority {
   private RegulusInputPriority() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
      if (!event.isPickBlock()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (AbilityInputManager.heartKeyOwns(client, event.getKeyMapping())) {
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }
}
