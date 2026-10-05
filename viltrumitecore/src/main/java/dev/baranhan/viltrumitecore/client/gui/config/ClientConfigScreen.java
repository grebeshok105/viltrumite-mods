package dev.baranhan.viltrumitecore.client.gui.config;

import dev.baranhan.viltrumitecore.config.ViltrumiteClientConfig;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClientConfigScreen extends Screen {
   private final Screen parent;
   private ClientConfigScreen.ConfigListWidget listWidget;

   public ClientConfigScreen(Screen parent) {
      super(Component.translatable("gui.viltrumitecore.config.client.title"));
      this.parent = parent;
   }

   protected void init() {
      super.init();
      this.listWidget = new ClientConfigScreen.ConfigListWidget(this.minecraft, this.width, this.height, 40, this.height - 40, 44);
      this.addRenderableWidget(this.listWidget);
      Button alwaysRenderOffhandBtn = Button.builder(
            Component.translatable(
               "gui.viltrumitecore.config.client.render_offhand",
               new Object[]{
                  Component.translatable(
                     ViltrumiteClientConfig.INSTANCE.alwaysRenderOffhand ? "gui.viltrumitecore.config.generic.on" : "gui.viltrumitecore.config.generic.off"
                  )
               }
            ),
            button -> {
               ViltrumiteClientConfig.INSTANCE.alwaysRenderOffhand = !ViltrumiteClientConfig.INSTANCE.alwaysRenderOffhand;
               button.setMessage(
                  Component.translatable(
                     "gui.viltrumitecore.config.client.render_offhand",
                     new Object[]{
                        Component.translatable(
                           ViltrumiteClientConfig.INSTANCE.alwaysRenderOffhand
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
      this.listWidget.addEntry(new ClientConfigScreen.SingleButtonEntry(alwaysRenderOffhandBtn));
      this.addRenderableWidget(Button.builder(Component.translatable("gui.viltrumitecore.config.generic.save_back"), button -> {
         ViltrumiteClientConfig.save();
         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
   }

   private abstract class ConfigListEntry extends Entry<ClientConfigScreen.ConfigListEntry> {
   }

   private class ConfigListWidget extends ContainerObjectSelectionList<ClientConfigScreen.ConfigListEntry> {
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

      public int addEntry(ClientConfigScreen.ConfigListEntry entry) {
         return super.addEntry(entry);
      }
   }

   private class SingleButtonEntry extends ClientConfigScreen.ConfigListEntry {
      private final Button button;

      public SingleButtonEntry(Button button) {
         this.button = button;
      }

      public void render(
         GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick
      ) {
         this.button.setX(left);
         this.button.setY(top + 4);
         this.button.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      public List<? extends GuiEventListener> children() {
         return Collections.singletonList(this.button);
      }

      public List<? extends NarratableEntry> narratables() {
         return Collections.singletonList(this.button);
      }
   }
}
