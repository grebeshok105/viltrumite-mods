package dev.baranhan.viltrumitecore.client.render.vfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** Full-screen white flash (Unibeam into the eyes, own core explosion). Fades out linearly. */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class FlashOverlay {
   private static float strength;
   private static int ticks;
   private static int left;

   private FlashOverlay() {
   }

   /** Starts (or strengthens) a flash; a weaker flash never cuts a stronger one short. */
   public static void flash(float newStrength, int newTicks) {
      float s = Math.max(0.0F, Math.min(1.0F, newStrength));
      if (newTicks <= 0 || s <= 0.0F) {
         return;
      }

      if (s * newTicks >= strength * left) {
         strength = s;
         ticks = newTicks;
         left = newTicks;
      }
   }

   /** Pure alpha 0..1 for the remaining time. */
   public static float alpha(float strength, int ticks, int left, float partialTick) {
      if (ticks <= 0 || left <= 0) {
         return 0.0F;
      }

      float t = Math.max(0.0F, (left - partialTick) / ticks);
      return Math.max(0.0F, Math.min(1.0F, strength * t));
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END && left > 0 && !Minecraft.getInstance().isPaused()) {
         left--;
      }
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Post event) {
      float a = alpha(strength, ticks, left, event.getPartialTick());
      if (a <= 0.0F) {
         return;
      }

      GuiGraphics graphics = event.getGuiGraphics();
      int argb = ((int)(a * 255.0F) << 24) | 0xFFFFFF;
      graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), argb);
   }
}
