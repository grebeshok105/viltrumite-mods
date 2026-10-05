package dev.baranhan.viltrumitecore.client.gui.config;

import dev.baranhan.viltrumitecore.config.ViltrumiteCameraConfig;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CameraConfigScreen extends Screen {
   private final Screen parent;
   private CameraConfigScreen.ConfigListWidget listWidget;
   private EditBox cameraOffsetXField;
   private EditBox cameraOffsetYField;
   private EditBox cameraOffsetZField;

   public CameraConfigScreen(Screen parent) {
      super(Component.translatable("gui.viltrumitecore.config.camera.title"));
      this.parent = parent;
   }

   protected void init() {
      super.init();
      this.listWidget = new CameraConfigScreen.ConfigListWidget(this.minecraft, this.width, this.height, 40, this.height - 40, 44);
      this.addRenderableWidget(this.listWidget);
      this.cameraOffsetXField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.cameraOffsetXField.setValue(String.valueOf(ViltrumiteCameraConfig.INSTANCE.cameraOffsetX));
      this.cameraOffsetXField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new CameraConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.camera.offset_x"), this.cameraOffsetXField));
      this.cameraOffsetYField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.cameraOffsetYField.setValue(String.valueOf(ViltrumiteCameraConfig.INSTANCE.cameraOffsetY));
      this.cameraOffsetYField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new CameraConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.camera.offset_y"), this.cameraOffsetYField));
      this.cameraOffsetZField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.cameraOffsetZField.setValue(String.valueOf(ViltrumiteCameraConfig.INSTANCE.cameraOffsetZ));
      this.cameraOffsetZField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new CameraConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.camera.offset_z"), this.cameraOffsetZField));
      Button customCameraBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.camera.custom",
               new Object[]{
                  Component.translatable(
                     ViltrumiteCameraConfig.INSTANCE.enableCustomCamera ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteCameraConfig.INSTANCE.enableCustomCamera = !ViltrumiteCameraConfig.INSTANCE.enableCustomCamera;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.camera.custom",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteCameraConfig.INSTANCE.enableCustomCamera
                              ? "gui.viltrumitecore.config.generic.on"
                              : "gui.viltrumitecore.config.generic.off"
                        )
                     }
                  )
               );
            }
         )
         .bounds(0, 0, 200, 20)
         .build();
      Button crosshairBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.camera.crosshair",
               new Object[]{
                  Component.translatable(
                     ViltrumiteCameraConfig.INSTANCE.enableThirdPersonCrosshair
                        ? "gui.viltrumitecore.config.generic.on"
                        : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteCameraConfig.INSTANCE.enableThirdPersonCrosshair = !ViltrumiteCameraConfig.INSTANCE.enableThirdPersonCrosshair;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.camera.crosshair",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteCameraConfig.INSTANCE.enableThirdPersonCrosshair
                              ? "gui.viltrumitecore.config.generic.on"
                              : "gui.viltrumitecore.config.generic.off"
                        )
                     }
                  )
               );
            }
         )
         .bounds(0, 0, 200, 20)
         .build();
      this.listWidget.addEntry(new CameraConfigScreen.DoubleButtonEntry(customCameraBtn, crosshairBtn));
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.config.generic.save_back"), button -> {
         try {
            ViltrumiteCameraConfig.INSTANCE.cameraOffsetX = Float.parseFloat(this.cameraOffsetXField.getValue());
            ViltrumiteCameraConfig.INSTANCE.cameraOffsetY = Float.parseFloat(this.cameraOffsetYField.getValue());
            ViltrumiteCameraConfig.INSTANCE.cameraOffsetZ = Float.parseFloat(this.cameraOffsetZField.getValue());
            ViltrumiteCameraConfig.save();
         } catch (NumberFormatException var3) {
         }

         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
   }

   private abstract class ConfigListEntry extends Entry<CameraConfigScreen.ConfigListEntry> {
   }

   private class ConfigListWidget extends ContainerObjectSelectionList<CameraConfigScreen.ConfigListEntry> {
      public ConfigListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
         super(client, width, height, top, bottom, itemHeight);
      }

      public int getRowWidth() {
         return 200;
      }

      protected int getScrollbarPosition() {
         return this.width / 2 + 115;
      }

      protected int getMaxPosition() {
         return super.getMaxPosition() + 10;
      }

      public int addEntry(CameraConfigScreen.ConfigListEntry entry) {
         return super.addEntry(entry);
      }
   }

   private class DoubleButtonEntry extends CameraConfigScreen.ConfigListEntry {
      private final Button topButton;
      private final Button bottomButton;

      public DoubleButtonEntry(Button topButton, Button bottomButton) {
         this.topButton = topButton;
         this.bottomButton = bottomButton;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.topButton.setX(left);
         this.topButton.setY(top + 4);
         this.topButton.render(guiGraphics, mouseX, mouseY, partialTick);
         this.bottomButton.setX(left);
         this.bottomButton.setY(top + 28);
         this.bottomButton.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.topButton, this.bottomButton);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(this.topButton, this.bottomButton);
      }
   }

   private class TextFieldEntry extends CameraConfigScreen.ConfigListEntry {
      private final Component label;
      private final EditBox textField;

      public TextFieldEntry(Component label, EditBox textField) {
         this.label = label;
         this.textField = textField;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         guiGraphics.drawString(CameraConfigScreen.this.font, this.label, left, top + 4, 10526880, true);
         this.textField.setX(left);
         this.textField.setY(top + 18);
         this.textField.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return Collections.singletonList(this.textField);
      }

      public List<? extends NarratableEntry> narratables() {
         return Collections.singletonList(this.textField);
      }
   }
}
