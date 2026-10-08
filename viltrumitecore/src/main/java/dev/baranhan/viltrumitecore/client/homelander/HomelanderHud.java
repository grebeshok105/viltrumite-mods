package dev.baranhan.viltrumitecore.client.homelander;

import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.homelander.HomelanderRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** Eye heat bar above the hotbar (spec §6.3): shown while heat > 0, blinks while locked. */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class HomelanderHud {
   private static final int WIDTH = 64;
   private static final int HEIGHT = 4;

   private HomelanderHud() {
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Post event) {
      Minecraft client = Minecraft.getInstance();
      if (client.options.hideGui || client.player == null || client.player.isSpectator()) {
         return;
      }

      HeroPublicSnapshot snapshot = HomelanderPoser.homelander(client.player);
      if (snapshot == null || snapshot.resource() <= 0 && !snapshot.resourceLocked()) {
         return;
      }

      float heat = Mth.clamp(snapshot.resource() / (HomelanderRules.HEAT_MAX * 10.0F), 0.0F, 1.0F);
      boolean locked = snapshot.resourceLocked();
      if (locked && (client.player.tickCount / 4) % 2 == 1) {
         return;
      }

      GuiGraphics graphics = event.getGuiGraphics();
      int x = graphics.guiWidth() / 2 - WIDTH / 2;
      int y = graphics.guiHeight() - 59;
      graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xC0101018);
      int fill = Math.round(WIDTH * heat);
      // Orange when cool, deep red near the limit; grey-red while locked.
      int r = 255;
      int g = locked ? 70 : (int)Mth.lerp(heat, 170.0F, 40.0F);
      int b = locked ? 60 : 30;
      int color = 0xFF000000 | r << 16 | g << 8 | b;
      graphics.fill(x, y, x + fill, y + HEIGHT, color);
      graphics.fill(x, y, x + fill, y + 1, 0x60FFFFFF);
   }
}
