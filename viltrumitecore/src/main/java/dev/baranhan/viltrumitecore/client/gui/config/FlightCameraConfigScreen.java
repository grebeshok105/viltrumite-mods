package dev.baranhan.viltrumitecore.client.gui.config;

import dev.baranhan.viltrumiteflight.config.ViltrumiteFlightCameraConfig;
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

public class FlightCameraConfigScreen extends Screen {
   private final Screen parent;
   private FlightCameraConfigScreen.ConfigListWidget listWidget;
   private EditBox maxCameraRollField;
   private EditBox cameraRollMultiplierField;
   private EditBox cameraRollRoughnessField;

   public FlightCameraConfigScreen(Screen parent) {
      super(Component.translatable("gui.viltrumitecore.config.flight_camera.title"));
      this.parent = parent;
   }

   protected void init() {
      super.init();
      this.listWidget = new FlightCameraConfigScreen.ConfigListWidget(this.minecraft, this.width, this.height, 40, this.height - 40, 44);
      this.addRenderableWidget(this.listWidget);
      this.maxCameraRollField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.maxCameraRollField.setValue(String.valueOf(ViltrumiteFlightCameraConfig.INSTANCE.maxCameraRoll));
      this.maxCameraRollField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(
            new FlightCameraConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_camera.max_roll"), this.maxCameraRollField)
         );
      this.cameraRollMultiplierField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.cameraRollMultiplierField.setValue(String.valueOf(ViltrumiteFlightCameraConfig.INSTANCE.cameraRollMultiplier));
      this.cameraRollMultiplierField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(
            new FlightCameraConfigScreen.TextFieldEntry(
               Component.translatable("gui.viltrumitecore.config.flight_camera.roll_mult"), this.cameraRollMultiplierField
            )
         );
      this.cameraRollRoughnessField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.cameraRollRoughnessField.setValue(String.valueOf(ViltrumiteFlightCameraConfig.INSTANCE.cameraRollRoughness));
      this.cameraRollRoughnessField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(
            new FlightCameraConfigScreen.TextFieldEntry(
               Component.translatable("gui.viltrumitecore.config.flight_camera.roll_roughness"), this.cameraRollRoughnessField
            )
         );
      Button cameraRollBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.flight_camera.roll_enable",
               new Object[]{
                  Component.translatable(
                     ViltrumiteFlightCameraConfig.INSTANCE.cameraRoll ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteFlightCameraConfig.INSTANCE.cameraRoll = !ViltrumiteFlightCameraConfig.INSTANCE.cameraRoll;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.flight_camera.roll_enable",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteFlightCameraConfig.INSTANCE.cameraRoll ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                        )
                     }
                  )
               );
            }
         )
         .bounds(0, 0, 200, 20)
         .build();
      this.listWidget.addEntry(new FlightCameraConfigScreen.SingleButtonEntry(cameraRollBtn));
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.config.generic.save_back"), button -> {
         try {
            ViltrumiteFlightCameraConfig.INSTANCE.maxCameraRoll = Float.parseFloat(this.maxCameraRollField.getValue());
            ViltrumiteFlightCameraConfig.INSTANCE.cameraRollMultiplier = Float.parseFloat(this.cameraRollMultiplierField.getValue());
            ViltrumiteFlightCameraConfig.INSTANCE.cameraRollRoughness = Float.parseFloat(this.cameraRollRoughnessField.getValue());
            ViltrumiteFlightCameraConfig.save();
         } catch (NumberFormatException var3) {
         }

         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.fill(0, 0, this.width, this.height, -805306368);
      guiGraphics.fillGradient(0, 0, this.width, 60, -16777216, 0);
      guiGraphics.fillGradient(0, this.height - 60, this.width, this.height, 0, -16777216);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   private abstract class ConfigListEntry extends Entry<FlightCameraConfigScreen.ConfigListEntry> {
   }

   private class ConfigListWidget extends ContainerObjectSelectionList<FlightCameraConfigScreen.ConfigListEntry> {
      public ConfigListWidget(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
         super(minecraft, width, height, y0, y1, itemHeight);
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

      public int addEntry(FlightCameraConfigScreen.ConfigListEntry entry) {
         return super.addEntry(entry);
      }
   }

   private class DoubleButtonEntry extends FlightCameraConfigScreen.ConfigListEntry {
      private final Button topButton;
      private final Button bottomButton;

      public DoubleButtonEntry(Button topButton, Button bottomButton) {
         this.topButton = topButton;
         this.bottomButton = bottomButton;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.topButton.setX(x);
         this.topButton.setY(y + 4);
         this.topButton.render(guiGraphics, mouseX, mouseY, partialTick);
         this.bottomButton.setX(x);
         this.bottomButton.setY(y + 28);
         this.bottomButton.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return List.of(this.topButton, this.bottomButton);
      }

      public List<? extends NarratableEntry> narratables() {
         return List.of(this.topButton, this.bottomButton);
      }
   }

   private class SingleButtonEntry extends FlightCameraConfigScreen.ConfigListEntry {
      private final Button button;

      public SingleButtonEntry(Button button) {
         this.button = button;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.button.setX(x);
         this.button.setY(y + 8);
         this.button.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return Collections.singletonList(this.button);
      }

      public List<? extends NarratableEntry> narratables() {
         return Collections.singletonList(this.button);
      }
   }

   private class TextFieldEntry extends FlightCameraConfigScreen.ConfigListEntry {
      private final Component label;
      private final EditBox textField;

      public TextFieldEntry(Component label, EditBox textField) {
         this.label = label;
         this.textField = textField;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         guiGraphics.drawString(FlightCameraConfigScreen.this.minecraft.font, this.label, x, y + 4, 10526880, false);
         this.textField.setX(x);
         this.textField.setY(y + 18);
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
