package dev.baranhan.viltrumitecore.client.ironman;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.baranhan.viltrumitecore.hero.HeroAction;
import dev.baranhan.viltrumitecore.hero.HeroPublicSnapshot;
import dev.baranhan.viltrumitecore.hero.ironman.combat.RightTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Crosshair per RMB tool while the suit is worn (spec §15.2, centre; Stage 3
 * moves it into the helmet HUD). Replaces the vanilla crosshair in first
 * person; a repulsor charge draws a growing ring around it.
 */
@EventBusSubscriber(
   modid = "viltrumitecore",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class IronManCrosshair {
   private static final int SIZE = 32;
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[RightTool.values().length];

   static {
      for (RightTool tool : RightTool.values()) {
         TEXTURES[tool.ordinal()] = new ResourceLocation("viltrumitecore", "textures/gui/ironman/crosshair/" + textureName(tool) + ".png");
      }
   }

   private IronManCrosshair() {
   }

   /** Texture name per tool; SIGNATURE uses the Stage 4 mark visuals (laser until then). */
   public static String textureName(RightTool tool) {
      return switch (tool) {
         case REPULSOR -> "repulsor";
         case NANO_BLADE -> "blade";
         case NANO_HAMMER -> "hammer";
         case SIGNATURE -> "laser";
         case JACKHAMMER -> "jackhammer";
         case HULK_REPULSOR -> "hulk_repulsor";
      };
   }

   @SubscribeEvent
   public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
      if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player == null || client.options.hideGui || !client.options.getCameraType().isFirstPerson() || player.isSpectator()) {
         return;
      }

      HeroPublicSnapshot snapshot = IronManView.of(player);
      if (snapshot == null || !IronManView.worn(snapshot)) {
         return;
      }

      event.setCanceled(true);
      GuiGraphics graphics = event.getGuiGraphics();
      int cx = graphics.guiWidth() / 2;
      int cy = graphics.guiHeight() / 2;
      RightTool tool = IronManView.tool(snapshot);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      float alpha = snapshot.resourceLocked() ? 0.45F : 1.0F;
      graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
      graphics.blit(TEXTURES[tool.ordinal()], cx - SIZE / 2, cy - SIZE / 2, 0, 0, SIZE, SIZE, SIZE, SIZE);
      graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      float charge = IronManView.channel(snapshot, HeroAction.SECONDARY_USE) ? IronManView.progress(snapshot, HeroAction.SECONDARY_USE, client.getFrameTime()) : 0.0F;
      if (charge > 0.0F) {
         chargeRing(graphics, cx, cy, charge);
      }

      RenderSystem.disableBlend();
   }

   /** Pixel ring of 24 dots that fills clockwise with the charge. */
   private static void chargeRing(GuiGraphics graphics, int cx, int cy, float charge) {
      int dots = 24;
      int lit = Math.round(dots * charge);
      for (int i = 0; i < dots; i++) {
         double a = -Math.PI / 2.0 + i * 2.0 * Math.PI / dots;
         int x = cx + (int)Math.round(Math.cos(a) * 12.0);
         int y = cy + (int)Math.round(Math.sin(a) * 12.0);
         int color = i < lit ? (charge >= 1.0F ? 0xFFFFFFFF : 0xFF96F0FF) : 0x50305060;
         graphics.fill(x, y, x + 1, y + 1, color);
      }
   }
}
