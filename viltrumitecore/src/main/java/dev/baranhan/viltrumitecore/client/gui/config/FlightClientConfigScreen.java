package dev.baranhan.viltrumitecore.client.gui.config;

import dev.baranhan.viltrumiteflight.config.ViltrumiteConfigClient;
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

public class FlightClientConfigScreen extends Screen {
   private final Screen parent;
   private FlightClientConfigScreen.ConfigListWidget listWidget;
   private EditBox fovMultiplierField;
   private EditBox smoothFovField;
   private EditBox windVolumeField;
   private EditBox flightRotYField;
   private EditBox sonicBoomVolumeField;
   private EditBox maxBankAngleField;

   public FlightClientConfigScreen(Screen parent) {
      super(Component.translatable("gui.viltrumitecore.config.flight_client.title"));
      this.parent = parent;
   }

   protected void init() {
      super.init();
      this.listWidget = new FlightClientConfigScreen.ConfigListWidget(this.minecraft, this.width, this.height, 40, this.height - 40, 44);
      this.addRenderableWidget(this.listWidget);
      this.fovMultiplierField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.fovMultiplierField.setValue(String.valueOf(ViltrumiteConfigClient.INSTANCE.fovMultiplier));
      this.fovMultiplierField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(
            new FlightClientConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_client.fov_mult"), this.fovMultiplierField)
         );
      this.smoothFovField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.smoothFovField.setValue(String.valueOf(ViltrumiteConfigClient.INSTANCE.smoothFov));
      this.smoothFovField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new FlightClientConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_client.smooth_fov"), this.smoothFovField));
      this.windVolumeField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.windVolumeField.setValue(String.valueOf(ViltrumiteConfigClient.INSTANCE.windVolumeMultiplier));
      this.windVolumeField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new FlightClientConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_client.wind_vol"), this.windVolumeField));
      this.flightRotYField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.flightRotYField.setValue(String.valueOf(ViltrumiteConfigClient.INSTANCE.flightRotY));
      this.flightRotYField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new FlightClientConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_client.flight_rot"), this.flightRotYField));
      this.sonicBoomVolumeField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.sonicBoomVolumeField.setValue(String.valueOf(ViltrumiteConfigClient.INSTANCE.sonicBoomVolume));
      this.sonicBoomVolumeField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(
            new FlightClientConfigScreen.TextFieldEntry(
               Component.translatable("gui.viltrumitecore.config.flight_client.sonic_boom_vol"), this.sonicBoomVolumeField
            )
         );
      this.maxBankAngleField = new EditBox(this.font, 0, 0, 200, 20, Component.literal(""));
      this.maxBankAngleField.setValue(String.valueOf(ViltrumiteConfigClient.INSTANCE.maxBankAngle));
      this.maxBankAngleField.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
      this.listWidget
         .addEntry(new FlightClientConfigScreen.TextFieldEntry(Component.translatable("gui.viltrumitecore.config.flight_client.max_bank"), this.maxBankAngleField));
      Button fovEffectBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.flight_client.fov_effect",
               new Object[]{
                  Component.translatable(
                     ViltrumiteConfigClient.INSTANCE.enableFovEffect ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteConfigClient.INSTANCE.enableFovEffect = !ViltrumiteConfigClient.INSTANCE.enableFovEffect;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.flight_client.fov_effect",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteConfigClient.INSTANCE.enableFovEffect ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                        )
                     }
                  )
               );
            }
         )
         .bounds(0, 0, 200, 20)
         .build();
      Button windSoundBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.flight_client.wind_sound",
               new Object[]{
                  Component.translatable(
                     ViltrumiteConfigClient.INSTANCE.enableWindLoopSound ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteConfigClient.INSTANCE.enableWindLoopSound = !ViltrumiteConfigClient.INSTANCE.enableWindLoopSound;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.flight_client.wind_sound",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteConfigClient.INSTANCE.enableWindLoopSound
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
      this.listWidget.addEntry(new FlightClientConfigScreen.DoubleButtonEntry(fovEffectBtn, windSoundBtn));
      Button sonicBoomBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.flight_client.sonic_boom_sound",
               new Object[]{
                  Component.translatable(
                     ViltrumiteConfigClient.INSTANCE.enableSonicBoomSound ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteConfigClient.INSTANCE.enableSonicBoomSound = !ViltrumiteConfigClient.INSTANCE.enableSonicBoomSound;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.flight_client.sonic_boom_sound",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteConfigClient.INSTANCE.enableSonicBoomSound
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
      this.listWidget.addEntry(new FlightClientConfigScreen.SingleButtonEntry(sonicBoomBtn));
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.config.generic.save_back"), button -> {
         try {
            ViltrumiteConfigClient.INSTANCE.fovMultiplier = Float.parseFloat(this.fovMultiplierField.getValue());
            ViltrumiteConfigClient.INSTANCE.smoothFov = Float.parseFloat(this.smoothFovField.getValue());
            ViltrumiteConfigClient.INSTANCE.windVolumeMultiplier = Float.parseFloat(this.windVolumeField.getValue());
            ViltrumiteConfigClient.INSTANCE.flightRotY = Float.parseFloat(this.flightRotYField.getValue());
            ViltrumiteConfigClient.INSTANCE.sonicBoomVolume = Float.parseFloat(this.sonicBoomVolumeField.getValue());
            ViltrumiteConfigClient.INSTANCE.maxBankAngle = Float.parseFloat(this.maxBankAngleField.getValue());
            ViltrumiteConfigClient.save();
         } catch (NumberFormatException var3x) {
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

   private abstract class ConfigListEntry extends Entry<FlightClientConfigScreen.ConfigListEntry> {
   }

   private class ConfigListWidget extends ContainerObjectSelectionList<FlightClientConfigScreen.ConfigListEntry> {
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

      public int addEntry(FlightClientConfigScreen.ConfigListEntry entry) {
         return super.addEntry(entry);
      }
   }

   private class DoubleButtonEntry extends FlightClientConfigScreen.ConfigListEntry {
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

   private class SingleButtonEntry extends FlightClientConfigScreen.ConfigListEntry {
      private final Button button;

      public SingleButtonEntry(Button button) {
         this.button = button;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.button.setX(left);
         this.button.setY(top + 8);
         this.button.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return Collections.singletonList(this.button);
      }

      public List<? extends NarratableEntry> narratables() {
         return Collections.singletonList(this.button);
      }
   }

   private class TextFieldEntry extends FlightClientConfigScreen.ConfigListEntry {
      private final Component label;
      private final EditBox textField;

      public TextFieldEntry(Component label, EditBox textField) {
         this.label = label;
         this.textField = textField;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         guiGraphics.drawString(FlightClientConfigScreen.this.font, this.label, left, top + 4, 10526880, true);
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
