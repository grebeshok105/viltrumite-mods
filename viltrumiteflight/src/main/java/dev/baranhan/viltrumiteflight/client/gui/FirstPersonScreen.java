package dev.baranhan.viltrumiteflight.client.gui;

import dev.baranhan.viltrumiteflight.client.PoseDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FirstPersonScreen extends Screen {
   public FirstPersonScreen() {
      super(Component.literal("First Person Tool"));
   }

   protected void init() {
      int sliderWidth = 200;
      int sliderHeight = 20;
      int startX = (this.width - sliderWidth) / 2;
      int startY = this.height / 4;
      int gap = 24;
      this.addRenderableWidget(
         new PoseSlider(
            startX,
            startY,
            sliderWidth,
            sliderHeight,
            "Translate X: ",
            -10.0F,
            10.0F,
            PoseDataManager.FP.translateX,
            val -> PoseDataManager.FP.translateX = val
         )
      );
      this.addRenderableWidget(
         new PoseSlider(
            startX,
            startY + gap,
            sliderWidth,
            sliderHeight,
            "Translate Y: ",
            -10.0F,
            10.0F,
            PoseDataManager.FP.translateY,
            val -> PoseDataManager.FP.translateY = val
         )
      );
      this.addRenderableWidget(
         new PoseSlider(
            startX,
            startY + gap * 2,
            sliderWidth,
            sliderHeight,
            "Translate Z: ",
            -10.0F,
            10.0F,
            PoseDataManager.FP.translateZ,
            val -> PoseDataManager.FP.translateZ = val
         )
      );
      this.addRenderableWidget(
         new PoseSlider(
            startX,
            startY + gap * 4,
            sliderWidth,
            sliderHeight,
            "Rotate X: ",
            -180.0F,
            180.0F,
            PoseDataManager.FP.rotateX,
            val -> PoseDataManager.FP.rotateX = val
         )
      );
      this.addRenderableWidget(
         new PoseSlider(
            startX,
            startY + gap * 5,
            sliderWidth,
            sliderHeight,
            "Rotate Y: ",
            -180.0F,
            180.0F,
            PoseDataManager.FP.rotateY,
            val -> PoseDataManager.FP.rotateY = val
         )
      );
      this.addRenderableWidget(
         new PoseSlider(
            startX,
            startY + gap * 6,
            sliderWidth,
            sliderHeight,
            "Rotate Z: ",
            -180.0F,
            180.0F,
            PoseDataManager.FP.rotateZ,
            val -> PoseDataManager.FP.rotateZ = val
         )
      );
      this.addRenderableWidget(Button.builder(Component.literal("RESET"), button -> {
         PoseDataManager.FP.reset();
         this.minecraft.setScreen(new FirstPersonScreen());
      }).bounds(this.width - 70, 10, 60, 20).build());
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      this.renderBackground(guiGraphics);
      super.render(guiGraphics, mouseX, mouseY, delta);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
