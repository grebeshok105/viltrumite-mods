package dev.baranhan.viltrumitecore.client.gui;

import dev.baranhan.viltrumitecore.hero.HeroId;
import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.HeroChoiceC2SPacket;
import dev.baranhan.viltrumitecore.network.packet.RaceChoiceC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class RaceSelectionScreen extends Screen {
   private static final ResourceLocation HOMELANDER_SKIN = new ResourceLocation("viltrumitecore", "textures/entity/hero/homelander.png");
   private static final ResourceLocation REGULUS_SKIN = new ResourceLocation("viltrumitecore", "textures/entity/hero/regulus.png");

   public RaceSelectionScreen() {
      super(Component.translatable("gui.viltrumitecore.race_selection.title"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.race_selection.become_homelander"), button -> {
         this.sendHeroChoice(HeroId.HOMELANDER);
         this.onClose();
      }).bounds(centerX - 155, centerY - 20, 100, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.race_selection.become_regulus"), button -> {
         this.sendHeroChoice(HeroId.REGULUS);
         this.onClose();
      }).bounds(centerX - 50, centerY - 20, 100, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.race_selection.remain_human"), button -> {
         this.sendHeroChoice(HeroId.HUMAN);
         this.onClose();
      }).bounds(centerX + 55, centerY - 20, 100, 20).build());
   }

   private void sendLegacyChoice(boolean choseViltrumite) {
      CoreMessages.sendToServer(new RaceChoiceC2SPacket(choseViltrumite));
   }

   private void sendHeroChoice(HeroId id) {
      CoreMessages.sendToServer(new HeroChoiceC2SPacket(id.key()));
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      guiGraphics.drawCenteredString(this.font, Component.translatable("gui.viltrumitecore.race_selection.question"), centerX, centerY - 60, 16777215);

      // Head previews above each hero button: base face + hat layer, drawn skin-flat like a player head.
      drawHead(guiGraphics, HOMELANDER_SKIN, centerX - 105 - 12, centerY - 48);
      drawHead(guiGraphics, REGULUS_SKIN, centerX - 12, centerY - 48);
   }

   private static void drawHead(GuiGraphics guiGraphics, ResourceLocation skin, int x, int y) {
      guiGraphics.blit(skin, x, y, 24, 24, 8.0F, 8.0F, 8, 8, 64, 64);
      guiGraphics.blit(skin, x, y, 24, 24, 40.0F, 8.0F, 8, 8, 64, 64);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }
}
