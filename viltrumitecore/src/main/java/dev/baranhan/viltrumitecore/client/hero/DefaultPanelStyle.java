package dev.baranhan.viltrumitecore.client.hero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The original panel drawing, moved as is from ViltrumiteInGameHudMixin. */
final class DefaultPanelStyle implements PanelStyle {
   private static final ResourceLocation HOTBAR_TEXTURE = new ResourceLocation("viltrumitecore", "textures/gui/ability_hotbar.png");

   @Override
   public void drawFrames(GuiGraphics graphics, Bounds flightBar, Bounds abilityBar) {
      graphics.blit(HOTBAR_TEXTURE, flightBar.x(), flightBar.y(), 0.0F, 22.0F, flightBar.width(), flightBar.height(), 128, 186);
      graphics.blit(HOTBAR_TEXTURE, abilityBar.x(), abilityBar.y(), 0.0F, 44.0F, abilityBar.width(), abilityBar.height(), 128, 186);
   }

   @Override
   public void drawSlot(GuiGraphics graphics, int x, int y, boolean empty) {
   }

   @Override
   public void drawCooldown(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 16, y + 16, -1875692749);
   }

   @Override
   public int accentColor() {
      return 16769280;
   }
}
