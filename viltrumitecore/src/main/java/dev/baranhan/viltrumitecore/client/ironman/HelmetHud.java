package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Inside the closed helmet (first person, spec §15.2): a thin holographic
 * frame at the screen edges (texture made by
 * tools/assets/make_ironman_stage3_assets.py), fading with the helmet fold, and
 * a scanline sweep while the mask closes or opens. Drawn under the HUD text.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class HelmetHud {
   public static final ResourceLocation FRAME = new ResourceLocation("viltrumitecore", "textures/gui/ironman/helmet_frame.png");
   private static final int TEX_W = 480;
   private static final int TEX_H = 270;

   private HelmetHud() {
   }

   /** Helmet closure seen from inside (0 = open), local player. */
   public static float closure(LocalPlayer player, HeroPublicSnapshot snapshot, float partialTick) {
      return IronManView.worn(snapshot) ? HelmetAnim.progress(player, snapshot, partialTick) : 0.0F;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onRenderGui(RenderGuiEvent.Post event) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (client.options.hideGui || player == null || player.isSpectator() || !client.options.getCameraType().isFirstPerson()) {
         return;
      }

      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null) {
         return;
      }

      float partialTick = event.getPartialTick();
      float closure = closure(player, snapshot, partialTick);
      if (closure <= 0.01F) {
         return;
      }

      GuiGraphics graphics = event.getGuiGraphics();
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      float flicker = 0.92F + 0.08F * (float)Math.sin((player.tickCount + partialTick) * 0.35F);
      graphics.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, closure) * 0.85F * flicker);
      graphics.blit(FRAME, 0, 0, width, height, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);
      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      float toggle = HelmetAnim.toggleProgress(player);
      if (toggle >= 0.0F) {
         // Scanline: sweeps down while closing, up while opening.
         boolean closing = HelmetAnim.closedFlag(snapshot);
         int y = Math.round((closing ? toggle : 1.0F - toggle) * height);
         graphics.fill(0, y - 1, width, y + 1, 0x9060E0FF);
         graphics.fill(0, y - 6, width, y - 1, 0x2060E0FF);
      }

      RenderSystem.disableBlend();
   }
}
