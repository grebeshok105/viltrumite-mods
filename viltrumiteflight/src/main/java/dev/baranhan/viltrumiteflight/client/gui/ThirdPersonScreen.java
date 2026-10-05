package dev.baranhan.viltrumiteflight.client.gui;

import dev.baranhan.viltrumiteflight.client.PoseDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ThirdPersonScreen extends Screen {
   public ThirdPersonScreen() {
      super(Component.literal("Third Person Tool"));
   }

   protected void init() {
      int sliderWidth = 100;
      int sliderHeight = 16;
      int gapX = 105;
      int gapY = 20;
      int startX = 10;
      int startY = 20;
      this.buildPartControls(PoseDataManager.TP.rightArm, "R-Arm", startX, startY, sliderWidth, sliderHeight, gapY);
      this.buildPartControls(PoseDataManager.TP.leftArm, "L-Arm", startX + gapX, startY, sliderWidth, sliderHeight, gapY);
      this.buildPartControls(PoseDataManager.TP.head, "Head", startX + gapX * 2, startY, sliderWidth, sliderHeight, gapY);
      this.buildPartControls(PoseDataManager.TP.cape, "Cape", startX + gapX * 3, startY, sliderWidth, sliderHeight, gapY);
      int row2Y = startY + gapY * 7;
      this.buildPartControls(PoseDataManager.TP.rightLeg, "R-Leg", startX, row2Y, sliderWidth, sliderHeight, gapY);
      this.buildPartControls(PoseDataManager.TP.leftLeg, "L-Leg", startX + gapX, row2Y, sliderWidth, sliderHeight, gapY);
      this.buildPartControls(PoseDataManager.TP.body, "Body", startX + gapX * 2, row2Y, sliderWidth, sliderHeight, gapY);
      int masterX = startX + gapX * 4 + 20;
      PoseDataManager.PartTransform[] upperBody = new PoseDataManager.PartTransform[]{
         PoseDataManager.TP.rightArm, PoseDataManager.TP.leftArm, PoseDataManager.TP.head, PoseDataManager.TP.body, PoseDataManager.TP.cape
      };
      this.buildGroupControls("UPPER MASTER", upperBody, masterX, startY, sliderWidth, sliderHeight, gapY);
      PoseDataManager.PartTransform[] lowerBody = new PoseDataManager.PartTransform[]{PoseDataManager.TP.rightLeg, PoseDataManager.TP.leftLeg};
      this.buildGroupControls("LOWER MASTER", lowerBody, masterX, row2Y, sliderWidth, sliderHeight, gapY);
      PoseDataManager.PartTransform[] allBody = new PoseDataManager.PartTransform[]{
         PoseDataManager.TP.rightArm,
         PoseDataManager.TP.leftArm,
         PoseDataManager.TP.head,
         PoseDataManager.TP.body,
         PoseDataManager.TP.rightLeg,
         PoseDataManager.TP.leftLeg,
         PoseDataManager.TP.cape
      };
      this.buildGroupControls("FULL MASTER", allBody, masterX + gapX, startY, sliderWidth, sliderHeight, gapY);
      this.addRenderableWidget(Button.builder(Component.literal("RESET ALL"), button -> {
         PoseDataManager.TP.resetAll();
         this.minecraft.setScreen(new ThirdPersonScreen());
      }).bounds(this.width - 90, 10, 80, 20).build());
   }

   private void buildGroupControls(String label, PoseDataManager.PartTransform[] parts, int x, int y, int w, int h, int gap) {
      this.addRenderableWidget(new PoseSlider(x, y, w, h, label + " X: ", -10.0F, 10.0F, parts[0].x, val -> {
         for (PoseDataManager.PartTransform p : parts) {
            p.x = val;
         }
      }));
      this.addRenderableWidget(new PoseSlider(x, y + gap, w, h, label + " Y: ", -10.0F, 10.0F, parts[0].y, val -> {
         for (PoseDataManager.PartTransform p : parts) {
            p.y = val;
         }
      }));
      this.addRenderableWidget(new PoseSlider(x, y + gap * 2, w, h, label + " Z: ", -10.0F, 10.0F, parts[0].z, val -> {
         for (PoseDataManager.PartTransform p : parts) {
            p.z = val;
         }
      }));
   }

   private void buildPartControls(PoseDataManager.PartTransform part, String prefix, int x, int y, int w, int h, int gap) {
      this.addRenderableWidget(new PoseSlider(x, y, w, h, prefix + " P: ", -180.0F, 180.0F, part.pitch, val -> part.pitch = val));
      this.addRenderableWidget(new PoseSlider(x, y + gap, w, h, prefix + " Y: ", -180.0F, 180.0F, part.yaw, val -> part.yaw = val));
      this.addRenderableWidget(new PoseSlider(x, y + gap * 2, w, h, prefix + " R: ", -180.0F, 180.0F, part.roll, val -> part.roll = val));
      this.addRenderableWidget(new PoseSlider(x, y + gap * 3, w, h, prefix + " X: ", -10.0F, 10.0F, part.x, val -> part.x = val));
      this.addRenderableWidget(new PoseSlider(x, y + gap * 4, w, h, prefix + " Y: ", -10.0F, 10.0F, part.y, val -> part.y = val));
      this.addRenderableWidget(new PoseSlider(x, y + gap * 5, w, h, prefix + " Z: ", -10.0F, 10.0F, part.z, val -> part.z = val));
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
      super.render(guiGraphics, mouseX, mouseY, delta);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
