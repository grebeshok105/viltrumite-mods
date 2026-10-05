package dev.baranhan.viltrumitecore.client.gui;

import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.RaceChoiceC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RaceSelectionScreen extends Screen {
   public RaceSelectionScreen() {
      super(Component.translatable("gui.viltrumitecore.race_selection.title"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.race_selection.become_viltrumite"), button -> {
         this.sendChoiceToServer(true);
         this.onClose();
      }).bounds(centerX - 105, centerY - 20, 100, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.race_selection.remain_human"), button -> {
         this.sendChoiceToServer(false);
         this.onClose();
      }).bounds(centerX + 5, centerY - 20, 100, 20).build());
   }

   private void sendChoiceToServer(boolean choseViltrumite) {
      CoreMessages.sendToServer(new RaceChoiceC2SPacket(choseViltrumite));
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawCenteredString(
         this.font, Component.translatable("gui.viltrumitecore.race_selection.question"), this.width / 2, this.height / 2 - 60, 16777215
      );
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }
}
