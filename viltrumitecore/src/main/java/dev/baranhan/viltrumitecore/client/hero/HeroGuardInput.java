package dev.baranhan.viltrumitecore.client.hero;

import dev.baranhan.viltrumitecore.hero.HeroRegistry;
import dev.baranhan.viltrumitecore.hero.MouseButton;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.HeroMouseC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Client half of the guard seam: while the local hero returns a guard action,
 * the vanilla swap-hands key (default F) is consumed before the vanilla
 * keybind pass (tick START) and its held edges go to the server as
 * {@link MouseButton#GUARD}. The server also cancels the swap event.
 */
@EventBusSubscriber(modid = "viltrumitecore", bus = Bus.FORGE, value = {Dist.CLIENT})
public final class HeroGuardInput {
   private static boolean sentDown;

   private HeroGuardInput() {
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.START) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      Player player = client.player;
      if (player == null) {
         sentDown = false;
         return;
      }

      boolean owned = HeroRegistry.get(player).guardAction(player) != null;
      if (owned) {
         while (client.options.keySwapOffhand.consumeClick()) {
         }
      }

      boolean focused = client.screen == null && client.isWindowActive();
      boolean down = owned && focused && client.options.keySwapOffhand.isDown();
      if (down != sentDown) {
         sentDown = down;
         CoreMessages.sendToServer(new HeroMouseC2SPacket(MouseButton.GUARD, down));
      }
   }
}
