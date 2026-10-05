package dev.baranhan.viltrumitecore.client.gui.config;

import dev.baranhan.viltrumitecore.network.CoreMessages;
import dev.baranhan.viltrumitecore.network.packet.FlightConfigSyncC2SPacket;
import dev.baranhan.viltrumiteflight.config.ViltrumiteConfig;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
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

public class FlightConfigScreen extends Screen {
   private final Screen parent;
   private FlightConfigScreen.ConfigListWidget listWidget;
   private EditBox maxFlightSpeedField;
   private EditBox throttleSpeedField;

   public FlightConfigScreen(Screen parent) {
      super(Component.translatable("gui.viltrumitecore.config.flight_server.title"));
      this.parent = parent;
   }

   protected void init() {
      super.init();
      boolean isOp = this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
      this.listWidget = new FlightConfigScreen.ConfigListWidget(this.minecraft, this.width, this.height, 40, this.height - 40, 44);
      this.addRenderableWidget(this.listWidget);
      float initialMaxSpeed = 0.0F;
      float initialThrottleSpeed = 0.017F;
      if (this.minecraft.player instanceof ViltrumiteFlightPlayer flightPlayer) {
         initialMaxSpeed = flightPlayer.getMaxFlightSpeed();
         initialThrottleSpeed = flightPlayer.getThrottleSpeed();
      }

      this.maxFlightSpeedField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.maxFlightSpeedField.setValue(String.valueOf(initialMaxSpeed));
      this.maxFlightSpeedField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.maxFlightSpeedField.setEditable(isOp);
      this.listWidget
         .addEntry(new FlightConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_server.max_speed"), this.maxFlightSpeedField));
      this.throttleSpeedField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.throttleSpeedField.setValue(String.valueOf(initialThrottleSpeed));
      this.throttleSpeedField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.throttleSpeedField.setEditable(isOp);
      this.listWidget
         .addEntry(new FlightConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_server.throttle"), this.throttleSpeedField));
      Button heatBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.flight_server.heat",
               new Object[]{
                  Component.translatable(
                     ViltrumiteConfig.INSTANCE.isHeatEnabled ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteConfig.INSTANCE.isHeatEnabled = !ViltrumiteConfig.INSTANCE.isHeatEnabled;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.flight_server.heat",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteConfig.INSTANCE.isHeatEnabled ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                        )
                     }
                  )
               );
            }
         )
         .bounds(0, 0, 200, 20)
         .build();
      heatBtn.active = isOp;
      Button breakBlocksBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.flight_server.break_blocks",
               new Object[]{
                  Component.translatable(
                     ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff = !ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.flight_server.break_blocks",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                        )
                     }
                  )
               );
            }
         )
         .bounds(0, 0, 200, 20)
         .build();
      breakBlocksBtn.active = isOp;
      this.listWidget.addEntry(new FlightConfigScreen.DoubleButtonEntry(heatBtn, breakBlocksBtn));
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.config.generic.save_back"), button -> {
         if (isOp) {
            try {
               float maxSpd = Float.parseFloat(this.maxFlightSpeedField.getValue());
               float thrSpd = Float.parseFloat(this.throttleSpeedField.getValue());
               boolean heat = ViltrumiteConfig.INSTANCE.isHeatEnabled;
               boolean breakBlks = ViltrumiteConfig.INSTANCE.breakBlocksOnTakeoff;
               CoreMessages.sendToServer(new FlightConfigSyncC2SPacket(maxSpd, thrSpd, heat, breakBlks));
            } catch (NumberFormatException var7x) {
            }
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

   private abstract class ConfigListEntry extends Entry<FlightConfigScreen.ConfigListEntry> {
   }

   private class ConfigListWidget extends ContainerObjectSelectionList<FlightConfigScreen.ConfigListEntry> {
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

      public int addEntry(FlightConfigScreen.ConfigListEntry entry) {
         return super.addEntry(entry);
      }
   }

   private class DoubleButtonEntry extends FlightConfigScreen.ConfigListEntry {
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

   private class TextFieldEntry extends FlightConfigScreen.ConfigListEntry {
      private final Component label;
      private final EditBox textField;

      public TextFieldEntry(Component label, EditBox textField) {
         this.label = label;
         this.textField = textField;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         guiGraphics.drawString(FlightConfigScreen.this.font, this.label, left, top + 4, 10526880, true);
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
