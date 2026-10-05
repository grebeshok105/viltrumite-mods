package dev.baranhan.viltrumitecore.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ViltrumiteDashboardScreen extends Screen {
   private static final ResourceLocation TEXTURE = new ResourceLocation("viltrumitecore", "textures/gui/dashboard.png");
   private int guiLeft;
   private int guiTop;
   private final int guiWidth = 197;
   private final int guiHeight = 149;
   private Button abilityButton;
   private Button configButton;

   public ViltrumiteDashboardScreen() {
      super(Component.translatable("gui.viltrumitecore.dashboard.title"));
   }

   protected void init() {
      super.init();
      this.guiLeft = (this.width - 197) / 2;
      this.guiTop = (this.height - 149) / 2;
      this.abilityButton = new ViltrumiteDashboardScreen.ViltrumiteIconButton(
         this.guiLeft + 5,
         this.guiTop + 29,
         224,
         240,
         Component.translatable("gui.viltrumitecore.dashboard.abilities"),
         button -> this.minecraft.setScreen(new ViltrumiteAbilityScreen()),
         Tooltip.create(Component.translatable("gui.viltrumitecore.dashboard.abilities.tooltip"))
      );
      this.addRenderableWidget(this.abilityButton);
      this.configButton = new ViltrumiteDashboardScreen.ViltrumiteIconButton(
         this.guiLeft + 176,
         this.guiTop + 29,
         240,
         240,
         Component.translatable("gui.viltrumitecore.dashboard.configuration"),
         button -> this.minecraft.setScreen(new ViltrumiteConfigScreen(this)),
         Tooltip.create(Component.translatable("gui.viltrumitecore.dashboard.configuration.tooltip"))
      );
      this.addRenderableWidget(this.configButton);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.fill(0, 0, this.width, this.height, -1073741824);
      guiGraphics.blit(TEXTURE, this.guiLeft, this.guiTop, 0, 0, 197, 149);
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.viltrumitecore.dashboard.soldier"), this.guiLeft + 99, this.guiTop + 5, -22016);
      if (this.minecraft.player != null) {
         guiGraphics.drawCenteredString(this.font, this.minecraft.player.getName().getString(), this.guiLeft + 99, this.guiTop + 16, 16777215);
      }

      if (this.minecraft.player != null) {
         int boxX = this.guiLeft + 52;
         int boxY = this.guiTop + 39;
         int boxWidth = 92;
         int boxHeight = 100;
         guiGraphics.fillGradient(boxX, boxY, boxX + boxWidth, boxY + boxHeight, -1728053248, -587202560);
         guiGraphics.renderOutline(boxX - 1, boxY - 1, boxWidth + 2, boxHeight + 2, -22016);
         int entityX = boxX + boxWidth / 2;
         int entityY = boxY + boxHeight - 6;
         int size = 45;
         float mouseXOffset = (float)entityX - (float)mouseX;
         float mouseYOffset = (float)((double)entityY - (double)size * 1.5) - (float)mouseY;
         InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, entityX, entityY, size, mouseXOffset, mouseYOffset, this.minecraft.player);
      }

      super.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class ViltrumiteIconButton extends Button {
      private final int u;
      private final int v;

      public ViltrumiteIconButton(int x, int y, int u, int v, Component message, OnPress onPress, Tooltip tooltip) {
         super(x, y, 16, 16, message, onPress, Button.DEFAULT_NARRATION);
         this.u = u;
         this.v = v;
         this.setTooltip(tooltip);
      }

      public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         float scale = this.isHovered ? 1.12F : 1.0F;
         guiGraphics.pose().pushPose();
         int centerX = this.getX() + this.getWidth() / 2;
         int centerY = this.getY() + this.getHeight() / 2;
         guiGraphics.pose().translate((float)centerX, (float)centerY, 0.0F);
         guiGraphics.pose().scale(scale, scale, 1.0F);
         guiGraphics.pose().translate((float)(-centerX), (float)(-centerY), 0.0F);
         guiGraphics.blit(ViltrumiteDashboardScreen.TEXTURE, this.getX(), this.getY(), this.u, this.v, this.getWidth(), this.getHeight());
         guiGraphics.pose().popPose();
      }
   }
}
